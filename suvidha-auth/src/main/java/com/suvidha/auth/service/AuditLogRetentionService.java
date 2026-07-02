package com.suvidha.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);
    private static final long RETENTION_DAYS = 730;

    private final AuditService auditService;

    public AuditLogRetentionService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldAuditLogs() {
        Instant cutoff = Instant.now().minusSeconds(RETENTION_DAYS * 86400L);
        int deleted = auditService.cleanupOldLogs(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} audit log records older than {} days", deleted, RETENTION_DAYS);
        }
    }
}
