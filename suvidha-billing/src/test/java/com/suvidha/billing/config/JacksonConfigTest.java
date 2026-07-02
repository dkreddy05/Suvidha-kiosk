package com.suvidha.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.response.BillDTO;
import com.suvidha.billing.dto.response.PaymentOrderDTO;
import com.suvidha.billing.dto.response.ReceiptDetails;
import com.suvidha.billing.dto.response.SpecBillResponse;
import com.suvidha.billing.dto.response.SpecPaymentResponse;
import com.suvidha.billing.dto.response.PaymentHistoryResponse;
import com.suvidha.billing.dto.response.UtilityAccountDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("BigDecimal serializes with exactly 2 decimal places")
    void bigDecimalSerializesWithTwoDecimals() throws Exception {
        String json = objectMapper.writeValueAsString(BigDecimal.valueOf(100.0));
        assertThat(json).isEqualTo("100.00");
    }

    @Test
    @DisplayName("BigDecimal.ZERO serializes as 0.00")
    void bigDecimalZeroSerializesAsTwoDecimals() throws Exception {
        String json = objectMapper.writeValueAsString(BigDecimal.ZERO);
        assertThat(json).isEqualTo("0.00");
    }

    @Test
    @DisplayName("BillDTO amount fields serialize with 2 decimal places")
    void billDtoAmountsSerializeWithTwoDecimals() throws Exception {
        BillDTO dto = BillDTO.builder()
                .amount(new BigDecimal("1234.5"))
                .paidAmount(BigDecimal.ZERO)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"amount\":1234.50");
        assertThat(json).contains("\"paidAmount\":0.00");
    }

    @Test
    @DisplayName("PaymentOrderDTO amount serializes with 2 decimal places")
    void paymentOrderDtoAmountSerializesWithTwoDecimals() throws Exception {
        PaymentOrderDTO dto = PaymentOrderDTO.builder()
                .amount(new BigDecimal("2500"))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"amount\":2500.00");
    }

    @Test
    @DisplayName("SpecBillResponse amountDue serializes with 2 decimal places")
    void specBillResponseAmountDueSerializesWithTwoDecimals() throws Exception {
        SpecBillResponse resp = SpecBillResponse.builder()
                .amountDue(new BigDecimal("875.25"))
                .build();

        String json = objectMapper.writeValueAsString(resp);
        assertThat(json).contains("\"amount_due\":875.25");
    }

    @Test
    @DisplayName("SpecPaymentResponse amount serializes with 2 decimal places")
    void specPaymentResponseAmountSerializesWithTwoDecimals() throws Exception {
        SpecPaymentResponse resp = SpecPaymentResponse.builder()
                .amount(new BigDecimal("320"))
                .build();

        String json = objectMapper.writeValueAsString(resp);
        assertThat(json).contains("\"amount\":320.00");
    }

    @Test
    @DisplayName("ReceiptDetails amount serializes with 2 decimal places")
    void receiptDetailsAmountSerializesWithTwoDecimals() throws Exception {
        ReceiptDetails receipt = ReceiptDetails.builder()
                .amount(new BigDecimal("450.5"))
                .build();

        String json = objectMapper.writeValueAsString(receipt);
        assertThat(json).contains("\"amount\":450.50");
    }

    @Test
    @DisplayName("PaymentHistoryResponse.PaymentSummary amount serializes with 2 decimal places")
    void paymentHistorySummaryAmountSerializesWithTwoDecimals() throws Exception {
        PaymentHistoryResponse.PaymentSummary summary = PaymentHistoryResponse.PaymentSummary.builder()
                .amount(new BigDecimal("125"))
                .build();

        String json = objectMapper.writeValueAsString(summary);
        assertThat(json).contains("\"amount\":125.00");
    }

    @Test
    @DisplayName("UtilityAccountDTO.LatestBillDTO amount serializes with 2 decimal places")
    void utilityAccountLatestBillAmountSerializesWithTwoDecimals() throws Exception {
        UtilityAccountDTO.LatestBillDTO latestBill = UtilityAccountDTO.LatestBillDTO.builder()
                .amount(new BigDecimal("675.3"))
                .build();

        String json = objectMapper.writeValueAsString(latestBill);
        assertThat(json).contains("\"amount\":675.30");
    }

    @Test
    @DisplayName("Null BigDecimal serializes as null")
    void nullBigDecimalSerializesAsNull() throws Exception {
        BillDTO dto = BillDTO.builder()
                .amount(null)
                .paidAmount(null)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).contains("\"amount\":null");
        assertThat(json).contains("\"paidAmount\":null");
    }
}
