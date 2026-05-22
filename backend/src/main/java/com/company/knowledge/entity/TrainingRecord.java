package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_training_record")
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime trainingDate;

    @Column(length = 50)
    private String trainingType; // 线下/会议

    @Column(length = 100)
    private String trainer;

    @Column(columnDefinition = "TEXT")
    private String participantIds; // JSON格式存储用户ID列表

    @Column(columnDefinition = "TEXT")
    private String attachmentIds; // JSON格式存储附件ID列表

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

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
