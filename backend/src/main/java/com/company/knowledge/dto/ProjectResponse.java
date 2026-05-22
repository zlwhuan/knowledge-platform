package com.company.knowledge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String customerName,
        String stage,
        String status,
        Integer progress,
        BigDecimal contractAmount,
        BigDecimal receivedAmount,
        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDate acceptanceDate,
        LocalDate warrantyUntil,
        String salesOwner,
        String projectManager,
        String implementationOwner,
        String documentOwner,
        String serviceOwner,
        String riskLevel,
        String contractStatus,
        String paymentStatus,
        String acceptanceStatus,
        String serviceStatus,
        String projectContactIds,
        String projectContactLinks,
        String relatedContactNotes,
        String latestProgressSummary,
        String description,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ProjectActivityResponse> activities,
        List<ProjectProgressRecordResponse> progressRecords
) {
}
