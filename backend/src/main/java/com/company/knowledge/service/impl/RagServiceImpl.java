package com.company.knowledge.service.impl;

import com.company.knowledge.dto.RagSearchResult;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;
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

    public RagServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void syncKnowledgeItem(KnowledgeItem item) {
        if (!enabled) {
            logger.debug("RAG service is disabled, skipping sync for item {}", item.getId());
            return;
        }

        try {
            // Build sync request
            Map<String, Object> request = new HashMap<>();
            request.put("item_id", item.getId().toString());
            request.put("title", item.getTitle());
            request.put("content", item.getContentMarkdown() != null ? item.getContentMarkdown() : "");
            request.put("category", item.getCategory() != null ? item.getCategory().getName() : "");
            request.put("project", item.getProject() != null ? item.getProject().getName() : "");
            request.put("tags", parseTags(item.getTags()));
            request.put("source", item.getSource() != null ? item.getSource() : "");
            request.put("doc_type", "products");
            request.put("attachments", Collections.emptyList());

            // Call RAG service
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
            // Build sync request with attachments
            Map<String, Object> request = new HashMap<>();
            request.put("item_id", item.getId().toString());
            request.put("title", item.getTitle());
            request.put("content", item.getContentMarkdown() != null ? item.getContentMarkdown() : "");
            request.put("category", item.getCategory() != null ? item.getCategory().getName() : "");
            request.put("project", item.getProject() != null ? item.getProject().getName() : "");
            request.put("tags", parseTags(item.getTags()));
            request.put("source", item.getSource() != null ? item.getSource() : "");
            request.put("doc_type", "products");

            // Build attachments list
            List<Map<String, Object>> attachmentsList = new ArrayList<>();
            for (AttachmentContent att : attachments) {
                if (att.getContent() != null && !att.getContent().trim().isEmpty()) {
                    Map<String, Object> attMap = new HashMap<>();
                    attMap.put("filename", att.getFilename());
                    attMap.put("content", att.getContent());
                    attMap.put("content_type", att.getContentType());
                    attachmentsList.add(attMap);
                }
            }
            request.put("attachments", attachmentsList);

            // Call RAG service
            String url = ragServiceUrl + "/api/rag/sync";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                Integer chunksCreated = (Integer) body.get("chunks_created");
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
            // Build sync request for attachment
            Map<String, Object> request = new HashMap<>();
            request.put("item_id", "attachment_" + attachment.getId());
            request.put("title", attachment.getOriginalFileName());
            request.put("content", content);
            request.put("category", attachment.getItem() != null ? 
                    (attachment.getItem().getCategory() != null ? attachment.getItem().getCategory().getName() : "") : "");
            request.put("tags", Collections.emptyList());
            request.put("source", "attachment");
            request.put("doc_type", "attachment");
            request.put("attachments", Collections.emptyList());

            // Call RAG service
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
            String url = ragServiceUrl + "/api/rag/rebuild";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

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