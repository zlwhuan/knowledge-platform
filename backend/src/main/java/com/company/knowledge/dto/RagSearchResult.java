package com.company.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for RAG search results
 */
public class RagSearchResult {

    @JsonProperty("rank")
    private Integer rank;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("section")
    private String section;

    @JsonProperty("page")
    private String page;

    @JsonProperty("product")
    private String product;

    @JsonProperty("doc_type")
    private String docType;

    @JsonProperty("path")
    private String path;

    @JsonProperty("text")
    private String text;

    public RagSearchResult() {}

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "RagSearchResult{" +
                "rank=" + rank +
                ", score=" + score +
                ", chunkId='" + chunkId + '\'' +
                ", title='" + title + '\'' +
                ", section='" + section + '\'' +
                ", product='" + product + '\'' +
                ", docType='" + docType + '\'' +
                ", path='" + path + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(100, text.length())) + "..." : "null") + '\'' +
                '}';
    }
}