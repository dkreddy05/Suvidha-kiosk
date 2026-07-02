package com.suvidha.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final String RATE_LIMIT_PREFIX = "ratelimit:otp:";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);

    public RateLimiterService(Optional<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate.orElse(null);
    }

    /**
     * Checks whether an OTP request for the given phone number is allowed.
     *
     * <p>Fails closed: if Redis is unavailable (null or throws), the request is
     * denied and a circuit-breaker alert is logged once per outage window.</p>
     *
     * @param phoneNumber the target phone number
     * @return true if the request is within rate limits, false otherwise
     */
    public boolean isAllowed(String phoneNumber) {
        if (redisTemplate == null) {
            logCircuitBreakerAlert("Redis is not configured — rate limiter denying all requests");
            return false;
        }
        try {
            String key = RATE_LIMIT_PREFIX + phoneNumber;
            String count = redisTemplate.opsForValue().get(key);
            if (count == null) {
                redisTemplate.opsForValue().set(key, "1", WINDOW);
                return true;
            }
            long current = Long.parseLong(count);
            if (current >= MAX_ATTEMPTS) {
                log.warn("Rate limit exceeded for phone number: {}", phoneNumber);
                return false;
            }
            redisTemplate.opsForValue().increment(key);
            circuitOpen.set(false);
            return true;
        } catch (Exception e) {
            logCircuitBreakerAlert("Redis unavailable — rate limiter denying all requests: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Logs a circuit-breaker alert at most once per outage window to avoid
     * log flooding while Redis is down.
     */
    private void logCircuitBreakerAlert(String message, Object... args) {
        if (circuitOpen.compareAndSet(false, true)) {
            log.error("CIRCUIT_BREAKER_OPEN: RateLimiterService — " + message, args);
        }
    }
}
