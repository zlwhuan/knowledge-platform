package com.company.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeItemResponse(
        Long id,
        String title,
        String type,
        String summary,
        String contentMarkdown,
        String tags,
        String source,
        String operationLog,
        Long projectId,
        String projectName,
        Long categoryId,
        String categoryName,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AttachmentResponse> attachments,
        String governanceStatus
) {
}
