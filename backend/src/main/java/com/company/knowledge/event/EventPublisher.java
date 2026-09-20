package com.company.knowledge.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publisher for knowledge platform events
 */
@Component
public class EventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * Publish a knowledge item created event
     */
    public void publishKnowledgeItemCreated(Long itemId, String title) {
        KnowledgeItemEvent event = new KnowledgeItemEvent(
                KnowledgeItemEvent.Type.CREATED, itemId, title);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish a knowledge item updated event
     */
    public void publishKnowledgeItemUpdated(Long itemId, String title) {
        KnowledgeItemEvent event = new KnowledgeItemEvent(
                KnowledgeItemEvent.Type.UPDATED, itemId, title);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish a knowledge item deleted event
     */
    public void publishKnowledgeItemDeleted(Long itemId, String title) {
        KnowledgeItemEvent event = new KnowledgeItemEvent(
                KnowledgeItemEvent.Type.DELETED, itemId, title);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish an attachment created event
     */
    public void publishAttachmentCreated(Long attachmentId, Long itemId, String filename) {
        AttachmentEvent event = new AttachmentEvent(
                AttachmentEvent.Type.CREATED, attachmentId, itemId, filename);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish an attachment deleted event
     */
    public void publishAttachmentDeleted(Long attachmentId, Long itemId, String filename) {
        AttachmentEvent event = new AttachmentEvent(
                AttachmentEvent.Type.DELETED, attachmentId, itemId, filename);
        applicationEventPublisher.publishEvent(event);
    }
}