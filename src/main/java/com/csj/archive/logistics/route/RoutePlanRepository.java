package com.csj.archive.logistics.route;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoutePlanRepository extends JpaRepository<RoutePlanEntity, Long> {
    Optional<RoutePlanEntity> findByRoutePlanId(String routePlanId);

    Optional<RoutePlanEntity> findBySourceEventId(String sourceEventId);

    Page<RoutePlanEntity> findByFactoryId(String factoryId, Pageable pageable);

    long countByDelayedTrue();

    long countByDeviatedTrue();

    long countByRequiresColdChainTrueAndDelayedTrue();

    @Query("""
            select p from RoutePlanEntity p
            where (:factoryId is null or p.factoryId = :factoryId)
              and (:start is null or p.createdAt >= :start)
              and (:end is null or p.createdAt < :end)
            """)
    List<RoutePlanEntity> findForSummary(@Param("factoryId") String factoryId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
