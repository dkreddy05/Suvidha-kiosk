package com.suvidha.billing.dto.request;

import com.suvidha.billing.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VerifyOwnershipRequest {
    @NotBlank(message = "consumerNo is required")
    @Size(max = 32, message = "consumerNo too long")
    private String consumerNo;

    @NotNull(message = "serviceType is required")
    private ServiceType serviceType;

    public String getConsumerNo() {
        return consumerNo;
    }

    public void setConsumerNo(String consumerNo) {
        this.consumerNo = consumerNo;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
