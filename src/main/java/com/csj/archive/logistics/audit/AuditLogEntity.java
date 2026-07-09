package com.csj.archive.logistics.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    @Column(name = "before_status", length = 50)
    private String beforeStatus;

    @Column(name = "after_status", length = 50)
    private String afterStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private JsonNode detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(
            String traceId,
            String actor,
            AuditAction action,
            String targetType,
            String targetId,
            String beforeStatus,
            String afterStatus,
            JsonNode detail,
            LocalDateTime createdAt
    ) {
        this.traceId = traceId;
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.beforeStatus = beforeStatus;
        this.afterStatus = afterStatus;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long id() {
        return id;
    }

    public AuditAction action() {
        return action;
    }
}
