package com.company.knowledge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerCompanyResponse(
        Long id,
        String name,
        String shortName,
        String industry,
        String customerType,
        String level,
        String region,
        String address,
        String website,
        String mainPhone,
        String email,
        String ownerName,
        String source,
        String status,
        String cooperationStage,
        String tags,
        String notes,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String healthLevel,
        LocalDate nextFollowupDate,
        List<CustomerContactResponse> contacts,
        List<CustomerFollowupResponse> followups
) {
}
