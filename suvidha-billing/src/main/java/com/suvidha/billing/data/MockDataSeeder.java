package com.suvidha.billing.data;

import com.suvidha.billing.entity.Bill;
import com.suvidha.billing.entity.ServiceAccount;
import com.suvidha.billing.entity.Transaction;
import com.suvidha.billing.enums.BillStatus;
import com.suvidha.billing.enums.ServiceType;
import com.suvidha.billing.repository.BillRepository;
import com.suvidha.billing.repository.ServiceAccountRepository;
import com.suvidha.billing.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Profile("dev")
@Component
public class MockDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockDataSeeder.class);

    private final ServiceAccountRepository accountRepo;
    private final BillRepository billRepo;
    private final TransactionRepository txnRepo;

    public MockDataSeeder(ServiceAccountRepository accountRepo,
                          BillRepository billRepo,
                          TransactionRepository txnRepo) {
        this.accountRepo = accountRepo;
        this.billRepo = billRepo;
        this.txnRepo = txnRepo;
    }

    @Override
    public void run(String... args) {
        if (accountRepo.count() > 0) {
            log.info("Database already contains service accounts — skipping mock data seed.");
            return;
        }
        log.info("Seeding mock billing data...");
        seed();
        log.info("Mock billing data seeded successfully.");
    }

    private void seed() {
        ServiceAccount acct1 = saveAccount("CIT-0001", ServiceType.ELECTRICITY, "E123456789",
                "Tata Power", "123 Main St, Delhi", "9999999991");
        ServiceAccount acct2 = saveAccount("CIT-0001", ServiceType.WATER, "W987654321",
                "Municipal Corp", "123 Main St, Delhi", "9999999991");
        ServiceAccount acct3 = saveAccount("CIT-0001", ServiceType.GAS, "G555555555",
                "Indraprastha Gas Ltd", "123 Main St, Delhi", "9999999991");
        ServiceAccount acct4 = saveAccount("CIT-0002", ServiceType.ELECTRICITY, "E111111111",
                "BSES Yamuna", "456 Oak Ave, Noida", "9999999992");
        ServiceAccount acct5 = saveAccount("CIT-0002", ServiceType.WATER, "W222222222",
                "Delhi Jal Board", "456 Oak Ave, Noida", "9999999992");

        generateBills(acct1, BigDecimal.valueOf(1200.0), List.of(BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.PARTIAL, BillSeed.UNPAID, BillSeed.OVERDUE, BillSeed.PAID, BillSeed.PAID));
        generateBills(acct2, BigDecimal.valueOf(450.0),  List.of(BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.OVERDUE, BillSeed.PAID, BillSeed.PAID));
        generateBills(acct3, BigDecimal.valueOf(750.0),  List.of(BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PARTIAL, BillSeed.UNPAID, BillSeed.UNPAID, BillSeed.OVERDUE, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID));
        generateBills(acct4, BigDecimal.valueOf(900.0),  List.of(BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.PARTIAL, BillSeed.PAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.OVERDUE));
        generateBills(acct5, BigDecimal.valueOf(350.0),  List.of(BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.PAID, BillSeed.PAID, BillSeed.UNPAID, BillSeed.UNPAID, BillSeed.PAID, BillSeed.OVERDUE, BillSeed.PAID, BillSeed.PAID));
    }

    private ServiceAccount saveAccount(String citizenId, ServiceType type, String accountNo,
                                       String provider, String address, String mobile) {
        return accountRepo.save(ServiceAccount.builder()
                .citizenId(citizenId)
                .serviceType(type)
                .accountNo(accountNo)
                .providerName(provider)
                .address(address)
                .registeredMobile(mobile)
                .isActive(true)
                .build());
    }

    private void generateBills(ServiceAccount account, BigDecimal baseAmount, List<BillSeed> statuses) {
        int year = 2025;
        for (int month = 0; month < 12; month++) {
            LocalDate periodStart = LocalDate.of(year, month + 1, 1);
            LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
            LocalDate dueDate = LocalDate.of(year, month + 1, 15);

            BigDecimal variance = BigDecimal.valueOf(Math.random() - 0.5)
                    .multiply(baseAmount)
                    .multiply(BigDecimal.valueOf(0.4));
            BigDecimal totalAmount = baseAmount.add(variance).setScale(2, RoundingMode.HALF_UP);
            BillSeed seed = statuses.get(month);
            BigDecimal lateFee = (seed == BillSeed.OVERDUE)
                    ? totalAmount.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal amountPaid;
            BigDecimal remainingBalance;
            BillStatus status;

            switch (seed) {
                case PAID -> {
                    amountPaid = totalAmount;
                    remainingBalance = BigDecimal.ZERO;
                    status = BillStatus.PAID;
                }
                case PARTIAL -> {
                    amountPaid = totalAmount.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
                    remainingBalance = totalAmount.subtract(amountPaid);
                    status = BillStatus.PARTIALLY_PAID;
                }
                case OVERDUE -> {
                    amountPaid = BigDecimal.ZERO;
                    remainingBalance = totalAmount;
                    status = BillStatus.OVERDUE;
                }
                default -> {
                    amountPaid = BigDecimal.ZERO;
                    remainingBalance = totalAmount;
                    status = BillStatus.PENDING;
                }
            }

            Bill bill = billRepo.save(Bill.builder()
                    .billNumber("BILL-" + account.getAccountNo() + "-" + (month + 1) + "-" + year)
                    .account(account)
                    .citizenId(account.getCitizenId())
                    .meterReadingId(UUID.randomUUID())
                    .billingPeriodStart(periodStart)
                    .billingPeriodEnd(periodEnd)
                    .totalAmount(totalAmount)
                    .amountPaid(amountPaid)
                    .remainingBalance(remainingBalance)
                    .lateFee(lateFee.compareTo(BigDecimal.ZERO) > 0 ? lateFee : null)
                    .dueDate(dueDate)
                    .status(status)
                    .build());

            if (seed == BillSeed.PAID) {
                createTransaction(account, bill, totalAmount);
            }
        }
    }

    private void createTransaction(ServiceAccount account, Bill bill, BigDecimal amount) {
        String txnId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        txnRepo.save(Transaction.builder()
                .billId(bill.getId())
                .accountId(account.getId())
                .transactionId(txnId)
                .razorpayOrderId("order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .razorpayPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .amount(amount)
                .status("success")
                .paymentMethod("mock_upi")
                .receiptData("{\"receiptId\":\"rcpt_" + txnId.substring(0, 6).toLowerCase()
                        + "\",\"transactionId\":\"" + txnId
                        + "\",\"accountId\":\"" + account.getId()
                        + "\",\"consumerId\":\"" + account.getAccountNo()
                        + "\",\"utilityType\":\"" + account.getServiceType().name().toLowerCase()
                        + "\",\"amount\":" + amount
                        + ",\"paymentMethod\":\"mock_upi\"}")
                .build());
    }

    private enum BillSeed { PAID, UNPAID, OVERDUE, PARTIAL }
}
