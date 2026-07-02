package com.suvidha.billing.dto.request;

import com.suvidha.billing.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ConfirmLinkRequest {
    @NotBlank(message = "consumerNo is required")
    @Size(max = 32, message = "consumerNo too long")
    private String consumerNo;

    @NotBlank(message = "otp is required")
    @Pattern(regexp = "\\d{6}", message = "otp must be a 6-digit number")
    private String otp;

    @NotNull(message = "serviceType is required")
    private ServiceType serviceType;

    public String getConsumerNo() {
        return consumerNo;
    }

    public void setConsumerNo(String consumerNo) {
        this.consumerNo = consumerNo;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
