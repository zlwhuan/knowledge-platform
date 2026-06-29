package com.company.knowledge.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectRequest {
    @NotBlank
    @Size(max = 200)
    private String name;
    @NotBlank
    @Size(max = 200)
    private String customerName;
    @NotBlank
    @Pattern(regexp = "商机立项|合同执行|实施交付|验收结算|售后维保", message = "项目阶段不合法")
    private String stage;
    @NotBlank
    @Pattern(regexp = "进行中|已完成|暂停", message = "项目状态不合法")
    private String status;
    @Min(0)
    @Max(100)
    private Integer progress = 0;
    @DecimalMin(value = "0", message = "合同金额不能为负数")
    private BigDecimal contractAmount = BigDecimal.ZERO;
    @DecimalMin(value = "0", message = "回款金额不能为负数")
    private BigDecimal receivedAmount = BigDecimal.ZERO;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate acceptanceDate;
    private LocalDate warrantyUntil;
    private String salesOwner;
    private String projectManager;
    private String implementationOwner;
    private String documentOwner;
    private String serviceOwner;
    @Pattern(regexp = "^$|^低$|^中$|^高$", message = "风险等级不合法")
    private String riskLevel;
    private String contractStatus;
    private String paymentStatus;
    private String acceptanceStatus;
    private String serviceStatus;
    private String projectContactIds;
    private String projectContactLinks;
    private String relatedContactNotes;
    private String description;

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
}
