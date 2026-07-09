package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.entity.OutboxEvent;
import com.suvidha.billing.entity.OutboxEvent.OutboxStatus;
import com.suvidha.billing.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OutboxProcessor(OutboxEventRepository outboxEventRepository,
                          StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}", initialDelayString = "${outbox.initial-delay-ms:5000}")
    @Transactional
    public void processOutboxEvents() {
        try {
            var pendingEvents = outboxEventRepository.findPendingEvents(DEFAULT_BATCH_SIZE);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            log.debug("Processing {} pending outbox events", pendingEvents.size());
            for (OutboxEvent event : pendingEvents) {
                processEvent(event);
            }
        } catch (Exception e) {
            log.error("Error in outbox processor", e);
        }
    }

    private void processEvent(OutboxEvent event) {
        try {
            event.markProcessing();
            outboxEventRepository.save(event);

            switch (event.getEventType()) {
                case "OTP_SENT" -> handleOtpSent(event);
                case "ACCOUNT_LINKED" -> handleAccountLinked(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            event.markCompleted();
            outboxEventRepository.save(event);
            log.debug("Successfully processed outbox event: {}", event.getId());

        } catch (Exception e) {
            log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
            event.markFailed(e.getMessage());
            outboxEventRepository.save(event);
        }
    }

    private void handleOtpSent(OutboxEvent event) throws Exception {
        Map<String, String> payload = objectMapper.readValue(event.getPayload(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        String mobile = payload.get("mobile");
        String otp = payload.get("otp");
        Duration ttl = Duration.ofMinutes(5);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String hash = Base64.getEncoder().encodeToString(digest.digest(otp.getBytes(StandardCharsets.UTF_8)));
        redisTemplate.opsForValue().set("link_otp:" + mobile, hash, ttl);
        redisTemplate.opsForValue().set("link_cooldown:" + mobile, "1", Duration.ofMinutes(2));

        log.info("Outbox published OTP for mobile ending in: {}",
                mobile.substring(mobile.length() - 4));
    }

    private void handleAccountLinked(OutboxEvent event) throws Exception {
        Map<String, String> payload = objectMapper.readValue(event.getPayload(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        String citizenId = payload.get("citizenId");
        String consumerNo = payload.get("consumerNo");
        String key = "account_linked:" + citizenId + ":" + consumerNo;

        redisTemplate.opsForValue().set(key, "1", Duration.ofDays(30));

        log.info("Outbox published account linked event for citizen: {}", citizenId);
    }

    @Scheduled(cron = "${outbox.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupCompletedEvents() {
        var cutoff = java.time.LocalDateTime.now().minusDays(7);
        int deleted = outboxEventRepository.deleteCompletedEventsBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} completed outbox events older than {}", deleted, cutoff);
        }
    }
}
