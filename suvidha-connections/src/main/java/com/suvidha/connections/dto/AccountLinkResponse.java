package com.suvidha.connections.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkResponse {
    private String id;
    private String citizenId;
    private String accountNumber;
    private String utilityType;
    private String providerName;
    private String address;
    private String status;
}
