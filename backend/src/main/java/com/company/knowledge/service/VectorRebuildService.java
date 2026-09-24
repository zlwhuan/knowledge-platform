package com.company.knowledge.service;

import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.KnowledgeItem;
import com.company.knowledge.repository.AttachmentRepository;
import com.company.knowledge.repository.KnowledgeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 真·全库重建：从 MySQL 拉齐知识条目（列注释拼文本）+ 附件正文，
 * 与 docs-vault 一起交给 Python 合并重建向量索引。
 */
@Service
public class VectorRebuildService {

    private static final Logger logger = LoggerFactory.getLogger(VectorRebuildService.class);

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentContentExtractor contentExtractor;
    private final KnowledgeItemVectorTextBuilder textBuilder;
    private final TransactionTemplate transactionTemplate;
    private final RestTemplate restTemplate;

    @Value("${rag.service.url:http://localhost:8081}")
    private String ragServiceUrl;

    public VectorRebuildService(
            KnowledgeItemRepository knowledgeItemRepository,
            AttachmentRepository attachmentRepository,
            AttachmentContentExtractor contentExtractor,
            KnowledgeItemVectorTextBuilder textBuilder,
            TransactionTemplate transactionTemplate,
            RestTemplate ragRestTemplate
    ) {
        this.knowledgeItemRepository = knowledgeItemRepository;
        this.attachmentRepository = attachmentRepository;
        this.contentExtractor = contentExtractor;
        this.textBuilder = textBuilder;
        this.transactionTemplate = transactionTemplate;
        this.restTemplate = ragRestTemplate;
    }

    /**
     * 全库重建：返回给前端的摘要
     */
    public Map<String, Object> rebuildFullLibrary() {
        List<Map<String, Object>> items = transactionTemplate.execute(status -> exportPlatformItems());
        if (items == null) {
            items = List.of();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("keep_existing_platform", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                ragServiceUrl + "/api/rag/rebuild-full",
                HttpMethod.POST,
                entity,
                Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "queued");
        result.put("platform_items", items.size());
        result.put("message", "全库重建已排队：" + items.size() + " 条知识条目（仅平台数据，含分类树）");
        if (response.getBody() != null) {
            result.put("rag", response.getBody());
        }
        return result;
    }

    private List<Map<String, Object>> exportPlatformItems() {
        List<KnowledgeItem> all = knowledgeItemRepository.findAll();
        List<Map<String, Object>> payload = new ArrayList<>(all.size());

        for (KnowledgeItem item : all) {
            // 触发懒加载
            if (item.getCategory() != null) {
                item.getCategory().getName();
            }
            if (item.getProject() != null) {
                item.getProject().getName();
            }

            Map<String, Object> row = new HashMap<>();
            row.put("item_id", String.valueOf(item.getId()));
            row.put("title", item.getTitle() == null ? "" : item.getTitle());
            // 关键：列注释 + 列值 拼成的大文本（与增量同步一致）
            row.put("content", textBuilder.build(item));
            // 分类树路径，如 01_标准化资料/手术麻醉
            row.put("category", textBuilder.categoryPath(item));
            row.put("project", textBuilder.projectName(item));
            row.put("tags", splitTags(item.getTags()));
            row.put("source", item.getSource() == null ? "" : item.getSource());
            // doc_type 用根分类名，替代旧 products/faq/meeting/competitor
            row.put("doc_type", textBuilder.rootCategoryName(item));

            List<Map<String, Object>> attachments = new ArrayList<>();
            for (Attachment att : attachmentRepository.findByItemIdOrderByUploadedAtDesc(item.getId())) {
                String resolved = String.valueOf(contentExtractor.resolveUploadPath(att.getFilePath()));
                String extracted = contentExtractor.extractContent(att.getFilePath(), att.getContentType());
                if (extracted == null || extracted.isBlank()) {
                    logger.warn("Skip attachment {}: {} (path={}, resolved={}) — empty/unsupported extract",
                            att.getId(), att.getOriginalFileName(), att.getFilePath(), resolved);
                    continue;
                }
                Map<String, Object> attRow = new HashMap<>();
                attRow.put("attachment_id", String.valueOf(att.getId()));
                attRow.put("filename", att.getOriginalFileName());
                attRow.put("file_path", att.getFilePath() == null ? "" : att.getFilePath());
                attRow.put("content", textBuilder.buildAttachmentText(item, att, extracted));
                attRow.put("content_type", att.getContentType() == null ? "" : att.getContentType());
                attachments.add(attRow);
            }
            row.put("attachments", attachments);
            payload.add(row);
        }
        logger.info("Exported {} knowledge items for full library rebuild", payload.size());
        return payload;
    }

    private String resolveFilePath(String filePath) {
        return String.valueOf(contentExtractor.resolveUploadPath(filePath));
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : tags.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }
}
