package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class TrainingRecordRequest {

    @NotBlank(message = "培训主题不能为空")
    private String title;

    private String content;

    @NotBlank(message = "培训时间不能为空")
    private LocalDateTime trainingDate;

    private String trainingType;

    private String trainer;

    private String participantIds;

    private String attachmentIds;

    private String remarks;

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
}
