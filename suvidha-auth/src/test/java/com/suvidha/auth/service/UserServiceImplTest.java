package com.suvidha.auth.service;

import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.exception.InvalidRequestException;
import com.suvidha.auth.exception.SessionNotVerifiedException;
import com.suvidha.auth.exception.UserAlreadyExistsException;
import com.suvidha.auth.model.AadharEncryptionConverter;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private CitizenRepo citizenRepo;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        System.setProperty("AADHAR_BLIND_INDEX_KEY", "test-blind-index-key-32bytes!");
        userService = new UserServiceImpl(citizenRepo, redisTemplate);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(citizenRepo.findByConsumerId(anyString())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(userService, "allowTestAadhar", false);
    }

    private RegisterRequest validRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("test-session");
        req.setMobile("9876543210");
        req.setAadhar("123456789012");
        req.setName("Test User");
        req.setLanguagePreference("en");
        return req;
    }

    @Test
    @DisplayName("registerUser should succeed with valid request")
    void registerUser_shouldSucceedWithValidRequest() {
        ReflectionTestUtils.setField(userService, "allowTestAadhar", true);
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("test-session");
        req.setMobile("9876543210");
        req.setAadhar("AUTO_TEST");
        req.setName("Test User");
        req.setLanguagePreference("en");

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("status", "VERIFIED");
        sessionData.put("mobile", "9876543210");

        when(hashOperations.entries("session:test-session")).thenReturn(sessionData);
        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(citizenRepo.save(any(Citizen.class))).thenAnswer(inv -> {
            Citizen c = inv.getArgument(0);
            c.setId("generated-uuid");
            return c;
        });

        UserAuthDto result = userService.registerUser(req);

        assertNotNull(result);
        assertEquals("generated-uuid", result.getId());
        assertEquals("9876543210", result.getMobile());
        assertEquals("Test User", result.getName());
        verify(redisTemplate).delete("session:test-session");
    }

    @Test
    @DisplayName("registerUser should throw when required fields are missing")
    void registerUser_shouldThrowWhenFieldsMissing() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("test-session");

        assertThrows(InvalidRequestException.class,
                () -> userService.registerUser(req));
    }

    @Test
    @DisplayName("registerUser should throw generic message when mobile already exists")
    void registerUser_shouldThrowGenericMessageWhenMobileAlreadyExists() {
        RegisterRequest req = validRequest();

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.of(new Citizen()));
        when(citizenRepo.findByAadharHash(anyString())).thenReturn(Optional.empty());

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(req));

        assertEquals("Registration failed.", ex.getMessage());
    }

    @Test
    @DisplayName("registerUser should throw generic message when Aadhar already exists")
    void registerUser_shouldThrowGenericMessageWhenAadharAlreadyExists() {
        RegisterRequest req = validRequest();

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(citizenRepo.findByAadharHash(anyString())).thenReturn(Optional.of(new Citizen()));

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(req));

        assertEquals("Registration failed.", ex.getMessage());
    }

    @Test
    @DisplayName("registerUser should not leak Aadhar or mobile in error message")
    void registerUser_shouldNotLeakIdentifiersInErrorMessage() {
        RegisterRequest req = validRequest();

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(citizenRepo.findByAadharHash(anyString())).thenReturn(Optional.of(new Citizen()));

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(req));

        assertFalse(ex.getMessage().contains("123456789012"), "Aadhar must not appear in error");
        assertFalse(ex.getMessage().contains("9876543210"), "Mobile must not appear in error");
        assertFalse(ex.getMessage().toLowerCase().contains("aadhar"), "Must not mention Aadhar");
        assertFalse(ex.getMessage().toLowerCase().contains("mobile"), "Must not mention mobile");
    }

    @Test
    @DisplayName("registerUser should throw when session not verified")
    void registerUser_shouldThrowWhenSessionNotVerified() {
        RegisterRequest req = validRequest();

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("status", "PENDING");
        sessionData.put("mobile", "9876543210");

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(hashOperations.entries("session:test-session")).thenReturn(sessionData);

        assertThrows(SessionNotVerifiedException.class,
                () -> userService.registerUser(req));
    }

    @Test
    @DisplayName("registerUser should throw when session expired")
    void registerUser_shouldThrowWhenSessionExpired() {
        RegisterRequest req = validRequest();

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(hashOperations.entries("session:test-session")).thenReturn(new HashMap<>());

        assertThrows(SessionNotVerifiedException.class,
                () -> userService.registerUser(req));
    }

    @Test
    @DisplayName("registerUser should execute both lookups even when mobile exists (timing mitigation)")
    void registerUser_shouldExecuteBothLookupsRegardlessOfFirstResult() {
        RegisterRequest req = validRequest();

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.of(new Citizen()));
        when(citizenRepo.findByAadharHash(anyString())).thenReturn(Optional.empty());

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(req));

        verify(citizenRepo).findByMobile("9876543210");
        verify(citizenRepo).findByAadharHash(anyString());
    }

    @Test
    @DisplayName("registerUser should reject AUTO_ aadhar when allowTestAadhar is false and duplicate exists")
    void registerUser_shouldRejectTestAadharWhenDisabled() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("test-session");
        req.setMobile("9876543210");
        req.setAadhar("AUTO_TEST");
        req.setName("Test User");
        req.setLanguagePreference("en");

        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(citizenRepo.findByAadharHash(anyString())).thenReturn(Optional.of(new Citizen()));

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> userService.registerUser(req));

        assertEquals("Registration failed.", ex.getMessage());
    }

    @Test
    @DisplayName("registerUser should allow AUTO_ aadhar when allowTestAadhar is true")
    void registerUser_shouldAllowTestAadharWhenEnabled() {
        ReflectionTestUtils.setField(userService, "allowTestAadhar", true);

        RegisterRequest req = new RegisterRequest();
        req.setSessionId("test-session");
        req.setMobile("9876543210");
        req.setAadhar("AUTO_TEST");
        req.setName("Test User");
        req.setLanguagePreference("en");

        Map<Object, Object> sessionData = new HashMap<>();
        sessionData.put("status", "VERIFIED");
        sessionData.put("mobile", "9876543210");

        when(hashOperations.entries("session:test-session")).thenReturn(sessionData);
        when(citizenRepo.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(citizenRepo.save(any(Citizen.class))).thenAnswer(inv -> {
            Citizen c = inv.getArgument(0);
            c.setId("generated-uuid");
            return c;
        });

        UserAuthDto result = userService.registerUser(req);

        assertNotNull(result);
        assertEquals("generated-uuid", result.getId());
    }
}
