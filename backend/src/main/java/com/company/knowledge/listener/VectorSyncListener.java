package com.company.knowledge.listener;

import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;
import com.company.knowledge.event.AttachmentEvent;
import com.company.knowledge.event.KnowledgeItemEvent;
import com.company.knowledge.repository.AttachmentRepository;
import com.company.knowledge.repository.KnowledgeItemRepository;
import com.company.knowledge.service.AttachmentContentExtractor;
import com.company.knowledge.service.RagService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Listener for knowledge item and attachment events
 * Handles synchronization with vector database
 */
@Component
public class VectorSyncListener {

    private static final Logger logger = LoggerFactory.getLogger(VectorSyncListener.class);

    @Autowired
    private RagService ragService;

    @Autowired
    private KnowledgeItemRepository knowledgeItemRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private AttachmentContentExtractor contentExtractor;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Handle knowledge item events
     */
    @Async
    @EventListener
    public void handleKnowledgeItemEvent(KnowledgeItemEvent event) {
        logger.info("Handling knowledge item event: {}", event);

        try {
            switch (event.getType()) {
                case CREATED:
                case UPDATED:
                    syncKnowledgeItem(event.getItemId());
                    break;
                case DELETED:
                    ragService.deleteFromIndex(event.getItemId());
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to handle knowledge item event: {}", event, e);
        }
    }

    /**
     * Handle attachment events
     */
    @Async
    @EventListener
    public void handleAttachmentEvent(AttachmentEvent event) {
        logger.info("Handling attachment event: {}", event);

        try {
            switch (event.getType()) {
                case CREATED:
                    syncAttachment(event.getAttachmentId());
                    break;
                case DELETED:
                    ragService.deleteAttachmentFromIndex(event.getAttachmentId());
                    // 删除附件后，先删除父知识条目再重新同步（确保旧附件内容被清除）
                    if (event.getItemId() != null) {
                        logger.info("Deleting and re-syncing parent knowledge item {} after attachment deletion", event.getItemId());
                        ragService.deleteFromIndex(event.getItemId());
                        syncKnowledgeItem(event.getItemId());
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to handle attachment event: {}", event, e);
        }
    }

    /**
     * Sync a knowledge item and its attachments to vector database
     */
    private void syncKnowledgeItem(Long itemId) {
        // Use TransactionTemplate to run in a transaction and initialize lazy associations
        transactionTemplate.executeWithoutResult(status -> {
            KnowledgeItem item = entityManager.find(KnowledgeItem.class, itemId);
            if (item == null) {
                logger.warn("Knowledge item not found: {}", itemId);
                return;
            }

            // Force initialization of lazy associations while session is open
            item.getCategory().getName();
            if (item.getProject() != null) {
                item.getProject().getName();
            }
            
            logger.info("Syncing knowledge item: {} - {}", item.getId(), item.getTitle());

            // Get attachments for this item
            List<Attachment> attachments = attachmentRepository.findByItemIdOrderByUploadedAtDesc(itemId);
            logger.info("Found {} attachments for item {}", attachments.size(), itemId);
            
            // Extract content from attachments
            List<RagService.AttachmentContent> attachmentContents = new ArrayList<>();
            for (Attachment attachment : attachments) {
                try {
                    // Fix file path - convert relative to absolute if needed
                    String filePath = attachment.getFilePath();
                    logger.info("Attachment {} original filePath: {}", attachment.getId(), filePath);
                    
                    if (filePath != null && !filePath.startsWith("/") && !filePath.contains(":")) {
                        // Relative path, prepend uploads directory
                        filePath = System.getProperty("user.dir") + "/uploads/" + filePath;
                    }
                    logger.info("Attachment {} resolved filePath: {}", attachment.getId(), filePath);
                    
                    String content = contentExtractor.extractContent(
                            filePath,
                            attachment.getContentType()
                    );
                    logger.info("Attachment {} extracted content length: {}", 
                            attachment.getId(), content != null ? content.length() : 0);
                    
                    if (content != null && !content.trim().isEmpty()) {
                        attachmentContents.add(new RagService.AttachmentContent(
                                attachment.getId(),
                                attachment.getOriginalFileName(),
                                content,
                                attachment.getContentType()
                        ));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to extract content from attachment {}: {}",
                            attachment.getId(), e.getMessage(), e);
                }
            }

            // Sync to vector database
            if (attachmentContents.isEmpty()) {
                ragService.syncKnowledgeItem(item);
            } else {
                ragService.syncKnowledgeItemWithAttachments(item, attachmentContents);
            }

            logger.info("Successfully synced knowledge item {} with {} attachments",
                    itemId, attachmentContents.size());
        });
    }

    /**
     * Sync a single attachment to vector database
     */
    private void syncAttachment(Long attachmentId) {
        transactionTemplate.executeWithoutResult(status -> {
            Attachment attachment = entityManager.find(Attachment.class, attachmentId);
            if (attachment == null) {
                logger.warn("Attachment not found: {}", attachmentId);
                return;
            }

            logger.info("Syncing attachment: {} - {}", attachment.getId(), attachment.getOriginalFileName());

            try {
                // Fix file path - convert relative to absolute if needed
                String filePath = attachment.getFilePath();
                if (filePath != null && !filePath.startsWith("/") && !filePath.contains(":")) {
                    // Relative path, prepend uploads directory
                    filePath = System.getProperty("user.dir") + "/uploads/" + filePath;
                }
                
                String content = contentExtractor.extractContent(
                        filePath,
                        attachment.getContentType()
                );

                if (content != null && !content.trim().isEmpty()) {
                    ragService.syncAttachment(attachment, content);
                    logger.info("Successfully synced attachment {}", attachmentId);
                } else {
                    logger.debug("No content extracted from attachment {}", attachmentId);
                }
            } catch (Exception e) {
                logger.error("Failed to sync attachment {}", attachmentId, e);
            }
        });
    }
}