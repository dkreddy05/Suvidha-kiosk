package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.ConfirmPaymentRequest;
import com.suvidha.billing.dto.request.LinkAccountRequest;
import com.suvidha.billing.dto.request.PayBillRequest;
import com.suvidha.billing.dto.response.*;

import java.util.List;

/**
 * Facade service that bridges ServiceAccount, Bill, and Transaction
 * repositories into the exact DTO shapes the kiosk frontend expects.
 */
public interface BillingFacadeService {

    /** GET /api/v1/billing/accounts?mobile=... */
    List<UtilityAccountDTO> getAccountsForMobile(String mobile);

    /** GET /api/v1/billing/bills/{accountId} */
    List<BillDTO> getBillsForAccount(String accountId, String citizenId);

    /** GET /api/v1/billing/bill/{billId} */
    BillDTO getBillById(String billId, String citizenId);

    /**
     * POST /api/v1/billing/pay
     * Returns PaymentOrderDTO for UPI/CARD, or a simple status map for CASH.
     */
    Object initiatePay(PayBillRequest req, String citizenId);

    /** POST /api/v1/billing/pay/confirm */
    PaymentConfirmDTO confirmPayment(ConfirmPaymentRequest req, String idempotencyKey, String citizenId);

    /** GET /api/v1/billing/receipt/{paymentId} */
    ReceiptDTO getReceipt(String paymentId, String citizenId);

    /** POST /api/v1/billing/accounts/link */
    UtilityAccountDTO linkAccount(LinkAccountRequest req, String citizenId, String mobile);
}
