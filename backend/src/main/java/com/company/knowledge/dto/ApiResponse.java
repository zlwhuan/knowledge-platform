package com.company.knowledge.dto;

public record ApiResponse<T>(boolean success, String message, T data, Long total, Integer page, Integer size, Integer pages) {
    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, null, null, null, null);
    }

    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "ok", data); }
    public static <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(true, message, data); }
    public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(false, message, null); }

    public static <T> ApiResponse<T> paged(T data, long total, int page, int size, int pages) {
        return new ApiResponse<>(true, "ok", data, total, page, size, pages);
    }
}
