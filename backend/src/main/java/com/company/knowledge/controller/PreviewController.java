package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.PreviewMetaResponse;
import com.company.knowledge.service.PreviewService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachments")
public class PreviewController {

    private final PreviewService previewService;

    public PreviewController(PreviewService previewService) {
        this.previewService = previewService;
    }

    @GetMapping("/{attachmentId}/preview")
    public ApiResponse<PreviewMetaResponse> previewMeta(@PathVariable Long attachmentId) {
        return ApiResponse.ok(previewService.getPreviewMeta(attachmentId));
    }

    @GetMapping("/{attachmentId}/preview/file")
    public ResponseEntity<Resource> previewFile(@PathVariable Long attachmentId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, previewService.getPreviewContentType(attachmentId))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(previewService.getPreviewResource(attachmentId));
    }
}
