package com.suvidha.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OtpSendRequest {

    @NotBlank(message = "phone_number is required")
    @Pattern(regexp = "^\\+91[0-9]{10}$", message = "phone_number must be in format +91XXXXXXXXXX")
    @JsonProperty("phone_number")
    private String phoneNumber;

    @NotBlank(message = "otp_code is required")
    @Size(max = 6, message = "otp_code too long")
    @JsonProperty("otp_code")
    private String otpCode;

    @NotBlank(message = "citizen_id is required")
    @Size(max = 36, message = "citizen_id too long")
    @JsonProperty("citizen_id")
    private String citizenId;

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }
}
