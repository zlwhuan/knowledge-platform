package com.company.knowledge.service;

import com.company.knowledge.dto.PreviewMetaResponse;
import org.springframework.core.io.Resource;

public interface PreviewService {
    PreviewMetaResponse getPreviewMeta(Long attachmentId);
    Resource getPreviewResource(Long attachmentId);
    String getPreviewContentType(Long attachmentId);
}
