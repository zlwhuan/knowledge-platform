package com.company.knowledge.service;

import com.company.knowledge.dto.AttachmentDetailResponse;
import com.company.knowledge.dto.AttachmentResponse;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    List<AttachmentResponse> listByItemId(Long itemId);
    Page<AttachmentDetailResponse> listAll(String keyword, String contentType, Pageable pageable);
    AttachmentResponse upload(Long itemId, MultipartFile file, String uploadedBy);
    Resource download(Long attachmentId);
    String downloadFileName(Long attachmentId);
    String downloadContentType(Long attachmentId);
    void delete(Long attachmentId);
}
