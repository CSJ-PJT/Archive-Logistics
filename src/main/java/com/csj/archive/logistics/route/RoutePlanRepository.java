package com.csj.archive.logistics.route;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlanEntity, Long> {
    Optional<RoutePlanEntity> findByRoutePlanId(String routePlanId);

    Optional<RoutePlanEntity> findBySourceEventId(String sourceEventId);

    List<RoutePlanEntity> findByFactoryId(String factoryId);

    Page<RoutePlanEntity> findByFactoryId(String factoryId, Pageable pageable);

    long countByDelayedTrue();

    long countByDeviatedTrue();

    long countByRequiresColdChainTrueAndDelayedTrue();

    List<RoutePlanEntity> findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String factoryId, 
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    List<RoutePlanEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);
}
