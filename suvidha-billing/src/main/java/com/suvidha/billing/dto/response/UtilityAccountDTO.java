package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Exactly mirrors the TypeScript UtilityAccountDTO in the kiosk frontend.
 * Field names must stay in sync with types.ts.
 */
@Data
@Builder
public class UtilityAccountDTO {

    private String id;
    private String citizenId;
    private String accountNumber;   // matches TS: accountNumber (NOT accountNo)
    private String utilityType;     // matches TS: UtilityType ('ELECTRICITY'|'GAS'|'WATER')
    private String providerName;
    private String address;

    private LatestBillDTO latestBill; // nullable

    @Data
    @Builder
    public static class LatestBillDTO {
        private String billId;
        private String billMonth;    // e.g. "2025-03"
        private BigDecimal amount;
        private String dueDate;      // ISO date string
        private String status;       // matches TS: BillStatus ('PENDING'|'PAID'|'OVERDUE')
    }
}
