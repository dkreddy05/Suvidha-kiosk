package com.suvidha.auth.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyOtpResponseDTO {
    private String accessToken;
    private CitizenDTO citizen;
    private boolean isNewUser;

    public VerifyOtpResponseDTO() {
    }

    public VerifyOtpResponseDTO(String accessToken, CitizenDTO citizen, boolean isNewUser) {
        this.accessToken = accessToken;
        this.citizen = citizen;
        this.isNewUser = isNewUser;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public CitizenDTO getCitizen() {
        return citizen;
    }

    public void setCitizen(CitizenDTO citizen) {
        this.citizen = citizen;
    }

    @JsonProperty("isNewUser")
    public boolean isNewUser() {
        return isNewUser;
    }

    public void setNewUser(boolean isNewUser) {
        this.isNewUser = isNewUser;
    }
}
