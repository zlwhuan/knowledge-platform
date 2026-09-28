package com.company.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 LLM 客户端（小米 MiMo / 其他兼容端点）
 */
@Service
public class LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(LlmClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${llm.api.base-url:${LLM_API_BASE_URL:}}")
    private String baseUrl;

    @Value("${llm.api.key:${LLM_API_KEY:}}")
    private String apiKey;

    @Value("${llm.model:${LLM_MODEL:}}")
    private String model;

    @Value("${llm.timeout-seconds:90}")
    private int timeoutSeconds;

    public LlmClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** 清洗密钥：去空白/引号/误粘贴的 Bearer 前缀 */
    private String cleanKey(String raw) {
        if (raw == null) return "";
        String k = raw.trim();
        if ((k.startsWith("\"") && k.endsWith("\"")) || (k.startsWith("'") && k.endsWith("'"))) {
            k = k.substring(1, k.length() - 1).trim();
        }
        if (k.regionMatches(true, 0, "bearer ", 0, 7)) {
            k = k.substring(7).trim();
        }
        return k;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && !cleanKey(apiKey).isBlank()
                && model != null && !model.isBlank();
    }

    public String getModel() {
        return model;
    }

    /** 前端可选的模型名 → 实际请求模型 ID */
    private String resolveModel(String modelOverride) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            return mapModelName(modelOverride.trim());
        }
        return model == null ? "" : model.trim();
    }

    /** UI 展示名映射到网关模型 ID；未知名原样透传 */
    private String mapModelName(String name) {
        String n = name.trim();
        // 展示名 → 网关 ID（与 Start-Backend / LLM_MODEL 保持一致）
        switch (n) {
            case "MiMo V2.6 Flash":
            case "mimo-v2.6-flash":
                return "mimo-v2.6-flash";
            case "MiMo V2.6 Pro":
            case "mimo-v2.6-pro":
                return "mimo-v2.6-pro";
            case "MiMo V2.5":
            case "mimo-v2.5":
                return "mimo-v2.5";
            default:
                return n;
        }
    }

    public String keyFingerprint() {
        String k = cleanKey(apiKey);
        if (k.isBlank()) return "(empty)";
        String prefix = k.length() <= 4 ? "****" : k.substring(0, 4);
        return prefix + "****(len=" + k.length() + ")";
    }

    /**
     * 发起一次 chat.completions（含可选 tools），返回完整 JSON 节点
     */
    public JsonNode chatCompletion(JsonNode messages, JsonNode tools) {
        return chatCompletion(messages, tools, null);
    }

    /** modelOverride：会话内切换模型；为空则用默认 LLM_MODEL */
    public JsonNode chatCompletion(JsonNode messages, JsonNode tools, String modelOverride) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM 未配置：请设置 LLM_API_BASE_URL / LLM_API_KEY / LLM_MODEL");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", resolveModel(modelOverride));
        body.put("temperature", 0.3);
        body.set("messages", messages);
        if (tools != null && tools.isArray() && tools.size() > 0) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        String url = trimSlash(baseUrl) + "/chat/completions";
        String payload = body.toString();

        // 先用标准 Bearer；401 时再试网关常见兼容头
        try {
            return doPost(url, payload, buildAuthHeaders("bearer"));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                logger.warn("Bearer auth failed (401), retrying with alternate auth headers");
                try {
                    return doPost(url, payload, buildAuthHeaders("x-api-key"));
                } catch (IllegalStateException e2) {
                    if (e2.getMessage() != null && e2.getMessage().contains("401")) {
                        return doPost(url, payload, buildAuthHeaders("api-key"));
                    }
                    throw e2;
                }
            }
            throw e;
        }
    }

    private HttpHeaders buildAuthHeaders(String mode) {
        String key = cleanKey(apiKey);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        switch (mode) {
            case "x-api-key" -> {
                headers.set("x-api-key", key);
                headers.setBearerAuth(key);
            }
            case "api-key" -> {
                headers.set("api-key", key);
                headers.setBearerAuth(key);
            }
            default -> headers.setBearerAuth(key);
        }
        return headers;
    }

    private JsonNode doPost(String url, String payload, HttpHeaders headers) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(payload, headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            int status = e.getStatusCode().value();
            logger.error("LLM HTTP {}: {}", status, body);
            throw new IllegalStateException("模型调用失败 HTTP " + status + "：" + shortBody(body));
        } catch (Exception e) {
            logger.error("LLM call failed: {}", e.getMessage());
            throw new IllegalStateException("模型调用失败：" + e.getMessage());
        }
    }

    private String shortBody(String body) {
        if (body == null || body.isBlank()) return "(无响应体)";
        String b = body.trim();
        return b.length() > 300 ? b.substring(0, 300) + "…" : b;
    }

    private String trimSlash(String url) {
        if (url == null) return "";
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    /**
     * 流式 chat.completions：回调 onDelta 推送增量文本，返回完整 JSON（含 tool_calls）。
     * 无工具轮次时前端可逐字展示；有 tool_calls 时走完整 JSON 继续循环。
     */
    public JsonNode chatCompletionStream(JsonNode messages, JsonNode tools, java.util.function.Consumer<String> onDelta) {
        return chatCompletionStream(messages, tools, null, onDelta);
    }

    /** modelOverride：会话内切换模型；为空则用默认 LLM_MODEL */
    public JsonNode chatCompletionStream(JsonNode messages, JsonNode tools, String modelOverride,
                                         java.util.function.Consumer<String> onDelta) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM 未配置：请设置 LLM_API_BASE_URL / LLM_API_KEY / LLM_MODEL");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", resolveModel(modelOverride));
        body.put("temperature", 0.3);
        body.put("stream", true);
        body.set("messages", messages);
        if (tools != null && tools.isArray() && tools.size() > 0) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        String url = trimSlash(baseUrl) + "/chat/completions";
        String key = cleanKey(apiKey);

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(Math.max(timeoutSeconds, 120)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key)
                .header("Accept", "text/event-stream")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        StringBuilder contentBuf = new StringBuilder();
        // tool_calls 按 index 聚合
        java.util.Map<Integer, ObjectNode> toolAcc = new java.util.TreeMap<>();

        try {
            java.net.http.HttpResponse<java.util.stream.Stream<String>> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() >= 400) {
                String err = response.body().collect(java.util.stream.Collectors.joining("\n"));
                throw new IllegalStateException("模型调用失败 HTTP " + response.statusCode() + "：" + shortBody(err));
            }

            response.body().forEach(line -> {
                if (line == null || line.isBlank()) return;
                String data = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
                if (data.isEmpty() || "[DONE]".equals(data)) return;
                JsonNode chunk;
                try {
                    chunk = objectMapper.readTree(data);
                } catch (Exception ignore) {
                    return;
                }
                JsonNode delta = chunk.path("choices").path(0).path("delta");

                if (delta.hasNonNull("content")) {
                    String piece = delta.get("content").asText("");
                    if (!piece.isEmpty()) {
                        contentBuf.append(piece);
                        if (onDelta != null) onDelta.accept(piece);
                    }
                }

                JsonNode toolDeltas = delta.path("tool_calls");
                if (toolDeltas.isArray()) {
                    for (JsonNode td : toolDeltas) {
                        int idx = td.path("index").asInt(0);
                        ObjectNode acc = toolAcc.computeIfAbsent(idx, i -> {
                            ObjectNode n = objectMapper.createObjectNode();
                            ObjectNode fn = objectMapper.createObjectNode();
                            fn.put("name", "");
                            fn.put("arguments", "");
                            n.put("id", "call_" + i);
                            n.put("type", "function");
                            n.set("function", fn);
                            return n;
                        });
                        if (td.hasNonNull("id")) {
                            acc.put("id", td.get("id").asText());
                        }
                        JsonNode fnDelta = td.path("function");
                        if (fnDelta.hasNonNull("name")) {
                            String name = fnDelta.get("name").asText();
                            if (!name.isEmpty()) {
                                acc.with("function").put("name", name);
                            }
                        }
                        if (fnDelta.hasNonNull("arguments")) {
                            String args = fnDelta.get("arguments").asText("");
                            if (!args.isEmpty()) {
                                String prev = acc.with("function").path("arguments").asText("");
                                acc.with("function").put("arguments", prev + args);
                            }
                        }
                    }
                }
            });

            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "assistant");
            message.put("content", contentBuf.toString());
            if (!toolAcc.isEmpty()) {
                ArrayNode calls = objectMapper.createArrayNode();
                toolAcc.values().forEach(calls::add);
                message.set("tool_calls", calls);
            }

            ObjectNode choice = objectMapper.createObjectNode();
            choice.set("message", message);
            ArrayNode choices = objectMapper.createArrayNode();
            choices.add(choice);
            ObjectNode result = objectMapper.createObjectNode();
            result.set("choices", choices);
            result.put("streamed", true);
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("LLM stream call failed: {}", e.getMessage());
            throw new IllegalStateException("模型调用失败：" + e.getMessage());
        }
    }

    public ArrayNode newArray() {
        return objectMapper.createArrayNode();
    }

    public ObjectNode newObject() {
        return objectMapper.createObjectNode();
    }

    public JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    public String writeValueAsString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.nullNode();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, Map.class);
    }

    public ObjectMapper mapper() {
        return objectMapper;
    }

    public List<String> configuredInfo() {
        List<String> info = new ArrayList<>();
        info.add("base_url=" + (baseUrl == null || baseUrl.isBlank() ? "(empty)" : baseUrl.trim()));
        info.add("model=" + (model == null || model.isBlank() ? "(empty)" : model.trim()));
        info.add("api_key=" + keyFingerprint());
        return info;
    }
}
