package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.ConfirmPaymentRequest;
import com.suvidha.billing.dto.response.PaymentConfirmDTO;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.exception.BusinessRuleException;
import com.suvidha.billing.exception.UnauthorizedException;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingFacadeServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private BillingFacadeServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        service = new BillingFacadeServiceImpl(
                mock(com.suvidha.billing.repository.ServiceAccountRepository.class),
                billRepository,
                transactionRepository,
                idempotencyService);
    }

    @Test
    void confirmPayment_returnsCachedResponse_whenIdempotencyKeyFoundInRedis() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_123");
        req.setPaymentId("pay_123");
        req.setSignature("sig_123");

        PaymentConfirmDTO cached = PaymentConfirmDTO.builder()
                .paymentId("pay_cached")
                .receiptUrl("/api/v1/billing/receipt/pay_cached")
                .build();

        when(idempotencyService.getCachedResponse("idem-key-1")).thenReturn(Optional.of(cached));

        PaymentConfirmDTO result = service.confirmPayment(req, "idem-key-1", "citizen-1");

        assertThat(result.getPaymentId()).isEqualTo("pay_cached");
        assertThat(result.getReceiptUrl()).isEqualTo("/api/v1/billing/receipt/pay_cached");
        verifyNoInteractions(transactionRepository);
        verifyNoMoreInteractions(idempotencyService);
    }

    @Test
    void confirmPayment_processesPayment_whenIdempotencyKeyNotInRedis() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_new")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(100.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(100.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_new");
        req.setPaymentId("pay_new");
        req.setSignature("sig_new");

        when(idempotencyService.getCachedResponse("idem-key-2")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_new")).thenReturn(List.of(tx));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmDTO result = service.confirmPayment(req, "idem-key-2", "citizen-1");

        assertThat(result.getPaymentId()).isEqualTo("pay_new");
        assertThat(tx.getStatus()).isEqualTo("CAPTURED");
        assertThat(tx.getIdempotencyKey()).isEqualTo("idem-key-2");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);

        verify(idempotencyService).cacheResponse(eq("idem-key-2"), any(PaymentConfirmDTO.class));
    }

    @Test
    void confirmPayment_processesPayment_whenNoIdempotencyKeyProvided() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_no_idem")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(50.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(50.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_no_idem");
        req.setPaymentId("pay_no_idem");
        req.setSignature("sig_no_idem");

        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_no_idem")).thenReturn(List.of(tx));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmDTO result = service.confirmPayment(req, null, "citizen-1");

        assertThat(result.getPaymentId()).isEqualTo("pay_no_idem");
        assertThat(tx.getStatus()).isEqualTo("CAPTURED");
        assertThat(tx.getIdempotencyKey()).isNull();
        verify(idempotencyService, never()).cacheResponse(anyString(), any());
    }

    @Test
    void confirmPayment_returnsEarly_whenTransactionAlreadyCaptured_andCachesResponse() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_captured")
                .razorpayPaymentId("pay_already_done")
                .citizenId("citizen-1")
                .status("CAPTURED")
                .amount(BigDecimal.valueOf(200.0))
                .build();

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_captured");
        req.setPaymentId("pay_new_attempt");
        req.setSignature("sig_new");

        when(idempotencyService.getCachedResponse("idem-key-3")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_captured")).thenReturn(List.of(tx));

        PaymentConfirmDTO result = service.confirmPayment(req, "idem-key-3", "citizen-1");

        assertThat(result.getPaymentId()).isEqualTo("pay_already_done");
        verify(idempotencyService).cacheResponse(eq("idem-key-3"), any(PaymentConfirmDTO.class));
        verify(transactionRepository).findByIdempotencyKey(anyString());
        verify(transactionRepository).findAllByRazorpayOrderIdForUpdate(anyString());
    }

    @Test
    void confirmPayment_throwsUnauthorized_whenCitizenMismatch() {
        Transaction tx = Transaction.builder()
                .razorpayOrderId("order_other")
                .citizenId("citizen-other")
                .status("CREATED")
                .build();

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_other");

        when(idempotencyService.getCachedResponse("idem-key-4")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_other")).thenReturn(List.of(tx));

        assertThatThrownBy(() -> service.confirmPayment(req, "idem-key-4", "citizen-wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void confirmPayment_throws_whenOrderNotFound() {
        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_missing");

        when(idempotencyService.getCachedResponse("idem-key-5")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_missing")).thenReturn(List.of());

        assertThatThrownBy(() -> service.confirmPayment(req, "idem-key-5", "citizen-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void confirmPayment_duplicateCalls_withSameKey_returnCachedResponse() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_dupe")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(75.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(75.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_dupe");
        req.setPaymentId("pay_dupe");
        req.setSignature("sig_dupe");

        when(idempotencyService.getCachedResponse("idem-key-dupe")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_dupe")).thenReturn(List.of(tx));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmDTO firstResult = service.confirmPayment(req, "idem-key-dupe", "citizen-1");
        assertThat(firstResult.getPaymentId()).isEqualTo("pay_dupe");

        PaymentConfirmDTO cached = PaymentConfirmDTO.builder()
                .paymentId("pay_dupe")
                .receiptUrl("/api/v1/billing/receipt/pay_dupe")
                .build();
        when(idempotencyService.getCachedResponse("idem-key-dupe")).thenReturn(Optional.of(cached));

        ConfirmPaymentRequest req2 = new ConfirmPaymentRequest();
        req2.setOrderId("order_dupe");
        req2.setPaymentId("pay_dupe_2");
        req2.setSignature("sig_dupe_2");

        PaymentConfirmDTO secondResult = service.confirmPayment(req2, "idem-key-dupe", "citizen-1");

        assertThat(secondResult.getPaymentId()).isEqualTo("pay_dupe");
        assertThat(secondResult.getPaymentId()).isNotEqualTo("pay_dupe_2");
        verify(transactionRepository, times(1)).saveAll(anyList());
    }

    @Test
    void confirmPayment_usesPessimisticLock_forUpdate() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_lock")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(30.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(30.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_lock");
        req.setPaymentId("pay_lock");
        req.setSignature("sig_lock");

        when(idempotencyService.getCachedResponse("idem-lock")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_lock")).thenReturn(List.of(tx));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        lenient().when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        service.confirmPayment(req, "idem-lock", "citizen-1");

        verify(transactionRepository).findAllByRazorpayOrderIdForUpdate("order_lock");
        verify(transactionRepository, never()).findAllByRazorpayOrderId(anyString());
    }

    @Test
    void confirmPayment_secondConcurrentCall_seesCapturedStatus() {
        UUID billId = UUID.randomUUID();

        Transaction txFirst = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_concurrent")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(60.0))
                .build();

        Transaction txSecond = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_concurrent")
                .razorpayPaymentId("pay_first")
                .citizenId("citizen-1")
                .status("CAPTURED")
                .amount(BigDecimal.valueOf(60.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(60.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_concurrent");
        req.setPaymentId("pay_first");
        req.setSignature("sig_first");

        when(idempotencyService.getCachedResponse("idem-c1")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_concurrent"))
                .thenReturn(List.of(txFirst));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmDTO firstResult = service.confirmPayment(req, "idem-c1", "citizen-1");
        assertThat(firstResult.getPaymentId()).isEqualTo("pay_first");

        txFirst.setStatus("CAPTURED");
        txFirst.setRazorpayPaymentId("pay_first");

        ConfirmPaymentRequest req2 = new ConfirmPaymentRequest();
        req2.setOrderId("order_concurrent");
        req2.setPaymentId("pay_second");
        req2.setSignature("sig_second");

        when(idempotencyService.getCachedResponse("idem-c2")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_concurrent"))
                .thenReturn(List.of(txSecond));

        PaymentConfirmDTO secondResult = service.confirmPayment(req2, "idem-c2", "citizen-1");

        assertThat(secondResult.getPaymentId()).isEqualTo("pay_first");
        assertThat(secondResult.getPaymentId()).isNotEqualTo("pay_second");
        verify(transactionRepository, times(1)).saveAll(anyList());
    }

    @Test
    void confirmPayment_propagatesOptimisticLockFailure_whenBillVersionConflict() {
        UUID billId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .billId(billId)
                .razorpayOrderId("order_version")
                .citizenId("citizen-1")
                .status("CREATED")
                .amount(BigDecimal.valueOf(40.0))
                .build();

        Bill bill = new Bill();
        bill.setId(billId);
        bill.setCitizenId("citizen-1");
        bill.setTotalAmount(BigDecimal.valueOf(40.0));
        bill.setStatus(BillStatus.PENDING);

        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
        req.setOrderId("order_version");
        req.setPaymentId("pay_version");
        req.setSignature("sig_version");

        when(idempotencyService.getCachedResponse("idem-ver")).thenReturn(Optional.empty());
        when(transactionRepository.findAllByRazorpayOrderIdForUpdate("order_version")).thenReturn(List.of(tx));
        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(transactionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenThrow(new OptimisticLockingFailureException("version conflict"));

        assertThatThrownBy(() -> service.confirmPayment(req, "idem-ver", "citizen-1"))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("version conflict");
    }
}
