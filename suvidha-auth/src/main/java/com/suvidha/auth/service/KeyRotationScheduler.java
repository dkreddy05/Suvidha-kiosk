package com.suvidha.auth.service;

import com.suvidha.auth.model.JwtKeyVersion;
import com.suvidha.auth.repo.JwtKeyVersionRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class KeyRotationScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeyRotationScheduler.class);

    private final RsaKeyService rsaKeyService;
    private final JwtKeyVersionRepo jwtKeyVersionRepo;

    public KeyRotationScheduler(RsaKeyService rsaKeyService, JwtKeyVersionRepo jwtKeyVersionRepo) {
        this.rsaKeyService = rsaKeyService;
        this.jwtKeyVersionRepo = jwtKeyVersionRepo;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void rotateKeysIfNeeded() {
        log.info("Running scheduled JWT key rotation check");
        try {
            Optional<JwtKeyVersion> activeKeyOpt = jwtKeyVersionRepo.findByIsActiveTrue();
            if (activeKeyOpt.isEmpty()) {
                log.warn("No active key found. Generating new key...");
                rsaKeyService.generateNewKey();
                return;
            }

            JwtKeyVersion activeKey = activeKeyOpt.get();
            if (activeKey.getExpiresAt().isBefore(Instant.now())) {
                log.info("Active key {} has expired. Rotating...", activeKey.getKid());
                rsaKeyService.generateNewKey();
                log.info("Key rotation completed successfully");
            } else {
                log.debug("Active key {} is still valid until {}", activeKey.getKid(), activeKey.getExpiresAt());
            }
        } catch (Exception e) {
            log.error("JWT key rotation failed: {}", e.getMessage(), e);
        }
    }
}
