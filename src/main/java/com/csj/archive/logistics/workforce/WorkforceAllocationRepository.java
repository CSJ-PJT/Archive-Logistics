package com.csj.archive.logistics.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkforceAllocationRepository extends JpaRepository<WorkforceAllocationEntity, Long> {
    Optional<WorkforceAllocationEntity> findTopByWorkDateLessThanEqualOrderByWorkDateDescCreatedAtDesc(LocalDate workDate);
}
