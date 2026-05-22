package com.company.knowledge.dto;

public record PreviewMetaResponse(
        String kind,
        String previewUrl,
        String fileName,
        String contentType,
        Long fileSize,
        String message,
        String onlyOfficeApiUrl,
        String onlyOfficeDocumentUrl,
        String onlyOfficeFileType,
        String onlyOfficeDocumentKey
) {
}
