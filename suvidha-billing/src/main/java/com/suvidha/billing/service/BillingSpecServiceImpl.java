package com.suvidha.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvidha.billing.dto.request.SpecLinkAccountRequest;
import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.*;
import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.exception.*;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingSpecServiceImpl implements BillingSpecService {

    private static final Logger log = LoggerFactory.getLogger(BillingSpecServiceImpl.class);

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of("mock_card", "mock_upi", "mock_neft");
    private static final Map<String, ServiceType> UTILITY_TYPE_MAP = Map.of(
            "electricity", ServiceType.ELECTRICITY,
            "gas", ServiceType.GAS,
            "water", ServiceType.WATER
    );

    private final ServiceAccountRepository accountRepo;
    private final BillRepository billRepo;
    private final TransactionRepository txnRepo;
    private final ObjectMapper objectMapper;

    public BillingSpecServiceImpl(ServiceAccountRepository accountRepo,
                                  BillRepository billRepo,
                                  TransactionRepository txnRepo,
                                  ObjectMapper objectMapper) {
        this.accountRepo = accountRepo;
        this.billRepo = billRepo;
        this.txnRepo = txnRepo;
        this.objectMapper = objectMapper;
    }

    // ── Link Account ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public SpecAccountResponse linkAccount(SpecLinkAccountRequest request, String citizenId, String mobile) {
        String utilType = request.getUtilityType().toLowerCase();
        ServiceType serviceType = UTILITY_TYPE_MAP.get(utilType);
        if (serviceType == null) {
            throw new InvalidConsumerIdException("Invalid utility_type: " + request.getUtilityType()
                    + ". Must be one of: electricity, gas, water");
        }

        validateConsumerId(request.getConsumerId(), serviceType);

        // Check for duplicate
        Optional<ServiceAccount> existing = accountRepo.findByAccountNoAndServiceType(
                request.getConsumerId(), serviceType);
        if (existing.isPresent()) {
            throw new AccountAlreadyLinkedException("Account already linked");
        }

        ServiceAccount account = ServiceAccount.builder()
                .citizenId(citizenId)
                .serviceType(serviceType)
                .accountNo(request.getConsumerId())
                .registeredMobile(mobile != null ? mobile : "")
                .isActive(true)
                .build();

        ServiceAccount saved = accountRepo.save(account);
        log.info("Linked account {} for citizen {}", saved.getId(), citizenId);
        return toSpecAccountResponse(saved);
    }

    // ── Get Accounts ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AccountsListResponse getAccounts(String citizenId) {
        List<ServiceAccount> accounts = accountRepo.findByCitizenId(citizenId);
        List<SpecAccountResponse> mapped = accounts.stream()
                .map(this::toSpecAccountResponse)
                .collect(Collectors.toList());
        return AccountsListResponse.builder().accounts(mapped).build();
    }

    // ── Get Bills ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SpecBillsResponse getBills(String accountId, String citizenId) {
        ServiceAccount account = findAccountOrThrow(accountId, citizenId);
        List<Bill> bills = billRepo.findByAccount_IdOrderByDueDateDesc(account.getId());
        List<SpecBillResponse> mapped = bills.stream()
                .map(this::toSpecBillResponse)
                .collect(Collectors.toList());
        return SpecBillsResponse.builder()
                .accountId(account.getId())
                .bills(mapped)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SpecBillResponse getBillById(String accountId, String billId, String citizenId) {
        ServiceAccount account = findAccountOrThrow(accountId, citizenId);
        UUID billUuid;
        try {
            billUuid = UUID.fromString(billId);
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Bill not found");
        }
        Bill bill = billRepo.findById(billUuid)
                .filter(b -> account.getId().equals(b.getAccountId()))
                .orElseThrow(() -> new AccountNotFoundException("Bill not found"));
        return toSpecBillResponse(bill);
    }

    // ── Process Payment ───────────────────────────────────────────────────

    @Override
    @Transactional
    public SpecPaymentResponse processPayment(String accountId, SpecPaymentRequest request, String citizenId) {
        ServiceAccount account = findAccountOrThrow(accountId, citizenId);

        if (!account.isActive()) {
            throw new UnauthorizedException("Account is not active");
        }

        // Validate payment method
        String method = request.getPaymentMethod().toLowerCase();
        if (!VALID_PAYMENT_METHODS.contains(method)) {
            throw new InvalidConsumerIdException("Invalid payment_method: " + request.getPaymentMethod()
                    + ". Must be one of: mock_card, mock_upi, mock_neft");
        }

        // Find the bill
        UUID billUuid;
        try {
            billUuid = UUID.fromString(request.getBillId());
        } catch (IllegalArgumentException e) {
            throw new AccountNotFoundException("Bill not found");
        }
        Bill bill = billRepo.findById(billUuid)
                .filter(b -> account.getId().equals(b.getAccountId()))
                .orElseThrow(() -> new AccountNotFoundException("Bill not found"));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessRuleException("Bill has already been paid");
        }

        // Validate amount matches
        if (request.getAmount().subtract(bill.getTotalAmount()).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new PaymentAmountMismatchException(
                    "Payment amount does not match bill amount");
        }

        if (request.getAmount().scale() > 2) {
            throw new PaymentAmountMismatchException(
                    "Payment amount must have at most 2 decimal places");
        }

        // All mock methods succeed immediately
        String txnId = "txn_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String receiptId = "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        LocalDateTime now = LocalDateTime.now();

        // Build receipt
        ReceiptDetails receipt = ReceiptDetails.builder()
                .receiptId(receiptId)
                .transactionId(txnId)
                .accountId(account.getId())
                .consumerId(account.getAccountNo())
                .utilityType(account.getServiceType().name().toLowerCase())
                .amount(request.getAmount())
                .paymentMethod(method)
                .timestamp(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z")
                .build();

        // Persist transaction
        String receiptJson;
        try {
            receiptJson = objectMapper.writeValueAsString(receipt);
        } catch (Exception e) {
            receiptJson = "{}";
        }

        Transaction txn = Transaction.builder()
                .billId(bill.getId())
                .accountId(account.getId())
                .transactionId(txnId)
                .razorpayOrderId(txnId)
                .amount(request.getAmount())
                .status("success")
                .paymentMethod(method)
                .receiptData(receiptJson)
                .build();
        txnRepo.save(txn);

        // Update bill status
        bill.setStatus(BillStatus.PAID);
        bill.setAmountPaid(request.getAmount());
        bill.setRemainingBalance(BigDecimal.ZERO);
        billRepo.save(bill);

        log.info("Payment processed: txn={} bill={} account={}", txnId, bill.getId(), account.getId());

        return SpecPaymentResponse.builder()
                .transactionId(txnId)
                .paymentStatus("success")
                .amount(request.getAmount())
                .paymentMethod(method)
                .receipt(receipt)
                .build();
    }

    // ── Payment History ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentHistoryResponse getPaymentHistory(String accountId, String citizenId, int limit, int offset) {
        ServiceAccount account = findAccountOrThrow(accountId, citizenId);
        int page = offset / Math.max(limit, 1);
        Page<Transaction> txnPage = txnRepo.findByAccountIdOrderByCreatedAtDesc(
                account.getId(), PageRequest.of(page, limit));

        List<PaymentHistoryResponse.PaymentSummary> payments = txnPage.getContent().stream()
                .map(t -> PaymentHistoryResponse.PaymentSummary.builder()
                        .transactionId(t.getTransactionId())
                        .amount(t.getAmount())
                        .paymentMethod(t.getPaymentMethod())
                        .paymentStatus(t.getStatus())
                        .createdAt(t.getCreatedAt() != null
                                ? t.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                                : null)
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("limit", limit);
        pagination.put("offset", offset);
        pagination.put("total", txnPage.getTotalElements());

        return PaymentHistoryResponse.builder()
                .accountId(account.getId())
                .payments(payments)
                .pagination(pagination)
                .build();
    }

    // ── Receipt ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SpecPaymentResponse getReceipt(String accountId, String transactionId, String citizenId) {
        ServiceAccount account = findAccountOrThrow(accountId, citizenId);
        Transaction txn = txnRepo.findByTransactionId(transactionId)
                .filter(t -> account.getId().equals(t.getAccountId()))
                .orElseThrow(() -> new AccountNotFoundException("Transaction not found"));

        ReceiptDetails receipt;
        try {
            receipt = objectMapper.readValue(txn.getReceiptData(), ReceiptDetails.class);
        } catch (Exception e) {
            receipt = ReceiptDetails.builder()
                    .transactionId(txn.getTransactionId())
                    .accountId(account.getId())
                    .amount(txn.getAmount())
                    .paymentMethod(txn.getPaymentMethod())
                    .build();
        }

        return SpecPaymentResponse.builder()
                .transactionId(txn.getTransactionId())
                .paymentStatus(txn.getStatus())
                .amount(txn.getAmount())
                .paymentMethod(txn.getPaymentMethod())
                .receipt(receipt)
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private ServiceAccount findAccountOrThrow(String accountId, String citizenId) {
        ServiceAccount account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (!account.getCitizenId().equals(citizenId)) {
            throw new UnauthorizedException("Access denied");
        }
        return account;
    }

    private void validateConsumerId(String consumerId, ServiceType serviceType) {
        boolean valid = switch (serviceType) {
            case ELECTRICITY -> consumerId.matches("^E\\d{9}$");
            case GAS -> consumerId.matches("^G\\d{9}$");
            case WATER -> consumerId.matches("^W\\d{9}$");
        };
        if (!valid) {
            throw new InvalidConsumerIdException(
                    "Invalid consumer ID format for utility type " + serviceType.name().toLowerCase()
                            + ". Expected format: " + getExpectedFormat(serviceType));
        }
    }

    private String getExpectedFormat(ServiceType serviceType) {
        return switch (serviceType) {
            case ELECTRICITY -> "E followed by 9 digits (e.g. E123456789)";
            case GAS -> "G followed by 9 digits (e.g. G123456789)";
            case WATER -> "W followed by 9 digits (e.g. W123456789)";
        };
    }

    private SpecAccountResponse toSpecAccountResponse(ServiceAccount a) {
        return SpecAccountResponse.builder()
                .accountId(a.getId())
                .utilityType(a.getServiceType().name().toLowerCase())
                .consumerId(a.getAccountNo())
                .accountStatus(a.isActive() ? "active" : "inactive")
                .createdAt(a.getCreatedAt() != null
                        ? a.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                        : null)
                .build();
    }

    private SpecBillResponse toSpecBillResponse(Bill b) {
        String period = b.getBillingPeriodStart() + " to " + b.getBillingPeriodEnd();
        String status = switch (b.getStatus()) {
            case PAID -> "paid";
            case OVERDUE -> "overdue";
            default -> "unpaid";
        };
        return SpecBillResponse.builder()
                .billId(b.getId().toString())
                .billPeriod(period)
                .amountDue(b.getTotalAmount())
                .dueDate(b.getDueDate().toString())
                .billStatus(status)
                .createdAt(b.getCreatedAt() != null
                        ? b.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z"
                        : null)
                .build();
    }
}
