package com.csj.archive.logistics.runtime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface ArchiveOsRuntimeDeliveryRepository extends JpaRepository<ArchiveOsRuntimeDeliveryEntity, Long> {
    boolean existsByEventId(String eventId);
    List<ArchiveOsRuntimeDeliveryEntity> findByStatusIn(Collection<ArchiveOsDeliveryStatus> status, Pageable pageable);
    List<ArchiveOsRuntimeDeliveryEntity> findByCorrelationId(String correlationId);
    long countByStatus(ArchiveOsDeliveryStatus status);
    List<ArchiveOsRuntimeDeliveryEntity> findByStatus(ArchiveOsDeliveryStatus status, Pageable pageable);
    List<ArchiveOsRuntimeDeliveryEntity> findByCorrelationId(String correlationId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from ArchiveOsRuntimeDeliveryEntity d
            where d.status = :status and d.publishedAt is null
              and (d.publishingStartedAt <= :cutoff or (d.publishingStartedAt is null and d.updatedAt <= :cutoff))
            order by d.updatedAt asc
            """)
    List<ArchiveOsRuntimeDeliveryEntity> findStalePublishing(@Param("status") ArchiveOsDeliveryStatus status,
                                                              @Param("cutoff") LocalDateTime cutoff,
                                                              Pageable pageable);
}
