package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_assessment_record")
public class AssessmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String assessmentType; // 模拟售前/本地测试

    @Column(nullable = false)
    private LocalDateTime assessmentDate;

    @Column(columnDefinition = "TEXT")
    private String assessorIds; // JSON格式存储被考核人员ID列表

    @Column(length = 10)
    private String grade; // A/B/C/D

    @Column(columnDefinition = "TEXT")
    private String evaluation;

    @Column
    private Long trainingRecordId; // 关联的培训记录ID

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
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
