package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.AttachmentDetailResponse;
import com.company.knowledge.dto.AttachmentResponse;
import com.company.knowledge.entity.RoleType;
import com.company.knowledge.entity.UserAccount;
import com.company.knowledge.service.AttachmentService;
import com.company.knowledge.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final AuthService authService;

    public AttachmentController(AttachmentService attachmentService, AuthService authService) {
        this.attachmentService = attachmentService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Page<AttachmentDetailResponse>> listAll(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String contentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        authService.requireUser(token);
        return ApiResponse.ok(attachmentService.listAll(keyword, contentType, PageRequest.of(page - 1, size)));
    }

    @GetMapping("/item/{itemId}")
    public ApiResponse<List<AttachmentResponse>> listByItem(@PathVariable Long itemId) {
        return ApiResponse.ok(attachmentService.listByItemId(itemId));
    }

    @PostMapping("/upload")
    public ApiResponse<AttachmentResponse> upload(@RequestHeader("X-Auth-Token") String token, @RequestParam Long itemId, @RequestParam MultipartFile file) {
        UserAccount user = authService.requireUser(token);
        return ApiResponse.ok("上传成功", attachmentService.upload(itemId, file, user.getDisplayName()));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        String encodedName = java.net.URLEncoder.encode(attachmentService.downloadFileName(attachmentId), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachmentService.downloadContentType(attachmentId)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(attachmentService.download(attachmentId));
    }

    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(@RequestHeader("X-Auth-Token") String token, @PathVariable Long attachmentId) {
        UserAccount user = authService.requireUser(token);
        authService.requireAnyRole(user, RoleType.ADMIN, RoleType.SALES, RoleType.PRESALES, RoleType.DELIVERY_OPS);
        attachmentService.delete(attachmentId);
        return ApiResponse.ok("删除成功", null);
    }
}
