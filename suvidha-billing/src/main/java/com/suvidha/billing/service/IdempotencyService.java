package com.suvidha.billing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.response.PaymentConfirmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "idempotency:billing:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<PaymentConfirmDTO> getCachedResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, PaymentConfirmDTO.class));
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached idempotency response for key={}: {}", idempotencyKey, e.getMessage());
        }
        return Optional.empty();
    }

    public void cacheResponse(String idempotencyKey, PaymentConfirmDTO response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response for key={}", idempotencyKey, e);
        }
    }
}
