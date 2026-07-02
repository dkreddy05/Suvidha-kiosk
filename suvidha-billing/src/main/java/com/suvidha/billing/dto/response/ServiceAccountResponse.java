package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceAccountResponse {
    private String id;
    private String citizenId;
    private ServiceType serviceType;
    private String accountNo;
    private String providerName;
    private String address;
    private String registeredMobile;
    private boolean isActive;
}
