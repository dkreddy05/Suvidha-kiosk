package com.suvidha.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for POST /api/v1/billing/accounts/link
 * Matches the useLinkAccount mutation body from the kiosk frontend.
 */
@Data
public class LinkAccountRequest {

    @NotBlank(message = "accountNumber is required")
    @Size(max = 32, message = "accountNumber too long")
    private String accountNumber;

    @NotBlank(message = "utilityType is required")
    @Size(max = 32, message = "utilityType too long")
    private String utilityType;   // ELECTRICITY | GAS | WATER

    @NotBlank(message = "providerName is required")
    @Size(max = 128, message = "providerName too long")
    private String providerName;

    @NotBlank(message = "address is required")
    @Size(max = 512, message = "address too long")
    private String address;
}
