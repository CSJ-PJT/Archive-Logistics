package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsCostEventEntity;
import com.csj.archive.logistics.economy.model.LogisticsCostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LogisticsCostEventRepository extends JpaRepository<LogisticsCostEventEntity, Long> {
    Optional<LogisticsCostEventEntity> findByEventId(String eventId);

    Optional<LogisticsCostEventEntity> findByIdempotencyKey(String idempotencyKey);

    Page<LogisticsCostEventEntity> findBySettlementCycleId(String settlementCycleId, Pageable pageable);

    Page<LogisticsCostEventEntity> findByPaidToService(String paidToService, Pageable pageable);

    Page<LogisticsCostEventEntity> findByCostType(LogisticsCostType costType, Pageable pageable);

    @Query("""
            select coalesce(sum(e.costAmount), 0) from LogisticsCostEventEntity e
            """)
    Long sumCost();
}

