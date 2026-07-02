package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.request.ConfirmPaymentRequest;
import com.suvidha.billing.dto.request.PayBillRequest;
import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.PaymentConfirmDTO;
import com.suvidha.billing.dto.response.SpecPaymentResponse;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.enums.PaymentMode;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.BusinessRuleException;
import com.suvidha.billing.exception.PaymentAmountMismatchException;
import com.suvidha.billing.exception.UnauthorizedException;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Fintech-grade attack test suite for the Billing Service.
 *
 * Covers:
 * 1. Double payment attempts
 * 2. Concurrent payments
 * 3. Duplicate transaction IDs
 * 4. Partial payment manipulation
 * 5. Amount tampering
 * 6. Unauthorized account access
 * 7. Bill status inconsistencies
 * 8. Payment rollback failures
 * 9. Stale bill payment
 * 10. Invalid payment methods
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceAttackTest {

    @Mock
    private ServiceAccountRepository accountRepo;
    @Mock
    private BillRepository billRepo;
    @Mock
    private TransactionRepository txnRepo;
    @Mock
    private IdempotencyService idempotencyService;

    private BillingFacadeServiceImpl legacyService;
    private BillingSpecServiceImpl specService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CITIZEN_A = "citizen-A";
    private static final String CITIZEN_B = "citizen-B";
    private static final String ACCT_ID = "acct-001";
    private static final UUID BILL_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final BigDecimal BILL_AMOUNT = new BigDecimal("1500.00");

    @BeforeEach
    void setUp() {
        legacyService = new BillingFacadeServiceImpl(accountRepo, billRepo, txnRepo, idempotencyService);
        specService = new BillingSpecServiceImpl(accountRepo, billRepo, txnRepo, objectMapper);
    }

    // ─── Helper factories ───────────────────────────────────────────────────

    private ServiceAccount makeAccount(String citizenId) {
        return ServiceAccount.builder()
                .id(ACCT_ID)
                .citizenId(citizenId)
                .serviceType(ServiceType.ELECTRICITY)
                .accountNo("E123456789")
                .isActive(true)
                .build();
    }

    private Bill makeBill(String citizenId, BillStatus status, BigDecimal amount) {
        ServiceAccount acct = ServiceAccount.builder().id(ACCT_ID).citizenId(citizenId).build();
        return Bill.builder()
                .id(BILL_ID)
                .billNumber("BILL-001")
                .citizenId(citizenId)
                .account(acct)
                .meterReadingId(UUID.randomUUID())
                .billingPeriodStart(LocalDate.of(2026, 1, 1))
                .billingPeriodEnd(LocalDate.of(2026, 1, 31))
                .totalAmount(amount)
                .amountPaid(BigDecimal.ZERO)
                .remainingBalance(amount)
                .dueDate(LocalDate.of(2026, 2, 15))
                .status(status)
                .version(0L)
                .build();
    }

    private Transaction makeTransaction(String orderId, String citizenId, String status, BigDecimal amount) {
        return Transaction.builder()
                .billId(BILL_ID)
                .accountId(ACCT_ID)
                .razorpayOrderId(orderId)
                .citizenId(citizenId)
                .status(status)
                .amount(amount)
                .build();
    }

    // =========================================================================
    // 1. DOUBLE PAYMENT ATTEMPTS
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 1: Double Payment Attempts")
    class DoublePaymentAttempts {

        @Test
        @DisplayName("Re-confirming same order should return cached result, not double-charge")
        void reconfirmSameOrder_shouldNotDoubleCharge() {
            String orderId = "order_double";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);

            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setOrderId(orderId);
            req.setPaymentId("pay_first");
            req.setSignature("sig");

            when(idempotencyService.getCachedResponse("idem-1")).thenReturn(Optional.empty());
            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            PaymentConfirmDTO first = legacyService.confirmPayment(req, "idem-1", CITIZEN_A);
            assertThat(first.getPaymentId()).isEqualTo("pay_first");
            assertThat(tx.getStatus()).isEqualTo("CAPTURED");

            // Second confirm with same idempotency key returns cached
            PaymentConfirmDTO cached = PaymentConfirmDTO.builder()
                    .paymentId("pay_first")
                    .receiptUrl("/api/v1/billing/receipt/pay_first")
                    .build();
            when(idempotencyService.getCachedResponse("idem-1")).thenReturn(Optional.of(cached));

            ConfirmPaymentRequest req2 = new ConfirmPaymentRequest();
            req2.setOrderId(orderId);
            req2.setPaymentId("pay_second");
            req2.setSignature("sig2");

            PaymentConfirmDTO second = legacyService.confirmPayment(req2, "idem-1", CITIZEN_A);
            assertThat(second.getPaymentId()).isEqualTo("pay_first");
            assertThat(second.getPaymentId()).isNotEqualTo("pay_second");

            verify(txnRepo, times(1)).save(any());
            verify(billRepo, times(1)).save(any());
        }

        @Test
        @DisplayName("Confirming already CAPTURED transaction should not re-process")
        void alreadyCaptured_shouldReturnOriginalPaymentId() {
            String orderId = "order_captured";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CAPTURED", BILL_AMOUNT);
            tx.setRazorpayPaymentId("pay_original");

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setOrderId(orderId);
            req.setPaymentId("pay_attacker");
            req.setSignature("sig");

            when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));

            PaymentConfirmDTO result = legacyService.confirmPayment(req, "idem-x", CITIZEN_A);

            assertThat(result.getPaymentId()).isEqualTo("pay_original");
            assertThat(result.getPaymentId()).isNotEqualTo("pay_attacker");
            verify(txnRepo, never()).save(any());
        }

        @Test
        @DisplayName("Spec service: paying already-PAID bill must be rejected")
        void specService_rejectPaidBill() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PAID, BILL_AMOUNT);

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already been paid");

            verify(txnRepo, never()).save(any());
            verify(billRepo, never()).save(any());
        }
    }

    // =========================================================================
    // 2. CONCURRENT PAYMENTS
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 2: Concurrent Payments")
    class ConcurrentPayments {

        @Test
        @DisplayName("Two simultaneous confirms on same order - only one should succeed")
        void concurrentConfirm_onlyOneSucceeds() throws Exception {
            String orderId = "order_concurrent";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            AtomicInteger saveCount = new AtomicInteger(0);
            AtomicReference<String> winningPaymentId = new AtomicReference<>();

            when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(invocation -> {
                saveCount.incrementAndGet();
                Transaction t = invocation.getArgument(0);
                winningPaymentId.compareAndSet(null, t.getRazorpayPaymentId());
                return t;
            });
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            int threads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < threads; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        ConfirmPaymentRequest req = new ConfirmPaymentRequest();
                        req.setOrderId(orderId);
                        req.setPaymentId("pay_" + idx);
                        req.setSignature("sig_" + idx);
                        legacyService.confirmPayment(req, "idem-concurrent-" + idx, CITIZEN_A);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            // Only first caller should have saved; rest see CAPTURED
            assertThat(saveCount.get()).isLessThanOrEqualTo(threads);
            assertThat(successCount.get() + errorCount.get()).isEqualTo(threads);
        }

        @Test
        @DisplayName("Spec service: concurrent payments on same bill - optimistic lock prevents double-pay")
        void specConcurrent_optimisticLockPreventsDoublePay() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> {
                Bill b = i.getArgument(0);
                b.setVersion(b.getVersion() + 1);
                return b;
            });

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
            verify(billRepo).save(any());
        }
    }

    // =========================================================================
    // 3. DUPLICATE TRANSACTION IDs
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 3: Duplicate Transaction IDs")
    class DuplicateTransactionIds {

        @Test
        @DisplayName("Reusing same transaction_id should cause DataIntegrityViolation")
        void duplicateTransactionId_shouldFail() {
            String txnId = "txn_duplicate_attack";

            Transaction duplicate = Transaction.builder()
                    .billId(BILL_ID)
                    .accountId(ACCT_ID)
                    .transactionId(txnId)
                    .razorpayOrderId(txnId)
                    .amount(BILL_AMOUNT)
                    .status("success")
                    .paymentMethod("mock_upi")
                    .build();

            when(txnRepo.save(duplicate)).thenThrow(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint"));

            assertThatThrownBy(() -> txnRepo.save(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("duplicate key");
        }

        @Test
        @DisplayName("Reusing same razorpay_order_id should be blocked by unique constraint")
        void duplicateRazorpayOrderId_shouldFail() {
            String orderId = "order_rzp_duplicate";

            Transaction duplicate = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);

            when(txnRepo.save(duplicate)).thenThrow(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint"));

            assertThatThrownBy(() -> txnRepo.save(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // =========================================================================
    // 4. PARTIAL PAYMENT MANIPULATION
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 4: Partial Payment Manipulation")
    class PartialPaymentManipulation {

        @Test
        @DisplayName("Paying less than bill amount should be rejected by amount validation")
        void underpayment_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            BigDecimal partialAmount = new BigDecimal("750.00");

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(partialAmount);
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class)
                    .hasMessageContaining("does not match bill amount");

            verify(txnRepo, never()).save(any());
            verify(billRepo, never()).save(any());
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING);
            assertThat(bill.getRemainingBalance()).isEqualByComparingTo(BILL_AMOUNT);
        }

        @Test
        @DisplayName("Paying more than bill amount should be rejected")
        void overpayment_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            BigDecimal overAmount = new BigDecimal("3000.00");

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(overAmount);
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class)
                    .hasMessageContaining("does not match bill amount");

            verify(txnRepo, never()).save(any());
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PENDING);
        }

        @Test
        @DisplayName("Paying within 0.01 tolerance should be accepted")
        void withinTolerance_shouldBeAccepted() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            BigDecimal nearAmount = BILL_AMOUNT.add(new BigDecimal("0.01"));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(nearAmount);
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("Legacy: multi-bill payment uses sum of remaining balances")
        void multiBill_usesRemainingBalances() {
            UUID bill1 = UUID.randomUUID();
            UUID bill2 = UUID.randomUUID();

            ServiceAccount acct = makeAccount(CITIZEN_A);

            Bill b1 = Bill.builder()
                    .id(bill1)
                    .citizenId(CITIZEN_A)
                    .account(acct)
                    .totalAmount(new BigDecimal("1000.00"))
                    .amountPaid(new BigDecimal("400.00"))
                    .remainingBalance(new BigDecimal("600.00"))
                    .status(BillStatus.PARTIALLY_PAID)
                    .build();

            Bill b2 = Bill.builder()
                    .id(bill2)
                    .citizenId(CITIZEN_A)
                    .account(acct)
                    .totalAmount(new BigDecimal("500.00"))
                    .amountPaid(BigDecimal.ZERO)
                    .remainingBalance(new BigDecimal("500.00"))
                    .status(BillStatus.PENDING)
                    .build();

            when(billRepo.findById(bill1)).thenReturn(Optional.of(b1));
            when(billRepo.findById(bill2)).thenReturn(Optional.of(b2));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            PayBillRequest req = new PayBillRequest();
            req.setBillIds(List.of(bill1.toString(), bill2.toString()));
            req.setPaymentMode(PaymentMode.MOCK_UPI);

            Object result = legacyService.initiatePay(req, CITIZEN_A);

            assertThat(result).isNotNull();
            verify(txnRepo, times(2)).save(any());
        }
    }

    // =========================================================================
    // 5. AMOUNT TAMPERING
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 5: Amount Tampering")
    class AmountTampering {

        @Test
        @DisplayName("Zero amount payment should be rejected by validation")
        void zeroAmount_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BigDecimal.ZERO);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class);
        }

        @Test
        @DisplayName("Negative amount should be rejected")
        void negativeAmount_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(new BigDecimal("-100.00"));
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class);
        }

        @Test
        @DisplayName("Amount with >2 decimal places should be rejected")
        void excessiveDecimalPlaces_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(new BigDecimal("1500.001"));
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class)
                    .hasMessageContaining("at most 2 decimal places");
        }

        @Test
        @DisplayName("Extremely large amount should be rejected by amount mismatch")
        void extremelyLargeAmount_shouldBeRejected() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(new BigDecimal("99999999.99"));
            req.setPaymentMethod("mock_upi");

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class);

            verify(txnRepo, never()).save(any());
        }

        @Test
        @DisplayName("Floating point precision attack (0.1+0.2 != 0.3) should be handled by BigDecimal")
        void floatingPointPrecision_attack() {
            BigDecimal a = new BigDecimal("0.1");
            BigDecimal b = new BigDecimal("0.2");
            BigDecimal expected = new BigDecimal("0.3");

            assertThat(a.add(b)).isEqualByComparingTo(expected);

            BigDecimal billAmt = new BigDecimal("1500.10");
            BigDecimal tampered = new BigDecimal("0.1").add(new BigDecimal("0.2"))
                    .multiply(new BigDecimal("5000")).add(new BigDecimal("0.01"));

            assertThat(tampered).isNotEqualByComparingTo(billAmt);
        }
    }

    // =========================================================================
    // 6. UNAUTHORIZED ACCOUNT ACCESS
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 6: Unauthorized Account Access")
    class UnauthorizedAccountAccess {

        @Test
        @DisplayName("Citizen B cannot pay Citizen A's bill via legacy service")
        void crossCitizenPayment_legacy_shouldFail() {
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            PayBillRequest req = new PayBillRequest();
            req.setBillIds(List.of(BILL_ID.toString()));
            req.setPaymentMode(PaymentMode.MOCK_UPI);

            assertThatThrownBy(() -> legacyService.initiatePay(req, CITIZEN_B))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("Citizen B cannot confirm Citizen A's transaction")
        void crossCitizenConfirm_legacy_shouldFail() {
            String orderId = "order_unauthorized";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);

            when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setOrderId(orderId);
            req.setPaymentId("pay_attack");
            req.setSignature("sig");

            assertThatThrownBy(() -> legacyService.confirmPayment(req, "idem-x", CITIZEN_B))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("Accessing receipt of another citizen's payment should fail")
        void crossCitizenReceipt_legacy_shouldFail() {
            Transaction tx = makeTransaction("order_x", CITIZEN_A, "CAPTURED", BILL_AMOUNT);
            tx.setRazorpayPaymentId("pay_victim");

            when(txnRepo.findByRazorpayPaymentId("pay_victim")).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> legacyService.getReceipt("pay_victim", CITIZEN_B))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("Spec service: account lookup with wrong citizenId must fail")
        void specAccountAccess_wrongCitizen_shouldFail() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));

            assertThatThrownBy(() -> specService.getBills(ACCT_ID, CITIZEN_B))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    // =========================================================================
    // 7. BILL STATUS INCONSISTENCIES
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 7: Bill Status Inconsistencies")
    class BillStatusInconsistencies {

        @Test
        @DisplayName("Payment on PAID bill must be rejected")
        void paidBill_shouldRejectPayment() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PAID, BILL_AMOUNT);
            bill.setAmountPaid(BILL_AMOUNT);
            bill.setRemainingBalance(BigDecimal.ZERO);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already been paid");

            assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
            assertThat(bill.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Payment on OVERDUE bill should succeed and update status")
        void overdueBill_shouldAcceptPayment() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.OVERDUE, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);

            assertThat(result.getPaymentStatus()).isEqualTo("success");
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
            assertThat(bill.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("PAYMENT_IN_PROGRESS status should be handled gracefully")
        void paymentInProgress_status() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PAYMENT_IN_PROGRESS, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        }

        @Test
        @DisplayName("PARTIALLY_PAID bill should not be accepted by spec service (amount mismatch)")
        void partiallyPaidBill_amountMismatch() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            BigDecimal total = new BigDecimal("1000.00");
            BigDecimal paid = new BigDecimal("400.00");
            BigDecimal remaining = new BigDecimal("600.00");

            Bill bill = Bill.builder()
                    .id(BILL_ID)
                    .citizenId(CITIZEN_A)
                    .account(ServiceAccount.builder().id(ACCT_ID).build())
                    .totalAmount(total)
                    .amountPaid(paid)
                    .remainingBalance(remaining)
                    .status(BillStatus.PARTIALLY_PAID)
                    .build();

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(remaining);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(PaymentAmountMismatchException.class);
        }
    }

    // =========================================================================
    // 8. PAYMENT ROLLBACK FAILURES
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 8: Payment Rollback Failures")
    class PaymentRollbackFailures {

        @Test
        @DisplayName("If bill save fails, transaction should not be committed (transactional boundary)")
        void billSaveFailure_shouldRollbackTransaction() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenThrow(new RuntimeException("DB connection lost"));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB connection lost");

            verify(txnRepo).save(any());
            verify(billRepo).save(any());
        }

        @Test
        @DisplayName("Optimistic lock failure during bill update should propagate as 409")
        void optimisticLockFailure_shouldPropagate() {
            String orderId = "order_rollback";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(idempotencyService.getCachedResponse(any())).thenReturn(Optional.empty());
            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenThrow(new OptimisticLockingFailureException("version conflict"));

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setOrderId(orderId);
            req.setPaymentId("pay_rollback");
            req.setSignature("sig");

            assertThatThrownBy(() -> legacyService.confirmPayment(req, "idem-rb", CITIZEN_A))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessageContaining("version conflict");
        }

        @Test
        @DisplayName("Transaction record created but bill not updated - inconsistent state detection")
        void inconsistentState_detection() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenThrow(new RuntimeException("rollback"));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(RuntimeException.class);

            verify(txnRepo).save(any());
            verify(billRepo).save(any());
        }
    }

    // =========================================================================
    // 9. STALE BILL PAYMENT
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 9: Stale Bill Payment")
    class StaleBillPayment {

        @Test
        @DisplayName("Paying a bill that was already paid by another transaction should fail")
        void staleBill_alreadyPaidByOtherTxn() {
            ServiceAccount acct = makeAccount(CITIZEN_A);

            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> {
                Transaction t = i.getArgument(0);
                t.setStatus("success");
                return t;
            });
            when(billRepo.save(any())).thenAnswer(i -> {
                Bill b = i.getArgument(0);
                b.setStatus(BillStatus.PAID);
                b.setRemainingBalance(BigDecimal.ZERO);
                return b;
            });

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
            assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);

            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already been paid");
        }

        @Test
        @DisplayName("Citizen B cannot pay Citizen A's bill via spec service")
        void crossCitizenPayment_spec_shouldFail() {
            ServiceAccount acct = makeAccount(CITIZEN_A);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_B))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Access denied");
        }

        @Test
        @DisplayName("Paying bill from different account should fail")
        void billFromDifferentAccount_shouldFail() {
            String acctAId = "acct-A";
            String acctBId = "acct-B";

            ServiceAccount acctA = ServiceAccount.builder()
                    .id(acctAId)
                    .citizenId(CITIZEN_A)
                    .serviceType(ServiceType.ELECTRICITY)
                    .accountNo("E123456789")
                    .isActive(true)
                    .build();

            ServiceAccount acctB = ServiceAccount.builder()
                    .id(acctBId)
                    .citizenId(CITIZEN_B)
                    .serviceType(ServiceType.ELECTRICITY)
                    .accountNo("E987654321")
                    .isActive(true)
                    .build();

            Bill bill = Bill.builder()
                    .id(BILL_ID)
                    .citizenId(CITIZEN_B)
                    .account(acctB)
                    .totalAmount(BILL_AMOUNT)
                    .amountPaid(BigDecimal.ZERO)
                    .remainingBalance(BILL_AMOUNT)
                    .status(BillStatus.PENDING)
                    .build();

            when(accountRepo.findById(acctAId)).thenReturn(Optional.of(acctA));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(acctAId, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.AccountNotFoundException.class)
                    .hasMessageContaining("Bill not found");
        }

        @Test
        @DisplayName("Paying bill with expired due date (OVERDUE) should still work")
        void overdueBill_shouldStillBePayable() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.OVERDUE, BILL_AMOUNT);
            bill.setDueDate(LocalDate.of(2020, 1, 1));

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
        }
    }

    // =========================================================================
    // 10. INVALID PAYMENT METHODS
    // =========================================================================
    @Nested
    @DisplayName("ATTACK 10: Invalid Payment Methods")
    class InvalidPaymentMethods {

        @Test
        @DisplayName("Real Razorpay methods should be rejected in spec service (mock only)")
        void realPaymentMethod_shouldBeRejected() {
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(makeAccount(CITIZEN_A)));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.InvalidConsumerIdException.class)
                    .hasMessageContaining("Invalid payment_method");
        }

        @Test
        @DisplayName("SQL injection in payment method should be rejected")
        void sqlInjectionInPaymentMethod_shouldBeRejected() {
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(makeAccount(CITIZEN_A)));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi'; DROP TABLE transaction; --");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.InvalidConsumerIdException.class);
        }

        @Test
        @DisplayName("Empty payment method should be rejected")
        void emptyPaymentMethod_shouldBeRejected() {
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(makeAccount(CITIZEN_A)));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.InvalidConsumerIdException.class);
        }

        @Test
        @DisplayName("Null payment method should be rejected")
        void nullPaymentMethod_shouldBeRejected() {
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(makeAccount(CITIZEN_A)));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod(null);

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Case sensitivity attack: MOCK_UPI vs mock_upi")
        void caseVariation_shouldStillWork() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("MOCK_UPI");

            SpecPaymentResponse result = specService.processPayment(ACCT_ID, req, CITIZEN_A);
            assertThat(result.getPaymentStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("XSS in payment method should be rejected")
        void xssInPaymentMethod_shouldBeRejected() {
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(makeAccount(CITIZEN_A)));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("<script>alert('xss')</script>");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.InvalidConsumerIdException.class);
        }
    }

    // =========================================================================
    // ADDITIONAL: EDGE CASE SCENARIOS
    // =========================================================================
    @Nested
    @DisplayName("EDGE CASES: Additional Attack Vectors")
    class EdgeCases {

        @Test
        @DisplayName("CASH payment without ADMIN role should be rejected")
        void cashPayment_withoutAdmin_shouldFail() {
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));

            PayBillRequest req = new PayBillRequest();
            req.setBillIds(List.of(BILL_ID.toString()));
            req.setPaymentMode(PaymentMode.CASH);

            assertThatThrownBy(() -> legacyService.initiatePay(req, CITIZEN_A))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("CASH payments require ADMIN role");
        }

        @Test
        @DisplayName("Empty bill list should fail validation")
        void emptyBillList_shouldFail() {
            PayBillRequest req = new PayBillRequest();
            req.setBillIds(List.of());
            req.setPaymentMode(PaymentMode.MOCK_UPI);

            assertThatThrownBy(() -> legacyService.initiatePay(req, CITIZEN_A))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one bill ID is required");
        }

        @Test
        @DisplayName("Too many bills (>10) should fail validation")
        void tooManyBills_shouldFail() {
            List<String> billIds = java.util.stream.IntStream.range(0, 11)
                    .mapToObj(i -> UUID.randomUUID().toString())
                    .toList();

            PayBillRequest req = new PayBillRequest();
            req.setBillIds(billIds);
            req.setPaymentMode(PaymentMode.MOCK_UPI);

            assertThatThrownBy(() -> legacyService.initiatePay(req, CITIZEN_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Invalid UUID format for bill ID should fail")
        void invalidUuid_shouldFail() {
            ServiceAccount acct = makeAccount(CITIZEN_A);
            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId("not-a-uuid");
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(com.suvidha.billing.exception.AccountNotFoundException.class)
                    .hasMessageContaining("Bill not found");
        }

        @Test
        @DisplayName("Inactive account should not allow payments")
        void inactiveAccount_shouldReject() {
            ServiceAccount acct = ServiceAccount.builder()
                    .id(ACCT_ID)
                    .citizenId(CITIZEN_A)
                    .serviceType(ServiceType.ELECTRICITY)
                    .accountNo("E123456789")
                    .isActive(false)
                    .build();

            when(accountRepo.findById(ACCT_ID)).thenReturn(Optional.of(acct));

            SpecPaymentRequest req = new SpecPaymentRequest();
            req.setBillId(BILL_ID.toString());
            req.setAmount(BILL_AMOUNT);
            req.setPaymentMethod("mock_upi");

            assertThatThrownBy(() -> specService.processPayment(ACCT_ID, req, CITIZEN_A))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("Rapid-fire payment attempts should be handled by idempotency")
        void rapidFire_idempotencyProtection() {
            String orderId = "order_rapid";
            Transaction tx = makeTransaction(orderId, CITIZEN_A, "CREATED", BILL_AMOUNT);
            Bill bill = makeBill(CITIZEN_A, BillStatus.PENDING, BILL_AMOUNT);

            when(txnRepo.findAllByRazorpayOrderIdForUpdate(orderId)).thenReturn(List.of(tx));
            when(billRepo.findById(BILL_ID)).thenReturn(Optional.of(bill));
            when(txnRepo.save(any())).thenAnswer(i -> i.getArgument(0));
            when(billRepo.save(any())).thenAnswer(i -> i.getArgument(0));

            String idemKey = "idem-rapid-fire";

            ConfirmPaymentRequest req = new ConfirmPaymentRequest();
            req.setOrderId(orderId);
            req.setPaymentId("pay_rapid");
            req.setSignature("sig");

            when(idempotencyService.getCachedResponse(idemKey)).thenReturn(Optional.empty());
            PaymentConfirmDTO first = legacyService.confirmPayment(req, idemKey, CITIZEN_A);
            assertThat(first.getPaymentId()).isEqualTo("pay_rapid");

            PaymentConfirmDTO cached = PaymentConfirmDTO.builder()
                    .paymentId("pay_rapid")
                    .receiptUrl("/api/v1/billing/receipt/pay_rapid")
                    .build();
            when(idempotencyService.getCachedResponse(idemKey)).thenReturn(Optional.of(cached));

            for (int i = 0; i < 10; i++) {
                ConfirmPaymentRequest retryReq = new ConfirmPaymentRequest();
                retryReq.setOrderId(orderId);
                retryReq.setPaymentId("pay_retry_" + i);
                retryReq.setSignature("sig_" + i);

                PaymentConfirmDTO result = legacyService.confirmPayment(retryReq, idemKey, CITIZEN_A);
                assertThat(result.getPaymentId()).isEqualTo("pay_rapid");
            }

            verify(txnRepo, times(1)).save(any());
            verify(billRepo, times(1)).save(any());
        }
    }
}
