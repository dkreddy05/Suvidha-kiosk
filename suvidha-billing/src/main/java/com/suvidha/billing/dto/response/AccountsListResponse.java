package com.suvidha.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AccountsListResponse {
    private List<SpecAccountResponse> accounts;
}
