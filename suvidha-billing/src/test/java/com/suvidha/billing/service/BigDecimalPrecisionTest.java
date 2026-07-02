package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.BillDTO;
import com.suvidha.billing.dto.response.PaymentOrderDTO;
import com.suvidha.billing.dto.response.ReceiptDetails;
import com.suvidha.billing.dto.response.SpecBillResponse;
import com.suvidha.billing.dto.response.SpecPaymentResponse;
import com.suvidha.billing.dto.response.PaymentHistoryResponse;
import com.suvidha.billing.dto.response.UtilityAccountDTO;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalPrecisionTest {

    @Test
    @DisplayName("BigDecimal preserves exact monetary precision — no floating-point drift")
    void bigDecimalPreservesExactPrecision() {
        BigDecimal a = new BigDecimal("1234.56");
        BigDecimal b = new BigDecimal("789.01");
        BigDecimal sum = a.add(b).setScale(2, RoundingMode.HALF_UP);

        assertThat(sum).isEqualByComparingTo(new BigDecimal("2023.57"));
    }

    @Test
    @DisplayName("Repeated addition does not accumulate floating-point errors")
    void repeatedAdditionNoDrift() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < 100; i++) {
            total = total.add(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
        }
        assertThat(total).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    @DisplayName("Bill entity stores and retrieves BigDecimal amounts correctly")
    void billEntityStoresBigDecimalCorrectly() {
        Bill bill = Bill.builder()
                .totalAmount(new BigDecimal("1234.56"))
                .amountPaid(new BigDecimal("500.00"))
                .remainingBalance(new BigDecimal("734.56"))
                .lateFee(new BigDecimal("61.73"))
                .build();

        assertThat(bill.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(bill.getAmountPaid()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(bill.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("734.56"));
        assertThat(bill.getLateFee()).isEqualByComparingTo(new BigDecimal("61.73"));
    }

    @Test
    @DisplayName("Transaction entity stores BigDecimal amount correctly")
    void transactionEntityStoresBigDecimalCorrectly() {
        Transaction txn = Transaction.builder()
                .amount(new BigDecimal("999.99"))
                .build();

        assertThat(txn.getAmount()).isEqualByComparingTo(new BigDecimal("999.99"));
    }

    @Test
    @DisplayName("Bill DTO uses BigDecimal for amount fields")
    void billDtoUsesBigDecimal() {
        BillDTO dto = BillDTO.builder()
                .amount(new BigDecimal("1234.56"))
                .paidAmount(new BigDecimal("500.00"))
                .build();

        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(dto.getPaidAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("PaymentOrderDTO uses BigDecimal for amount")
    void paymentOrderDtoUsesBigDecimal() {
        PaymentOrderDTO dto = PaymentOrderDTO.builder()
                .amount(new BigDecimal("2500.00"))
                .build();

        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("SpecPaymentRequest accepts and validates BigDecimal amount")
    void specPaymentRequestUsesBigDecimal() {
        SpecPaymentRequest req = new SpecPaymentRequest();
        req.setBillId(UUID.randomUUID().toString());
        req.setAmount(new BigDecimal("150.75"));
        req.setPaymentMethod("mock_upi");

        assertThat(req.getAmount()).isEqualByComparingTo(new BigDecimal("150.75"));
    }

    @Test
    @DisplayName("SpecBillResponse uses BigDecimal for amountDue")
    void specBillResponseUsesBigDecimal() {
        SpecBillResponse resp = SpecBillResponse.builder()
                .amountDue(new BigDecimal("875.25"))
                .build();

        assertThat(resp.getAmountDue()).isEqualByComparingTo(new BigDecimal("875.25"));
    }

    @Test
    @DisplayName("SpecPaymentResponse uses BigDecimal for amount")
    void specPaymentResponseUsesBigDecimal() {
        SpecPaymentResponse resp = SpecPaymentResponse.builder()
                .amount(new BigDecimal("320.00"))
                .build();

        assertThat(resp.getAmount()).isEqualByComparingTo(new BigDecimal("320.00"));
    }

    @Test
    @DisplayName("ReceiptDetails uses BigDecimal for amount")
    void receiptDetailsUsesBigDecimal() {
        ReceiptDetails receipt = ReceiptDetails.builder()
                .amount(new BigDecimal("450.50"))
                .build();

        assertThat(receipt.getAmount()).isEqualByComparingTo(new BigDecimal("450.50"));
    }

    @Test
    @DisplayName("PaymentHistoryResponse.PaymentSummary uses BigDecimal for amount")
    void paymentHistorySummaryUsesBigDecimal() {
        PaymentHistoryResponse.PaymentSummary summary = PaymentHistoryResponse.PaymentSummary.builder()
                .amount(new BigDecimal("125.00"))
                .build();

        assertThat(summary.getAmount()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("UtilityAccountDTO.LatestBillDTO uses BigDecimal for amount")
    void utilityAccountLatestBillUsesBigDecimal() {
        UtilityAccountDTO.LatestBillDTO latestBill = UtilityAccountDTO.LatestBillDTO.builder()
                .amount(new BigDecimal("675.30"))
                .build();

        assertThat(latestBill.getAmount()).isEqualByComparingTo(new BigDecimal("675.30"));
    }

    @Test
    @DisplayName("Stream reduce with BigDecimal correctly sums multiple bills")
    void streamReduceCorrectlySumsBills() {
        Bill b1 = Bill.builder().totalAmount(new BigDecimal("100.00")).remainingBalance(new BigDecimal("100.00")).build();
        Bill b2 = Bill.builder().totalAmount(new BigDecimal("200.50")).remainingBalance(new BigDecimal("200.50")).build();
        Bill b3 = Bill.builder().totalAmount(new BigDecimal("0.01")).remainingBalance(new BigDecimal("0.01")).build();

        List<Bill> bills = List.of(b1, b2, b3);
        BigDecimal total = bills.stream()
                .map(b -> b.getRemainingBalance() != null ? b.getRemainingBalance() : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(total).isEqualByComparingTo(new BigDecimal("300.51"));
    }

    @Test
    @DisplayName("BigDecimal compareTo correctly identifies equal values with different scales")
    void compareToHandlesDifferentScales() {
        BigDecimal a = new BigDecimal("100.00");
        BigDecimal b = new BigDecimal("100.0");

        assertThat(a.compareTo(b)).isZero();
        assertThat(a).isEqualByComparingTo(b);
    }

    @Test
    @DisplayName("Late fee calculation with BigDecimal is precise")
    void lateFeeCalculationPrecise() {
        BigDecimal totalAmount = new BigDecimal("1234.56");
        BigDecimal lateFee = totalAmount.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);

        assertThat(lateFee).isEqualByComparingTo(new BigDecimal("61.73"));
    }

    @Test
    @DisplayName("Partial payment calculation with BigDecimal is precise")
    void partialPaymentCalculationPrecise() {
        BigDecimal totalAmount = new BigDecimal("999.99");
        BigDecimal halfPayment = totalAmount.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = totalAmount.subtract(halfPayment);

        assertThat(halfPayment).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(remaining).isEqualByComparingTo(new BigDecimal("499.99"));
    }

    @Test
    @DisplayName("Amount comparison with epsilon tolerance works with BigDecimal")
    void amountComparisonWithEpsilon() {
        BigDecimal requestAmount = new BigDecimal("100.00");
        BigDecimal billAmount = new BigDecimal("100.00");
        BigDecimal epsilon = new BigDecimal("0.01");

        boolean amountsMatch = requestAmount.subtract(billAmount).abs().compareTo(epsilon) <= 0;
        assertThat(amountsMatch).isTrue();

        BigDecimal wrongAmount = new BigDecimal("100.01");
        boolean wrongMatch = wrongAmount.subtract(billAmount).abs().compareTo(epsilon) <= 0;
        assertThat(wrongMatch).isTrue();

        BigDecimal farOffAmount = new BigDecimal("101.00");
        boolean farMatch = farOffAmount.subtract(billAmount).abs().compareTo(epsilon) <= 0;
        assertThat(farMatch).isFalse();
    }
}
