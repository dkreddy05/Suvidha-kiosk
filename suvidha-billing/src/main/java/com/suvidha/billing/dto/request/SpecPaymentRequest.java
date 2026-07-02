package com.suvidha.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpecPaymentRequest {

    @NotBlank(message = "bill_id is required")
    @Size(max = 36, message = "bill_id too long")
    @JsonProperty("bill_id")
    private String billId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "payment_method is required")
    @Size(max = 32, message = "payment_method too long")
    @JsonProperty("payment_method")
    private String paymentMethod;
}
