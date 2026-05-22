package com.company.knowledge.service.impl;

import com.company.knowledge.dto.AttachmentDetailResponse;
import com.company.knowledge.dto.AttachmentResponse;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.AttachmentRepository;
import com.company.knowledge.repository.KnowledgeItemRepository;
import com.company.knowledge.service.AttachmentService;
import com.company.knowledge.service.PreviewWarmupService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;
    private final PreviewWarmupService previewWarmupService;
    private final Path uploadDir = Paths.get("uploads");

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository, KnowledgeItemRepository knowledgeItemRepository, PreviewWarmupService previewWarmupService) {
        this.attachmentRepository = attachmentRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.previewWarmupService = previewWarmupService;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法初始化上传目录", ex);
        }
    }

    @Override
    public List<AttachmentResponse> listByItemId(Long itemId) {
        return attachmentRepository.findByItemIdOrderByUploadedAtDesc(itemId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Page<AttachmentDetailResponse> listAll(String keyword, String contentType, Pageable pageable) {
        return attachmentRepository.findAll(keyword, contentType, pageable).map(this::toDetailResponse);
    }

    private static final java.util.Set<String> BLOCKED_EXTENSIONS = java.util.Set.of();

    private static boolean isBlockedExtension(String fileName) {
        return false;
    }

    @Override
    public AttachmentResponse upload(Long itemId, MultipartFile file, String uploadedBy) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        if (originalName.length() > 255) {
            throw new IllegalArgumentException("文件名过长");
        }
        if (isBlockedExtension(originalName)) {
            throw new IllegalArgumentException("不支持上传该类型的可执行文件");
        }
        KnowledgeItem item = knowledgeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("知识条目不存在"));
        String storedFileName = UUID.randomUUID() + "_" + originalName;
        Path target = uploadDir.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("附件保存失败", ex);
        }
        Attachment attachment = new Attachment();
        attachment.setItem(item);
        attachment.setOriginalFileName(originalName);
        attachment.setStoredFileName(storedFileName);
        attachment.setFilePath(target.toAbsolutePath().toString());
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setUploadedBy(StringUtils.hasText(uploadedBy) ? uploadedBy : "未知");
        attachment.setUploadedAt(LocalDateTime.now());
        Attachment saved = attachmentRepository.save(attachment);
        previewWarmupService.warmup(saved.getId());
        return toResponse(saved);
    }

    @Override
    public Resource download(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        try {
            Resource resource = new UrlResource(Paths.get(attachment.getFilePath()).toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("附件文件不存在");
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("附件路径无效", ex);
        }
    }

    @Override
    public String downloadFileName(Long attachmentId) {
        return findAttachment(attachmentId).getOriginalFileName();
    }

    @Override
    public String downloadContentType(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        return StringUtils.hasText(attachment.getContentType()) ? attachment.getContentType() : "application/octet-stream";
    }

    @Override
    public void delete(Long attachmentId) {
        Attachment attachment = findAttachment(attachmentId);
        try {
            Files.deleteIfExists(Paths.get(attachment.getFilePath()));
        } catch (IOException ex) {
            throw new IllegalStateException("删除附件文件失败", ex);
        }
        attachmentRepository.delete(attachment);
    }

    private Attachment findAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("附件不存在"));
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt()
        );
    }

    private AttachmentDetailResponse toDetailResponse(Attachment attachment) {
        KnowledgeItem item = attachment.getItem();
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        String projectName = item.getProject() != null ? item.getProject().getName() : null;
        return new AttachmentDetailResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt(),
                item.getId(),
                item.getTitle(),
                item.getType(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                categoryName,
                item.getProject() != null ? item.getProject().getId() : null,
                projectName
        );
    }
}
