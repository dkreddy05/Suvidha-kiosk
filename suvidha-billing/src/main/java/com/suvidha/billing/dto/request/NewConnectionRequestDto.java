package com.suvidha.billing.dto.request;

import com.suvidha.billing.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NewConnectionRequestDto {
    @NotNull(message = "serviceType is required")
    private ServiceType serviceType;

    @NotBlank(message = "address is required")
    @Size(max = 512, message = "address too long")
    private String address;

    @NotBlank(message = "propertyType is required")
    @Size(max = 64, message = "propertyType too long")
    private String propertyType;

    @Size(min = 6, max = 128, message = "providerName too long")
    private String providerName;

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
