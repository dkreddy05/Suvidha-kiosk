package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PaymentHistoryResponse {

    @JsonProperty("account_id")
    private String accountId;

    private List<PaymentSummary> payments;

    private Map<String, Object> pagination;

    @Data
    @Builder
    public static class PaymentSummary {

        @JsonProperty("transaction_id")
        private String transactionId;

        private BigDecimal amount;

        @JsonProperty("payment_method")
        private String paymentMethod;

        @JsonProperty("payment_status")
        private String paymentStatus;

        @JsonProperty("created_at")
        private String createdAt;
    }
}
