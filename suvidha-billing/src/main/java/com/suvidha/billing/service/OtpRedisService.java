package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class OtpRedisService {

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final Duration OTP_ATTEMPT_TTL = Duration.ofMinutes(15);
    private static final Pattern MOBILE_SANITIZE_PATTERN = Pattern.compile("[^0-9+]");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public OtpRedisService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Sanitizes a mobile number by stripping non-numeric characters (except +).
     * This is the ONLY place sanitization should happen — callers sanitize first,
     * then pass the sanitized value to all other methods.
     */
    public String sanitizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            throw new IllegalArgumentException("Mobile number cannot be null or blank");
        }
        String sanitized = MOBILE_SANITIZE_PATTERN.matcher(mobile.trim()).replaceAll("");
        if (sanitized.length() < 10 || sanitized.length() > 15) {
            throw new IllegalArgumentException("Invalid mobile number format");
        }
        return sanitized;
    }

    /**
     * Stores a BCrypt-hashed OTP for an already-sanitized mobile number.
     * Callers must sanitize the mobile before calling this method.
     */
    public void storeOtp(String sanitizedMobile, String otp, Duration ttl) {
        String hash = passwordEncoder.encode(otp);
        redis.opsForValue().set(keyOtp(sanitizedMobile), hash, ttl);
        resetOtpAttempts(sanitizedMobile);
    }

    /**
     * Verifies a raw OTP against the stored BCrypt hash for an already-sanitized mobile.
     * Returns true if the OTP matches and the key exists, false otherwise.
     */
    public boolean verifyOtp(String sanitizedMobile, String rawOtp) {
        String hash = redis.opsForValue().get(keyOtp(sanitizedMobile));
        if (hash == null) {
            return false;
        }
        return passwordEncoder.matches(rawOtp, hash);
    }

    /**
     * Retrieves a stored OTP hash for an already-sanitized mobile number.
     * @deprecated Use {@link #verifyOtp} instead — this returns the BCrypt hash, not the raw OTP.
     */
    @Deprecated
    public Optional<String> getOtp(String sanitizedMobile) {
        return Optional.ofNullable(redis.opsForValue().get(keyOtp(sanitizedMobile)));
    }

    /**
     * Stores a cooldown flag for an already-sanitized mobile number.
     */
    public void storeCooldown(String sanitizedMobile, Duration cooldown) {
        redis.opsForValue().set(keyCooldown(sanitizedMobile), "1", cooldown);
    }

    /**
     * Checks if the cooldown is active for an already-sanitized mobile number.
     */
    public boolean isCooldownActive(String sanitizedMobile) {
        Boolean has = redis.hasKey(keyCooldown(sanitizedMobile));
        return Boolean.TRUE.equals(has);
    }

    /**
     * Stores OTP context for an already-sanitized mobile number.
     */
    public void storeContext(String sanitizedMobile, OtpContext ctx, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(ctx);
            redis.opsForValue().set(keyCtx(sanitizedMobile), json, ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize otp context", e);
        }
    }

    /**
     * Retrieves OTP context for an already-sanitized mobile number.
     */
    public Optional<OtpContext> getContext(String sanitizedMobile) {
        String json = redis.opsForValue().get(keyCtx(sanitizedMobile));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, OtpContext.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize otp context", e);
        }
    }

    /**
     * Deletes all OTP-related keys for an already-sanitized mobile number.
     */
    public void deleteAllKeys(String sanitizedMobile) {
        redis.delete(keyOtp(sanitizedMobile));
        redis.delete(keyCooldown(sanitizedMobile));
        redis.delete(keyCtx(sanitizedMobile));
        redis.delete(keyAttempts(sanitizedMobile));
    }

    /**
     * Increments OTP attempt counter for an already-sanitized mobile number.
     */
    public void incrementOtpAttempts(String sanitizedMobile) {
        String key = keyAttempts(sanitizedMobile);
        Long attempts = redis.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redis.expire(key, OTP_ATTEMPT_TTL);
        }
    }

    /**
     * Returns the OTP attempt count for an already-sanitized mobile number.
     */
    public int getOtpAttempts(String sanitizedMobile) {
        String value = redis.opsForValue().get(keyAttempts(sanitizedMobile));
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Resets OTP attempts for an already-sanitized mobile number.
     */
    public void resetOtpAttempts(String sanitizedMobile) {
        redis.delete(keyAttempts(sanitizedMobile));
    }

    /**
     * Checks if the user has exceeded maximum OTP attempts.
     */
    public boolean isOtpLocked(String sanitizedMobile) {
        int attempts = getOtpAttempts(sanitizedMobile);
        return attempts >= MAX_OTP_ATTEMPTS;
    }

    private static String keyOtp(String mobile) {
        return "link_otp:" + mobile;
    }

    private static String keyCooldown(String mobile) {
        return "link_cooldown:" + mobile;
    }

    private static String keyCtx(String mobile) {
        return "link_ctx:" + mobile;
    }

    private static String keyAttempts(String mobile) {
        return "link_attempts:" + mobile;
    }
}
