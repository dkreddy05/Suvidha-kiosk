package com.suvidha.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpecLinkAccountRequest {

    @NotBlank(message = "utility_type is required")
    @Size(max = 32, message = "utility_type too long")
    @JsonProperty("utility_type")
    private String utilityType;

    @NotBlank(message = "consumer_id is required")
    @Size(max = 32, message = "consumer_id too long")
    @JsonProperty("consumer_id")
    private String consumerId;
}
