package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ProjectActivityRequest {
    @NotBlank
    private String recordType;
    @NotBlank
    private String content;
    private String ownerName;
    private LocalDateTime recordTime;

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public LocalDateTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalDateTime recordTime) { this.recordTime = recordTime; }
}
