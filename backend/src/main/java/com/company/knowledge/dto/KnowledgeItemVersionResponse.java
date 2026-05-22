package com.company.knowledge.dto;

import java.time.LocalDateTime;

public record KnowledgeItemVersionResponse(
    Long id, Long itemId, String title, String summary, String contentMarkdown,
    String tags, String createdBy, LocalDateTime createdAt
) {}
