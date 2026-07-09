package com.suvidha.connections.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccountRequest {

    @NotBlank(message = "accountNumber is required")
    @Size(max = 32, message = "accountNumber too long")
    private String accountNumber;

    @NotBlank(message = "utilityType is required")
    @Size(max = 32, message = "utilityType too long")
    private String utilityType;   // ELECTRICITY | GAS | WATER

    @Size(max = 128, message = "providerName too long")
    private String providerName;

    @Size(max = 512, message = "address too long")
    private String address;
}
