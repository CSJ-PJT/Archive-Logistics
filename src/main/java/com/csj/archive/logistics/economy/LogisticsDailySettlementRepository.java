package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsDailySettlementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LogisticsDailySettlementRepository extends JpaRepository<LogisticsDailySettlementEntity, Long> {
    Optional<LogisticsDailySettlementEntity> findBySettlementId(String settlementId);

    Optional<LogisticsDailySettlementEntity> findBySettlementCycleId(String settlementCycleId);

    List<LogisticsDailySettlementEntity> findBySettledAt(LocalDate settledAt);

    List<LogisticsDailySettlementEntity> findBySettledAtAndFactoryId(LocalDate settledAt, String factoryId);

    Page<LogisticsDailySettlementEntity> findBySettledAt(LocalDate settledAt, Pageable pageable);

    List<LogisticsDailySettlementEntity> findBySettledAtAndBilledToService(LocalDate settledAt, String billedToService);
}
