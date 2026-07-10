package com.csj.archive.logistics.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface NexusEventRepository extends JpaRepository<NexusEventEntity, Long> {
    Optional<NexusEventEntity> findByEventId(String eventId);

    Optional<NexusEventEntity> findByIdempotencyKey(String idempotencyKey);

    List<NexusEventEntity> findByEventIdIn(Collection<String> eventIds);

    List<NexusEventEntity> findByIdempotencyKeyIn(Collection<String> idempotencyKeys);

    long countByStatus(NexusEventStatus status);

    List<NexusEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select max(e.createdAt) from NexusEventEntity e")
    LocalDateTime latestCreatedAt();
}
