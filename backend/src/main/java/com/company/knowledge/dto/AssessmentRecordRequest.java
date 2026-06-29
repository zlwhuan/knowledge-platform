package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AssessmentRecordRequest {

    @NotBlank(message = "考核主题不能为空")
    private String title;

    private String assessmentType;

    @NotNull(message = "考核时间不能为空")
    private LocalDateTime assessmentDate;

    private String assessorIds;

    private String grade;

    private String evaluation;

    private Long trainingRecordId;

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
}
