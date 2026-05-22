package com.company.knowledge.service.impl;

import com.company.knowledge.dto.LoginResponse;
import com.company.knowledge.dto.RolePermissionRequest;
import com.company.knowledge.dto.RolePermissionResponse;
import com.company.knowledge.dto.UserRequest;
import com.company.knowledge.dto.UserResponse;
import com.company.knowledge.entity.RolePermissionConfig;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.exception.ForbiddenException;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.exception.UnauthorizedException;
import com.company.knowledge.repository.RolePermissionConfigRepository;
import com.company.knowledge.repository.UserAccountRepository;
import com.company.knowledge.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final UserAccountRepository userAccountRepository;
    private final RolePermissionConfigRepository rolePermissionConfigRepository;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserAccountRepository userAccountRepository, RolePermissionConfigRepository rolePermissionConfigRepository) {
        this.userAccountRepository = userAccountRepository;
        this.rolePermissionConfigRepository = rolePermissionConfigRepository;
    }

    @Override
    public LoginResponse login(String username, String password) {
        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ForbiddenException("用户已停用");
        }
        boolean matched = verifyPassword(password, user);
        if (!matched) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return new LoginResponse(token, toResponse(user));
    }

    private boolean verifyPassword(String raw, UserAccount user) {
        String stored = user.getPasswordHash();
        if (stored != null && stored.startsWith("$2")) {
            return ENCODER.matches(raw, stored);
        }
        // Legacy SHA-256 migration: detect old hash format
        String legacyHash = sha256(raw);
        if (legacyHash.equals(stored)) {
            log.info("用户 {} 密码从 SHA-256 迁移为 BCrypt", user.getUsername());
            user.setPasswordHash(ENCODER.encode(raw));
            userAccountRepository.save(user);
            return true;
        }
        return false;
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    @Override
    public UserAccount requireUser(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("请先登录");
        }
        Long userId = sessions.get(token);
        if (userId == null) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("登录用户不存在"));
    }

    @Override
    public void requireAnyRole(UserAccount user, RoleType... roles) {
        for (RoleType role : roles) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new ForbiddenException("当前用户无权限执行该操作");
    }

    @Override
    public List<UserResponse> listUsers() {
        return userAccountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("新增用户必须设置密码");
        }
        UserAccount user = new UserAccount();
        apply(user, request, true);
        return toResponse(userAccountRepository.save(user));
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        apply(user, request, false);
        return toResponse(userAccountRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        userAccountRepository.delete(user);
    }

    @Override
    public List<RolePermissionResponse> listRolePermissions() {
        return List.of(RoleType.values()).stream()
                .map(this::getRolePermissions)
                .toList();
    }

    @Override
    public RolePermissionResponse getRolePermissions(RoleType role) {
        return toRolePermissionResponse(ensureRolePermission(role));
    }

    @Override
    public RolePermissionResponse updateRolePermissions(RoleType role, RolePermissionRequest request) {
        RolePermissionConfig config = ensureRolePermission(role);
        config.setCanViewLibrary(request.getCanViewLibrary());
        config.setCanCreateContent(request.getCanCreateContent());
        config.setCanEditContent(request.getCanEditContent());
        config.setCanDeleteContent(request.getCanDeleteContent());
        config.setCanManageCategories(request.getCanManageCategories());
        config.setCanManageUsers(request.getCanManageUsers());
        config.setCanManageRoles(request.getCanManageRoles());
        config.setCanPreviewOffice(request.getCanPreviewOffice());
        config.setUpdatedAt(LocalDateTime.now());
        return toRolePermissionResponse(rolePermissionConfigRepository.save(config));
    }

    private void apply(UserAccount user, UserRequest request, boolean creating) {
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled() == null ? true : request.getEnabled());
        if (creating || StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(ENCODER.encode(request.getPassword()));
        }
        if (creating && user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), user.getEnabled(), user.getCreatedAt());
    }

    private RolePermissionConfig ensureRolePermission(RoleType role) {
        return rolePermissionConfigRepository.findByRole(role)
                .orElseGet(() -> rolePermissionConfigRepository.save(defaultRolePermission(role)));
    }

    private RolePermissionConfig defaultRolePermission(RoleType role) {
        RolePermissionConfig config = new RolePermissionConfig();
        config.setRole(role);
        config.setCanViewLibrary(true);
        config.setCanPreviewOffice(true);
        config.setCanCreateContent(role != RoleType.FINANCE);
        config.setCanEditContent(role == RoleType.ADMIN || role == RoleType.SALES || role == RoleType.PRESALES || role == RoleType.DELIVERY_OPS || role == RoleType.REVIEWER);
        config.setCanDeleteContent(role == RoleType.ADMIN || role == RoleType.SALES || role == RoleType.PRESALES || role == RoleType.DELIVERY_OPS || role == RoleType.REVIEWER);
        config.setCanManageCategories(role == RoleType.ADMIN);
        config.setCanManageUsers(role == RoleType.ADMIN);
        config.setCanManageRoles(role == RoleType.ADMIN);
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private RolePermissionResponse toRolePermissionResponse(RolePermissionConfig config) {
        return new RolePermissionResponse(
                config.getId(),
                config.getRole(),
                config.getCanViewLibrary(),
                config.getCanCreateContent(),
                config.getCanEditContent(),
                config.getCanDeleteContent(),
                config.getCanManageCategories(),
                config.getCanManageUsers(),
                config.getCanManageRoles(),
                config.getCanPreviewOffice(),
                config.getUpdatedAt()
        );
    }

    public static String hash(String raw) {
        return ENCODER.encode(raw);
    }
}
