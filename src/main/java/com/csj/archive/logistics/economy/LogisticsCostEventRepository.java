package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsCostEventEntity;
import com.csj.archive.logistics.economy.model.LogisticsCostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Collection;

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

    @Query("""
            select coalesce(sum(e.costAmount), 0) from LogisticsCostEventEntity e
            where e.createdAt >= :fromInclusive and e.createdAt <= :toInclusive
              and (e.simulationRunId is null or e.simulationRunId not like 'SIM-RUNTIME-%')
            """)
    Long sumRealizedNonRuntimeCostBetween(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toInclusive") LocalDateTime toInclusive
    );

    @Query("""
            select max(e.createdAt) from LogisticsCostEventEntity e
            where e.createdAt >= :fromInclusive and e.createdAt <= :toInclusive
              and (e.simulationRunId is null or e.simulationRunId not like 'SIM-RUNTIME-%')
            """)
    Optional<LocalDateTime> findLatestRealizedNonRuntimeCreatedAtBetween(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toInclusive") LocalDateTime toInclusive
    );

    @Query("select coalesce(sum(e.costAmount), 0) from LogisticsCostEventEntity e where e.costType in :types")
    Long sumCostByCostTypeIn(@Param("types") Collection<LogisticsCostType> types);
}
