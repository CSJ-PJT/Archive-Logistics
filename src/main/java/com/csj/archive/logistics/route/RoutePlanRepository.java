package com.csj.archive.logistics.route;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    long countByDelayedTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    long countByOrderIdNotNull();

    long countByExpressOrderTrue();

    long countByCustomerType(String customerType);

    @Query("select count(p) from RoutePlanEntity p where (p.customerType = :riskCustomerType) or (coalesce(p.riskLevel, 0) >= :riskLevelThreshold)")
    long countByHighRiskCustomerOrRiskLevel(@Param("riskCustomerType") String riskCustomerType,
                                           @Param("riskLevelThreshold") int riskLevelThreshold);

    List<RoutePlanEntity> findByFactoryIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String factoryId, 
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    List<RoutePlanEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    @Query("""
            select distinct p.factoryId from RoutePlanEntity p
            where p.createdAt >= :start and p.createdAt < :end
            order by p.factoryId
            """)
    List<String> findDistinctFactoryIdsByCreatedAtBetween(@Param("start") java.time.LocalDateTime start,
                                                          @Param("end") java.time.LocalDateTime end);
}
