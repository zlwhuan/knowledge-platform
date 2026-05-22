package com.company.knowledge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerFollowupResponse(
        Long id,
        String followupType,
        String content,
        String ownerName,
        LocalDate nextFollowupDate,
        String resultLevel,
        Long projectId,
        String projectName,
        LocalDateTime followupTime,
        String createdBy,
        LocalDateTime createdAt
) {
}
