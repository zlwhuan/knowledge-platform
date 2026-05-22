package com.company.knowledge.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(Long id, String originalFileName, Long fileSize, String contentType, String uploadedBy, LocalDateTime uploadedAt) {
}
