package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_customer_followup")
public class CustomerFollowup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerCompany customer;

    @Column(nullable = false, length = 50)
    private String followupType;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 100)
    private String ownerName;

    private LocalDate nextFollowupDate;

    @Column(length = 50)
    private String resultLevel;

    private Long projectId;

    @Column(length = 200)
    private String projectName;

    @Column(nullable = false)
    private LocalDateTime followupTime = LocalDateTime.now();

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CustomerCompany getCustomer() { return customer; }
    public void setCustomer(CustomerCompany customer) { this.customer = customer; }
    public String getFollowupType() { return followupType; }
    public void setFollowupType(String followupType) { this.followupType = followupType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public LocalDate getNextFollowupDate() { return nextFollowupDate; }
    public void setNextFollowupDate(LocalDate nextFollowupDate) { this.nextFollowupDate = nextFollowupDate; }
    public String getResultLevel() { return resultLevel; }
    public void setResultLevel(String resultLevel) { this.resultLevel = resultLevel; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public LocalDateTime getFollowupTime() { return followupTime; }
    public void setFollowupTime(LocalDateTime followupTime) { this.followupTime = followupTime; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
