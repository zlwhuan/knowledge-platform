package com.company.knowledge.event;

/**
 * Event for attachment changes
 */
public class AttachmentEvent {

    public enum Type {
        CREATED,
        DELETED
    }

    private final Type type;
    private final Long attachmentId;
    private final Long itemId;
    private final String filename;
    private final Long timestamp;

    public AttachmentEvent(Type type, Long attachmentId, Long itemId, String filename) {
        this.type = type;
        this.attachmentId = attachmentId;
        this.itemId = itemId;
        this.filename = filename;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() {
        return type;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getFilename() {
        return filename;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "AttachmentEvent{" +
                "type=" + type +
                ", attachmentId=" + attachmentId +
                ", itemId=" + itemId +
                ", filename='" + filename + '\'' +
                '}';
    }
}