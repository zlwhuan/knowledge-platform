package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String customerName;

    @Column(nullable = false, length = 50)
    private String stage;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(precision = 14, scale = 2)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate acceptanceDate;
    private LocalDate warrantyUntil;

    @Column(length = 100)
    private String salesOwner;

    @Column(length = 100)
    private String projectManager;

    @Column(length = 100)
    private String implementationOwner;

    @Column(length = 100)
    private String documentOwner;

    @Column(length = 100)
    private String serviceOwner;

    @Column(length = 50)
    private String riskLevel;

    @Column(length = 50)
    private String contractStatus;

    @Column(length = 50)
    private String paymentStatus;

    @Column(length = 50)
    private String acceptanceStatus;

    @Column(length = 50)
    private String serviceStatus;

    @Column(length = 1000)
    private String projectContactIds;

    @Column(length = 4000)
    private String projectContactLinks;

    @Column(length = 2000)
    private String relatedContactNotes;

    @Column(length = 2000)
    private String description;

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
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public BigDecimal getContractAmount() { return contractAmount; }
    public void setContractAmount(BigDecimal contractAmount) { this.contractAmount = contractAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public void setPlannedEndDate(LocalDate plannedEndDate) { this.plannedEndDate = plannedEndDate; }
    public LocalDate getAcceptanceDate() { return acceptanceDate; }
    public void setAcceptanceDate(LocalDate acceptanceDate) { this.acceptanceDate = acceptanceDate; }
    public LocalDate getWarrantyUntil() { return warrantyUntil; }
    public void setWarrantyUntil(LocalDate warrantyUntil) { this.warrantyUntil = warrantyUntil; }
    public String getSalesOwner() { return salesOwner; }
    public void setSalesOwner(String salesOwner) { this.salesOwner = salesOwner; }
    public String getProjectManager() { return projectManager; }
    public void setProjectManager(String projectManager) { this.projectManager = projectManager; }
    public String getImplementationOwner() { return implementationOwner; }
    public void setImplementationOwner(String implementationOwner) { this.implementationOwner = implementationOwner; }
    public String getDocumentOwner() { return documentOwner; }
    public void setDocumentOwner(String documentOwner) { this.documentOwner = documentOwner; }
    public String getServiceOwner() { return serviceOwner; }
    public void setServiceOwner(String serviceOwner) { this.serviceOwner = serviceOwner; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getContractStatus() { return contractStatus; }
    public void setContractStatus(String contractStatus) { this.contractStatus = contractStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getAcceptanceStatus() { return acceptanceStatus; }
    public void setAcceptanceStatus(String acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; }
    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public String getProjectContactIds() { return projectContactIds; }
    public void setProjectContactIds(String projectContactIds) { this.projectContactIds = projectContactIds; }
    public String getProjectContactLinks() { return projectContactLinks; }
    public void setProjectContactLinks(String projectContactLinks) { this.projectContactLinks = projectContactLinks; }
    public String getRelatedContactNotes() { return relatedContactNotes; }
    public void setRelatedContactNotes(String relatedContactNotes) { this.relatedContactNotes = relatedContactNotes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
