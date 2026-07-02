package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpRedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OtpRedisService service;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new OtpRedisService(redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("storeOtp stores BCrypt hash, not plaintext")
    void storeOtp_storesBcryptHashNotPlaintext() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        service.storeOtp("9876543210", "123456", Duration.ofMinutes(5));

        verify(valueOperations).set(
                eq("link_otp:9876543210"),
                argThat(hash -> hash != null && hash.startsWith("$2a$")),
                any(Duration.class));
    }

    @Test
    @DisplayName("storeOtp does not store raw OTP value")
    void storeOtp_doesNotStoreRawOtp() {
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        service.storeOtp("9876543210", "123456", Duration.ofMinutes(5));

        verify(valueOperations).set(
                eq("link_otp:9876543210"),
                argThat(hash -> !hash.equals("123456")),
                any(Duration.class));
    }

    @Test
    @DisplayName("verifyOtp returns true for correct OTP")
    void verifyOtp_returnsTrueForCorrectOtp() {
        String hash = encoder.encode("123456");
        when(valueOperations.get("link_otp:9876543210")).thenReturn(hash);

        boolean result = service.verifyOtp("9876543210", "123456");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("verifyOtp returns false for wrong OTP")
    void verifyOtp_returnsFalseForWrongOtp() {
        String hash = encoder.encode("123456");
        when(valueOperations.get("link_otp:9876543210")).thenReturn(hash);

        boolean result = service.verifyOtp("9876543210", "654321");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifyOtp returns false when no OTP stored")
    void verifyOtp_returnsFalseWhenNoOtpStored() {
        when(valueOperations.get("link_otp:9876543210")).thenReturn(null);

        boolean result = service.verifyOtp("9876543210", "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("verifyOtp returns false for empty stored value")
    void verifyOtp_returnsFalseForEmptyStoredValue() {
        when(valueOperations.get("link_otp:9876543210")).thenReturn("");

        boolean result = service.verifyOtp("9876543210", "123456");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getOtp returns the BCrypt hash (deprecated behavior)")
    void getOtp_returnsBcryptHash() {
        String hash = encoder.encode("123456");
        when(valueOperations.get("link_otp:9876543210")).thenReturn(hash);

        var result = service.getOtp("9876543210");

        assertThat(result).isPresent();
        assertThat(result.get()).startsWith("$2a$");
        assertThat(result.get()).isNotEqualTo("123456");
    }
}
