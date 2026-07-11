package com.csj.archive.logistics.runtime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ShipmentRuntimeEventRepository extends JpaRepository<ShipmentRuntimeEventEntity, Long> {
    boolean existsByRoutePlanIdAndEventType(String routePlanId, String eventType);
    List<ShipmentRuntimeEventEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);
    List<ShipmentRuntimeEventEntity> findByCorrelationIdOrderByOccurredAtDesc(String correlationId);
    List<ShipmentRuntimeEventEntity> findByShipmentIdOrderByOccurredAtDesc(String shipmentId);
    List<ShipmentRuntimeEventEntity> findByRoutePlanIdIn(Collection<String> routePlanIds);
}
