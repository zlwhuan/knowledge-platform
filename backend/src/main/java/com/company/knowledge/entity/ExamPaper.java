package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_paper")
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String category; // 销售、售前、实施、通用

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Integer totalScore = 100; // 总分

    @Column(nullable = false)
    private Integer passScore = 60; // 及格分数

    @Column(nullable = false)
    private Integer duration = 60; // 考试时长（分钟）

    @Column(nullable = false)
    private Integer questionCount = 0; // 题目数量

    @Column(nullable = false)
    private Boolean randomOrder = true; // 是否随机排序

    @Column(nullable = false)
    private Boolean showAnswer = false; // 考试后是否显示答案

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String questionIds; // 题目ID列表（JSON格式）

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public Integer getPassScore() { return passScore; }
    public void setPassScore(Integer passScore) { this.passScore = passScore; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    public Boolean getRandomOrder() { return randomOrder; }
    public void setRandomOrder(Boolean randomOrder) { this.randomOrder = randomOrder; }
    public Boolean getShowAnswer() { return showAnswer; }
    public void setShowAnswer(Boolean showAnswer) { this.showAnswer = showAnswer; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getQuestionIds() { return questionIds; }
    public void setQuestionIds(String questionIds) { this.questionIds = questionIds; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
