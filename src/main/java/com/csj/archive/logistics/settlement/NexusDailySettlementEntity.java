package com.csj.archive.logistics.settlement;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nexus_daily_settlement")
public class NexusDailySettlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, unique = true, length = 100)
    private String settlementId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "factory_id", nullable = false, length = 50)
    private String factoryId;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "total_shipments", nullable = false)
    private int totalShipments;

    @Column(name = "delayed_shipments", nullable = false)
    private int delayedShipments;

    @Column(name = "held_shipments", nullable = false)
    private int heldShipments;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    @Column(name = "total_logistics_cost", nullable = false)
    private long totalLogisticsCost;

    @Column(name = "manufacturing_impact_cost", nullable = false)
    private long manufacturingImpactCost;

    @Column(name = "manufacturing_share_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal manufacturingShareRate;

    @Column(name = "on_time_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal onTimeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NexusDailySettlementStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error")
    private String lastError;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nexus_response", columnDefinition = "jsonb")
    private JsonNode nexusResponse;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NexusDailySettlementEntity() {
    }

    public NexusDailySettlementEntity(NexusDailySettlementDraft draft, LocalDateTime now) {
        this.settlementId = draft.settlementId();
        this.idempotencyKey = draft.idempotencyKey();
        this.source = draft.source();
        this.settlementDate = draft.settlementDate();
        this.factoryId = draft.factoryId();
        this.currency = draft.currency();
        this.status = NexusDailySettlementStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = now;
        apply(draft, now);
    }

    public void apply(NexusDailySettlementDraft draft, LocalDateTime now) {
        this.totalShipments = draft.totalShipments();
        this.delayedShipments = draft.delayedShipments();
        this.heldShipments = draft.heldShipments();
        this.totalQuantity = draft.totalQuantity();
        this.totalLogisticsCost = draft.totalLogisticsCost();
        this.manufacturingImpactCost = draft.manufacturingImpactCost();
        this.manufacturingShareRate = draft.manufacturingShareRate();
        this.onTimeRate = draft.onTimeRate();
        this.updatedAt = now;
    }

    public void markSent(JsonNode response, LocalDateTime now) {
        this.status = NexusDailySettlementStatus.SENT;
        this.nexusResponse = response;
        this.sentAt = now;
        this.lastError = null;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }

    public void markDryRun(LocalDateTime now) {
        this.status = NexusDailySettlementStatus.DRY_RUN;
        this.sentAt = null;
        this.lastError = null;
        this.nextRetryAt = null;
        this.updatedAt = now;
    }

    public void scheduleRetry(String errorMessage, int maxRetryCount, LocalDateTime now) {
        this.retryCount += 1;
        this.lastError = trim(errorMessage);
        this.updatedAt = now;
        if (retryCount >= maxRetryCount) {
            this.status = NexusDailySettlementStatus.FAILED;
            this.nextRetryAt = null;
            return;
        }
        this.status = NexusDailySettlementStatus.RETRY;
        long delayMinutes = Math.min(60, 1L << Math.min(retryCount, 5));
        this.nextRetryAt = now.plusMinutes(delayMinutes);
    }

    public String settlementId() {
        return settlementId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public String source() {
        return source;
    }

    public LocalDate settlementDate() {
        return settlementDate;
    }

    public String factoryId() {
        return factoryId;
    }

    public String currency() {
        return currency;
    }

    public int totalShipments() {
        return totalShipments;
    }

    public int delayedShipments() {
        return delayedShipments;
    }

    public int heldShipments() {
        return heldShipments;
    }

    public long totalQuantity() {
        return totalQuantity;
    }

    public long totalLogisticsCost() {
        return totalLogisticsCost;
    }

    public long manufacturingImpactCost() {
        return manufacturingImpactCost;
    }

    public BigDecimal manufacturingShareRate() {
        return manufacturingShareRate;
    }

    public BigDecimal onTimeRate() {
        return onTimeRate;
    }

    public NexusDailySettlementStatus status() {
        return status;
    }

    public int retryCount() {
        return retryCount;
    }

    public String lastError() {
        return lastError;
    }

    public LocalDateTime sentAt() {
        return sentAt;
    }

    public LocalDateTime nextRetryAt() {
        return nextRetryAt;
    }

    private String trim(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
