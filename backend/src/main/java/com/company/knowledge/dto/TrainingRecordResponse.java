package com.company.knowledge.dto;

import java.time.LocalDateTime;

public class TrainingRecordResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime trainingDate;
    private String trainingType;
    private String trainer;
    private String participantIds;
    private String attachmentIds;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public TrainingRecordResponse() {}

    public TrainingRecordResponse(Long id, String title, String content, LocalDateTime trainingDate,
                                   String trainingType, String trainer, String participantIds,
                                   String attachmentIds, String remarks, String createdBy,
                                   LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.trainingDate = trainingDate;
        this.trainingType = trainingType;
        this.trainer = trainer;
        this.participantIds = participantIds;
        this.attachmentIds = attachmentIds;
        this.remarks = remarks;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTrainingDate() { return trainingDate; }
    public void setTrainingDate(LocalDateTime trainingDate) { this.trainingDate = trainingDate; }
    public String getTrainingType() { return trainingType; }
    public void setTrainingType(String trainingType) { this.trainingType = trainingType; }
    public String getTrainer() { return trainer; }
    public void setTrainer(String trainer) { this.trainer = trainer; }
    public String getParticipantIds() { return participantIds; }
    public void setParticipantIds(String participantIds) { this.participantIds = participantIds; }
    public String getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(String attachmentIds) { this.attachmentIds = attachmentIds; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
