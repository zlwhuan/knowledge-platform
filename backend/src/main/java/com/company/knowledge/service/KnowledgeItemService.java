package com.company.knowledge.service;

import com.company.knowledge.dto.DashboardStatsResponse;
import com.company.knowledge.dto.KnowledgeItemRequest;
import com.company.knowledge.dto.KnowledgeItemResponse;
import com.company.knowledge.dto.KnowledgeItemVersionResponse;
import java.util.List;
import java.util.Map;

public interface KnowledgeItemService {
    List<KnowledgeItemResponse> list(String keyword, Long categoryId, String type, Long projectId);
    Map<String, Object> listPaged(String keyword, Long categoryId, String type, Long projectId, int page, int size);
    KnowledgeItemResponse get(Long id);
    KnowledgeItemResponse create(KnowledgeItemRequest request, String operatorName);
    KnowledgeItemResponse update(Long id, KnowledgeItemRequest request, String operatorName);
    void delete(Long id);
    void bulkDelete(List<Long> ids);
    DashboardStatsResponse dashboard();
    Map<String, Object> importFromExcel(org.springframework.web.multipart.MultipartFile file, String operatorName);
    List<KnowledgeItemVersionResponse> getVersions(Long itemId);
}
