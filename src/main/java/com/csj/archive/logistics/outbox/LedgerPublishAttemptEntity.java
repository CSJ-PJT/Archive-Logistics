package com.csj.archive.logistics.outbox;

import com.csj.archive.logistics.ledger.LedgerContractMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_publish_attempt")
public class LedgerPublishAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, length = 100)
    private String batchId;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "ledger_enabled", nullable = false)
    private boolean ledgerEnabled;

    @Column(name = "ledger_endpoint", length = 500)
    private String ledgerEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_mode", nullable = false, length = 100)
    private LedgerContractMode contractMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 30)
    private LedgerPublishResultStatus resultStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected LedgerPublishAttemptEntity() {
    }

    public LedgerPublishAttemptEntity(String batchId, int eventCount, int successCount, int failureCount,
                                      boolean ledgerEnabled, String ledgerEndpoint, LedgerContractMode contractMode,
                                      LedgerPublishResultStatus resultStatus, String errorMessage,
                                      LocalDateTime startedAt, LocalDateTime completedAt) {
        this.batchId = batchId;
        this.eventCount = eventCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.ledgerEnabled = ledgerEnabled;
        this.ledgerEndpoint = ledgerEndpoint;
        this.contractMode = contractMode;
        this.resultStatus = resultStatus;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }
}
