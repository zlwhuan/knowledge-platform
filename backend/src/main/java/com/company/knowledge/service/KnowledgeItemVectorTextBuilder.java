package com.company.knowledge.service;

import com.company.knowledge.entity.Attachment;
import com.company.knowledge.entity.Category;
import com.company.knowledge.entity.KnowledgeItem;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 把知识条目拼成向量化大文本：列注释 + 列值。
 * 分类解析为树路径（父分类/子分类），关联项目解析为项目名。
 * 增量同步与全库重建必须共用本类，保证文本格式一致。
 */
@Component
public class KnowledgeItemVectorTextBuilder {

    /** 分类树路径分隔符，与向量侧 product 字段一致 */
    public static final String CATEGORY_PATH_SEPARATOR = "/";

    public String build(KnowledgeItem item) {
        StringBuilder sb = new StringBuilder(512);
        appendField(sb, "标题", item.getTitle());
        appendField(sb, "正文（Markdown）", item.getContentMarkdown());
        appendField(sb, "摘要", item.getSummary());
        appendField(sb, "资料类型", item.getType());
        appendField(sb, "资料来源", item.getSource());
        appendField(sb, "所属分类ID", categoryPath(item.getCategory()));
        appendField(sb, "关联项目ID", projectName(item));
        return sb.toString().trim();
    }

    /**
     * 附件正文同样带「列注释：列值」，并继承所属条目的分类树
     */
    public String buildAttachmentText(KnowledgeItem item, Attachment attachment, String extractedContent) {
        StringBuilder sb = new StringBuilder(256);
        appendField(sb, "原始文件名", attachment.getOriginalFileName());
        appendField(sb, "文件 MIME 类型", attachment.getContentType());
        appendField(sb, "所属知识条目ID", item != null ? item.getTitle() : "");
        appendField(sb, "所属分类ID", item != null ? categoryPath(item.getCategory()) : "");
        appendField(sb, "关联项目ID", item != null ? projectName(item) : "");
        appendField(sb, "正文", extractedContent);
        return sb.toString().trim();
    }

    /** 条目所属分类树路径，如 01_标准化资料/手术麻醉 */
    public String categoryPath(KnowledgeItem item) {
        return categoryPath(item == null ? null : item.getCategory());
    }

    public String categoryPath(Category category) {
        if (category == null) {
            return "";
        }
        Deque<String> parts = new ArrayDeque<>();
        Category cur = category;
        int guard = 0;
        while (cur != null && guard++ < 32) {
            String name = cur.getName();
            if (name != null && !name.isBlank()) {
                parts.addFirst(name.trim());
            }
            cur = cur.getParent();
        }
        return String.join(CATEGORY_PATH_SEPARATOR, parts);
    }

    /** 根分类名（树第一层），可用于 doc_type 归类 */
    public String rootCategoryName(KnowledgeItem item) {
        if (item == null || item.getCategory() == null) {
            return "";
        }
        Category cur = item.getCategory();
        int guard = 0;
        while (cur.getParent() != null && guard++ < 32) {
            cur = cur.getParent();
        }
        return cur.getName() == null ? "" : cur.getName();
    }

    public String projectName(KnowledgeItem item) {
        if (item == null || item.getProject() == null || item.getProject().getName() == null) {
            return "";
        }
        return item.getProject().getName();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        sb.append(label).append("：").append(value == null ? "" : value.trim()).append('\n');
    }
}
