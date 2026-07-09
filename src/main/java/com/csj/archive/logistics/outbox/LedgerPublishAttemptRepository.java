package com.csj.archive.logistics.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerPublishAttemptRepository extends JpaRepository<LedgerPublishAttemptEntity, Long> {
}
