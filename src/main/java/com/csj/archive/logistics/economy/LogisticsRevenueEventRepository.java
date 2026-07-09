package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsRevenueEventEntity;
import com.csj.archive.logistics.economy.model.LogisticsRevenueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LogisticsRevenueEventRepository extends JpaRepository<LogisticsRevenueEventEntity, Long> {
    Optional<LogisticsRevenueEventEntity> findByEventId(String eventId);

    Optional<LogisticsRevenueEventEntity> findByIdempotencyKey(String idempotencyKey);

    Page<LogisticsRevenueEventEntity> findBySettlementCycleId(String settlementCycleId, Pageable pageable);

    Page<LogisticsRevenueEventEntity> findByBilledToService(String billedToService, Pageable pageable);

    Page<LogisticsRevenueEventEntity> findByRevenueType(LogisticsRevenueType revenueType, Pageable pageable);

    @Query("""
            select coalesce(sum(e.revenueAmount), 0) from LogisticsRevenueEventEntity e
            """)
    Long sumRevenue();
}

