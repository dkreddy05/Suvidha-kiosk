package com.suvidha.auth.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Session ID is required")
    @Size(max = 128, message = "Session ID too long")
    private String sessionId;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @NotBlank(message = "Aadhar number is required")
    @Pattern(regexp = "^(AUTO_[A-Za-z0-9_]+|[0-9]{12})$",
            message = "Aadhar must be 12 digits or a valid test format (AUTO_*)")
    @Size(max = 64, message = "Aadhar number too long")
    private String aadhar;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Language preference is required")
    @Pattern(regexp = "^(en|hi|te|ta)$", message = "Language must be one of: en, hi, te, ta")
    private String languagePreference;

    public RegisterRequest() {
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

    public String getAadhar() {
        return aadhar;
    }

    public void setAadhar(String aadhar) {
        this.aadhar = aadhar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguagePreference() {
        return languagePreference;
    }

    public void setLanguagePreference(String languagePreference) {
        this.languagePreference = languagePreference;
    }
}
