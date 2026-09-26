package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.SkillChatRequest;
import com.company.knowledge.dto.SkillChatResponse;
import com.company.knowledge.dto.SkillDefinition;
import com.company.knowledge.dto.SkillSaveRequest;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.SkillCatalogService;
import com.company.knowledge.service.SkillChatService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 技能助手：技能列表 / 对话 / 管理（增删改）
 */
@RestController
@RequestMapping("/api/skills")
public class SkillAssistantController {

    private final SkillCatalogService skillCatalogService;
    private final SkillChatService skillChatService;
    private final AuthService authService;

    public SkillAssistantController(
            SkillCatalogService skillCatalogService,
            SkillChatService skillChatService,
            AuthService authService) {
        this.skillCatalogService = skillCatalogService;
        this.skillChatService = skillChatService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<SkillDefinition>> list() {
        return ApiResponse.ok(skillCatalogService.list());
    }

    /** 管理端：含停用技能 */
    @GetMapping("/admin")
    public ApiResponse<List<SkillDefinition>> listAll(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok(skillCatalogService.listAll());
    }

    @PostMapping
    public ApiResponse<SkillDefinition> create(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody SkillSaveRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("创建成功", skillCatalogService.create(request, user.getDisplayName()));
    }

    @PutMapping("/{id}")
    public ApiResponse<SkillDefinition> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody SkillSaveRequest request) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok("保存成功", skillCatalogService.update(id, request, user.getDisplayName()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        skillCatalogService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        return ApiResponse.ok(skillChatService.readiness());
    }

    @PostMapping("/chat")
    public ApiResponse<SkillChatResponse> chat(@RequestBody SkillChatRequest request) {
        String skillId = request.skillId() == null ? "" : request.skillId().trim();
        SkillChatResponse response = skillChatService.chat(
                skillId,
                request.message(),
                request.history()
        );
        return ApiResponse.ok(response);
    }

    /**
     * SSE 流式对话：event=token|status|sources|error|done
     */
    @PostMapping("/chat/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(
            @RequestBody SkillChatRequest request) {
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(300000L);
        String skillId = request.skillId() == null ? "" : request.skillId().trim();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                skillChatService.chatStream(skillId, request.message(), request.history(), (type, data) -> {
                    try {
                        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                                .writeValueAsString(data == null ? "" : data);
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name(type)
                                .data(json));
                        if ("done".equals(type)) {
                            emitter.complete();
                        }
                    } catch (Exception e) {
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ignore) {
                            // ignore
                        }
                    }
                });
            } catch (Exception e) {
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("error")
                            .data("\"" + e.getMessage().replace("\"", "'") + "\""));
                    emitter.complete();
                } catch (Exception ignore) {
                    // ignore
                }
            } finally {
                executor.shutdown();
            }
        });
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> { });
        return emitter;
    }
}
