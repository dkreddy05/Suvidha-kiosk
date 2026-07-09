package com.suvidha.connections.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityAccountDTO {

    private String id;
    private String citizenId;
    private String accountNumber;
    private String utilityType;
    private String providerName;
    private String address;
    private LatestBillDTO latestBill; // nullable

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatestBillDTO {
        private String billId;
        private String billMonth;
        private BigDecimal amount;
        private String dueDate;
        private String status;
    }
}
