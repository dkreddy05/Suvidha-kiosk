package com.suvidha.billing.dto.request;

import com.suvidha.billing.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AccountVerificationRequestDto {
    @NotBlank(message = "consumerNo is required")
    @Size(max = 32, message = "consumerNo too long")
    private String consumerNo;

    @NotBlank(message = "accountHolderName is required")
    @Size(max = 128, message = "accountHolderName too long")
    private String accountHolderName;

    @NotBlank(message = "registeredMobile is required")
    @Size(max = 20, message = "registeredMobile too long")
    private String registeredMobile;

    @NotBlank(message = "address is required")
    @Size(max = 512, message = "address too long")
    private String address;

    @NotBlank(message = "providerName is required")
    @Size(max = 128, message = "providerName too long")
    private String providerName;

    @NotNull(message = "serviceType is required")
    private ServiceType serviceType;

    public String getConsumerNo() {
        return consumerNo;
    }

    public void setConsumerNo(String consumerNo) {
        this.consumerNo = consumerNo;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getRegisteredMobile() {
        return registeredMobile;
    }

    public void setRegisteredMobile(String registeredMobile) {
        this.registeredMobile = registeredMobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }
}
