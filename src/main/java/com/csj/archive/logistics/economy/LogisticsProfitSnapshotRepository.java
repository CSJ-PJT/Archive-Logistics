package com.csj.archive.logistics.economy;

import com.csj.archive.logistics.economy.model.LogisticsProfitSnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface LogisticsProfitSnapshotRepository extends JpaRepository<LogisticsProfitSnapshotEntity, String> {
    Optional<LogisticsProfitSnapshotEntity> findTopByOrderBySettlementDateDesc();

    Optional<LogisticsProfitSnapshotEntity> findTopByOrderByCreatedAtDesc();

    Page<LogisticsProfitSnapshotEntity> findBySettlementDate(LocalDate settlementDate, Pageable pageable);

    List<LogisticsProfitSnapshotEntity> findTop30ByOrderByCreatedAtDesc();
}
