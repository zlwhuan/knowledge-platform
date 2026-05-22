package com.company.knowledge.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_knowledge_item_version")
public class KnowledgeItemVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private KnowledgeItem item;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String contentMarkdown;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public KnowledgeItem getItem() { return item; }
    public void setItem(KnowledgeItem item) { this.item = item; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
