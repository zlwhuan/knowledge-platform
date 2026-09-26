package com.company.knowledge.service;

import com.company.knowledge.dto.RagSearchResult;
import com.company.knowledge.dto.SkillChatResponse;
import com.company.knowledge.dto.SkillDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能对话引擎：系统提示词 + RAG 工具调用循环 + 出处收集
 */
@Service
public class SkillChatService {

    private static final Logger logger = LoggerFactory.getLogger(SkillChatService.class);
    private static final int MAX_TOOL_ROUNDS = 4;

    private final SkillCatalogService skillCatalogService;
    private final LlmClient llmClient;
    private final RagService ragService;

    public SkillChatService(SkillCatalogService skillCatalogService, LlmClient llmClient, RagService ragService) {
        this.skillCatalogService = skillCatalogService;
        this.llmClient = llmClient;
        this.ragService = ragService;
    }

    public boolean isReady() {
        return llmClient.isConfigured();
    }

    public Map<String, Object> readiness() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("llmConfigured", llmClient.isConfigured());
        info.put("llm", llmClient.configuredInfo());
        info.put("skills", skillCatalogService.list().size());
        info.put("keyFingerprint", llmClient.keyFingerprint());
        return info;
    }

    /**
     * 流式对话：onEvent(type, data) 推送 status/token/sources/done/error
     */
    public void chatStream(String skillId, String message, List<Map<String, String>> history,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        SkillDefinition skill = skillCatalogService.find(skillId).orElse(null);
        if (skill == null) {
            onEvent.accept("error", "技能不存在：" + skillId);
            onEvent.accept("done", "");
            return;
        }
        if (!llmClient.isConfigured()) {
            onEvent.accept("error", "模型未配置。请在后端环境变量设置 LLM_API_BASE_URL、LLM_API_KEY、LLM_MODEL 后重启服务。");
            onEvent.accept("done", "");
            return;
        }
        if (message == null || message.isBlank()) {
            onEvent.accept("error", "请输入问题");
            onEvent.accept("done", "");
            return;
        }

        try {
            ArrayNode messages = llmClient.newArray();
            ObjectNode system = llmClient.newObject();
            system.put("role", "system");
            system.put("content", buildSystemPrompt(skill));
            messages.add(system);

            if (history != null) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String content = turn.getOrDefault("content", "");
                    if (content == null || content.isBlank()) continue;
                    if (!"user".equals(role) && !"assistant".equals(role)) continue;
                    ObjectNode m = llmClient.newObject();
                    m.put("role", role);
                    m.put("content", content);
                    messages.add(m);
                }
            }

            ObjectNode user = llmClient.newObject();
            user.put("role", "user");
            user.put("content", message.trim());
            messages.add(user);

            JsonNode tools = buildTools(skill);
            Map<String, Map<String, Object>> sourceMap = new LinkedHashMap<>();
            int toolRounds = 0;

            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                final boolean[] isToolRound = {false};
                JsonNode response = llmClient.chatCompletionStream(messages, tools, delta -> {
                    if (!isToolRound[0]) {
                        onEvent.accept("token", delta);
                    }
                });
                JsonNode choice = response.path("choices").path(0).path("message");

                JsonNode toolCalls = choice.path("tool_calls");
                if (toolCalls.isArray() && toolCalls.size() > 0) {
                    isToolRound[0] = true;
                    toolRounds++;
                    onEvent.accept("status", "正在检索知识库…");
                    ObjectNode assistant = llmClient.newObject();
                    assistant.put("role", "assistant");
                    if (choice.hasNonNull("content")) {
                        assistant.set("content", choice.get("content"));
                    } else {
                        assistant.putNull("content");
                    }
                    assistant.set("tool_calls", toolCalls);
                    messages.add(assistant);

                    for (JsonNode call : toolCalls) {
                        String callId = call.path("id").asText("call_" + round);
                        String fnName = call.path("function").path("name").asText("");
                        String argsJson = call.path("function").path("arguments").asText("{}");
                        String toolResult = executeTool(fnName, argsJson, skill, sourceMap);

                        ObjectNode toolMsg = llmClient.newObject();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", callId);
                        toolMsg.put("content", toolResult);
                        messages.add(toolMsg);
                    }
                    continue;
                }

                // 纯文本回答已通过 token 增量推完；补一条空 token 保证前端收尾
                onEvent.accept("sources", new ArrayList<>(sourceMap.values()));
                onEvent.accept("done", Map.of("toolRounds", toolRounds));
                return;
            }

            onEvent.accept("error", "工具调用轮次过多，请简化问题后重试");
            onEvent.accept("done", "");
        } catch (Exception e) {
            logger.error("Skill stream chat failed: skillId={}", skillId, e);
            onEvent.accept("error", e.getMessage() == null ? "对话失败" : e.getMessage());
            onEvent.accept("done", "");
        }
    }

    public SkillChatResponse chat(String skillId, String message, List<Map<String, String>> history) {
        SkillDefinition skill = skillCatalogService.find(skillId).orElse(null);
        if (skill == null) {
            return SkillChatResponse.fail(skillId, "技能不存在：" + skillId);
        }
        if (!llmClient.isConfigured()) {
            return SkillChatResponse.fail(skillId,
                    "模型未配置。请在后端环境变量设置 LLM_API_BASE_URL、LLM_API_KEY、LLM_MODEL 后重启服务。");
        }
        if (message == null || message.isBlank()) {
            return SkillChatResponse.fail(skillId, "请输入问题");
        }

        try {
            ArrayNode messages = llmClient.newArray();
            ObjectNode system = llmClient.newObject();
            system.put("role", "system");
            system.put("content", buildSystemPrompt(skill));
            messages.add(system);

            if (history != null) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String content = turn.getOrDefault("content", "");
                    if (content == null || content.isBlank()) continue;
                    if (!"user".equals(role) && !"assistant".equals(role)) continue;
                    ObjectNode m = llmClient.newObject();
                    m.put("role", role);
                    m.put("content", content);
                    messages.add(m);
                }
            }

            ObjectNode user = llmClient.newObject();
            user.put("role", "user");
            user.put("content", message.trim());
            messages.add(user);

            JsonNode tools = buildTools(skill);
            Map<String, Map<String, Object>> sourceMap = new LinkedHashMap<>();
            int toolRounds = 0;

            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                JsonNode response = llmClient.chatCompletion(messages, tools);
                JsonNode choice = response.path("choices").path(0).path("message");

                JsonNode toolCalls = choice.path("tool_calls");
                if (toolCalls.isArray() && toolCalls.size() > 0) {
                    toolRounds++;
                    // 把 assistant（含 tool_calls）写回对话
                    ObjectNode assistant = llmClient.newObject();
                    assistant.put("role", "assistant");
                    if (choice.hasNonNull("content")) {
                        assistant.set("content", choice.get("content"));
                    } else {
                        assistant.putNull("content");
                    }
                    assistant.set("tool_calls", toolCalls);
                    messages.add(assistant);

                    for (JsonNode call : toolCalls) {
                        String callId = call.path("id").asText("call_" + round);
                        String fnName = call.path("function").path("name").asText("");
                        String argsJson = call.path("function").path("arguments").asText("{}");
                        String toolResult = executeTool(fnName, argsJson, skill, sourceMap);

                        ObjectNode toolMsg = llmClient.newObject();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", callId);
                        toolMsg.put("content", toolResult);
                        messages.add(toolMsg);
                    }
                    continue;
                }

                String answer = choice.path("content").asText("");
                if (answer == null || answer.isBlank()) {
                    answer = "（模型未返回内容，请换一种问法或稍后再试）";
                }
                return SkillChatResponse.ok(answer, new ArrayList<>(sourceMap.values()), skillId, toolRounds);
            }

            return SkillChatResponse.fail(skillId, "工具调用轮次过多，请简化问题后重试");
        } catch (Exception e) {
            logger.error("Skill chat failed: skillId={}", skillId, e);
            return SkillChatResponse.fail(skillId, e.getMessage() == null ? "对话失败" : e.getMessage());
        }
    }

    /** 从结构化字段或 path 解析所属知识条目 ID */
    private String resolveItemId(RagSearchResult r) {
        if (r.getItemId() != null && !r.getItemId().isBlank()) {
            return r.getItemId();
        }
        String path = r.getPath() == null ? "" : r.getPath();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("knowledge_items/(?:attachment_)?(\\d+)")
                .matcher(path);
        if (m.find()) {
            return m.group(1);
        }
        m = java.util.regex.Pattern.compile("attachments/(\\d+)/").matcher(path);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private String buildSystemPrompt(SkillDefinition skill) {
        StringBuilder sb = new StringBuilder();
        sb.append(skill.systemPrompt()).append("\n\n");
        sb.append("## 通用约束\n");
        sb.append("- 回答使用简体中文，面向公司同事，直接给结论。\n");
        sb.append("- 涉及产品功能/参数/价格/工期时，必须基于检索到的知识库内容。\n");
        sb.append("- 库内无依据时明确写「文档库中未找到」，禁止编造。\n");
        sb.append("- 引用依据时标注：标题 · 出处（locator 或 path）。\n");
        sb.append("- 不要输出与业务无关的内容。");
        return sb.toString();
    }

    private JsonNode buildTools(SkillDefinition skill) {
        List<String> allowed = skill.tools() == null || skill.tools().isEmpty()
                ? List.of("rag_search")
                : skill.tools();
        ArrayNode tools = llmClient.newArray();

        if (allowed.contains("rag_search")) {
            ObjectNode tool = llmClient.newObject();
            tool.put("type", "function");
            ObjectNode fn = llmClient.newObject();
            fn.put("name", "rag_search");
            fn.put("description", "在公司产品知识库中混合检索（向量+BM25），返回相关文本块与出处");
            ObjectNode params = llmClient.newObject();
            params.put("type", "object");
            ObjectNode props = llmClient.newObject();
            ObjectNode query = llmClient.newObject();
            query.put("type", "string");
            query.put("description", "检索关键词或问题");
            props.set("query", query);
            ObjectNode topK = llmClient.newObject();
            topK.put("type", "integer");
            topK.put("description", "返回条数，默认 5");
            props.set("top_k", topK);
            ObjectNode category = llmClient.newObject();
            category.put("type", "string");
            category.put("description", "可选分类树路径过滤");
            props.set("category", category);
            params.set("properties", props);
            params.set("required", llmClient.valueToTree(List.of("query")));
            fn.set("parameters", params);
            tool.set("function", fn);
            tools.add(tool);
        }
        return tools;
    }

    private String executeTool(
            String fnName,
            String argsJson,
            SkillDefinition skill,
            Map<String, Map<String, Object>> sourceMap) {
        if (!"rag_search".equals(fnName)) {
            return "未知工具：" + fnName;
        }
        if (skill.tools() != null && !skill.tools().isEmpty() && !skill.tools().contains("rag_search")) {
            return "该技能不允许使用 rag_search";
        }

        try {
            JsonNode args = llmClient.parse(argsJson);
            String query = args.path("query").asText("");
            int topK = args.path("top_k").asInt(5);
            String category = args.hasNonNull("category") ? args.path("category").asText(null) : null;
            if (query == null || query.isBlank()) {
                return "{\"error\":\"query 不能为空\"}";
            }
            topK = Math.max(1, Math.min(topK, 10));

            List<RagSearchResult> results = ragService.search(query, topK, category, null);
            List<Map<String, Object>> payload = new ArrayList<>();
            for (RagSearchResult r : results) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("title", r.getTitle());
                row.put("section", r.getSection());
                row.put("page", r.getPage());
                row.put("product", r.getProduct());
                row.put("path", r.getPath());
                row.put("text", r.getText());
                row.put("score", r.getScore());
                row.put("item_id", resolveItemId(r));
                row.put("attachment_id", r.getAttachmentId() == null ? "" : r.getAttachmentId());
                row.put("source_kind", r.getSourceKind() == null ? "" : r.getSourceKind());
                row.put("locator", r.getLocator() == null ? "" : r.getLocator());
                row.put("filename", r.getFilename() == null ? "" : r.getFilename());
                row.put("file_path", r.getFilePath() == null ? "" : r.getFilePath());
                payload.add(row);
                addMergedSource(sourceMap, r);
            }
            return llmClient.writeValueAsString(Map.of(
                    "query", query,
                    "count", payload.size(),
                    "results", payload
            ));
        } catch (Exception e) {
            logger.warn("rag_search tool failed: {}", e.getMessage());
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * 出处合并：同一知识条目 / 同一附件只保留一条，章节合并展示。
     * item:key=knowledge_items/{id}，attachment:key=attachments/{item}/{attId}/{filename}
     */
    @SuppressWarnings("unchecked")
    private void addMergedSource(Map<String, Map<String, Object>> sourceMap, RagSearchResult r) {
        String path = r.getPath() == null ? "" : r.getPath();
        String sourceKind = r.getSourceKind() == null
                ? (path.startsWith("attachments/") ? "attachment" : "item")
                : r.getSourceKind();
        String itemId = resolveItemId(r);
        String attachmentId = r.getAttachmentId() == null ? "" : r.getAttachmentId();
        String key = "attachment".equals(sourceKind)
                ? ("att:" + (attachmentId.isBlank() ? path : itemId + ":" + attachmentId))
                : ("item:" + (itemId.isBlank() ? path : itemId));

        Map<String, Object> src = sourceMap.computeIfAbsent(key, k -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", r.getTitle());
            m.put("path", path);
            m.put("product", r.getProduct());
            m.put("score", r.getScore());
            m.put("item_id", itemId);
            m.put("attachment_id", attachmentId);
            m.put("source_kind", sourceKind);
            m.put("filename", r.getFilename() == null ? "" : r.getFilename());
            m.put("file_path", r.getFilePath() == null ? "" : r.getFilePath());
            m.put("page", r.getPage() == null ? "" : r.getPage());
            m.put("sections", new ArrayList<String>());
            return m;
        });

        List<String> sections = (List<String>) src.get("sections");
        String section = r.getSection() == null ? "" : r.getSection().trim();
        String title = String.valueOf(src.getOrDefault("title", ""));
        if (!section.isEmpty() && !section.equals(title) && !sections.contains(section)) {
            sections.add(section);
        }
        src.put("locator", buildMergedLocator(sourceKind,
                String.valueOf(src.getOrDefault("filename", "")),
                sections,
                String.valueOf(src.getOrDefault("page", "")),
                title));
    }

    private String buildMergedLocator(String sourceKind, String filename, List<String> sections, String page, String title) {
        List<String> parts = new ArrayList<>();
        if ("attachment".equals(sourceKind)) {
            parts.add(filename == null || filename.isBlank() || "null".equals(filename) ? "附件" : "附件 " + filename);
        } else {
            parts.add("条目正文");
        }
        if (page != null && !page.isBlank() && !"null".equals(page)) {
            parts.add("p." + page);
        }
        if (sections != null && !sections.isEmpty()) {
            parts.add(String.join("、", sections));
        }
        return String.join(" · ", parts);
    }
}
