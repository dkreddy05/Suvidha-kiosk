package com.suvidha.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Mirrors TypeScript BillDTO in types.ts exactly.
 */
@Data
@Builder
public class BillDTO {
    private String id;
    private String accountId;
    private String billNumber;
    private String billMonth;    // "YYYY-MM" derived from billingPeriodStart
    private BigDecimal amount;       // totalAmount
    private String dueDate;      // ISO date
    private BigDecimal paidAmount;
    private String status;       // BillStatus as string: PENDING | PAID | OVERDUE
    private String paidAt;       // ISO datetime, nullable
}
