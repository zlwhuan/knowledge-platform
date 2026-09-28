package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.SkillChatRequest;
import com.company.knowledge.dto.SkillChatResponse;
import com.company.knowledge.dto.SkillChatSessionResponse;
import com.company.knowledge.dto.SkillChatMessageResponse;
import com.company.knowledge.dto.SkillDefinition;
import com.company.knowledge.dto.SkillSaveRequest;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AuthService;
import com.company.knowledge.service.SkillCatalogService;
import com.company.knowledge.service.SkillChatService;
import com.company.knowledge.service.SkillChatSessionService;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能助手：技能列表 / 对话 / 管理（增删改）
 */
@RestController
@RequestMapping("/api/skills")
public class SkillAssistantController {

    private final SkillCatalogService skillCatalogService;
    private final SkillChatService skillChatService;
    private final AuthService authService;
    private final SkillChatSessionService sessionService;
    private final com.company.knowledge.service.AttachmentContentExtractor attachmentExtractor;
    private final com.company.knowledge.service.PreviewService previewService;

    /** 会话 → 进行中生成的停止标记（点停止用；断线不触发） */
    private static final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicBoolean> cancelRegistry =
            new java.util.concurrent.ConcurrentHashMap<>();

    public SkillAssistantController(
            SkillCatalogService skillCatalogService,
            SkillChatService skillChatService,
            AuthService authService,
            SkillChatSessionService sessionService,
            com.company.knowledge.service.AttachmentContentExtractor attachmentExtractor,
            com.company.knowledge.service.PreviewService previewService) {
        this.skillCatalogService = skillCatalogService;
        this.skillChatService = skillChatService;
        this.authService = authService;
        this.sessionService = sessionService;
        this.attachmentExtractor = attachmentExtractor;
        this.previewService = previewService;
    }

    @GetMapping
    public ApiResponse<List<SkillDefinition>> list(
            @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        String username = "";
        try {
            if (token != null && !token.isBlank()) {
                username = authService.requireUser(token).getUsername();
            }
        } catch (Exception ignore) {
            // 匿名只看公共
        }
        return ApiResponse.ok(skillCatalogService.listForUser(username));
    }

    /** 管理端：含停用技能；scope=public|personal|all */
    @GetMapping("/admin")
    public ApiResponse<List<SkillDefinition>> listAll(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestParam(value = "scope", required = false, defaultValue = "all") String scope) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN);
        return ApiResponse.ok(skillCatalogService.listScoped(scope, user.getUsername(), true));
    }

    @PostMapping
    public ApiResponse<SkillDefinition> create(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody SkillSaveRequest request) {
        UserAccount user = authService.requireUser(token);
        // 个人技能：非管理员也可建（归属自己）；公共技能仅管理员
        String owner = request.ownerUsername();
        boolean personal = owner != null && !owner.isBlank();
        if (!personal) {
            authService.requireAnyRole(user, RoleType.ADMIN);
        } else if (!user.getUsername().equals(owner) && !isAdmin(user)) {
            return ApiResponse.fail("只能创建自己的个人技能");
        }
        return ApiResponse.ok("创建成功", skillCatalogService.create(request, user.getDisplayName()));
    }

    @PutMapping("/{id}")
    public ApiResponse<SkillDefinition> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody SkillSaveRequest request) {
        UserAccount user = authService.requireUser(token);
        if (!isAdmin(user)) {
            // 非管理员只能改自己的个人技能
            var existing = skillCatalogService.listAll().stream()
                    .filter(s -> id.equals(s.dbId())).findFirst().orElse(null);
            if (existing == null) return ApiResponse.fail("技能不存在");
            boolean mine = existing.ownerUsername() != null
                    && existing.ownerUsername().equals(user.getUsername());
            if (!mine) return ApiResponse.fail("无权修改该技能");
        }
        return ApiResponse.ok("保存成功", skillCatalogService.update(id, request, user.getDisplayName()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        UserAccount user = authService.requireUser(token);
        if (!isAdmin(user)) {
            var existing = skillCatalogService.listAll().stream()
                    .filter(s -> id.equals(s.dbId())).findFirst().orElse(null);
            if (existing == null) return ApiResponse.fail("技能不存在");
            boolean mine = existing.ownerUsername() != null
                    && existing.ownerUsername().equals(user.getUsername());
            if (!mine) return ApiResponse.fail("无权删除该技能");
        }
        skillCatalogService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    private boolean isAdmin(UserAccount user) {
        try {
            authService.requireAnyRole(user, RoleType.ADMIN);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/readiness")
    public ApiResponse<Map<String, Object>> readiness() {
        return ApiResponse.ok(skillChatService.readiness());
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SkillChatSessionResponse>> listSessions(@RequestHeader("X-Auth-Token") String token) {
        UserAccount user = authService.requireUser(token);
        return ApiResponse.ok(sessionService.listByUsername(user.getUsername()));
    }

    @PostMapping("/sessions")
    public ApiResponse<SkillChatSessionResponse> createSession(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody(required = false) Map<String, Object> body) {
        UserAccount user = authService.requireUser(token);
        String skillId = body == null ? "" : String.valueOf(body.getOrDefault("skillId", ""));
        String title = body == null ? "" : String.valueOf(body.getOrDefault("title", ""));
        return ApiResponse.ok("创建成功", sessionService.create(user.getUsername(), skillId, title));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<SkillChatMessageResponse>> listMessages(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long sessionId) {
        UserAccount user = authService.requireUser(token);
        return ApiResponse.ok(sessionService.listMessages(user.getUsername(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long sessionId) {
        UserAccount user = authService.requireUser(token);
        sessionService.deleteSession(user.getUsername(), sessionId);
        // 清掉会话临时向量
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            String rag = System.getenv("RAG_SERVICE_URL") != null ? System.getenv("RAG_SERVICE_URL") : "http://localhost:8081";
            rt.delete(rag.replaceAll("/+$", "") + "/api/rag/session/" + sessionId);
        } catch (Exception e) {
            // ignore
        }
        return ApiResponse.ok("删除成功", null);
    }

    /** 会话临时附件：抽取文本后入「会话级」向量，不进公共库 */
    @PostMapping("/sessions/{sessionId}/attachments")
    public ApiResponse<Map<String, Object>> uploadSessionAttachment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long sessionId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        UserAccount user = authService.requireUser(token);
        try {
            var detail = sessionService.listMessages(user.getUsername(), sessionId);
            // 会话存在性校验
        } catch (Exception e) {
            return ApiResponse.fail("会话不存在");
        }
        try {
            String attId = String.valueOf(System.currentTimeMillis());
            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            // 存盘用安全文件名（去路径分隔符/空格），展示仍用原名
            String safeName = originalName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
            if (safeName.length() > 180) {
                safeName = safeName.substring(safeName.length() - 180);
            }
            java.nio.file.Path uploadDir = java.nio.file.Paths
                    .get(System.getProperty("user.dir"), "uploads", "session-att")
                    .toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(uploadDir);
            String storedName = sessionId + "_" + attId + "_" + safeName;
            java.nio.file.Path target = uploadDir.resolve(storedName);
            // 不能用 transferTo：嵌入式 Tomcat 会把相对路径解析到临时 work 目录
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String text = attachmentExtractor.extractContent(
                    target.toAbsolutePath().toString(), file.getContentType());
            if (text == null || text.isBlank()) {
                java.nio.file.Files.deleteIfExists(target);
                return ApiResponse.fail("未能从附件提取文本（仅支持 md/txt/docx/pdf/xlsx/pptx/html/图片 OCR）");
            }
            Map<String, Object> body = new HashMap<>();
            body.put("session_id", String.valueOf(sessionId));
            body.put("attachment_id", attId);
            body.put("filename", originalName);
            body.put("title", "会话附件");
            body.put("text", text);

            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            String rag = System.getenv("RAG_SERVICE_URL") != null ? System.getenv("RAG_SERVICE_URL") : "http://localhost:8081";
            try {
                rt.postForEntity(rag.replaceAll("/+$", "") + "/api/rag/session-attach", body, Map.class);
            } catch (Exception ragEx) {
                // 向量库不可用不影响附件问答：全文已落 MySQL，对话时直接注入
            }

            Map<String, Object> saved = sessionService.saveAttachment(
                    sessionId, attId, originalName, text,
                    target.toAbsolutePath().toString(), file.getContentType(),
                    java.nio.file.Files.size(target));

            Map<String, Object> result = new HashMap<>();
            result.put("attachmentId", attId);
            result.put("filename", originalName);
            result.put("chars", text.length());
            result.put("id", saved.get("id"));
            return ApiResponse.ok("附件已加入会话", result);
        } catch (Exception e) {
            return ApiResponse.fail("附件处理失败：" + e.getMessage());
        }
    }

    /** 会话附件预览元数据（走原有预览页） */
    @GetMapping("/session-attachments/{previewKey}/preview")
    public ApiResponse<com.company.knowledge.dto.PreviewMetaResponse> sessionAttachmentPreview(
            @PathVariable String previewKey) {
        var att = findSessionAttachmentByKey(previewKey);
        if (att == null) {
            return ApiResponse.fail("附件不存在");
        }
        return ApiResponse.ok(previewService.getSessionAttachmentPreviewMeta(
                att.getFilename(), att.getContentType(), att.getFilePath(), previewKey));
    }

    @GetMapping("/session-attachments/{previewKey}/preview/file")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> sessionAttachmentPreviewFile(
            @PathVariable String previewKey) {
        var att = findSessionAttachmentByKey(previewKey);
        if (att == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                        previewService.getSessionAttachmentContentType(
                                att.getFilename(), att.getContentType(), att.getFilePath(), previewKey))
                .body(previewService.getSessionAttachmentPreviewResource(
                        att.getFilename(), att.getContentType(), att.getFilePath(), previewKey));
    }

    @GetMapping("/session-attachments/{previewKey}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> sessionAttachmentDownload(
            @PathVariable String previewKey) {
        var att = findSessionAttachmentByKey(previewKey);
        if (att == null || att.getFilePath() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        try {
            var resource = new org.springframework.core.io.UrlResource(
                    java.nio.file.Paths.get(att.getFilePath()).toUri());
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                            att.getContentType() == null ? "application/octet-stream" : att.getContentType())
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(att.getFilename(), java.nio.charset.StandardCharsets.UTF_8))
                    .body(resource);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    private com.company.knowledge.entity.SkillSessionAttachment findSessionAttachmentByKey(String previewKey) {
        // previewKey 约定：{sessionId}_{attachmentId}
        try {
            if (previewKey != null && previewKey.contains("_")) {
                String[] parts = previewKey.split("_", 2);
                Long sid = Long.parseLong(parts[0]);
                return sessionService.getAttachment(sid, parts[1]);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    /** 会话附件列表（恢复会话时回显） */
    @GetMapping("/sessions/{sessionId}/attachments")
    public ApiResponse<java.util.List<Map<String, Object>>> listSessionAttachments(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long sessionId) {
        UserAccount user = authService.requireUser(token);
        try {
            sessionService.listMessages(user.getUsername(), sessionId);
        } catch (Exception e) {
            return ApiResponse.fail("会话不存在");
        }
        return ApiResponse.ok(sessionService.listAttachments(sessionId));
    }

    /** 删除会话附件（同步删向量） */
    @DeleteMapping("/sessions/{sessionId}/attachments/{attachmentId}")
    public ApiResponse<String> deleteSessionAttachment(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long sessionId,
            @PathVariable String attachmentId) {
        UserAccount user = authService.requireUser(token);
        try {
            sessionService.listMessages(user.getUsername(), sessionId);
        } catch (Exception e) {
            return ApiResponse.fail("会话不存在");
        }
        sessionService.deleteAttachment(sessionId, attachmentId);
        return ApiResponse.ok("已删除");
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
     * SSE 流式对话：event=token|status|trace|sources|error|done
     */
    @PostMapping("/chat/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter chatStream(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody SkillChatRequest request) {
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(300000L);
        String skillId = request.skillId() == null ? "" : request.skillId().trim();
        String username = "anonymous";
        try {
            if (token != null && !token.isBlank()) {
                UserAccount u = authService.requireUser(token);
                username = u.getUsername();
            }
        } catch (Exception ignore) {
            // 匿名也可对话，但不落库
        }
        final String uname = username;
        final Long sessionId = request.sessionId();
        final String sid = sessionId == null ? null : String.valueOf(sessionId);
        String ctxTmp = "";
        if (sessionId != null) {
            ctxTmp = "本会话会话号：" + sessionId;
        }
        final String sessionCtx = ctxTmp;

        // 会话附件原文直接注入上下文（大附件截断到预算内，细节可再检索）
        String attCtxTmp = "";
        java.util.List<Map<String, Object>> attSources = new java.util.ArrayList<>();
        if (sessionId != null) {
            try {
                var attEntities = sessionService.listAttachmentEntities(sessionId);
                attCtxTmp = buildAttachmentContext(attEntities);
                String q = request.message() == null ? "" : request.message();
                boolean askAboutAtt = q.contains("附件") || q.contains("文件") || q.contains("上传")
                        || q.contains("这个") || q.contains("里面") || q.contains("内容")
                        || q.contains("写了什么") || q.contains("是什么");
                if (askAboutAtt) {
                    for (var a : attEntities) {
                        Map<String, Object> src = new LinkedHashMap<>();
                        src.put("title", a.getFilename());
                        src.put("path", "sessions/" + sessionId + "/" + a.getAttachmentId() + "/" + a.getFilename());
                        src.put("filename", a.getFilename());
                        src.put("source_kind", "session_attachment");
                        src.put("item_id", "");
                        src.put("attachment_id", a.getAttachmentId() == null ? "" : a.getAttachmentId());
                        src.put("score", 1.0);
                        src.put("note", "会话临时附件");
                        attSources.add(src);
                    }
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
        final String attachmentCtx = attCtxTmp;
        final java.util.List<Map<String, Object>> attachmentSources = attSources;

        java.util.concurrent.atomic.AtomicBoolean cancelled =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        // 会话级停止标记：点「停止」时置位；客户端断线不置位，生成继续写库
        if (sessionId != null) {
            cancelRegistry.put(sessionId, cancelled);
        }
        Set<String> consumedKeys = sessionId != null
                ? sessionService.getConsumedKeys(sessionId)
                : new java.util.LinkedHashSet<>();

        if (sessionId != null && request.message() != null && !request.message().isBlank()) {
            try {
                sessionService.appendUserMessage(sessionId, request.message());
            } catch (Exception e) {
                // ignore
            }
        }

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            StringBuilder answerBuf = new StringBuilder();
            List<Map<String, Object>> sourcesOut = new java.util.ArrayList<>();
            List<Map<String, Object>> tracesOut = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicBoolean clientGone = new java.util.concurrent.atomic.AtomicBoolean(false);
            try {
                skillChatService.chatStream(skillId, request.skillIds(), request.message(), request.history(), consumedKeys, sid, sessionCtx, attachmentCtx, attachmentSources, request.model(), (type, data) -> {
                    if (cancelled.get()) {
                        throw new IllegalStateException("cancelled");
                    }
                    if ("token".equals(type) && data instanceof String s) {
                        answerBuf.append(s);
                    } else if ("sources".equals(type) && data instanceof List<?> list) {
                        for (Object o : list) {
                            if (o instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = (Map<String, Object>) o;
                                sourcesOut.add(m);
                            }
                        }
                    } else if ("trace".equals(type) && data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) data;
                        tracesOut.add(m);
                    }
                    try {
                        String json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                                .writeValueAsString(data == null ? "" : data);
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name(type)
                                .data(json));
                        if ("done".equals(type)) {
                            emitter.complete();
                        }
                    } catch (java.io.IOException e) {
                        // 客户端断开（切页/刷新）：不再中断生成，继续写完并落库
                        clientGone.set(true);
                    } catch (IllegalStateException e) {
                        if (e.getMessage() != null && e.getMessage().contains("cancelled")) {
                            throw e;
                        }
                        clientGone.set(true);
                    } catch (Exception e) {
                        clientGone.set(true);
                    }
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("cancelled") || cancelled.get()) {
                    try {
                        emitter.complete();
                    } catch (Exception ignore) {
                        // ignore
                    }
                } else {
                    try {
                        emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                                .name("error")
                                .data("\"" + msg.replace("\"", "'") + "\""));
                        emitter.complete();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            } finally {
                // 无论客户端是否还在，都把回答落库，便于切页回来恢复
                if (sessionId != null && answerBuf.length() > 0) {
                    try {
                        sessionService.appendAssistantMessage(
                                sessionId, answerBuf.toString(), sourcesOut, tracesOut);
                        sessionService.saveConsumedKeys(sessionId, consumedKeys);
                        if (!sourcesOut.isEmpty()) {
                            sessionService.saveSourceCache(sessionId, sourcesOut);
                        }
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                if (sessionId != null) {
                    cancelRegistry.remove(sessionId);
                }
                executor.shutdown();
                if (clientGone.get()) {
                    try {
                        emitter.complete();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }
        });
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        emitter.onError(ex -> cancelled.set(true));
        emitter.onCompletion(() -> cancelled.set(true));
        return emitter;
    }

    /**
     * 拼会话附件原文给模型。单附件超长时保留头尾，中间用 rag_search 可补；
     * 附件不多时尽量全文注入，避免「问附件内容答不上来」。
     */
    private String buildAttachmentContext(java.util.List<com.company.knowledge.entity.SkillSessionAttachment> atts) {
        if (atts == null || atts.isEmpty()) {
            return "";
        }
        final int budget = 12000;
        StringBuilder sb = new StringBuilder();
        sb.append("【会话附件原文】用户已在本会话上传 ").append(atts.size()).append(" 个附件，内容如下。\n");
        sb.append("回答「附件里是什么/写了什么」时直接依据以下原文，禁止说没有收到附件。\n\n");
        int used = 0;
        for (var a : atts) {
            String content = a.getContent() == null ? "" : a.getContent();
            String name = a.getFilename() == null ? "附件" : a.getFilename();
            if (used >= budget) {
                sb.append("---\n【").append(name).append("】内容过长未并入本轮，需要时用 rag_search 检索。\n");
                continue;
            }
            int remain = budget - used;
            String body;
            if (content.length() <= remain) {
                body = content;
            } else if (remain < 400) {
                body = "";
            } else {
                int head = (int) (remain * 0.7);
                int tail = remain - head;
                body = content.substring(0, head)
                        + "\n…[中间略，需要细节可用 rag_search]…\n"
                        + content.substring(content.length() - tail);
            }
            sb.append("=== 附件：").append(name)
                    .append("（").append(content.length()).append(" 字）===\n");
            if (body.isEmpty()) {
                sb.append("（本轮上下文预算不足，未注入原文）\n");
            } else {
                sb.append(body).append('\n');
            }
            sb.append('\n');
            used += body.length() + 40;
        }
        return sb.toString();
    }
}
