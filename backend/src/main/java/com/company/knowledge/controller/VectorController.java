package com.company.knowledge.controller;

import com.company.knowledge.dto.ApiResponse;
import com.company.knowledge.dto.RagSearchResult;
import com.company.knowledge.event.EventPublisher;
import com.company.knowledge.service.RagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 向量库管理接口：快捷检索、索引维护、健康状态
 */
@RestController
@RequestMapping("/api/vector")
public class VectorController {

    private final RagService ragService;
    private final EventPublisher eventPublisher;
    private final com.company.knowledge.service.VectorRebuildService vectorRebuildService;

    public VectorController(
            RagService ragService,
            EventPublisher eventPublisher,
            com.company.knowledge.service.VectorRebuildService vectorRebuildService) {
        this.ragService = ragService;
        this.eventPublisher = eventPublisher;
        this.vectorRebuildService = vectorRebuildService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        boolean healthy = ragService.isHealthy();
        return ApiResponse.ok(Map.of(
                "healthy", healthy,
                "service", healthy ? "ready" : "unavailable"
        ));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(ragService.getStatusMap());
    }

    @PostMapping("/search")
    public ApiResponse<List<RagSearchResult>> search(@RequestBody Map<String, Object> body) {
        String query = body.get("query") == null ? "" : String.valueOf(body.get("query"));
        int topK = body.get("top_k") instanceof Number n ? n.intValue() : 8;
        String category = body.get("category") == null ? null : String.valueOf(body.get("category"));
        String docType = body.get("doc_type") == null ? null : String.valueOf(body.get("doc_type"));
        if (query.isBlank()) {
            return ApiResponse.fail("请输入检索词");
        }
        return ApiResponse.ok(ragService.search(query.trim(), topK, emptyToNull(category), emptyToNull(docType)));
    }

    @GetMapping("/sources")
    public ApiResponse<Map<String, Object>> sources(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String docType) {
        return ApiResponse.ok(ragService.listSources(emptyToNull(category), emptyToNull(docType)));
    }

    @GetMapping("/chunks")
    public ApiResponse<Map<String, Object>> chunks(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String docType,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ApiResponse.ok(ragService.listChunks(emptyToNull(path), emptyToNull(q), emptyToNull(docType), limit, offset));
    }

    @GetMapping("/chunks/{chunkId}")
    public ApiResponse<Map<String, Object>> getChunk(@PathVariable String chunkId) {
        return ApiResponse.ok(ragService.getChunk(chunkId));
    }

    @DeleteMapping("/chunks/{chunkId}")
    public ApiResponse<Map<String, Object>> deleteChunk(@PathVariable String chunkId) {
        return ApiResponse.ok(ragService.deleteChunk(chunkId));
    }

    @DeleteMapping("/sources")
    public ApiResponse<Map<String, Object>> deleteSource(@RequestParam String path) {
        return ApiResponse.ok(ragService.deleteSource(path));
    }

    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex(@RequestBody Map<String, String> body) {
        String path = body.getOrDefault("path", "");
        String mode = body.getOrDefault("mode", "source");
        return ApiResponse.ok(ragService.reindex(path, mode));
    }

    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild() {
        // 真·全库重建：MySQL 知识条目（列注释拼文本）+ docs-vault 合并重建
        return ApiResponse.ok(vectorRebuildService.rebuildFullLibrary());
    }

    /**
     * 从 MySQL 重新同步某条知识条目到向量库（复用事件监听链路）
     */
    @PostMapping("/items/{itemId}/resync")
    public ApiResponse<Map<String, Object>> resyncItem(@PathVariable Long itemId) {
        eventPublisher.publishKnowledgeItemUpdated(itemId, "manual-resync");
        return ApiResponse.ok(Map.of(
                "status", "success",
                "itemId", itemId,
                "message", "已触发重新同步"
        ));
    }

    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
