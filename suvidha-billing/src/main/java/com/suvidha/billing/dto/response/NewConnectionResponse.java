package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.ConnectionStatus;
import com.suvidha.billing.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewConnectionResponse {
    private String id;
    private String citizenId;
    private ServiceType serviceType;
    private String address;
    private String propertyType;
    private String providerName;
    private ConnectionStatus status;
    private String refNo;
}
