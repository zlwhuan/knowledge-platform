package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_question")
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category; // 销售、售前、实施、通用

    @Column(nullable = false, length = 50)
    private String type; // single, multiple, judge, fill, essay

    @Column(nullable = false, length = 50)
    private String difficulty; // easy, medium, hard

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 题目内容

    @Column(columnDefinition = "TEXT")
    private String options; // 选项（JSON格式）

    @Column(columnDefinition = "TEXT")
    private String answer; // 正确答案

    @Column(columnDefinition = "TEXT")
    private String explanation; // 答案解析

    @Column(nullable = false)
    private Integer score = 5; // 题目分值

    @Column(nullable = false)
    private Integer useCount = 0; // 使用次数

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getUseCount() { return useCount; }
    public void setUseCount(Integer useCount) { this.useCount = useCount; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
