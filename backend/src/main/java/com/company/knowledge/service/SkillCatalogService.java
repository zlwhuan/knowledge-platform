package com.company.knowledge.service;

import com.company.knowledge.dto.SkillDefinition;
import com.company.knowledge.dto.SkillSaveRequest;
import com.company.knowledge.entity.SkillDefinitionEntity;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.SkillDefinitionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 技能目录：MySQL 为主，classpath:skills/*.json 作初始化种子
 */
@Service
public class SkillCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(SkillCatalogService.class);

    private final SkillDefinitionRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 进程内缓存，写操作后刷新 */
    private volatile Map<String, SkillDefinition> cache = new LinkedHashMap<>();

    public SkillCatalogService(SkillDefinitionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public synchronized void init() {
        try {
            if (repository.count() == 0) {
                seedFromClasspath();
            }
            reloadCache();
            logger.info("Skills ready: {}", cache.keySet());
        } catch (Exception e) {
            logger.error("Skill catalog init failed", e);
            // 退化：仍尝试读库
            reloadCache();
        }
    }

    private void seedFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:skills/*.json");
            int order = 0;
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    SkillDefinition def = objectMapper.readValue(in, new TypeReference<>() {});
                    if (def == null || def.id() == null || def.id().isBlank()) continue;
                    if (repository.existsBySkillKey(def.id())) continue;
                    SkillDefinitionEntity entity = new SkillDefinitionEntity();
                    entity.setSkillKey(def.id());
                    entity.setName(def.name());
                    entity.setDescription(def.description());
                    entity.setIcon(def.icon());
                    entity.setSystemPrompt(def.systemPrompt());
                    entity.setTools(joinTools(def.tools()));
                    entity.setSortOrder(order++);
                    entity.setEnabled(true);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    repository.save(entity);
                    logger.info("Seeded skill {}", def.id());
                } catch (Exception e) {
                    logger.error("Failed to seed skill {}", resource.getFilename(), e);
                }
            }
        } catch (Exception e) {
            logger.warn("No classpath skills to seed: {}", e.getMessage());
        }
    }

    private void reloadCache() {
        Map<String, SkillDefinition> next = new LinkedHashMap<>();
        for (SkillDefinitionEntity e : repository.findAllByOrderBySortOrderAscIdAsc()) {
            if (Boolean.FALSE.equals(e.getEnabled())) continue;
            next.put(e.getSkillKey(), toDto(e));
        }
        cache = next;
    }

    public List<SkillDefinition> list() {
        return new ArrayList<>(cache.values());
    }

    /** 对话用：公共 + 指定用户的个人技能（启用中） */
    public List<SkillDefinition> listForUser(String username) {
        List<SkillDefinition> out = new ArrayList<>();
        for (SkillDefinition e : cache.values()) {
            if (isPublic(e) || ownerMatches(e, username)) {
                out.add(e);
            }
        }
        return out;
    }

    /** 含停用技能，供管理界面 */
    public List<SkillDefinition> listAll() {
        List<SkillDefinition> all = new ArrayList<>();
        for (SkillDefinitionEntity e : repository.findAllByOrderBySortOrderAscIdAsc()) {
            all.add(toDto(e));
        }
        return all;
    }

    /** 管理界面：scope=public|personal|all；管理员 personal 看全部个人技能 */
    public List<SkillDefinition> listScoped(String scope, String username, boolean admin) {
        List<SkillDefinition> all = listAll();
        List<SkillDefinition> out = new ArrayList<>();
        for (SkillDefinition s : all) {
            boolean pub = isPublic(s);
            if ("public".equals(scope)) {
                if (pub) out.add(s);
            } else if ("personal".equals(scope)) {
                if (!pub && (admin || ownerMatches(s, username))) out.add(s);
            } else {
                out.add(s);
            }
        }
        return out;
    }

    private boolean isPublic(SkillDefinition s) {
        return s.ownerUsername() == null || s.ownerUsername().isBlank();
    }

    private boolean ownerMatches(SkillDefinition s, String username) {
        return username != null && !username.isBlank()
                && username.equals(s.ownerUsername());
    }

    public Optional<SkillDefinition> find(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Transactional
    public SkillDefinition create(SkillSaveRequest req, String updatedBy) {
        if (req.skillKey() == null || req.skillKey().isBlank()) {
            throw new IllegalArgumentException("技能编码不能为空");
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("技能名称不能为空");
        }
        if (req.systemPrompt() == null || req.systemPrompt().isBlank()) {
            throw new IllegalArgumentException("系统提示词不能为空");
        }
        String key = req.skillKey().trim();
        if (repository.existsBySkillKey(key)) {
            throw new IllegalArgumentException("技能编码已存在：" + key);
        }
        SkillDefinitionEntity entity = new SkillDefinitionEntity();
        entity.setSkillKey(key);
        apply(entity, req, updatedBy);
        repository.save(entity);
        reloadCache();
        return toDto(entity);
    }

    @Transactional
    public SkillDefinition update(Long id, SkillSaveRequest req, String updatedBy) {
        SkillDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("技能不存在"));
        // skillKey 允许改，但不能撞车
        if (req.skillKey() != null && !req.skillKey().isBlank()) {
            String key = req.skillKey().trim();
            if (!key.equals(entity.getSkillKey()) && repository.existsBySkillKey(key)) {
                throw new IllegalArgumentException("技能编码已存在：" + key);
            }
            entity.setSkillKey(key);
        }
        apply(entity, req, updatedBy);
        repository.save(entity);
        reloadCache();
        return toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        SkillDefinitionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("技能不存在"));
        repository.delete(entity);
        reloadCache();
    }

    private void apply(SkillDefinitionEntity entity, SkillSaveRequest req, String updatedBy) {
        if (req.name() != null) entity.setName(req.name().trim());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.icon() != null) entity.setIcon(req.icon());
        if (req.systemPrompt() != null) entity.setSystemPrompt(req.systemPrompt());
        if (req.tools() != null) entity.setTools(joinTools(req.tools()));
        if (req.scopeCategoryIds() != null) {
            entity.setScopeCategoryIds(joinIds(req.scopeCategoryIds()));
        }
        if (req.scopeItemIds() != null) {
            entity.setScopeItemIds(joinIds(req.scopeItemIds()));
        }
        // 个人技能归属：仅创建时可指定；空 = 公共
        if (entity.getId() == null && req.ownerUsername() != null) {
            entity.setOwnerUsername(req.ownerUsername().trim());
        }
        if (req.sortOrder() != null) entity.setSortOrder(req.sortOrder());
        if (req.enabled() != null) entity.setEnabled(req.enabled());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(updatedBy);
    }

    private SkillDefinition toDto(SkillDefinitionEntity e) {
        return new SkillDefinition(
                e.getId(),
                e.getSkillKey(),
                e.getName(),
                e.getDescription(),
                e.getIcon(),
                e.getSystemPrompt(),
                splitTools(e.getTools()),
                splitIds(e.getScopeCategoryIds()),
                splitIds(e.getScopeItemIds()),
                e.getSortOrder(),
                e.getEnabled(),
                e.getOwnerUsername() == null ? "" : e.getOwnerUsername()
        );
    }

    private String joinTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) return "rag_search";
        return String.join(",", tools.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private List<String> splitTools(String tools) {
        if (tools == null || tools.isBlank()) return List.of("rag_search");
        return Arrays.stream(tools.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String joinIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return String.join(",", ids.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private List<String> splitIds(String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
