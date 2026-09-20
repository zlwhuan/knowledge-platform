package com.company.knowledge.event;

/**
 * Event for knowledge item changes
 */
public class KnowledgeItemEvent {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Type type;
    private final Long itemId;
    private final String title;
    private final Long timestamp;

    public KnowledgeItemEvent(Type type, Long itemId, String title) {
        this.type = type;
        this.itemId = itemId;
        this.title = title;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "KnowledgeItemEvent{" +
                "type=" + type +
                ", itemId=" + itemId +
                ", title='" + title + '\'' +
                '}';
    }
}