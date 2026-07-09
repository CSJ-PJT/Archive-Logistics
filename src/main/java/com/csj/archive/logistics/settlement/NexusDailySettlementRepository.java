package com.csj.archive.logistics.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NexusDailySettlementRepository extends JpaRepository<NexusDailySettlementEntity, Long> {
    Optional<NexusDailySettlementEntity> findBySettlementId(String settlementId);

    Optional<NexusDailySettlementEntity> findBySettlementDateAndFactoryId(LocalDate settlementDate, String factoryId);

    Page<NexusDailySettlementEntity> findBySettlementDate(LocalDate settlementDate, Pageable pageable);

    List<NexusDailySettlementEntity> findBySettlementDate(LocalDate settlementDate);

    long countByStatus(NexusDailySettlementStatus status);
}
