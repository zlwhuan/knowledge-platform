package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_customer_company")
public class CustomerCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String shortName;

    @Column(length = 100)
    private String industry;

    @Column(length = 100)
    private String customerType;

    @Column(length = 50)
    private String level;

    @Column(length = 100)
    private String region;

    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String website;

    @Column(length = 100)
    private String mainPhone;

    @Column(length = 200)
    private String email;

    @Column(length = 100)
    private String ownerName;

    @Column(length = 100)
    private String source;

    @Column(length = 50)
    private String status;

    @Column(length = 50)
    private String cooperationStage;

    @Column(length = 1000)
    private String tags;

    @Column(length = 2000)
    private String notes;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getMainPhone() { return mainPhone; }
    public void setMainPhone(String mainPhone) { this.mainPhone = mainPhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCooperationStage() { return cooperationStage; }
    public void setCooperationStage(String cooperationStage) { this.cooperationStage = cooperationStage; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
