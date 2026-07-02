package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SpecBillResponse {

    @JsonProperty("bill_id")
    private String billId;

    @JsonProperty("bill_period")
    private String billPeriod;

    @JsonProperty("amount_due")
    private BigDecimal amountDue;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("bill_status")
    private String billStatus;

    @JsonProperty("created_at")
    private String createdAt;
}
