package com.company.knowledge.service;

import com.company.knowledge.dto.PreviewMetaResponse;
import org.springframework.core.io.Resource;

public interface PreviewService {
    PreviewMetaResponse getPreviewMeta(Long attachmentId);
    Resource getPreviewResource(Long attachmentId);
    String getPreviewContentType(Long attachmentId);

    /** 会话临时附件预览（与知识库附件共用同一套预览策略） */
    PreviewMetaResponse getSessionAttachmentPreviewMeta(String fileName, String contentType, String filePath, String previewKey);
    Resource getSessionAttachmentPreviewResource(String fileName, String contentType, String filePath, String previewKey);
    String getSessionAttachmentContentType(String fileName, String contentType, String filePath, String previewKey);
}
