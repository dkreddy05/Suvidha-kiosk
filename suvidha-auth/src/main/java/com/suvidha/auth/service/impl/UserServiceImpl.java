package com.suvidha.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.Role;
import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.exception.InvalidRequestException;
import com.suvidha.auth.exception.SessionNotVerifiedException;
import com.suvidha.auth.exception.UserAlreadyExistsException;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.repo.CitizenRepo;
import com.suvidha.auth.service.UserService;
import java.time.Instant;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final CitizenRepo citizenRepo;
    private final StringRedisTemplate template;
    private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

    private static final String SESSION_PREFIX = "session";

    @Value("${app.registration.allow-test-aadhar:false}")
    private boolean allowTestAadhar;

    public UserServiceImpl(CitizenRepo citizenRepo, StringRedisTemplate template) {
        this.citizenRepo = citizenRepo;
        this.template = template;
    }

    @Override
    public UserAuthDto registerUser(RegisterRequest request) {
        if (isBlank(request.getSessionId())
                || isBlank(request.getMobile())
                || isBlank(request.getName())
                || isBlank(request.getLanguagePreference())) {
            throw new InvalidRequestException(
                    "All fields (sessionId, mobile, name, languagePreference) are required.");
        }

        String mobile = request.getMobile().trim();
        String aadhar = request.getAadhar();
        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        boolean mobileExists = citizenRepo.findByMobile(mobile).isPresent();
        boolean aadharExists = (!allowTestAadhar || !aadhar.startsWith("AUTO_"))
                && citizenRepo.findByAadhar(aadhar).isPresent();

        if (mobileExists || aadharExists) {
            throw new UserAlreadyExistsException("Registration failed.");
        }
        String sessionKey = SESSION_PREFIX + ":" + request.getSessionId();
        Map<Object, Object> sessionData = template.opsForHash().entries(sessionKey);

        if (sessionData.isEmpty()) {
            throw new SessionNotVerifiedException(
                    "Session not found or has expired. Please verify OTP first.");
        }

        String status = (String) sessionData.get("status");
        if (!"VERIFIED".equals(status)) {
            throw new SessionNotVerifiedException(
                    "Session is not verified. Please complete OTP verification.");
        }
        String sessionMobile = (String) sessionData.get("mobile");
        if (!mobile.equals(sessionMobile)) {
            throw new InvalidRequestException(
                    "Mobile number does not match the verified session.");
        }

        String consumerId = generateConsumerId();
        Citizen newUser = new Citizen(
                mobile,
                aadhar,
                request.getName().trim(),
                request.getLanguagePreference().trim(),
                request.getRole(),
                Instant.now());
        newUser.setConsumerId(consumerId);
        Citizen savedUser = citizenRepo.save(newUser);
        template.delete(sessionKey);
        return toDto(savedUser);
    }

    private String generateConsumerId() {
        for (int attempt = 0; attempt < 100; attempt++) {
            StringBuilder sb = new StringBuilder("C");
            for (int i = 0; i < 9; i++) {
                sb.append(secureRandom.nextInt(10));
            }
            String candidate = sb.toString();
            if (citizenRepo.findByConsumerId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate unique consumer ID");
    }

    private UserAuthDto toDto(Citizen user) {
        UserAuthDto dto = new UserAuthDto();
        dto.setId(user.getId());
        dto.setMobile(user.getMobile());
        dto.setName(user.getName());
        dto.setLanguagePreference(user.getLanguagePreference());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setConsumerId(user.getConsumerId());
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
