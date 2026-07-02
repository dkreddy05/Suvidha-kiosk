package com.suvidha.billing.dto.response;

import lombok.Builder;
import lombok.Data;

/** Returned by GET /api/v1/billing/receipt/{paymentId} */
@Data
@Builder
public class ReceiptDTO {
    private String receiptHtml;
    private Object receiptJson;  // arbitrary JSON object for programmatic use
}
