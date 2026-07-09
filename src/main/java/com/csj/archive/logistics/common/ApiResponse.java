package com.csj.archive.logistics.common;

public record ApiResponse<T>(
        String traceId,
        T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(TraceIdFilter.currentTraceId(), data);
    }
}
