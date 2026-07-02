package com.suvidha.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Mirrors TypeScript PaymentOrderDTO — returned when frontend initiates a payment.
 * For CASH payments, the controller returns { status: "PAID" } instead.
 */
@Data
@Builder
public class PaymentOrderDTO {
    private String orderId;
    private BigDecimal amount;
    private String currency;    // always "INR"
    private String keyId;       // Razorpay key_id (public)
    private String qrCodeData;  // base64 data URI for UPI QR, nullable
    private String upiId;       // VPA for display, nullable
}
