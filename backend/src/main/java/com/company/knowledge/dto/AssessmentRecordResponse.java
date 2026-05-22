package com.company.knowledge.dto;

import java.time.LocalDateTime;

public class AssessmentRecordResponse {

    private Long id;
    private String title;
    private String assessmentType;
    private LocalDateTime assessmentDate;
    private String assessorIds;
    private String grade;
    private String evaluation;
    private Long trainingRecordId;
    private String trainingRecordTitle;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public AssessmentRecordResponse() {}

    public AssessmentRecordResponse(Long id, String title, String assessmentType, LocalDateTime assessmentDate,
                                    String assessorIds, String grade, String evaluation,
                                    Long trainingRecordId, String trainingRecordTitle, String createdBy,
                                    LocalDateTime createdAt, String updatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.assessmentType = assessmentType;
        this.assessmentDate = assessmentDate;
        this.assessorIds = assessorIds;
        this.grade = grade;
        this.evaluation = evaluation;
        this.trainingRecordId = trainingRecordId;
        this.trainingRecordTitle = trainingRecordTitle;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAssessmentType() { return assessmentType; }
    public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }
    public LocalDateTime getAssessmentDate() { return assessmentDate; }
    public void setAssessmentDate(LocalDateTime assessmentDate) { this.assessmentDate = assessmentDate; }
    public String getAssessorIds() { return assessorIds; }
    public void setAssessorIds(String assessorIds) { this.assessorIds = assessorIds; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getEvaluation() { return evaluation; }
    public void setEvaluation(String evaluation) { this.evaluation = evaluation; }
    public Long getTrainingRecordId() { return trainingRecordId; }
    public void setTrainingRecordId(Long trainingRecordId) { this.trainingRecordId = trainingRecordId; }
    public String getTrainingRecordTitle() { return trainingRecordTitle; }
    public void setTrainingRecordTitle(String trainingRecordTitle) { this.trainingRecordTitle = trainingRecordTitle; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
