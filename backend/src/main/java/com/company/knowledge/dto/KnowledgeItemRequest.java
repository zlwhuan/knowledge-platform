package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class KnowledgeItemRequest {
    @NotBlank
    @Size(max = 200)
    private String title;
    @NotBlank
    @Size(max = 50)
    private String type;
    @NotNull
    private Long categoryId;
    @Size(max = 1000)
    private String summary;
    private String contentMarkdown;
    @Size(max = 500)
    private String tags;
    @Size(max = 200)
    private String source;
    private Long projectId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
