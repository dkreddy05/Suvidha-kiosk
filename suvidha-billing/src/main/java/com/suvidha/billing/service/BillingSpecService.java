package com.suvidha.billing.service;

import com.suvidha.billing.dto.request.SpecLinkAccountRequest;
import com.suvidha.billing.dto.request.SpecPaymentRequest;
import com.suvidha.billing.dto.response.*;

public interface BillingSpecService {

    SpecAccountResponse linkAccount(SpecLinkAccountRequest request, String citizenId, String mobile);

    AccountsListResponse getAccounts(String citizenId);

    SpecBillsResponse getBills(String accountId, String citizenId);

    SpecBillResponse getBillById(String accountId, String billId, String citizenId);

    SpecPaymentResponse processPayment(String accountId, SpecPaymentRequest request, String citizenId);

    PaymentHistoryResponse getPaymentHistory(String accountId, String citizenId, int limit, int offset);

    SpecPaymentResponse getReceipt(String accountId, String transactionId, String citizenId);
}
