package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_record")
public class ExamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private ExamPaper paper;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 50)
    private String status = "in_progress"; // in_progress, submitted, graded

    @Column(nullable = false)
    private Integer score = 0; // 得分

    @Column(nullable = false)
    private Boolean passed = false; // 是否及格

    @Column(nullable = false)
    private Integer correctCount = 0; // 正确题数

    @Column(nullable = false)
    private Integer totalQuestions = 0; // 总题数

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime submitTime;

    @Column
    private Integer duration = 0; // 用时（分钟）

    @Column(columnDefinition = "TEXT")
    private String answers; // 答案记录（JSON格式）

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamPaper getPaper() { return paper; }
    public void setPaper(ExamPaper paper) { this.paper = paper; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
