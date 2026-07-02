package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyOtpResponseDTO {
    private String accessToken;
    private String refreshToken;
    private CitizenDTO citizen;

    @JsonProperty("isNewUser")
    private boolean newUser;

    public VerifyOtpResponseDTO() {
    }

    public VerifyOtpResponseDTO(String accessToken, String refreshToken, CitizenDTO citizen, boolean newUser) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.citizen = citizen;
        this.newUser = newUser;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public CitizenDTO getCitizen() {
        return citizen;
    }

    public void setCitizen(CitizenDTO citizen) {
        this.citizen = citizen;
    }

    @JsonProperty("isNewUser")
    public boolean isNewUser() {
        return newUser;
    }

    public void setNewUser(boolean newUser) {
        this.newUser = newUser;
    }
}
