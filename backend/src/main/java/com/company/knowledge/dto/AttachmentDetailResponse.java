package com.company.knowledge.dto;

import java.time.LocalDateTime;

public record AttachmentDetailResponse(
    Long id,
    String originalFileName,
    Long fileSize,
    String contentType,
    String uploadedBy,
    LocalDateTime uploadedAt,
    Long itemId,
    String itemTitle,
    String itemType,
    Long categoryId,
    String categoryName,
    Long projectId,
    String projectName
) {}
