package com.csj.archive.logistics.audit;

import com.csj.archive.logistics.common.TraceIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuditLogService {
    private static final String SYSTEM_ACTOR = "Archive-Logitics";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(AuditAction action, String targetType, String targetId, String beforeStatus, String afterStatus, Object detail) {
        JsonNode detailNode = detail == null ? null : objectMapper.valueToTree(detail);
        auditLogRepository.save(new AuditLogEntity(
                TraceIdFilter.currentTraceId(),
                SYSTEM_ACTOR,
                action,
                targetType,
                targetId,
                beforeStatus,
                afterStatus,
                detailNode,
                LocalDateTime.now(clock)
        ));
    }

    public long countDuplicates() {
        return auditLogRepository.countByAction(AuditAction.DUPLICATE_EVENT_RECEIVED);
    }
}
