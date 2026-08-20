package com.csj.archive.logistics.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LogisticsOutboxRepository extends JpaRepository<LogisticsOutboxEntity, Long> {
    Optional<LogisticsOutboxEntity> findByEventId(String eventId);

    Optional<LogisticsOutboxEntity> findByAggregateId(String aggregateId);

    List<LogisticsOutboxEntity> findByAggregateIdIn(Collection<String> aggregateIds);

    Page<LogisticsOutboxEntity> findByStatus(OutboxStatus status, Pageable pageable);

    List<LogisticsOutboxEntity> findByStatus(OutboxStatus status);

    List<LogisticsOutboxEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OutboxStatus status);

    long countByStatusIn(Collection<OutboxStatus> statuses);

    @Query("select max(e.createdAt) from LogisticsOutboxEntity e")
    LocalDateTime latestCreatedAt();

    @Query("select min(e.createdAt) from LogisticsOutboxEntity e where e.status in :statuses")
    LocalDateTime oldestCreatedAtByStatusIn(@Param("statuses") Collection<OutboxStatus> statuses);

    @Query("""
            select e from LogisticsOutboxEntity e
            where e.status in :statuses
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            order by e.createdAt asc
            """)
    List<LogisticsOutboxEntity> findPublishable(@Param("statuses") Collection<OutboxStatus> statuses,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    @Query(value = """
            select * from logistics_outbox_event
            where payload ->> 'correlationId' = :correlationId
            order by created_at asc
            limit :limit
            """, nativeQuery = true)
    List<LogisticsOutboxEntity> findByCorrelationId(@Param("correlationId") String correlationId,
                                                     @Param("limit") int limit);
}
