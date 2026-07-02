package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SpecPaymentResponse {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("payment_status")
    private String paymentStatus;

    private BigDecimal amount;

    @JsonProperty("payment_method")
    private String paymentMethod;

    private ReceiptDetails receipt;
}
