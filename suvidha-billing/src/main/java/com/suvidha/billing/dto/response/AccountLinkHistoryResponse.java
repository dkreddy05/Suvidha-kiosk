package com.suvidha.billing.dto.response;

import com.suvidha.billing.enums.LinkRequestStatus;
import com.suvidha.billing.enums.UtilityType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountLinkHistoryResponse {
    private String id;
    private String citizenId;
    private String accountNo;
    private String mobile;
    private UtilityType utilityType;
    private LinkRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
