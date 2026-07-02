package com.suvidha.auth.repo;

import com.suvidha.auth.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByCitizenIdOrderByCreatedAtDesc(String citizenId, Pageable pageable);
    int deleteByCreatedAtBefore(Instant cutoff);
}
