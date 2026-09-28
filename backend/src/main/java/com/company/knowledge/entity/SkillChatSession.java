package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 技能助手会话（按用户隔离）
 */
@Entity
@Table(name = "kp_skill_session")
public class SkillChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(length = 64)
    private String skillId;

    @Column(length = 200)
    private String title;

    /** 会话内已检索过的块 key，JSON 数组（跨轮去重） */
    @Column(columnDefinition = "TEXT")
    private String consumedKeys;

    /** 会话内已拿到的检索出处摘要，JSON（供后续轮次复用） */
    @Column(columnDefinition = "TEXT")
    private String sourceCache;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSkillId() { return skillId; }
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getConsumedKeys() { return consumedKeys; }
    public void setConsumedKeys(String consumedKeys) { this.consumedKeys = consumedKeys; }
    public String getSourceCache() { return sourceCache; }
    public void setSourceCache(String sourceCache) { this.sourceCache = sourceCache; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
