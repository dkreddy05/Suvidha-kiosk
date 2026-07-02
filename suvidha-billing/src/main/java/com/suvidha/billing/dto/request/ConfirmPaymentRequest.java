package com.suvidha.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmPaymentRequest {

    @NotBlank(message = "orderId is required")
    @Size(max = 64, message = "orderId too long")
    private String orderId;

    @NotBlank(message = "paymentId is required")
    @Size(max = 64, message = "paymentId too long")
    private String paymentId;

    @NotBlank(message = "signature is required")
    @Size(max = 256, message = "signature too long")
    private String signature;
}
