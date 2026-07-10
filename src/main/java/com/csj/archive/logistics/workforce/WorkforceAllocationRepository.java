package com.csj.archive.logistics.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkforceAllocationRepository extends JpaRepository<WorkforceAllocationEntity, Long> {
    Optional<WorkforceAllocationEntity> findTopByWorkDateLessThanEqualOrderByWorkDateDescCreatedAtDesc(LocalDate workDate);

    List<WorkforceAllocationEntity> findByWorkDate(LocalDate workDate);

    List<WorkforceAllocationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByWorkdayIdAndRoleType(String workdayId, String roleType);
}
