package com.suvidha.auth.Dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoSizeConstraintsTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("RegisterRequest rejects oversized sessionId")
    void registerRequest_rejectsOversizedSessionId() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("x".repeat(129));
        req.setMobile("9876543210");
        req.setAadhar("123456789012");
        req.setName("Test User");
        req.setLanguagePreference("en");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sessionId"));
    }

    @Test
    @DisplayName("RegisterRequest rejects oversized aadhar")
    void registerRequest_rejectsOversizedAadhar() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("session-1");
        req.setMobile("9876543210");
        req.setAadhar("x".repeat(65));
        req.setName("Test User");
        req.setLanguagePreference("en");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("aadhar"));
    }

    @Test
    @DisplayName("RegisterRequest accepts valid-sized fields")
    void registerRequest_acceptsValidSizes() {
        RegisterRequest req = new RegisterRequest();
        req.setSessionId("session-" + "x".repeat(118));
        req.setMobile("9876543210");
        req.setAadhar("123456789012");
        req.setName("A".repeat(100));
        req.setLanguagePreference("en");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(req);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("OtpRequest rejects oversized sessionId")
    void otpRequest_rejectsOversizedSessionId() {
        OtpRequest req = new OtpRequest("x".repeat(129), "9876543210", "123456");

        Set<ConstraintViolation<OtpRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("sessionId"));
    }

    @Test
    @DisplayName("OtpRequest rejects oversized otp")
    void otpRequest_rejectsOversizedOtp() {
        OtpRequest req = new OtpRequest("session-1", "9876543210", "1234567");

        Set<ConstraintViolation<OtpRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("otp"));
    }
}
