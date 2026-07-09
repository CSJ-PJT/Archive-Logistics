package com.csj.archive.logistics.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RouteCostRepository extends JpaRepository<RouteCostEntity, Long> {
    Optional<RouteCostEntity> findByRoutePlanId(String routePlanId);

    List<RouteCostEntity> findByRoutePlanIdIn(Collection<String> routePlanIds);

    long countByRequiresApprovalTrue();

    @Query("select coalesce(sum(c.totalCost), 0) from RouteCostEntity c")
    Long sumTotalCost();
}
