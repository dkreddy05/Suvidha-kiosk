import apiClient from './client';
import type { UtilityAccount, Bill, Payment, PaymentRequest, PaymentConfirmRequest, Provider, ServiceType, BillStatus, PaymentMethod, PaymentStatus } from '@/types/billing';
import type { PaginatedResponse } from '@/types/api';

export const billingApi = {
  getAccounts: async (): Promise<UtilityAccount[]> => {
    const response = await apiClient.get('/billing/accounts');
    const data = response.data;
    if (data && typeof data === 'object' && 'accounts' in data && Array.isArray(data.accounts)) {
      return data.accounts.map((acc: any) => ({
        id: acc.account_id,
        citizenId: '',
        accountNumber: acc.consumer_id,
        serviceType: (acc.utility_type || '').toUpperCase() as ServiceType,
        providerName: acc.utility_type === 'electricity' ? 'Electricity Board' : acc.utility_type === 'water' ? 'Water Authority' : 'Gas Distribution',
        status: (acc.account_status || 'active').toUpperCase() as any,
      }));
    }
    return Array.isArray(data) ? data : [];
  },

  getBills: async (accountId: string, page = 0, size = 10): Promise<PaginatedResponse<Bill>> => {
    const response = await apiClient.get(`/billing/accounts/${accountId}/bills`, {
      params: { page, size },
    });
    const bills = (response.data.bills || []).map((b: any) => ({
      id: b.bill_id,
      accountId,
      billNumber: b.bill_id.slice(0, 8).toUpperCase(),
      billMonth: b.bill_period ? b.bill_period.slice(0, 7) : '',
      amount: b.amount_due,
      dueDate: b.due_date,
      status: b.bill_status === 'paid' ? 'PAID' : b.bill_status === 'overdue' ? 'OVERDUE' : 'PENDING',
    }));
    return {
      content: bills,
      totalElements: bills.length,
      totalPages: 1,
      size: 10,
      number: page,
      first: true,
      last: true,
    };
  },

  getBillById: async (accountId: string, billId: string): Promise<Bill> => {
    const response = await apiClient.get(`/billing/accounts/${accountId}/bills/${billId}`);
    const b = response.data;
    return {
      id: b.bill_id,
      accountId,
      billNumber: b.bill_id.slice(0, 8).toUpperCase(),
      billMonth: b.bill_period ? b.bill_period.slice(0, 7) : '',
      amount: b.amount_due,
      dueDate: b.due_date,
      status: b.bill_status === 'paid' ? 'PAID' : b.bill_status === 'overdue' ? 'OVERDUE' : 'PENDING',
    };
  },

  getPayments: async (accountId: string, page = 0, size = 10): Promise<PaginatedResponse<Payment>> => {
    const response = await apiClient.get(`/billing/accounts/${accountId}/payments`, {
      params: { limit: size, offset: page * size },
    });
    const payments = (response.data.payments || []).map((p: any) => ({
      transactionId: p.transaction_id,
      accountId,
      amount: p.amount,
      method: p.payment_method as PaymentMethod,
      status: p.payment_status as PaymentStatus,
      createdAt: p.created_at,
      billIds: [],
      receiptUrl: `/api/billing/accounts/${accountId}/payments/${p.transaction_id}/receipt`,
    }));
    return {
      content: payments,
      totalElements: payments.length,
      totalPages: 1,
      size,
      number: page,
      first: true,
      last: true,
    };
  },

  processPayment: async (accountId: string, data: PaymentRequest): Promise<Payment> => {
    const body = {
      bill_id: data.billIds[0],
      amount: data.amount,
      payment_method: data.method,
    };
    const response = await apiClient.post(`/billing/accounts/${accountId}/payments`, body, {
      headers: { 'Idempotency-Key': data.idempotencyKey },
    });
    const p = response.data;
    return {
      transactionId: p.transaction_id,
      accountId,
      amount: p.amount,
      method: p.payment_method as PaymentMethod,
      status: p.payment_status as PaymentStatus,
      createdAt: p.created_at,
      billIds: data.billIds,
      receiptUrl: p.receipt_url,
    };
  },

  confirmPayment: async (data: PaymentConfirmRequest): Promise<Payment> => {
    const response = await apiClient.post('/billing/pay/confirm', data, {
      headers: { 'Idempotency-Key': data.idempotencyKey },
    });
    return response.data;
  },

  getReceipt: async (paymentId: string): Promise<Blob> => {
    const response = await apiClient.get(`/billing/receipt/${paymentId}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  getProviders: async (): Promise<Provider[]> => {
    const response = await apiClient.get('/billing/accounts/providers');
    return response.data;
  },

  linkAccount: async (data: { accountNumber: string; provider: string }): Promise<any> => {
    const body = {
      utility_type: data.provider,
      consumer_id: data.accountNumber,
    };
    const response = await apiClient.post('/billing/accounts', body);
    return response.data;
  },
};
