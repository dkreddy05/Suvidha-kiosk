package com.suvidha.auth.service;

import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.exception.InvalidRequestException;
import com.suvidha.auth.exception.OtpRateLimitExceededException;
import com.suvidha.auth.exception.OtpSessionInvalidException;
import com.suvidha.auth.exception.OtpIncorrectException;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.service.impl.AuthenticationServiceImpl;
import com.suvidha.auth.token.JwtToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private CitizenRepo citizenRepo;

    @Mock
    private JwtToken jwtToken;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private AuthenticationServiceImpl authenticationService;

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("otp:" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(redisTemplate, citizenRepo, jwtToken);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("sendOtp should generate OTP and return session ID")
    void sendOtp_shouldGenerateOtpAndReturnSessionId() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        StringBuilder devOtp = new StringBuilder();
        String sessionId = authenticationService.sendOtp(null, "9876543210", devOtp);

        assertNotNull(sessionId);
        assertFalse(devOtp.toString().isEmpty());
        assertEquals(6, devOtp.toString().length());
        verify(hashOperations).putAll(eq("otp:" + sessionId), anyMap());
    }

    @Test
    @DisplayName("sendOtp should throw when mobile is blank")
    void sendOtp_shouldThrowWhenMobileIsBlank() {
        assertThrows(InvalidRequestException.class,
                () -> authenticationService.sendOtp(null, "", null));
    }

    @Test
    @DisplayName("sendOtp should reuse existing session ID if provided")
    void sendOtp_shouldReuseExistingSessionId() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        String sessionId = authenticationService.sendOtp("existing-session", "9876543210", null);

        assertEquals("existing-session", sessionId);
    }

    @Test
    @DisplayName("sendOtp should throw rate limit exception after 3 requests")
    void sendOtp_shouldThrowRateLimitAfterThreeRequests() {
        when(valueOperations.increment(anyString())).thenReturn(4L);

        assertThrows(OtpRateLimitExceededException.class,
                () -> authenticationService.sendOtp(null, "9876543210", null));
    }

    @Test
    @DisplayName("verifyOtp should throw when sessionId is blank")
    void verifyOtp_shouldThrowWhenSessionIdBlank() {
        assertThrows(InvalidRequestException.class,
                () -> authenticationService.verifyOtp("", "123456"));
    }

    @Test
    @DisplayName("verifyOtp should throw when OTP session is expired")
    void verifyOtp_shouldThrowWhenSessionExpired() {
        when(hashOperations.entries(anyString())).thenReturn(Collections.emptyMap());

        assertThrows(OtpSessionInvalidException.class,
                () -> authenticationService.verifyOtp("session-id", "123456"));
    }

    @Test
    @DisplayName("verifyOtp should throw when OTP is incorrect")
    void verifyOtp_shouldThrowWhenOtpIncorrect() {
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("mobile", "9876543210");
        sessionData.put("otp", hashOtp("123456"));
        sessionData.put("attempts", "0");

        when(hashOperations.entries(anyString())).thenReturn(sessionData);

        assertThrows(OtpIncorrectException.class,
                () -> authenticationService.verifyOtp("session-id", "999999"));
    }

    @Test
    @DisplayName("verifyOtp should succeed with correct OTP")
    void verifyOtp_shouldSucceedWithCorrectOtp() {
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("mobile", "9876543210");
        sessionData.put("otp", hashOtp("123456"));
        sessionData.put("attempts", "0");

        when(hashOperations.entries(eq("otp:session-id"))).thenReturn(sessionData);
        when(citizenRepo.findByMobile("9876543210")).thenReturn(java.util.Optional.empty());
        when(jwtToken.generateToken(anyString(), anyString(), anyString(), anyString())).thenReturn("jwt-token");

        VerifyOtpResponse response = authenticationService.verifyOtp("session-id", "123456");

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertFalse(response.isRegistered());
        assertEquals("9876543210", response.getMobile());
        verify(jwtToken).generateToken(anyString(), eq("9876543210"), eq(""), eq("USER"));
    }

    @Test
    @DisplayName("verifyOtp should include USER role for new (unregistered) citizen")
    void verifyOtp_shouldIncludeUserRoleForNewCitizen() {
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("mobile", "9876543210");
        sessionData.put("otp", hashOtp("123456"));
        sessionData.put("attempts", "0");

        when(hashOperations.entries(eq("otp:session-id"))).thenReturn(sessionData);
        when(citizenRepo.findByMobile("9876543210")).thenReturn(java.util.Optional.empty());
        when(jwtToken.generateToken(anyString(), anyString(), anyString(), anyString())).thenReturn("jwt-token");

        authenticationService.verifyOtp("session-id", "123456");

        verify(jwtToken).generateToken(
            eq("9876543210"),
            eq("9876543210"),
            eq(""),
            eq("USER")
        );
    }

    @Test
    @DisplayName("verifyOtp should include citizen's actual role for returning user")
    void verifyOtp_shouldIncludeCitizenRoleForReturningUser() {
        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("mobile", "9876543210");
        sessionData.put("otp", hashOtp("123456"));
        sessionData.put("attempts", "0");

        com.suvidha.auth.model.Citizen citizen = new com.suvidha.auth.model.Citizen();
        citizen.setId("citizen-uuid");
        citizen.setMobile("9876543210");
        citizen.setName("Test User");
        citizen.setRole(com.suvidha.auth.Dto.Role.ADMIN);

        when(hashOperations.entries(eq("otp:session-id"))).thenReturn(sessionData);
        when(citizenRepo.findByMobile("9876543210")).thenReturn(java.util.Optional.of(citizen));
        when(jwtToken.generateToken(anyString(), anyString(), anyString(), anyString())).thenReturn("jwt-token");

        authenticationService.verifyOtp("session-id", "123456");

        verify(jwtToken).generateToken(
            eq("citizen-uuid"),
            eq("9876543210"),
            eq("Test User"),
            eq("ADMIN")
        );
    }
}
