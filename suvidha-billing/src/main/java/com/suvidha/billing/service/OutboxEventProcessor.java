package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.entity.OutboxEvent;
import com.suvidha.billing.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Processes individual outbox events in their own transaction (REQUIRES_NEW).
 * This ensures one event failure doesn't roll back the entire batch.
 *
 * Must be a separate bean from OutboxProcessor so that Spring's AOP proxy
 * properly intercepts the @Transactional annotation.
 */
@Service
public class OutboxEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OutboxEventProcessor(OutboxEventRepository outboxEventRepository,
                                StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a single outbox event in its own transaction.
     * On success the event is marked COMPLETED; on failure it is marked FAILED
     * (with retry logic in {@link OutboxEvent#markFailed}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(OutboxEvent event) {
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
}
