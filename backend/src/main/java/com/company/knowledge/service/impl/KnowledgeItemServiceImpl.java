package com.company.knowledge.service.impl;

import com.company.knowledge.dto.AttachmentResponse;
import com.company.knowledge.dto.DashboardStatsResponse;
import com.company.knowledge.dto.KnowledgeItemRequest;
import com.company.knowledge.dto.KnowledgeItemResponse;
import com.company.knowledge.dto.KnowledgeItemVersionResponse;
import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.Category;
import com.company.knowledge.entity.KnowledgeItem;
import com.company.knowledge.entity.KnowledgeItemVersion;
import com.company.knowledge.entity.Project;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.AttachmentRepository;
import com.company.knowledge.repository.CategoryRepository;
import com.company.knowledge.repository.KnowledgeItemRepository;
import com.company.knowledge.repository.KnowledgeItemVersionRepository;
import com.company.knowledge.repository.ProjectRepository;
import com.company.knowledge.service.KnowledgeItemService;
import jakarta.persistence.criteria.Predicate;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeItemServiceImpl implements KnowledgeItemService {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final CategoryRepository categoryRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProjectRepository projectRepository;
    private final KnowledgeItemVersionRepository knowledgeItemVersionRepository;

    public KnowledgeItemServiceImpl(
            KnowledgeItemRepository knowledgeItemRepository,
            CategoryRepository categoryRepository,
            AttachmentRepository attachmentRepository,
            ProjectRepository projectRepository,
            KnowledgeItemVersionRepository knowledgeItemVersionRepository
    ) {
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.categoryRepository = categoryRepository;
        this.attachmentRepository = attachmentRepository;
        this.projectRepository = projectRepository;
        this.knowledgeItemVersionRepository = knowledgeItemVersionRepository;
    }

    @Override
    public List<KnowledgeItemResponse> list(String keyword, Long categoryId, String type, Long projectId) {
        Set<Long> categoryIds = categoryId == null ? Set.of() : collectCategoryIds(categoryId);
        return knowledgeItemRepository.findAll(buildSpecification(keyword, categoryIds, type, projectId))
                .stream().map(this::toResponse).toList();
    }

    @Override
    public Map<String, Object> listPaged(String keyword, Long categoryId, String type, Long projectId, int page, int size) {
        Set<Long> categoryIds = categoryId == null ? Set.of() : collectCategoryIds(categoryId);
        Specification<KnowledgeItem> specification = buildSpecification(keyword, categoryIds, type, projectId);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, Math.min(size, 100)));
        Page<KnowledgeItem> resultPage = knowledgeItemRepository.findAll(specification, pageable);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", resultPage.getContent().stream().map(this::toResponse).toList());
        result.put("total", resultPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", resultPage.getTotalPages());
        return result;
    }

    private Specification<KnowledgeItem> buildSpecification(String keyword, Set<Long> categoryIds, String type, Long projectId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), pattern),
                        cb.like(root.get("summary"), pattern),
                        cb.like(root.get("tags"), pattern),
                        cb.like(root.get("contentMarkdown"), pattern)
                ));
            }
            if (!categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            query.orderBy(cb.desc(root.get("updatedAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Override
    public KnowledgeItemResponse get(Long id) {
        return toResponse(findItem(id));
    }

    @Override
    public KnowledgeItemResponse create(KnowledgeItemRequest request, String operatorName) {
        KnowledgeItem item = new KnowledgeItem();
        apply(item, request, operatorName);
        return toResponse(knowledgeItemRepository.save(item));
    }

    @Override
    public KnowledgeItemResponse update(Long id, KnowledgeItemRequest request, String operatorName) {
        KnowledgeItem item = findItem(id);
        // Save version snapshot before update
        KnowledgeItemVersion version = new KnowledgeItemVersion();
        version.setItem(item);
        version.setTitle(item.getTitle());
        version.setSummary(item.getSummary());
        version.setContentMarkdown(item.getContentMarkdown());
        version.setTags(item.getTags());
        version.setCreatedBy(item.getUpdatedBy() != null ? item.getUpdatedBy() : "系统");
        knowledgeItemVersionRepository.save(version);

        apply(item, request, operatorName);
        return toResponse(knowledgeItemRepository.save(item));
    }

    @Override
    public void delete(Long id) {
        KnowledgeItem item = findItem(id);
        // 先删除关联的附件文件
        List<Attachment> attachments = attachmentRepository.findByItemIdOrderByUploadedAtDesc(id);
        for (Attachment attachment : attachments) {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(attachment.getFilePath()));
            } catch (Exception ignored) {}
        }
        attachmentRepository.deleteAll(attachments);
        // 再删除关联的版本记录
        knowledgeItemVersionRepository.deleteByItemId(id);
        // 最后删除资料
        knowledgeItemRepository.delete(item);
    }

    @Override
    public void bulkDelete(List<Long> ids) {
        List<KnowledgeItem> items = knowledgeItemRepository.findAllById(ids);
        for (Long id : ids) {
            // 先删除关联的附件文件
            List<Attachment> attachments = attachmentRepository.findByItemIdOrderByUploadedAtDesc(id);
            for (Attachment attachment : attachments) {
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(attachment.getFilePath()));
                } catch (Exception ignored) {}
            }
            attachmentRepository.deleteAll(attachments);
            // 再删除关联的版本记录
            knowledgeItemVersionRepository.deleteByItemId(id);
        }
        knowledgeItemRepository.deleteAll(items);
    }

    @Override
    public DashboardStatsResponse dashboard() {
        long itemCount = knowledgeItemRepository.count();
        long markdownItemCount = knowledgeItemRepository.countByContentMarkdownIsNotNullAndContentMarkdownNot("");
        long missingSummaryCount = knowledgeItemRepository.countBySummaryIsNull();
        long missingAttachmentCount = knowledgeItemRepository.countItemsWithoutAttachment();
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(7);
        long recentUpdatedCount = knowledgeItemRepository.countByUpdatedAtAfter(recentThreshold);
        return new DashboardStatsResponse(
                categoryRepository.count(),
                itemCount,
                attachmentRepository.count(),
                markdownItemCount,
                missingSummaryCount,
                missingAttachmentCount,
                recentUpdatedCount
        );
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file, String operatorName) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String title = getCellString(row, 0);
                    if (title.isBlank()) continue;
                    String type = getCellString(row, 1);
                    String categoryStr = getCellString(row, 2);
                    String summary = getCellString(row, 3);
                    String tags = getCellString(row, 4);
                    String source = getCellString(row, 5);

                    Category category = null;
                    if (!categoryStr.isBlank()) {
                        category = categoryRepository.findAll().stream()
                                .filter(c -> c.getName().equals(categoryStr.trim()))
                                .findFirst()
                                .orElse(null);
                    }
                    if (category == null) {
                        category = categoryRepository.findAll().stream().findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("无可用分类"));
                    }

                    KnowledgeItem item = new KnowledgeItem();
                    item.setTitle(title);
                    item.setType(type.isBlank() ? "文档" : type.trim());
                    item.setCategory(category);
                    item.setStatus("正常");
                    item.setSummary(summary);
                    item.setTags(tags);
                    item.setSource(source);
                    item.setCreatedBy(operatorName);
                    item.setUpdatedBy(operatorName);
                    item.setCreatedAt(LocalDateTime.now());
                    item.setUpdatedAt(LocalDateTime.now());
                    knowledgeItemRepository.save(item);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("导入文件解析失败: " + e.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    @Override
    public List<KnowledgeItemVersionResponse> getVersions(Long itemId) {
        return knowledgeItemVersionRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream().map(v -> new KnowledgeItemVersionResponse(
                    v.getId(), v.getItem().getId(), v.getTitle(), v.getSummary(),
                    v.getContentMarkdown(), v.getTags(), v.getCreatedBy(), v.getCreatedAt()
                )).toList();
    }

    private KnowledgeItem findItem(Long id) {
        return knowledgeItemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("知识条目不存在"));
    }

    private void apply(KnowledgeItem item, KnowledgeItemRequest request, String operatorName) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("分类不存在"));
        Project project = request.getProjectId() == null ? null : projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));

        item.setTitle(request.getTitle());
        item.setType(request.getType());
        item.setCategory(category);
        item.setProject(project);
        item.setStatus("正常");
        item.setSummary(request.getSummary());
        item.setContentMarkdown(request.getContentMarkdown());
        item.setTags(request.getTags());
        item.setSource(request.getSource());

        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(item.getCreatedBy())) {
            item.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
        item.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : item.getCreatedBy());
        item.setUpdatedAt(now);
        appendLog(item, "内容保存", "内容已更新", now);
    }

    private Set<Long> collectCategoryIds(Long rootId) {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Category cat : categories) {
            if (cat.getParent() != null) {
                childrenMap.computeIfAbsent(cat.getParent().getId(), k -> new ArrayList<>()).add(cat.getId());
            }
        }
        Set<Long> ids = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (ids.add(current)) {
                List<Long> children = childrenMap.getOrDefault(current, List.of());
                queue.addAll(children);
            }
        }
        return ids;
    }

    private KnowledgeItemResponse toResponse(KnowledgeItem item) {
        List<AttachmentResponse> attachments = attachmentRepository.findByItemIdOrderByUploadedAtDesc(item.getId()).stream()
                .map(this::toAttachmentResponse)
                .toList();
        String governanceStatus = computeGovernanceStatus(item, attachments);
        return new KnowledgeItemResponse(
                item.getId(),
                item.getTitle(),
                item.getType(),
                item.getSummary(),
                item.getContentMarkdown(),
                item.getTags(),
                item.getSource(),
                item.getOperationLog(),
                item.getProject() == null ? null : item.getProject().getId(),
                item.getProject() == null ? null : item.getProject().getName(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getCreatedBy(),
                item.getUpdatedBy(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                attachments,
                governanceStatus
        );
    }

    private String computeGovernanceStatus(KnowledgeItem item, List<AttachmentResponse> attachments) {
        boolean hasSummary = item.getSummary() != null && !item.getSummary().isBlank();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        if (hasSummary && hasAttachments) return "完整";
        if (!hasSummary && !hasAttachments) return "缺摘要/附件";
        if (!hasSummary) return "缺摘要";
        return "缺附件";
    }

    private void appendLog(KnowledgeItem item, String action, String detail, LocalDateTime time) {
        String line = "[" + time + "] " + action + " - " + detail;
        if (!StringUtils.hasText(item.getOperationLog())) {
            item.setOperationLog(line);
            return;
        }
        item.setOperationLog(item.getOperationLog() + "\n" + line);
    }

    private AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getFileSize(),
                attachment.getContentType(),
                attachment.getUploadedBy(),
                attachment.getUploadedAt()
        );
    }
}
