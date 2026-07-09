package com.suvidha.auth.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.suvidha.auth.service.AuthenticationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.model.Citizen;
import com.suvidha.auth.exception.InvalidRequestException;
import com.suvidha.auth.exception.OtpIncorrectException;
import com.suvidha.auth.exception.OtpMaxAttemptsExceededException;
import com.suvidha.auth.exception.OtpMobileMismatchException;
import com.suvidha.auth.exception.OtpRateLimitExceededException;
import com.suvidha.auth.exception.OtpSendFailedException;
import com.suvidha.auth.exception.OtpSessionInvalidException;
import com.suvidha.auth.exception.OtpVerifyFailedException;
import com.suvidha.auth.token.JwtToken;
import java.time.Duration;
import com.suvidha.auth.repo.CitizenRepo;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private StringRedisTemplate template;
    private CitizenRepo citizenRepo;
    private final SecureRandom secureRandom;
    private static final String OTP_PREFIX = "otp";
    private static final String SESSION_PREFIX = "session";
    private static final int MAX_ATTEMPTS = 5;
    private final JwtToken jwtToken;
    private static final Duration RATE_LIMIT_TTL = Duration.ofMinutes(15);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_SESSION_TTL = Duration.ofMinutes(10);

    public AuthenticationServiceImpl(StringRedisTemplate template,
            CitizenRepo citizenRepo, JwtToken jwtToken) {
        this.template = template;
        this.citizenRepo = citizenRepo;
        this.secureRandom = new SecureRandom();
        this.jwtToken = jwtToken;
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Use a static salt; OTPs are short-lived (5 min) and max 5 attempts
            byte[] hash = digest.digest(("otp:" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("OTP hashing failed", e);
        }
    }

    @Override
    public String sendOtp(String sessionId, String mobile, StringBuilder devOtp) {
        if (isBlank(mobile)) {
            throw new InvalidRequestException("mobile is required.");
        }

        if (isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }

        String rateLimitKey = "otp:ratelimit:" + mobile;
        try {
            Long count = template.opsForValue().increment(rateLimitKey);
            if (count != null) {
                if (count == 1) {
                    template.expire(rateLimitKey, RATE_LIMIT_TTL);
                }
                if (count > 3) {
                    throw new OtpRateLimitExceededException(
                            "OTP rate limit exceeded (max 3 requests per 15 mins). Please wait.");
                }
            }

            String otp = String.format("%06d", secureRandom.nextInt(900000) + 100000);
            String hashedOtp = hashOtp(otp);

            String key = OTP_PREFIX + ":" + sessionId;

            Map<String, String> hp = new HashMap<>();
            hp.put("otp", hashedOtp);
            hp.put("mobile", mobile);
            hp.put("attempts", "0");

            template.opsForHash().putAll(key, hp);
            template.expire(key, OTP_TTL);

            if (devOtp != null) {
                devOtp.append(otp);
            }

            return sessionId;
        } catch (OtpRateLimitExceededException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new OtpSendFailedException("Failed to send OTP. Please try again.", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VerifyOtpResponse verifyOtp(String sessionId, String otp) {
        if (isBlank(sessionId) || isBlank(otp)) {
            throw new InvalidRequestException("sessionId and otp are required.");
        }

        String otpKey = OTP_PREFIX + ":" + sessionId;
        try {
            Map<Object, Object> hp = template.opsForHash().entries(otpKey);
            if (hp.isEmpty()) {
                throw new OtpSessionInvalidException("Invalid or expired OTP session.");
            }

            String storedMobile = (String) hp.get("mobile");
            if (storedMobile == null) {
                throw new OtpMobileMismatchException("Mobile number does not match OTP session.");
            }

            int attempts = Integer.parseInt(hp.getOrDefault("attempts", "0").toString());
            if (attempts >= MAX_ATTEMPTS) {
                template.delete(otpKey);
                throw new OtpMaxAttemptsExceededException("Max OTP attempts exceeded. Please request a new OTP.");
            }

            String storedOtp = (String) hp.get("otp");
            if (storedOtp == null) {
                template.delete(otpKey);
                throw new OtpSessionInvalidException("Invalid or expired OTP session.");
            }

            if (!hashOtp(otp).equals(storedOtp)) {
                attempts++;
                template.opsForHash().put(otpKey, "attempts", String.valueOf(attempts));
                if (attempts >= MAX_ATTEMPTS) {
                    template.delete(otpKey);
                    throw new OtpMaxAttemptsExceededException(
                            "Max OTP attempts exceeded. Please request a new OTP.");
                }

                int remaining = MAX_ATTEMPTS - attempts;
                throw new OtpIncorrectException("Incorrect OTP.", remaining);
            }

            String sessionKey = SESSION_PREFIX + ":" + sessionId;
            Map<String, String> sessionData = new HashMap<>();
            sessionData.put("status", "VERIFIED");
            sessionData.put("mobile", storedMobile);
            sessionData.put("verifiedAt", Instant.now().toString());
            template.opsForHash().putAll(sessionKey, sessionData);
            template.expire(sessionKey, VERIFIED_SESSION_TTL);
            template.delete(otpKey);
            Citizen citizen = citizenRepo.findByMobile(storedMobile).orElse(null);
            String citizenId = citizen != null ? citizen.getId() : storedMobile;
            String role = citizen != null && citizen.getRole() != null
                ? citizen.getRole().name()
                : "USER";
            String token = jwtToken.generateToken(
                citizenId,
                storedMobile,
                citizen != null ? citizen.getName() : "",
                role
            );
            if (citizen == null) {
                return new VerifyOtpResponse(storedMobile, true, false, token);
            }
            return new VerifyOtpResponse(storedMobile, true, true, token);
        } catch (InvalidRequestException | OtpSessionInvalidException | OtpMobileMismatchException
                | OtpMaxAttemptsExceededException | OtpIncorrectException e) {
            throw e;
        } catch (Exception e) {
            throw new OtpVerifyFailedException("Failed to verify OTP. Please try again.", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
