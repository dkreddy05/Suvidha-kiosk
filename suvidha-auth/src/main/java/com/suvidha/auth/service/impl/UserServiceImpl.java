package com.suvidha.auth.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.exception.InvalidRequestException;
import com.suvidha.auth.exception.SessionNotVerifiedException;
import com.suvidha.auth.exception.UserAlreadyExistsException;
import com.suvidha.auth.model.UsersAuth;
import com.suvidha.auth.repo.UserAuthRepo;
import com.suvidha.auth.service.UserService;

import java.time.Instant;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserAuthRepo userAuthRepo;
    private final StringRedisTemplate template;

    private static final String SESSION_PREFIX = "session";

    public UserServiceImpl(UserAuthRepo userAuthRepo, StringRedisTemplate template) {
        this.userAuthRepo = userAuthRepo;
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
        if (userAuthRepo.findByMobile(request.getMobile()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "User with mobile " + request.getMobile() + " already exists.");
        }

        if (userAuthRepo.findByAadhar(request.getAadhar()).isPresent()) {
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
        if (!request.getMobile().equals(sessionMobile)) {
            throw new InvalidRequestException(
                    "Mobile number does not match the verified session.");
        }

        UsersAuth newUser = new UsersAuth(
                request.getMobile(),
                request.getAadhar(),
                request.getName(),
                request.getLanguagePreference(),
                request.getRole(),
                Instant.now());
        UsersAuth savedUser = userAuthRepo.save(newUser);
        template.delete(sessionKey);
        return toDto(savedUser);
    }

    private UserAuthDto toDto(UsersAuth user) {
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
