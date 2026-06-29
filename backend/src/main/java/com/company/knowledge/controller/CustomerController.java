package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.CustomerCompanyRequest;
import com.company.knowledge.dto.CustomerCompanyResponse;
import com.company.knowledge.dto.CustomerContactRequest;
import com.company.knowledge.dto.CustomerContactResponse;
import com.company.knowledge.dto.CustomerFollowupRequest;
import com.company.knowledge.dto.CustomerFollowupResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final AuthService authService;

    public CustomerController(CustomerService customerService, AuthService authService) {
        this.customerService = customerService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<CustomerCompanyResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.ok(customerService.list(keyword, ownerName, status, level, region));
    }

    @GetMapping("/paged")
    public ApiResponse<List<CustomerCompanyResponse>> listPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        java.util.Map<String, Object> result = customerService.listPaged(keyword, ownerName, status, level, region, page, size);
        return ApiResponse.paged(
                (java.util.List<CustomerCompanyResponse>) result.get("items"),
                (long) result.get("total"),
                (int) result.get("page"),
                (int) result.get("size"),
                (int) result.get("pages")
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerCompanyResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PostMapping
    public ApiResponse<CustomerCompanyResponse> create(@RequestHeader("X-Auth-Token") String token, @Valid @RequestBody CustomerCompanyRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("客户已创建", customerService.create(request, operatorName));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerCompanyResponse> update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody CustomerCompanyRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("客户已更新", customerService.update(id, request, operatorName));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        customerService.delete(id);
        return ApiResponse.ok("客户已删除", null);
    }

    @PostMapping("/{id}/contacts")
    public ApiResponse<CustomerContactResponse> addContact(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody CustomerContactRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("联系人已创建", customerService.addContact(id, request, operatorName));
    }

    @PutMapping("/contacts/{contactId}")
    public ApiResponse<CustomerContactResponse> updateContact(@RequestHeader("X-Auth-Token") String token, @PathVariable Long contactId, @Valid @RequestBody CustomerContactRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("联系人已更新", customerService.updateContact(contactId, request, operatorName));
    }

    @DeleteMapping("/contacts/{contactId}")
    public ApiResponse<Void> deleteContact(@RequestHeader("X-Auth-Token") String token, @PathVariable Long contactId) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        customerService.deleteContact(contactId);
        return ApiResponse.ok("联系人已删除", null);
    }

    @PostMapping("/{id}/followups")
    public ApiResponse<CustomerFollowupResponse> addFollowup(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody CustomerFollowupRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("跟进记录已创建", customerService.addFollowup(id, request, operatorName));
    }

    @PutMapping("/followups/{followupId}")
    public ApiResponse<CustomerFollowupResponse> updateFollowup(@RequestHeader("X-Auth-Token") String token, @PathVariable Long followupId, @Valid @RequestBody CustomerFollowupRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        String operatorName = user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername();
        return ApiResponse.ok("跟进记录已更新", customerService.updateFollowup(followupId, request, operatorName));
    }

    @DeleteMapping("/followups/{followupId}")
    public ApiResponse<Void> deleteFollowup(@RequestHeader("X-Auth-Token") String token, @PathVariable Long followupId) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS, RoleType.FINANCE);
        customerService.deleteFollowup(followupId);
        return ApiResponse.ok("跟进记录已删除", null);
    }
}
