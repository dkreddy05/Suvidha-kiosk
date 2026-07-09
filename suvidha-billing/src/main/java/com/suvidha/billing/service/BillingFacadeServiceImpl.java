package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.ConfirmPaymentRequest;
import com.suvidha.billing.dto.request.LinkAccountRequest;
import com.suvidha.billing.dto.request.PayBillRequest;
import com.suvidha.billing.dto.response.*;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.enums.PaymentMode;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.DuplicateRequestException;
import com.suvidha.billing.exception.UnauthorizedException;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BillingFacadeServiceImpl implements BillingFacadeService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ServiceAccountRepository serviceAccountRepository;
    private final BillRepository billRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    public BillingFacadeServiceImpl(
            ServiceAccountRepository serviceAccountRepository,
            BillRepository billRepository,
            TransactionRepository transactionRepository,
            IdempotencyService idempotencyService) {
        this.serviceAccountRepository = serviceAccountRepository;
        this.billRepository = billRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public List<UtilityAccountDTO> getAccountsForMobile(String mobile) {
        List<ServiceAccount> accounts = serviceAccountRepository
                .findByRegisteredMobileAndIsActive(mobile, true);
        return accounts.stream()
                .map(this::toUtilityAccountDTO)
                .toList();
    }

    @Override
    public List<BillDTO> getBillsForAccount(String accountId, String citizenId) {
        ServiceAccount account = serviceAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (!account.getCitizenId().equals(citizenId)) {
            throw new UnauthorizedException("Access denied to account " + accountId);
        }
        return billRepository.findByAccount_IdOrderByDueDateDesc(accountId)
                .stream()
                .map(this::toBillDTO)
                .toList();
    }

    @Override
    public BillDTO getBillById(String billId, String citizenId) {
        Bill bill = billRepository.findById(UUID.fromString(billId))
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));
        if (!bill.getCitizenId().equals(citizenId)) {
            throw new UnauthorizedException("Access denied to bill " + billId);
        }
        return toBillDTO(bill);
    }

    @Override
    @Transactional
    public Object initiatePay(PayBillRequest req, String citizenId) {
        if (req.getBillIds() == null || req.getBillIds().isEmpty()) {
            throw new IllegalArgumentException("At least one bill ID is required");
        }

        List<Bill> bills = req.getBillIds().stream()
                .map(id -> billRepository.findById(UUID.fromString(id))
                        .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id)))
                .toList();

        bills.forEach(b -> {
            if (!b.getCitizenId().equals(citizenId)) {
                throw new UnauthorizedException("Access denied to bill " + b.getId());
            }
        });

        BigDecimal totalAmount = bills.stream()
                .map(b -> b.getRemainingBalance() != null ? b.getRemainingBalance() : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (req.getPaymentMode() == PaymentMode.CASH) {
            throw new UnauthorizedException("CASH payments require ADMIN role");
        }

        String mockOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        bills.forEach(b -> {
            BigDecimal billAmount = (b.getRemainingBalance() != null ? b.getRemainingBalance() : b.getTotalAmount())
                    .setScale(2, RoundingMode.HALF_UP);
            Transaction tx = Transaction.builder()
                    .billId(b.getId())
                    .citizenId(citizenId)
                    .razorpayOrderId(mockOrderId)
                    .amount(billAmount)
                    .status("CREATED")
                    .paymentMethod(req.getPaymentMode().name())
                    .build();
            transactionRepository.save(tx);
        });

        return PaymentOrderDTO.builder()
                .orderId(mockOrderId)
                .amount(totalAmount)
                .currency("INR")
                .keyId("rzp_test_placeholder")
                .qrCodeData(null)
                .upiId(null)
                .build();
    }

    @Override
    @Transactional
    public PaymentConfirmDTO confirmPayment(ConfirmPaymentRequest req, String idempotencyKey, String citizenId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyService.getCachedResponse(idempotencyKey)
                    .orElseGet(() -> processPayment(req, idempotencyKey, citizenId));
        }
        return processPayment(req, idempotencyKey, citizenId);
    }

    private PaymentConfirmDTO processPayment(ConfirmPaymentRequest req, String idempotencyKey, String citizenId) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            transactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existing -> {
                    if (!existing.getRazorpayOrderId().equals(req.getOrderId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency key already associated with a different order");
                    }
                });
        }

        List<Transaction> txs = transactionRepository.findAllByRazorpayOrderIdForUpdate(req.getOrderId());
        if (txs.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + req.getOrderId());
        }

        // Verify ownership using the first transaction (all share the same citizenId)
        Transaction firstTx = txs.get(0);
        if (firstTx.getCitizenId() != null && !firstTx.getCitizenId().equals(citizenId)) {
            throw new UnauthorizedException("Access denied to transaction " + req.getOrderId());
        }

        // Idempotency: if already captured, return cached result
        if ("CAPTURED".equals(firstTx.getStatus())) {
            PaymentConfirmDTO cached = PaymentConfirmDTO.builder()
                    .paymentId(firstTx.getRazorpayPaymentId())
                    .receiptUrl("/api/v1/billing/receipt/" + firstTx.getRazorpayPaymentId())
                    .build();
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.cacheResponse(idempotencyKey, cached);
            }
            return cached;
        }

        verifyRazorpaySignature(req);

        // Mark ALL transactions in this order as CAPTURED — not just the first one
        for (Transaction tx : txs) {
            tx.setRazorpayPaymentId(req.getPaymentId());
            tx.setStatus("CAPTURED");
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                tx.setIdempotencyKey(idempotencyKey);
            }
        }
        transactionRepository.saveAll(txs);

        // Mark all associated bills as PAID
        for (Transaction tx : txs) {
            billRepository.findById(tx.getBillId()).ifPresent(bill -> {
                bill.setStatus(BillStatus.PAID);
                bill.setAmountPaid(bill.getTotalAmount());
                bill.setRemainingBalance(BigDecimal.ZERO);
                billRepository.save(bill);
            });
        }

        PaymentConfirmDTO result = PaymentConfirmDTO.builder()
                .paymentId(req.getPaymentId())
                .receiptUrl("/api/v1/billing/receipt/" + req.getPaymentId())
                .build();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.cacheResponse(idempotencyKey, result);
        }

        return result;
    }

    private void verifyRazorpaySignature(ConfirmPaymentRequest req) {
        if (razorpaySecret == null || razorpaySecret.isBlank()) {
            return;
        }
        try {
            String payload = req.getOrderId() + "|" + req.getPaymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpaySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expectedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(expectedBytes);
            if (!expectedSignature.equals(req.getSignature())) {
                throw new UnauthorizedException("Invalid payment signature");
            }
        } catch (Exception e) {
            throw new UnauthorizedException("Payment signature verification failed");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public ReceiptDTO getReceipt(String paymentId, String citizenId) {
        Transaction tx = transactionRepository.findByRazorpayPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (tx.getCitizenId() != null && !tx.getCitizenId().equals(citizenId)) {
            throw new UnauthorizedException("Access denied to receipt " + paymentId);
        }

        Bill bill = billRepository.findById(tx.getBillId())
                .orElseThrow(() -> new IllegalArgumentException("Bill not found for payment"));

        String html = buildReceiptHtml(bill, tx);

        return ReceiptDTO.builder()
                .receiptHtml(html)
                .receiptJson(Map.of(
                        "paymentId", paymentId,
                        "billId", bill.getId().toString(),
                        "billNumber", bill.getBillNumber(),
                        "amount", bill.getTotalAmount(),
                        "status", bill.getStatus().name(),
                        "paidAt", bill.getUpdatedAt() != null ? bill.getUpdatedAt().toString() : ""
                ))
                .build();
    }

    @Override
    @Transactional
    public UtilityAccountDTO linkAccount(LinkAccountRequest req, String citizenId, String mobile) {
        ServiceType serviceType = ServiceType.valueOf(req.getUtilityType().toUpperCase());

        ServiceAccount account = serviceAccountRepository
                .findByAccountNoAndServiceTypeForUpdate(req.getAccountNumber(), serviceType)
                .orElseGet(() -> ServiceAccount.builder()
                        .accountNo(req.getAccountNumber())
                        .serviceType(serviceType)
                        .providerName(req.getProviderName())
                        .address(req.getAddress())
                        .registeredMobile(mobile)
                        .citizenId(citizenId)
                        .isActive(true)
                        .build());

        account.setCitizenId(citizenId);
        account.setActive(true);
        if (req.getProviderName() != null && !req.getProviderName().isBlank()) {
            account.setProviderName(req.getProviderName());
        }
        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            account.setAddress(req.getAddress());
        }

        ServiceAccount saved = serviceAccountRepository.save(account);
        return toUtilityAccountDTO(saved);
    }

    private UtilityAccountDTO toUtilityAccountDTO(ServiceAccount sa) {
        Bill latestBill = billRepository.findTopByAccount_IdOrderByDueDateDesc(sa.getId()).orElse(null);

        UtilityAccountDTO.LatestBillDTO latestBillDTO = null;
        if (latestBill != null) {
            latestBillDTO = UtilityAccountDTO.LatestBillDTO.builder()
                    .billId(latestBill.getId().toString())
                    .billMonth(latestBill.getBillingPeriodStart() != null
                            ? latestBill.getBillingPeriodStart().format(MONTH_FMT) : null)
                    .amount(latestBill.getTotalAmount())
                    .dueDate(latestBill.getDueDate() != null ? latestBill.getDueDate().toString() : null)
                    .status(mapBillStatus(latestBill.getStatus()))
                    .build();
        }

        return UtilityAccountDTO.builder()
                .id(sa.getId())
                .citizenId(sa.getCitizenId())
                .accountNumber(sa.getAccountNo())
                .utilityType(sa.getServiceType().name())
                .providerName(sa.getProviderName())
                .address(sa.getAddress())
                .latestBill(latestBillDTO)
                .build();
    }

    private BillDTO toBillDTO(Bill b) {
        return BillDTO.builder()
                .id(b.getId().toString())
                .accountId(b.getAccountId())
                .billNumber(b.getBillNumber())
                .billMonth(b.getBillingPeriodStart() != null
                        ? b.getBillingPeriodStart().format(MONTH_FMT) : null)
                .amount(b.getTotalAmount())
                .dueDate(b.getDueDate() != null ? b.getDueDate().toString() : null)
                .paidAmount(b.getAmountPaid() != null ? b.getAmountPaid() : BigDecimal.ZERO)
                .status(mapBillStatus(b.getStatus()))
                .paidAt(BillStatus.PAID.equals(b.getStatus()) && b.getUpdatedAt() != null
                        ? b.getUpdatedAt().toString() : null)
                .build();
    }

    private String mapBillStatus(BillStatus status) {
        if (status == null) return "PENDING";
        return switch (status) {
            case PAID -> "PAID";
            case OVERDUE -> "OVERDUE";
            default -> "PENDING";
        };
    }

    private String buildReceiptHtml(Bill bill, Transaction tx) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Payment Receipt</title>
                <style>
                  body { font-family: sans-serif; max-width: 600px; margin: 40px auto; color: #1e293b; }
                  h1 { font-size: 24px; } table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
                  td { padding: 10px; border-bottom: 1px solid #e2e8f0; }
                  td:first-child { color: #64748b; }
                  .amount { font-size: 28px; font-weight: bold; color: #0ea5e9; }
                  .footer { margin-top: 40px; font-size: 12px; color: #94a3b8; text-align: center; }
                </style>
                </head>
                <body>
                  <h1>SUVIDHA Payment Receipt</h1>
                  <table>
                    <tr><td>Bill Number</td><td>%s</td></tr>
                    <tr><td>Payment ID</td><td>%s</td></tr>
                    <tr><td>Amount</td><td class="amount">₹%.2f</td></tr>
                    <tr><td>Due Date</td><td>%s</td></tr>
                    <tr><td>Status</td><td>PAID</td></tr>
                  </table>
                  <div class="footer">Thank you for using SUVIDHA. This is a computer-generated receipt.</div>
                </body>
                </html>
                """.formatted(
                        bill.getBillNumber(),
                        tx.getRazorpayPaymentId(),
                        bill.getTotalAmount(),
                        bill.getDueDate());
    }
}
