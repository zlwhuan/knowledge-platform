package com.company.knowledge.dto;

import java.time.LocalDateTime;

public record ProjectActivityResponse(
        Long id,
        String recordType,
        String content,
        String ownerName,
        LocalDateTime recordTime,
        String createdBy,
        LocalDateTime createdAt
) {
}
