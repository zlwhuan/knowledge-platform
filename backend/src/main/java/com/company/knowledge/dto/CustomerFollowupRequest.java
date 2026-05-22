package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerFollowupRequest {
    @NotBlank
    private String followupType;
    @NotBlank
    private String content;
    private String ownerName;
    private LocalDate nextFollowupDate;
    private String resultLevel;
    private Long projectId;
    private String projectName;
    private LocalDateTime followupTime;

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
}
