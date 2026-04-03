package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpSendResponseDTO {
    private String sessionId;
    private String message;
    private String devOtp;

    public OtpSendResponseDTO() {
    }

    public OtpSendResponseDTO(String sessionId, String message, String devOtp) {
        this.sessionId = sessionId;
        this.message = message;
        this.devOtp = devOtp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDevOtp() {
        return devOtp;
    }

    public void setDevOtp(String devOtp) {
        this.devOtp = devOtp;
    }
}
