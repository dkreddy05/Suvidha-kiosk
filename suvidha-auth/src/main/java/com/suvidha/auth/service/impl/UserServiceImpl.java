package com.suvidha.auth.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.suvidha.auth.Dto.RegisterRequest;
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

    private static final String SESSION_PREFIX = "session";

    public UserServiceImpl(CitizenRepo citizenRepo, StringRedisTemplate template) {
        this.citizenRepo = citizenRepo;
        this.template = template;
    }

    @Override
    public UserAuthDto registerUser(RegisterRequest request) {
        if (isBlank(request.getSessionId())
                || isBlank(request.getMobile())
                || isBlank(request.getAadhar())
                || isBlank(request.getName())
                || isBlank(request.getLanguagePreference())
                || request.getRole() == null) {
            throw new InvalidRequestException(
                    "All fields (sessionId, mobile, aadhar, name, languagePreference, role) are required.");
        }

        // Normalize inputs so uniqueness checks can't be bypassed via whitespace.
        String mobile = request.getMobile().trim();
        String aadhar = request.getAadhar().trim();

        if (citizenRepo.findByMobile(mobile).isPresent()) {
            throw new UserAlreadyExistsException(
                    "User with mobile " + mobile + " already exists.");
        }

        if (citizenRepo.findByAadhar(aadhar).isPresent()) {
            throw new UserAlreadyExistsException(
                    "User with this Aadhar number is already registered.");
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

        Citizen newUser = new Citizen(
                mobile,
                aadhar,
                request.getName().trim(),
                request.getLanguagePreference().trim(),
                request.getRole(),
                Instant.now());
        Citizen savedUser = citizenRepo.save(newUser);
        template.delete(sessionKey);
        return toDto(savedUser);
    }

    private UserAuthDto toDto(Citizen user) {
        UserAuthDto dto = new UserAuthDto();
        dto.setId(user.getId());
        dto.setMobile(user.getMobile());
        dto.setAadhar(user.getAadhar());
        dto.setName(user.getName());
        dto.setLanguagePreference(user.getLanguagePreference());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
