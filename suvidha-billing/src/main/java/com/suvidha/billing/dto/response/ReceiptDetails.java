package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReceiptDetails {

    @JsonProperty("receipt_id")
    private String receiptId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("consumer_id")
    private String consumerId;

    @JsonProperty("utility_type")
    private String utilityType;

    private BigDecimal amount;

    @JsonProperty("payment_method")
    private String paymentMethod;

    private String timestamp;
}
