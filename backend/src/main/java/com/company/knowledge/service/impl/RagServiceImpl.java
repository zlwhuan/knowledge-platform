package com.company.knowledge.service.impl;

import com.company.knowledge.dto.RagSearchResult;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;
import com.company.knowledge.service.KnowledgeItemVectorTextBuilder;
import com.company.knowledge.service.RagService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Implementation of RagService that communicates with Python RAG API service
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    @Value("${rag.service.url:http://localhost:8081}")
    private String ragServiceUrl;

    @Value("${rag.service.timeout:30000}")
    private int timeout;

    @Value("${rag.service.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeItemVectorTextBuilder textBuilder;

    public RagServiceImpl(KnowledgeItemVectorTextBuilder textBuilder) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.textBuilder = textBuilder;
    }

    /** 统一元数据：增量同步与全库重建共用（列注释文本 + 分类树） */
    private void putItemMeta(Map<String, Object> request, KnowledgeItem item) {
        request.put("item_id", item.getId().toString());
        request.put("title", item.getTitle() != null ? item.getTitle() : "");
        // 列注释 + 列值，与全库重建完全一致
        request.put("content", textBuilder.build(item));
        request.put("category", textBuilder.categoryPath(item));
        request.put("project", textBuilder.projectName(item));
        request.put("tags", parseTags(item.getTags()));
        request.put("source", item.getSource() != null ? item.getSource() : "");
        // doc_type 用根分类名，替代旧 products/faq/meeting/competitor
        request.put("doc_type", textBuilder.rootCategoryName(item));
    }

    @Override
    public void syncKnowledgeItem(KnowledgeItem item) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping sync for item {}", item.getId());
            return;
        }

        try {
            Map<String, Object> request = new HashMap<>();
            putItemMeta(request, item);
            request.put("attachments", Collections.emptyList());

            String url = ragServiceUrl + "/api/rag/sync";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully synced knowledge item {} to vector index", item.getId());
            } else {
                logger.warn("Failed to sync knowledge item {}: {}", item.getId(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to sync knowledge item {}", item.getId(), e);
        }
    }

    @Override
    public void syncKnowledgeItemWithAttachments(KnowledgeItem item, List<AttachmentContent> attachments) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping sync for item {}", item.getId());
            return;
        }

        try {
            Map<String, Object> request = new HashMap<>();
            putItemMeta(request, item);

            List<Map<String, Object>> attachmentsList = new ArrayList<>();
            for (AttachmentContent att : attachments) {
                if (att.getContent() != null && !att.getContent().trim().isEmpty()) {
                    Map<String, Object> attMap = new HashMap<>();
                    attMap.put("attachment_id", att.getAttachmentId() != null ? String.valueOf(att.getAttachmentId()) : "");
                    attMap.put("filename", att.getFilename());
                    attMap.put("file_path", att.getFilePath() != null ? att.getFilePath() : "");
                    // 附件文本也走列注释格式（含分类树）
                    attMap.put("content", textBuilder.buildAttachmentText(
                            item, stubAttachment(att), att.getContent()));
                    attMap.put("content_type", att.getContentType());
                    attachmentsList.add(attMap);
                }
            }
            request.put("attachments", attachmentsList);

            String url = ragServiceUrl + "/api/rag/sync";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                Integer chunksCreated = body != null ? (Integer) body.get("chunks_created") : null;
                logger.info("Successfully synced knowledge item {} with {} attachments ({} chunks) to vector index",
                        item.getId(), attachments.size(), chunksCreated);
            } else {
                logger.warn("Failed to sync knowledge item {}: {}", item.getId(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to sync knowledge item {} with attachments", item.getId(), e);
        }
    }

    @Override
    public void syncAttachment(Attachment attachment, String content) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping sync for attachment {}", attachment.getId());
            return;
        }

        try {
            KnowledgeItem item = attachment.getItem();
            Map<String, Object> request = new HashMap<>();
            request.put("item_id", "attachment_" + attachment.getId());
            request.put("title", attachment.getOriginalFileName());
            request.put("content", textBuilder.buildAttachmentText(item, attachment, content));
            request.put("category", item != null ? textBuilder.categoryPath(item) : "");
            request.put("project", item != null ? textBuilder.projectName(item) : "");
            request.put("tags", Collections.emptyList());
            request.put("source", "attachment");
            request.put("doc_type", item != null ? textBuilder.rootCategoryName(item) : "attachment");
            request.put("attachments", Collections.emptyList());

            String url = ragServiceUrl + "/api/rag/sync";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully synced attachment {} to vector index", attachment.getId());
            } else {
                logger.warn("Failed to sync attachment {}: {}", attachment.getId(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to sync attachment {}", attachment.getId(), e);
        }
    }

    private Attachment stubAttachment(AttachmentContent att) {
        Attachment stub = new Attachment();
        stub.setOriginalFileName(att.getFilename());
        stub.setContentType(att.getContentType());
        return stub;
    }

    @Override
    public void deleteFromIndex(Long itemId) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping delete for item {}", itemId);
            return;
        }

        try {
            String url = ragServiceUrl + "/api/rag/" + itemId;
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted knowledge item {} from vector index", itemId);
            } else {
                logger.warn("Failed to delete knowledge item {}: {}", itemId, response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to delete knowledge item {}", itemId, e);
        }
    }

    @Override
    public void deleteAttachmentFromIndex(Long attachmentId) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping delete for attachment {}", attachmentId);
            return;
        }

        try {
            String url = ragServiceUrl + "/api/rag/attachment_" + attachmentId;
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully deleted attachment {} from vector index", attachmentId);
            } else {
                logger.warn("Failed to delete attachment {}: {}", attachmentId, response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to delete attachment {}", attachmentId, e);
        }
    }

    @Override
    public List<RagSearchResult> search(String query, int topK, String category, String docType) {
        if (!enabled) {
            logger.debug("RAG service is disabled, returning empty results");
            return Collections.emptyList();
        }

        try {
            // Build search request
            Map<String, Object> request = new HashMap<>();
            request.put("query", query);
            request.put("top_k", topK);
            if (category != null && !category.isEmpty()) {
                request.put("category", category);
            }
            if (docType != null && !docType.isEmpty()) {
                request.put("doc_type", docType);
            }

            // Call RAG service
            String url = ragServiceUrl + "/api/rag/search";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<List<RagSearchResult>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<List<RagSearchResult>>() {});

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("RAG search returned {} results for query: {}", response.getBody().size(), query);
                return response.getBody();
            } else {
                logger.warn("RAG search failed: {}", response.getBody());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("RAG search failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<RagSearchResult> search(String query) {
        return search(query, 8, null, null);
    }

    @Override
    public void rebuildIndex() {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping rebuild");
            return;
        }

        try {
            // keep_existing_platform=true：vault 重建时保留已同步的平台切块
            Map<String, Object> request = new HashMap<>();
            request.put("keep_existing_platform", true);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    ragServiceUrl + "/api/rag/rebuild-full", HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully triggered full index rebuild");
            } else {
                logger.warn("Failed to trigger rebuild: {}", response.getBody());
            }
        } catch (Exception e) {
            logger.error("Failed to trigger rebuild", e);
        }
    }

    @Override
    public boolean isHealthy() {
        if (!enabled) {
            return false;
        }

        try {
            String url = ragServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("RAG service health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getStatus() {
        if (!enabled) {
            return "{\"status\":\"disabled\"}";
        }

        try {
            String url = ragServiceUrl + "/api/rag/status";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to get RAG status", e);
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public Map<String, Object> getStatusMap() {
        if (!enabled) {
            Map<String, Object> disabled = new HashMap<>();
            disabled.put("status", "disabled");
            disabled.put("enabled", false);
            return disabled;
        }
        try {
            String url = ragServiceUrl + "/api/rag/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody() != null ? new HashMap<>(response.getBody()) : new HashMap<>();
            body.put("enabled", true);
            body.put("serviceUrl", ragServiceUrl);
            return body;
        } catch (Exception e) {
            logger.error("Failed to get RAG status map", e);
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("enabled", true);
            err.put("message", e.getMessage());
            err.put("serviceUrl", ragServiceUrl);
            return err;
        }
    }

    @Override
    public Map<String, Object> listSources(String category, String docType) {
        if (!enabled) {
            return Map.of("total", 0, "sources", List.of());
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(ragServiceUrl + "/api/rag/sources");
            if (category != null && !category.isEmpty()) builder.queryParam("category", category);
            if (docType != null && !docType.isEmpty()) builder.queryParam("doc_type", docType);
            ResponseEntity<Map> response = restTemplate.getForEntity(builder.toUriString(), Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("total", 0, "sources", List.of());
        } catch (Exception e) {
            logger.error("Failed to list sources", e);
            return Map.of("total", 0, "sources", List.of(), "error", String.valueOf(e.getMessage()));
        }
    }

    @Override
    public Map<String, Object> listChunks(String path, String q, String docType, int limit, int offset) {
        if (!enabled) {
            return Map.of("total", 0, "items", List.of());
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(ragServiceUrl + "/api/rag/chunks")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset);
            if (path != null && !path.isEmpty()) builder.queryParam("path", path);
            if (q != null && !q.isEmpty()) builder.queryParam("q", q);
            if (docType != null && !docType.isEmpty()) builder.queryParam("doc_type", docType);
            ResponseEntity<Map> response = restTemplate.getForEntity(builder.toUriString(), Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("total", 0, "items", List.of());
        } catch (Exception e) {
            logger.error("Failed to list chunks", e);
            return Map.of("total", 0, "items", List.of(), "error", String.valueOf(e.getMessage()));
        }
    }

    @Override
    public Map<String, Object> getChunk(String chunkId) {
        if (!enabled) {
            throw new IllegalStateException("RAG service is disabled");
        }
        try {
            String url = ragServiceUrl + "/api/rag/chunks/" + chunkId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (Exception e) {
            logger.error("Failed to get chunk {}", chunkId, e);
            throw new IllegalStateException("获取切块失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> deleteChunk(String chunkId) {
        if (!enabled) {
            throw new IllegalStateException("RAG service is disabled");
        }
        try {
            String url = ragServiceUrl + "/api/rag/chunks/" + chunkId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("status", "success");
        } catch (Exception e) {
            logger.error("Failed to delete chunk {}", chunkId, e);
            throw new IllegalStateException("删除切块失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> deleteSource(String path) {
        if (!enabled) {
            throw new IllegalStateException("RAG service is disabled");
        }
        try {
            String url = UriComponentsBuilder.fromUriString(ragServiceUrl + "/api/rag/sources")
                    .queryParam("path", path)
                    .toUriString();
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("status", "success");
        } catch (Exception e) {
            logger.error("Failed to delete source {}", path, e);
            throw new IllegalStateException("移出索引失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> reindex(String path, String mode) {
        if (!enabled) {
            throw new IllegalStateException("RAG service is disabled");
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("path", path == null ? "" : path);
            request.put("mode", mode == null || mode.isEmpty() ? "source" : mode);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    ragServiceUrl + "/api/rag/reindex", HttpMethod.POST, entity, Map.class);
            return response.getBody() != null ? response.getBody() : Map.of("status", "success");
        } catch (Exception e) {
            logger.error("Failed to reindex path={}", path, e);
            throw new IllegalStateException("重建索引失败: " + e.getMessage());
        }
    }

    /**
     * Parse tags string to list
     */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(tags.split(","));
    }
}