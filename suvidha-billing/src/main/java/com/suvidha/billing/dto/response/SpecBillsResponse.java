package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SpecBillsResponse {

    @JsonProperty("account_id")
    private String accountId;

    private List<SpecBillResponse> bills;
}
