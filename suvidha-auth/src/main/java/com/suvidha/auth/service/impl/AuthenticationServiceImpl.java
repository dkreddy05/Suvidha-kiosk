package com.suvidha.auth.service.impl;

import org.springframework.stereotype.Service;
import com.suvidha.auth.service.AuthenticationService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.suvidha.auth.Dto.VerifyOtpResponse;
import com.suvidha.auth.model.UsersAuth;
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
import com.suvidha.auth.repo.UserAuthRepo;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private BCryptPasswordEncoder encoder;
    private StringRedisTemplate template;
    private UserAuthRepo userAuthRepo;
    private final SecureRandom secureRandom;
    private static final String OTP_PREFIX = "otp";
    private static final String SESSION_PREFIX = "session";
    private static final int MAX_ATTEMPTS = 3;
    private final JwtToken jwtToken;
    private static final Duration RATE_LIMIT_TTL = Duration.ofMinutes(1);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_SESSION_TTL = Duration.ofMinutes(10);

    public AuthenticationServiceImpl(BCryptPasswordEncoder encoder, StringRedisTemplate template,
            UserAuthRepo userAuthRepo, JwtToken jwtToken) {
        this.encoder = encoder;
        this.template = template;
        this.userAuthRepo = userAuthRepo;
        this.secureRandom = new SecureRandom();
        this.jwtToken = jwtToken;
    }

    @Override
    public String sendOtp(String sessionId, String mobile) {
        if (isBlank(mobile)) {
            throw new InvalidRequestException("mobile is required.");
        }

        if (isBlank(sessionId)) {
            sessionId = UUID.randomUUID().toString();
        }

        String rateLimitKey = "otp:rate:" + mobile;
        try {
            Boolean allow = template.opsForValue().setIfAbsent(rateLimitKey, "1", RATE_LIMIT_TTL);
            if (Boolean.FALSE.equals(allow)) {
                throw new OtpRateLimitExceededException(
                        "OTP already sent recently. Please wait before retrying.");
            }

            String otp = String.format("%06d", secureRandom.nextInt(1000000));
            String hashedOtp = encoder.encode(otp);
            System.out.println("OTP: " + otp);

            String key = OTP_PREFIX + ":" + sessionId;

            Map<String, String> hp = new HashMap<>();
            hp.put("otp", hashedOtp);
            hp.put("mobile", mobile);
            hp.put("attempts", "0");

            template.opsForHash().putAll(key, hp);
            template.expire(key, OTP_TTL);

            return sessionId;
        } catch (OtpRateLimitExceededException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new OtpSendFailedException("Failed to send OTP. Please try again.");
        }
    }

    @Override
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

            if (!encoder.matches(otp, storedOtp)) {
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
            UsersAuth user = userAuthRepo.findByMobile(storedMobile).orElse(null);
            if (user == null) {
                return new VerifyOtpResponse(storedMobile, true, false, null);
            }
            String token = jwtToken.generateToken(storedMobile);
            return new VerifyOtpResponse(storedMobile, true, true, token);
        } catch (InvalidRequestException | OtpSessionInvalidException | OtpMobileMismatchException
                | OtpMaxAttemptsExceededException | OtpIncorrectException e) {
            throw e;
        } catch (Exception e) {
            throw new OtpVerifyFailedException("Failed to verify OTP. Please try again.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
