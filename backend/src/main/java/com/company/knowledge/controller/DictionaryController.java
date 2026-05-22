package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.DictionaryGroupResponse;
import com.company.knowledge.dto.DictionaryItemRequest;
import com.company.knowledge.dto.DictionaryItemResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.DictionaryService;
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
@RequestMapping("/api/dictionaries")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final AuthService authService;

    public DictionaryController(DictionaryService dictionaryService, AuthService authService) {
        this.dictionaryService = dictionaryService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<DictionaryGroupResponse>> listAll() {
        return ApiResponse.ok(dictionaryService.listAll());
    }

    @GetMapping("/{dictType}")
    public ApiResponse<List<DictionaryItemResponse>> listByType(
            @PathVariable String dictType,
            @RequestParam(defaultValue = "false") boolean includeDisabled
    ) {
        return ApiResponse.ok(dictionaryService.listByType(dictType, includeDisabled));
    }

    @PostMapping
    public ApiResponse<DictionaryItemResponse> create(@RequestHeader("X-Auth-Token") String token, @Valid @RequestBody DictionaryItemRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("创建成功", dictionaryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DictionaryItemResponse> update(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id, @Valid @RequestBody DictionaryItemRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("更新成功", dictionaryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        dictionaryService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
