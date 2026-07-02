package com.suvidha.auth.service;

import com.suvidha.auth.model.AuditLog;
import com.suvidha.auth.repo.AuditLogRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepo auditLogRepo;

    public AuditService(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    public void log(String action, String citizenId, String details, HttpServletRequest request) {
        String ip = request != null ? request.getRemoteAddr() : null;
        String ua = request != null ? request.getHeader("User-Agent") : null;
        AuditLog log = new AuditLog(action, citizenId, details, ip, ua);
        auditLogRepo.save(log);
    }

    public void log(String action, String citizenId, String details) {
        log(action, citizenId, details, null);
    }

    public Page<AuditLog> getLogs(String citizenId, Pageable pageable) {
        return auditLogRepo.findByCitizenIdOrderByCreatedAtDesc(citizenId, pageable);
    }

    public int cleanupOldLogs(Instant cutoff) {
        return auditLogRepo.deleteByCreatedAtBefore(cutoff);
    }
}
