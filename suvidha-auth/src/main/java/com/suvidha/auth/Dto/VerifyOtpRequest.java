package com.suvidha.auth.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class VerifyOtpRequest {
    @NotBlank(message = "Session ID is required")
    @Size(max = 128, message = "Session ID too long")
    private String sessionId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(String sessionId, String otp) {
        this.sessionId = sessionId;
        this.otp = otp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
