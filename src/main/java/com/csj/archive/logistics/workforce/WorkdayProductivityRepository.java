package com.csj.archive.logistics.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkdayProductivityRepository extends JpaRepository<WorkdayProductivityEntity, Long> {
    Optional<WorkdayProductivityEntity> findTopByOrderByWorkDateDescCreatedAtDesc();

    Optional<WorkdayProductivityEntity> findByWorkDate(LocalDate workDate);

    List<WorkdayProductivityEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
