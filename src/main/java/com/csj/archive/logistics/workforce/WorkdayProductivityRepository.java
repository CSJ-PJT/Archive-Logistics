package com.csj.archive.logistics.workforce;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkdayProductivityRepository extends JpaRepository<WorkdayProductivityEntity, Long> {
    Optional<WorkdayProductivityEntity> findTopByOrderByWorkDateDescCreatedAtDesc();

    Optional<WorkdayProductivityEntity> findByWorkDate(LocalDate workDate);
}
