package com.suvidha.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors TypeScript PaymentConfirmDTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDTO {
    private String paymentId;
    private String receiptUrl;
}
