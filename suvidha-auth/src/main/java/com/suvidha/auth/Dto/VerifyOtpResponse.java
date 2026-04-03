package com.suvidha.auth.Dto;

public class VerifyOtpResponse {
    private String mobile;
    private boolean verified;
    private boolean registered;
    private String token;

    public VerifyOtpResponse() {
    }

    public VerifyOtpResponse(String mobile, boolean verified, boolean registered, String token) {
        this.mobile = mobile;
        this.verified = verified;
        this.registered = registered;
        this.token = token;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
