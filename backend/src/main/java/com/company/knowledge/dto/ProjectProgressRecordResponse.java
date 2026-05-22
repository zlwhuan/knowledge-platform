package com.company.knowledge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectProgressRecordResponse(
        Long id,
        String stage,
        String status,
        Integer progress,
        String riskLevel,
        String summary,
        String nextAction,
        LocalDate nextActionDueDate,
        String ownerName,
        LocalDateTime recordTime,
        String createdBy,
        LocalDateTime createdAt
) {
}
