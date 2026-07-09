package com.csj.archive.logistics.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    long countByAction(AuditAction action);
}
