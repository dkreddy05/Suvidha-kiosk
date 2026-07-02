package com.suvidha.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecAccountResponse {

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("utility_type")
    private String utilityType;

    @JsonProperty("consumer_id")
    private String consumerId;

    @JsonProperty("account_status")
    private String accountStatus;

    @JsonProperty("created_at")
    private String createdAt;
}
