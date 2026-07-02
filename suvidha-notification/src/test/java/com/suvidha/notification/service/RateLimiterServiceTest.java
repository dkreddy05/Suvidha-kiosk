package com.suvidha.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("isAllowed denies when Redis is null (fails closed)")
    void isAllowed_deniesWhenRedisIsNull() {
        RateLimiterService service = new RateLimiterService(Optional.empty());

        boolean result = service.isAllowed("+919876543210");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAllowed denies when Redis throws exception (fails closed)")
    void isAllowed_deniesWhenRedisThrows() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = service.isAllowed("+919876543210");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAllowed allows first request within window")
    void isAllowed_allowsFirstRequest() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));
        when(valueOperations.get("ratelimit:otp:+919876543210")).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        boolean result = service.isAllowed("+919876543210");

        assertThat(result).isTrue();
        verify(valueOperations).set(eq("ratelimit:otp:+919876543210"), eq("1"), any());
    }

    @Test
    @DisplayName("isAllowed allows up to MAX_ATTEMPTS requests")
    void isAllowed_allowsUpToMaxAttempts() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(valueOperations.get("ratelimit:otp:+919876543210"))
                .thenReturn(null)
                .thenReturn("1")
                .thenReturn("2");
        when(valueOperations.increment("ratelimit:otp:+919876543210")).thenReturn(2L).thenReturn(3L);

        assertThat(service.isAllowed("+919876543210")).isTrue();
        assertThat(service.isAllowed("+919876543210")).isTrue();
        assertThat(service.isAllowed("+919876543210")).isTrue();
    }

    @Test
    @DisplayName("isAllowed denies after MAX_ATTEMPTS exceeded")
    void isAllowed_deniesAfterMaxAttempts() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(valueOperations.get("ratelimit:otp:+919876543210"))
                .thenReturn(null)
                .thenReturn("1")
                .thenReturn("2")
                .thenReturn("3");

        assertThat(service.isAllowed("+919876543210")).isTrue();
        assertThat(service.isAllowed("+919876543210")).isTrue();
        assertThat(service.isAllowed("+919876543210")).isTrue();
        assertThat(service.isAllowed("+919876543210")).isFalse();
    }

    @Test
    @DisplayName("isAllowed denies consistently when Redis is down (circuit breaker)")
    void isAllowed_deniesConsistentlyWhenRedisDown() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertThat(service.isAllowed("+919876543210")).isFalse();
        assertThat(service.isAllowed("+919876543211")).isFalse();
        assertThat(service.isAllowed("+919876543212")).isFalse();
    }

    @Test
    @DisplayName("isAllowed recovers when Redis comes back online")
    void isAllowed_recoversWhenRedisBack() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        assertThat(service.isAllowed("+919876543210")).isFalse();

        reset(valueOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        assertThat(service.isAllowed("+919876543210")).isTrue();
    }

    @Test
    @DisplayName("isAllowed treats each phone number independently")
    void isAllowed_treatsPhoneNumbersIndependently() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(valueOperations.get("ratelimit:otp:+911111111111")).thenReturn("3");
        when(valueOperations.get("ratelimit:otp:+912222222222")).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        assertThat(service.isAllowed("+911111111111")).isFalse();
        assertThat(service.isAllowed("+912222222222")).isTrue();
    }
}
