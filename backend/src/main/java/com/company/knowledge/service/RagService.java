package com.company.knowledge.service;

import com.company.knowledge.dto.RagSearchResult;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;

import java.util.List;
import java.util.Map;

/**
 * RAG (Retrieval-Augmented Generation) Service interface
 * Handles synchronization of knowledge items to vector database
 */
public interface RagService {

    /**
     * Sync a single knowledge item to vector database
     * @param item The knowledge item to sync
     */
    void syncKnowledgeItem(KnowledgeItem item);

    /**
     * Sync a knowledge item with its attachments to vector database
     * @param item The knowledge item
     * @param attachments List of attachments with extracted content
     */
    void syncKnowledgeItemWithAttachments(KnowledgeItem item, List<AttachmentContent> attachments);

    /**
     * Sync an attachment to vector database
     * @param attachment The attachment to sync
     * @param content Extracted text content from the attachment
     */
    void syncAttachment(Attachment attachment, String content);

    /**
     * Delete a knowledge item from vector database
     * @param itemId The ID of the knowledge item to delete
     */
    void deleteFromIndex(Long itemId);

    /**
     * Delete an attachment from vector database
     * @param attachmentId The ID of the attachment to delete
     */
    void deleteAttachmentFromIndex(Long attachmentId);

    /**
     * Search the knowledge base
     * @param query The search query
     * @param topK Number of results to return
     * @param category Optional category filter
     * @param docType Optional document type filter
     * @return List of search results
     */
    List<RagSearchResult> search(String query, int topK, String category, String docType);

    /**
     * Search the knowledge base with default parameters
     * @param query The search query
     * @return List of search results
     */
    List<RagSearchResult> search(String query);

    /**
     * Trigger a full rebuild of the vector index
     */
    void rebuildIndex();

    /**
     * Check if the RAG service is available
     * @return true if service is healthy
     */
    boolean isHealthy();

    /**
     * Get the status of the RAG service
     * @return Status information as JSON string
     */
    String getStatus();

    /**
     * Get structured status map for the vector admin UI
     */
    Map<String, Object> getStatusMap();

    /**
     * List indexed sources with chunk counts
     */
    Map<String, Object> listSources(String category, String docType);

    /**
     * List indexed chunks for maintenance
     */
    Map<String, Object> listChunks(String path, String q, String docType, int limit, int offset);

    /**
     * Get one chunk by id
     */
    Map<String, Object> getChunk(String chunkId);

    /**
     * Delete one chunk from the index
     */
    Map<String, Object> deleteChunk(String chunkId);

    /**
     * Delete all chunks of a source path
     */
    Map<String, Object> deleteSource(String path);

    /**
     * Reindex a source path or the full store
     * @param path source path (optional when mode=full)
     * @param mode source | full
     */
    Map<String, Object> reindex(String path, String mode);

    /**
     * Content holder for attachment text extraction
     */
    class AttachmentContent {
        private Long attachmentId;
        private String filename;
        private String content;
        private String contentType;
        private String filePath;

        public AttachmentContent() {}

        public AttachmentContent(Long attachmentId, String filename, String content, String contentType) {
            this(attachmentId, filename, content, contentType, null);
        }

        public AttachmentContent(Long attachmentId, String filename, String content, String contentType, String filePath) {
            this.attachmentId = attachmentId;
            this.filename = filename;
            this.content = content;
            this.contentType = contentType;
            this.filePath = filePath;
        }

        public Long getAttachmentId() {
            return attachmentId;
        }

        public void setAttachmentId(Long attachmentId) {
            this.attachmentId = attachmentId;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }
}