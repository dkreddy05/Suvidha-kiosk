package com.suvidha.billing.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoSizeConstraintsTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("ConfirmPaymentRequest rejects oversized orderId")
    void confirmPaymentRequest_rejectsOversizedOrderId() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("x".repeat(65));
        req.setPaymentId("pay_123");
        req.setSignature("sig_123");

        Set<ConstraintViolation<ConfirmPaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("orderId"));
    }

    @Test
    @DisplayName("ConfirmPaymentRequest accepts valid-sized fields")
    void confirmPaymentRequest_acceptsValidSizes() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_" + "x".repeat(58));
        req.setPaymentId("pay_" + "x".repeat(59));
        req.setSignature("sig_" + "x".repeat(251));

        Set<ConstraintViolation<ConfirmPaymentRequest>> violations = validator.validate(req);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("LinkAccountRequest rejects oversized address")
    void linkAccountRequest_rejectsOversizedAddress() {
        LinkAccountRequest req = new LinkAccountRequest();
        req.setAccountNumber("ACC123");
        req.setUtilityType("ELECTRICITY");
        req.setProviderName("Provider");
        req.setAddress("x".repeat(513));

        Set<ConstraintViolation<LinkAccountRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("address"));
    }

    @Test
    @DisplayName("SpecPaymentRequest rejects oversized billId")
    void specPaymentRequest_rejectsOversizedBillId() {
        SpecPaymentRequest req = new SpecPaymentRequest();
        req.setBillId("x".repeat(37));
        req.setAmount(BigDecimal.valueOf(100.0));
        req.setPaymentMethod("mock_upi");

        Set<ConstraintViolation<SpecPaymentRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("billId"));
    }

    @Test
    @DisplayName("SpecLinkAccountRequest rejects oversized consumerId")
    void specLinkAccountRequest_rejectsOversizedConsumerId() {
        SpecLinkAccountRequest req = new SpecLinkAccountRequest();
        req.setUtilityType("electricity");
        req.setConsumerId("x".repeat(33));

        Set<ConstraintViolation<SpecLinkAccountRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("consumerId"));
    }
}
