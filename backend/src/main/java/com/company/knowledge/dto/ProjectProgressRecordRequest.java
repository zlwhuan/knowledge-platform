package com.company.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProjectProgressRecordRequest {
    @NotBlank
    private String stage;
    @NotBlank
    private String status;
    @Min(0)
    @Max(100)
    private Integer progress = 0;
    private String riskLevel;
    @NotBlank
    private String summary;
    private String nextAction;
    private LocalDate nextActionDueDate;
    private String ownerName;
    private LocalDateTime recordTime;

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public LocalDate getNextActionDueDate() { return nextActionDueDate; }
    public void setNextActionDueDate(LocalDate nextActionDueDate) { this.nextActionDueDate = nextActionDueDate; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
