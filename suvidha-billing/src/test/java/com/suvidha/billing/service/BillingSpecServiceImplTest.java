package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.SpecPaymentResponse;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.BusinessRuleException;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSpecServiceImplTest {

    @Mock
    private ServiceAccountRepository accountRepo;

    @Mock
    private BillRepository billRepo;

    @Mock
    private TransactionRepository txnRepo;

    private BillingSpecServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new BillingSpecServiceImpl(accountRepo, billRepo, txnRepo, objectMapper);
    }

    @Test
    void processPayment_rejectsAlreadyPaidBill() {
        ServiceAccount account = ServiceAccount.builder()
                .id("acct-1")
                .citizenId("citizen-1")
                .serviceType(ServiceType.ELECTRICITY)
                .accountNo("E123456789")
                .isActive(true)
                .build();

        UUID billId = UUID.randomUUID();
        Bill bill = Bill.builder()
                .id(billId)
                .citizenId("citizen-1")
                .totalAmount(BigDecimal.valueOf(100.0))
                .status(BillStatus.PAID)
                .account(account)
                .build();

        SpecPaymentRequest request = new SpecPaymentRequest();
        request.setBillId(billId.toString());
        request.setAmount(BigDecimal.valueOf(100.0));
        request.setPaymentMethod("mock_upi");

        when(accountRepo.findById("acct-1")).thenReturn(Optional.of(account));
        when(billRepo.findById(billId)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> service.processPayment("acct-1", request, "citizen-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been paid");

        verify(txnRepo, never()).save(any());
        verify(billRepo, never()).save(any());
    }

    @Test
    void processPayment_succeedsForPendingBill() {
        ServiceAccount account = ServiceAccount.builder()
                .id("acct-2")
                .citizenId("citizen-2")
                .serviceType(ServiceType.WATER)
                .accountNo("W123456789")
                .isActive(true)
                .build();

        UUID billId = UUID.randomUUID();
        Bill bill = Bill.builder()
                .id(billId)
                .citizenId("citizen-2")
                .totalAmount(BigDecimal.valueOf(50.0))
                .status(BillStatus.PENDING)
                .account(account)
                .build();

        SpecPaymentRequest request = new SpecPaymentRequest();
        request.setBillId(billId.toString());
        request.setAmount(BigDecimal.valueOf(50.0));
        request.setPaymentMethod("mock_card");

        when(accountRepo.findById("acct-2")).thenReturn(Optional.of(account));
        when(billRepo.findById(billId)).thenReturn(Optional.of(bill));
        when(txnRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SpecPaymentResponse result = service.processPayment("acct-2", request, "citizen-2");

        assertThat(result.getPaymentStatus()).isEqualTo("success");
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        verify(txnRepo).save(any());
        verify(billRepo).save(any());
    }

    @Test
    void processPayment_rejectsOverdueBill() {
        ServiceAccount account = ServiceAccount.builder()
                .id("acct-3")
                .citizenId("citizen-3")
                .serviceType(ServiceType.GAS)
                .accountNo("G123456789")
                .isActive(true)
                .build();

        UUID billId = UUID.randomUUID();
        Bill bill = Bill.builder()
                .id(billId)
                .citizenId("citizen-3")
                .totalAmount(BigDecimal.valueOf(75.0))
                .status(BillStatus.OVERDUE)
                .account(account)
                .build();

        SpecPaymentRequest request = new SpecPaymentRequest();
        request.setBillId(billId.toString());
        request.setAmount(BigDecimal.valueOf(75.0));
        request.setPaymentMethod("mock_neft");

        when(accountRepo.findById("acct-3")).thenReturn(Optional.of(account));
        when(billRepo.findById(billId)).thenReturn(Optional.of(bill));
        when(txnRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SpecPaymentResponse result = service.processPayment("acct-3", request, "citizen-3");

        assertThat(result.getPaymentStatus()).isEqualTo("success");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
    }
}
