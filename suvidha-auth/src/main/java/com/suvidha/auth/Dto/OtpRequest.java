package com.suvidha.auth.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpRequest {
    @Size(max = 128, message = "Session ID too long")
    private String sessionId;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @Size(max = 6, message = "OTP too long")
    private String otp;

    public OtpRequest() {
    }

    public OtpRequest(String sessionId, String mobile, String otp) {
        this.sessionId = sessionId;
        this.mobile = mobile;
        this.otp = otp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

}
