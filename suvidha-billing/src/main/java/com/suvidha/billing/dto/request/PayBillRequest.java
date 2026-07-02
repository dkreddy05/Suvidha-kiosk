package com.suvidha.billing.dto.request;

import com.suvidha.billing.enums.PaymentMode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PayBillRequest {

    @NotEmpty(message = "At least one bill ID is required")
    @Size(max = 10, message = "Cannot pay more than 10 bills at once")
    private List<String> billIds;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;
}
