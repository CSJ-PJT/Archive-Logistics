package com.csj.archive.logistics.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String traceId,
        int status,
        String code,
        String message,
        List<FieldViolation> fieldErrors,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String code, String message, List<FieldViolation> fieldErrors) {
        return new ErrorResponse(
                TraceIdFilter.currentTraceId(),
                status,
                code,
                message,
                fieldErrors == null ? List.of() : fieldErrors,
                Instant.now()
        );
    }

    public record FieldViolation(String field, String message) {
    }
}
