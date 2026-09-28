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
import java.util.Set;

/**
 * 技能对话引擎：系统提示词 + RAG 工具调用循环 + 出处收集
 */
@Service
public class SkillChatService {

    private static final Logger logger = LoggerFactory.getLogger(SkillChatService.class);
    private static final int MAX_TOOL_ROUNDS = 2;

    private final SkillCatalogService skillCatalogService;
    private final LlmClient llmClient;
    private final RagService ragService;
    private final com.company.knowledge.repository.CategoryRepository categoryRepository;
    private final KnowledgeItemVectorTextBuilder textBuilder;
    private final KnowledgeItemService knowledgeItemService;

    public SkillChatService(SkillCatalogService skillCatalogService, LlmClient llmClient, RagService ragService,
                            com.company.knowledge.repository.CategoryRepository categoryRepository,
                            KnowledgeItemVectorTextBuilder textBuilder,
                            KnowledgeItemService knowledgeItemService) {
        this.skillCatalogService = skillCatalogService;
        this.llmClient = llmClient;
        this.ragService = ragService;
        this.categoryRepository = categoryRepository;
        this.textBuilder = textBuilder;
        this.knowledgeItemService = knowledgeItemService;
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
     * 流式对话：onEvent(type, data) 推送 status/token/trace/sources/done/error
     * consumedKeys：会话内已检索块，跨轮/跨请求去重复用
     */
    public void chatStream(String skillId, String message, List<Map<String, String>> history,
                           Set<String> consumedKeys,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        chatStream(skillId, null, message, history, consumedKeys, null, null, onEvent);
    }

    /** 多技能合并：system 提示词拼接，工具取并集 */
    public void chatStream(String skillId, List<String> skillIds, String message,
                           List<Map<String, String>> history,
                           Set<String> consumedKeys,
                           String sessionId,
                           String sessionContext,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        chatStream(skillId, skillIds, message, history, consumedKeys, sessionId, sessionContext, null, onEvent);
    }

    /** attachmentContext：会话附件原文（已由调用方拼好），直接进上下文，不依赖模型检索 */
    public void chatStream(String skillId, List<String> skillIds, String message,
                           List<Map<String, String>> history,
                           Set<String> consumedKeys,
                           String sessionId,
                           String sessionContext,
                           String attachmentContext,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        chatStream(skillId, skillIds, message, history, consumedKeys, sessionId,
                sessionContext, attachmentContext, null, onEvent);
    }

    /** attachmentSources：会话附件出处（filename 等），随 answers 一起返回 */
    public void chatStream(String skillId, List<String> skillIds, String message,
                           List<Map<String, String>> history,
                           Set<String> consumedKeys,
                           String sessionId,
                           String sessionContext,
                           String attachmentContext,
                           List<Map<String, Object>> attachmentSources,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        chatStream(skillId, skillIds, message, history, consumedKeys, sessionId,
                sessionContext, attachmentContext, attachmentSources, null, onEvent);
    }

    /** modelOverride：会话内切换模型（展示名或 ID），为空用默认 */
    public void chatStream(String skillId, List<String> skillIds, String message,
                           List<Map<String, String>> history,
                           Set<String> consumedKeys,
                           String sessionId,
                           String sessionContext,
                           String attachmentContext,
                           List<Map<String, Object>> attachmentSources,
                           String modelOverride,
                           java.util.function.BiConsumer<String, Object> onEvent) {
        SkillDefinition skill = resolveSkill(skillId, skillIds);
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
            system.put("content", buildSystemPrompt(skill, sessionContext, attachmentContext != null && !attachmentContext.isBlank(), modelOverride));
            messages.add(system);

            if (attachmentContext != null && !attachmentContext.isBlank()) {
                ObjectNode att = llmClient.newObject();
                att.put("role", "system");
                att.put("content", attachmentContext);
                messages.add(att);
            }

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
            if (attachmentSources != null) {
                for (Map<String, Object> src : attachmentSources) {
                    Object key = src.get("path");
                    sourceMap.put(key == null ? "att:" + src.get("filename") : String.valueOf(key), src);
                }
            }
            if (consumedKeys == null) {
                consumedKeys = new java.util.LinkedHashSet<>();
            }
            int toolRounds = 0;
            long t0 = System.currentTimeMillis();

            for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
                final boolean[] isToolRound = {false};
                onEvent.accept("trace", trace("step", "模型思考中", "round=" + (round + 1), t0));
                JsonNode response = llmClient.chatCompletionStream(messages, tools, modelOverride, delta -> {
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
                        onEvent.accept("trace", trace("tool", "调用工具 " + fnName, argsJson, t0));
                        long toolStart = System.currentTimeMillis();
                        String toolResult = executeTool(fnName, argsJson, skill, sourceMap, consumedKeys, sessionId);
                        long toolMs = System.currentTimeMillis() - toolStart;
                        onEvent.accept("trace", trace("tool_result", "工具返回 (" + toolMs + "ms)", toolResult, t0));

                        ObjectNode toolMsg = llmClient.newObject();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", callId);
                        toolMsg.put("content", toolResult);
                        messages.add(toolMsg);
                    }
                    continue;
                }

                // 纯文本回答已通过 token 增量推完
                onEvent.accept("trace", trace("done", "回答完成", "sources=" + sourceMap.size(), t0));
                onEvent.accept("sources", new ArrayList<>(sourceMap.values()));
                onEvent.accept("done", Map.of("toolRounds", toolRounds, "elapsedMs", System.currentTimeMillis() - t0));
                return;
            }

            onEvent.accept("error", "工具调用轮次过多，请简化问题后重试");
            onEvent.accept("done", "");
        } catch (Exception e) {
            logger.error("Skill stream chat failed: skillId={}", skillId, e);
            onEvent.accept("trace", trace("error", "执行异常", String.valueOf(e.getMessage()), System.currentTimeMillis()));
            onEvent.accept("error", e.getMessage() == null ? "对话失败" : e.getMessage());
            onEvent.accept("done", "");
        }
    }

    private static Map<String, Object> trace(String type, String label, String detail, long t0) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("label", label);
        m.put("detail", detail == null ? "" : detail);
        m.put("elapsedMs", System.currentTimeMillis() - t0);
        return m;
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
            system.put("content", buildSystemPrompt(skill, null, false, null));
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
            Set<String> consumedKeys = new java.util.LinkedHashSet<>();
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
                        String toolResult = executeTool(fnName, argsJson, skill, sourceMap, consumedKeys, null);

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

    /** 单技能或合并多技能 */
    private SkillDefinition resolveSkill(String skillId, List<String> skillIds) {
        List<String> ids = new ArrayList<>();
        if (skillIds != null) {
            for (String s : skillIds) {
                if (s != null && !s.isBlank()) ids.add(s.trim());
            }
        }
        if (ids.isEmpty() && skillId != null && !skillId.isBlank()) {
            ids.add(skillId.trim());
        }
        // 未选技能：走通用助手，不偷偷套用某个技能的人设
        if (ids.isEmpty()) {
            return defaultSkill();
        }
        List<SkillDefinition> parts = new ArrayList<>();
        for (String id : ids) {
            skillCatalogService.find(id).ifPresent(parts::add);
        }
        if (parts.isEmpty()) {
            return defaultSkill();
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        // 合并提示词与工具
        StringBuilder prompt = new StringBuilder();
        List<String> tools = new ArrayList<>();
        for (SkillDefinition s : parts) {
            prompt.append("## 技能：").append(s.name()).append("\n").append(s.systemPrompt()).append("\n\n");
            if (s.tools() != null) {
                for (String t : s.tools()) {
                    if (!tools.contains(t)) tools.add(t);
                }
            }
        }
        return new SkillDefinition(
                null,
                "combined",
                "组合技能：" + String.join(" + ", parts.stream().map(SkillDefinition::name).toList()),
                parts.stream().map(SkillDefinition::description).reduce((a, b) -> a + "；" + b).orElse(""),
                "MULTI",
                prompt.toString(),
                tools.isEmpty() ? List.of("rag_search") : tools,
                parts.stream().flatMap(s -> s.scopeCategoryIds().stream()).distinct().toList(),
                parts.stream().flatMap(s -> s.scopeItemIds().stream()).distinct().toList(),
                0,
                true,
                ""
        );
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

    private String buildSystemPrompt(SkillDefinition skill, String sessionContext, boolean hasAttachments,
                                     String modelOverride) {
        StringBuilder sb = new StringBuilder();
        sb.append(skill.systemPrompt()).append("\n\n");
        // 自报模型型号，避免被问「你是什么模型」时瞎编
        String modelLabel = (modelOverride != null && !modelOverride.isBlank())
                ? modelOverride.trim()
                : (llmClient.getModel() == null ? "" : llmClient.getModel().trim());
        if (!modelLabel.isEmpty()) {
            sb.append("## 运行环境\n");
            sb.append("- 你当前运行的底层模型是：`").append(modelLabel).append("`（由平台 LLM 网关调用）。\n");
            sb.append("- 用户问「你是什么模型/谁在回答」时，直接回答上述模型名，不要编造其他型号。\n\n");
        }
        if (hasAttachments) {
            sb.append("## 本会话用户上传的附件\n");
            sb.append("用户已在本会话上传附件，完整抽取文本见后续 system 消息「【会话附件原文】」。\n");
            sb.append("用户问「这个附件/上传的文件/里面写了什么」时，**直接依据附件原文回答**，");
            sb.append("禁止说「没有收到附件」。原文未覆盖的细节才用 rag_search 补充。\n\n");
        } else if (sessionContext != null && !sessionContext.isBlank()) {
            sb.append("## 本会话上下文\n");
            sb.append(sessionContext).append("\n\n");
        }
        sb.append("## 通用约束\n");
        sb.append("- 回答使用简体中文，面向公司同事，直接给结论。\n");
        sb.append("- 涉及产品功能/参数/价格/工期时，必须基于检索到的知识库内容或会话附件原文。\n");
        sb.append("- 库内无依据时明确写「文档库中未找到」，禁止编造。\n");
        sb.append("- **正文里不要写出处、路径、链接或「参考」清单**；系统会在回答下方单独展示出处，正文只写结论与分析。\n");
        sb.append("- 不要输出与业务无关的内容。\n");
        sb.append("- 「此前已提供该片段」= 该证据已在本会话中，直接使用即可。\n\n");

        sb.append("## 工具选用（有工具时按此选择，能不调就不调）\n");
        sb.append("- **rag_search**：全库找线索。开放问答、找产品/方案/条款、不确定条目 ID 时用；query 带上具体产品/设备/型号（如「智能呼吸机 功能特点」），");
        sb.append("不要只用「功能」「优势」等泛词；最多 2 次。\n");
        sb.append("- **get_item**：按条目 ID 读全文。用户指定条目、或 rag_search 命中后要完整原文/细节/附件列表时用。\n");
        sb.append("- **get_param_table**：取参数表（结构化表头+行）。参数对照、规格比对、标书逐条响应时用；有条目 ID 传 item_id，没有就传产品关键词。\n");
        sb.append("- **典型组合**：① 问答两段式 = rag_search 定位 →（需要原文时）get_item → 作答；");
        sb.append("② 参数/标书 = rag_search 找参数条目 → get_param_table 取表 → 逐条比对。\n");
        sb.append("- **少调精调**：简单问题一次 rag_search 就答；追问/换说法时基于已有结果作答，不必再调；仅话题转向新对象时才调工具。\n");
        sb.append("- 若已检索到能回答问题的内容，立即作答，不要为了全面而反复检索。");
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

        if (allowed.contains("get_item")) {
            ObjectNode tool = llmClient.newObject();
            tool.put("type", "function");
            ObjectNode fn = llmClient.newObject();
            fn.put("name", "get_item");
            fn.put("description", "按知识条目 ID 读取完整详情（标题/摘要/正文/附件列表）。适合 rag_search 命中后追问原文。");
            ObjectNode params = llmClient.newObject();
            params.put("type", "object");
            ObjectNode props = llmClient.newObject();
            ObjectNode itemId = llmClient.newObject();
            itemId.put("type", "integer");
            itemId.put("description", "知识条目 ID");
            props.set("item_id", itemId);
            params.set("properties", props);
            params.set("required", llmClient.valueToTree(List.of("item_id")));
            fn.set("parameters", params);
            tool.set("function", fn);
            tools.add(tool);
        }

        if (allowed.contains("get_param_table")) {
            ObjectNode tool = llmClient.newObject();
            tool.put("type", "function");
            ObjectNode fn = llmClient.newObject();
            fn.put("name", "get_param_table");
            fn.put("description", "读取参数表：按条目 ID 取该条目正文中的参数表格；或按产品/关键词搜索参数类条目并返回表格数据。");
            ObjectNode params = llmClient.newObject();
            params.put("type", "object");
            ObjectNode props = llmClient.newObject();
            ObjectNode itemId = llmClient.newObject();
            itemId.put("type", "integer");
            itemId.put("description", "知识条目 ID（与 query 二选一，优先）");
            props.set("item_id", itemId);
            ObjectNode query = llmClient.newObject();
            query.put("type", "string");
            query.put("description", "产品名/关键词，用于搜索参数类知识条目");
            props.set("query", query);
            params.set("properties", props);
            params.set("required", llmClient.valueToTree(List.of()));
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
            Map<String, Map<String, Object>> sourceMap,
            Set<String> consumedKeys,
            String sessionId) {
        if ("get_item".equals(fnName)) {
            return executeGetItem(fnName, argsJson, skill, sourceMap);
        }
        if ("get_param_table".equals(fnName)) {
            return executeGetParamTable(fnName, argsJson, skill, sourceMap);
        }
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

            // 技能绑定范围：分类树 / 知识条目 硬过滤（避免取到无关知识）
            List<String> scopeItemIds = skill.scopeItemIds();
            List<String> scopeCategoryPaths = resolveCategoryPaths(skill.scopeCategoryIds());
            if (skill.scopeCategoryIds() != null && !skill.scopeCategoryIds().isEmpty() && scopeCategoryPaths.isEmpty()) {
                return "{\"error\":\"技能绑定的知识分类不存在或已删除，请管理员在「管理技能」中重新绑定\"}";
            }

            List<RagSearchResult> results = ragService.search(
                    query, topK, category, null, sessionId,
                    scopeItemIds == null || scopeItemIds.isEmpty() ? null : scopeItemIds,
                    scopeCategoryPaths.isEmpty() ? null : scopeCategoryPaths);
            // 锚定过滤：问题里的核心实体（产品/设备/型号）必须出现，剔除仅命中「功能」等泛词的块
            results = filterByAnchorTerms(query, results);
            List<Map<String, Object>> payload = new ArrayList<>();
            Set<String> seenKeys = new java.util.LinkedHashSet<>();
            for (RagSearchResult r : results) {
                String dedupeKey = r.getPath() + "|" + r.getSection() + "|" + r.getLocator();
                if (!seenKeys.add(dedupeKey)) {
                    continue;
                }
                // 跨轮去重：同一块已在对话里出现过，不再重复送全文
                String globalKey = r.getPath() + "|" + (r.getSection() == null ? "" : r.getSection());
                if (!consumedKeys.add(globalKey)) {
                    Map<String, Object> stub = new LinkedHashMap<>();
                    stub.put("title", r.getTitle());
                    stub.put("locator", r.getLocator());
                    stub.put("path", r.getPath());
                    stub.put("note", "此前已提供该片段，不再重复");
                    payload.add(stub);
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("title", r.getTitle());
                row.put("section", r.getSection());
                row.put("page", r.getPage());
                row.put("product", r.getProduct());
                row.put("path", r.getPath());
                // 证据单位=切块：整块交给模型；只限制条数，不肢解句子
                row.put("text", clipOversizedChunk(r.getText()));
                row.put("score", r.getScore());
                row.put("item_id", resolveItemId(r));
                row.put("attachment_id", r.getAttachmentId() == null ? "" : r.getAttachmentId());
                row.put("source_kind", r.getSourceKind() == null ? "" : r.getSourceKind());
                row.put("locator", r.getLocator() == null ? "" : r.getLocator());
                row.put("filename", r.getFilename() == null ? "" : r.getFilename());
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

    /** 技能绑定的分类 ID → 向量库分类路径（如 01_标准化资料/手术麻醉） */
    /** 未选技能时的通用人设：不带任何业务技能立场 */
    private static SkillDefinition defaultSkill() {
        return new SkillDefinition(
                null,
                "default",
                "通用助手",
                "公司知识库通用问答",
                "",
                "你是公司内部知识库通用助手。回答保持中性、直接，不要自称某个具体业务技能（如实施支持/售前/标书等）。\n"
                        + "涉及产品功能、参数、实施细节时，用 rag_search 检索知识库后作答并标注出处；库内没有就明确说未找到。\n"
                        + "用户可以在输入框上方选择具体技能以获得更专业的流程。",
                List.of("rag_search"),
                List.of(),
                List.of(),
                0,
                true,
                ""
        );
    }

    /** get_item：按条目 ID 读详情 */
    private String executeGetItem(String fnName, String argsJson, SkillDefinition skill,
                                  Map<String, Map<String, Object>> sourceMap) {
        if (skill.tools() != null && !skill.tools().isEmpty() && !skill.tools().contains("get_item")) {
            return "{\"error\":\"该技能不允许使用 get_item\"}";
        }
        try {
            JsonNode args = llmClient.parse(argsJson);
            long itemId = args.path("item_id").asLong(0);
            if (itemId <= 0) {
                return "{\"error\":\"item_id 不能为空\"}";
            }
            if (!inSkillScope(skill, String.valueOf(itemId))) {
                return "{\"error\":\"该条目不在技能绑定范围内\"}";
            }
            var item = knowledgeItemService.get(itemId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("item_id", item.id());
            row.put("title", item.title());
            row.put("type", item.type());
            row.put("summary", item.summary());
            row.put("category", item.categoryName());
            row.put("tags", item.tags());
            row.put("content", clipOversizedChunk(item.contentMarkdown() == null ? "" : item.contentMarkdown()));
            row.put("attachments", item.attachments() == null ? List.of()
                    : item.attachments().stream().map(a -> Map.of(
                            "id", a.id(),
                            "name", a.originalFileName() == null ? "" : a.originalFileName()
                    )).toList());
            addSourceFromItem(sourceMap, item);
            return llmClient.writeValueAsString(Map.of("ok", true, "item", row));
        } catch (Exception e) {
            return "{\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
        }
    }

    /** get_param_table：参数表（条目内 Markdown 表格 / 按关键词搜参数类条目） */
    private String executeGetParamTable(String fnName, String argsJson, SkillDefinition skill,
                                        Map<String, Map<String, Object>> sourceMap) {
        if (skill.tools() != null && !skill.tools().isEmpty() && !skill.tools().contains("get_param_table")) {
            return "{\"error\":\"该技能不允许使用 get_param_table\"}";
        }
        try {
            JsonNode args = llmClient.parse(argsJson);
            long itemId = args.path("item_id").asLong(0);
            String query = args.path("query").asText("");
            List<Map<String, Object>> tables = new ArrayList<>();

            if (itemId > 0) {
                if (!inSkillScope(skill, String.valueOf(itemId))) {
                    return "{\"error\":\"该条目不在技能绑定范围内\"}";
                }
                var item = knowledgeItemService.get(itemId);
                collectTables(item, tables, sourceMap);
            } else {
                if (query == null || query.isBlank()) {
                    return "{\"error\":\"item_id 与 query 至少填一个\"}";
                }
                // 参数类条目：标题/类型/标签含「参数/规格/配置」，或直接按关键词搜
                var hits = knowledgeItemService.list(query, null, null, null);
                int n = 0;
                for (var item : hits) {
                    if (n >= 5) break;
                    String hay = ((item.title() == null ? "" : item.title()) + " "
                            + (item.type() == null ? "" : item.type()) + " "
                            + (item.tags() == null ? "" : item.tags())).toLowerCase();
                    boolean looksParam = hay.contains("参数") || hay.contains("规格") || hay.contains("配置")
                            || hay.contains("param") || hay.contains("spec");
                    if (!looksParam && !hay.contains(query.toLowerCase())) continue;
                    if (!inSkillScope(skill, String.valueOf(item.id()))) continue;
                    int before = tables.size();
                    collectTables(item, tables, sourceMap);
                    if (tables.size() > before) n++;
                }
            }
            if (tables.isEmpty()) {
                return llmClient.writeValueAsString(Map.of("ok", true, "count", 0,
                        "note", "未在条目正文中找到 Markdown 参数表格（| 列1 | 列2 | 形式）"));
            }
            return llmClient.writeValueAsString(Map.of("ok", true, "count", tables.size(), "tables", tables));
        } catch (Exception e) {
            return "{\"error\":\"" + String.valueOf(e.getMessage()).replace("\"", "'") + "\"}";
        }
    }

    /** 技能范围校验：绑定了分类/条目时，条目必须命中其一 */
    private boolean inSkillScope(SkillDefinition skill, String itemId) {
        List<String> scopeItems = skill.scopeItemIds();
        List<String> scopeCats = skill.scopeCategoryIds();
        boolean hasItemScope = scopeItems != null && !scopeItems.isEmpty();
        boolean hasCatScope = scopeCats != null && !scopeCats.isEmpty();
        if (!hasItemScope && !hasCatScope) return true;
        if (hasItemScope && scopeItems.contains(itemId)) return true;
        if (hasCatScope) {
            try {
                var item = knowledgeItemService.get(Long.parseLong(itemId));
                String path = item.categoryName() == null ? "" : item.categoryName();
                List<String> paths = resolveCategoryPaths(scopeCats);
                for (String p : paths) {
                    if (path.equals(p) || path.startsWith(p + "/") || p.startsWith(path)) return true;
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
        return !hasCatScope && hasItemScope;
    }

    private void collectTables(com.company.knowledge.dto.KnowledgeItemResponse item,
                               List<Map<String, Object>> tables,
                               Map<String, Map<String, Object>> sourceMap) {
        String md = item.contentMarkdown() == null ? "" : item.contentMarkdown();
        List<List<String>> cur = new ArrayList<>();
        for (String line : md.split("\n")) {
            String t = line.trim();
            if (t.startsWith("|") && t.endsWith("|")) {
                if (t.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty()) continue;
                List<String> cells = new ArrayList<>();
                for (String c : t.substring(1, t.length() - 1).split("\\|", -1)) {
                    cells.add(c.trim());
                }
                cur.add(cells);
            } else {
                flushTable(item, cur, tables, sourceMap);
            }
        }
        flushTable(item, cur, tables, sourceMap);
    }

    private void flushTable(com.company.knowledge.dto.KnowledgeItemResponse item,
                            List<List<String>> rows,
                            List<Map<String, Object>> tables,
                            Map<String, Map<String, Object>> sourceMap) {
        if (rows.size() < 2) {
            rows.clear();
            return;
        }
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("item_id", item.id());
        t.put("title", item.title());
        t.put("headers", rows.get(0));
        t.put("rows", rows.subList(1, rows.size()));
        tables.add(t);
        addSourceFromItem(sourceMap, item);
        rows.clear();
    }

    private void addSourceFromItem(Map<String, Map<String, Object>> sourceMap,
                                   com.company.knowledge.dto.KnowledgeItemResponse item) {
        String key = "item:" + item.id();
        if (sourceMap.containsKey(key)) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", item.title() == null ? "" : item.title());
        m.put("path", "knowledge_items/" + item.id());
        m.put("product", item.categoryName() == null ? "" : item.categoryName());
        m.put("score", 1.0);
        m.put("item_id", String.valueOf(item.id()));
        m.put("attachment_id", "");
        m.put("source_kind", "item");
        m.put("filename", "");
        m.put("file_path", "");
        m.put("page", "");
        m.put("locator", item.title() == null ? "" : item.title());
        sourceMap.put(key, m);
    }

    private List<String> resolveCategoryPaths(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        for (String idStr : categoryIds) {
            if (idStr == null || idStr.isBlank()) continue;
            try {
                Long id = Long.parseLong(idStr.trim());
                categoryRepository.findById(id).ifPresent(cat -> {
                    String path = textBuilder.categoryPath(cat);
                    if (path != null && !path.isBlank()) {
                        paths.add(path);
                    }
                });
            } catch (NumberFormatException ignore) {
                // 允许直接存路径
                paths.add(idStr.trim());
            }
        }
        return paths;
    }

    /**
     * 锚定核心实体：query 里较长的专有名词（产品/设备/型号）必须出现在结果中。
     * 泛词（功能/优势/特点/方案 等）不能单独作为保留条件。
     */
    private static List<RagSearchResult> filterByAnchorTerms(String query, List<RagSearchResult> results) {
        if (query == null || query.isBlank() || results == null || results.isEmpty()) {
            return results;
        }
        List<String> anchors = extractAnchorTerms(query);
        if (anchors.isEmpty()) {
            return results;
        }
        List<RagSearchResult> kept = new ArrayList<>();
        for (RagSearchResult r : results) {
            String hay = ((r.getTitle() == null ? "" : r.getTitle()) + " "
                    + (r.getSection() == null ? "" : r.getSection()) + " "
                    + (r.getText() == null ? "" : r.getText()) + " "
                    + (r.getFilename() == null ? "" : r.getFilename())).toLowerCase();
            for (String a : anchors) {
                if (hay.contains(a)) {
                    kept.add(r);
                    break;
                }
            }
        }
        // 若过滤后为空（表述差异大），保留原结果，避免误杀
        return kept.isEmpty() ? results : kept;
    }

    /** 从提问中取「核心实体」：≥3 字的词；更长的优先；2 字泛词不当锚点 */
    private static List<String> extractAnchorTerms(String query) {
        List<String> anchors = new ArrayList<>();
        for (String part : query.toLowerCase().split("[\\s,，。；;、/\\\\|]+")) {
            String p = part.trim();
            if (p.length() >= 3 && !GENERIC_TERMS.contains(p)) {
                anchors.add(p);
            }
        }
        // 长词优先（更特异）
        anchors.sort((a, b) -> b.length() - a.length());
        // 最多取 2 个锚点，避免过严
        return anchors.size() > 2 ? anchors.subList(0, 2) : anchors;
    }

    private static final Set<String> GENERIC_TERMS = Set.of(
            "功能", "优势", "特点", "方案", "说明", "问题", "要求", "情况", "内容",
            "注意", "风险", "隐患", "事项", "建议", "方法", "流程", "步骤", "简介",
            "什么", "如何", "怎么", "为什么", "有哪些", "哪些"
    );

    /**
     * 正常切块原样给模型。仅当块异常超长（索引/入库问题）时截断，避免拖垮上下文。
     */
    private static String clipOversizedChunk(String text) {
        if (text == null) return "";
        String t = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        final int max = 1600;
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "\n…[内容过长已截断，完整原文见出处]";
    }

    private static List<String> splitQueryTokens(String query) {
        List<String> tokens = new ArrayList<>();
        for (String part : query.split("[\\s,，。；;、/\\\\|]+")) {
            String p = part.trim();
            if (p.length() >= 2) {
                tokens.add(p);
            }
        }
        return tokens;
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
        // 对外展示：条目=标题，附件=文件名；不带「条目正文/附件」等固定前缀
        if ("attachment".equals(sourceKind)) {
            if (filename != null && !filename.isBlank() && !"null".equals(filename)) {
                return filename;
            }
            return title == null || title.isBlank() || "null".equals(title) ? "附件" : title;
        }
        return title == null || title.isBlank() || "null".equals(title) ? "知识条目" : title;
    }
}
