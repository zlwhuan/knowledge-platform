package com.company.knowledge.service.impl;

import com.company.knowledge.service.PreviewService;
import com.company.knowledge.service.PreviewWarmupService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class PreviewWarmupServiceImpl implements PreviewWarmupService {

    private final PreviewService previewService;

    public PreviewWarmupServiceImpl(PreviewService previewService) {
        this.previewService = previewService;
    }

    @Override
    @Async("previewWarmupExecutor")
    public void warmup(Long attachmentId) {
        try {
            previewService.getPreviewMeta(attachmentId);
        } catch (Exception ignored) {
        }
    }
}
