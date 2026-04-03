package com.suvidha.auth.Dto;

public class OtpRequest {
    private String sessionId;
    private String mobile;
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
