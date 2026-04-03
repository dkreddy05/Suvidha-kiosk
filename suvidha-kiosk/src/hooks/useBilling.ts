import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { v4 as uuidv4 } from 'uuid'
import { axiosClient } from '../api/axiosClient'
import { endpoints } from '../api/endpoints'
import type {
  BillDTO,
  PaymentConfirmDTO,
  PaymentMode,
  PaymentOrderDTO,
  UtilityAccountDTO,
} from '../api/types'

export function useAccounts(mobile: string | null) {
  return useQuery({
    queryKey: ['billing', 'accounts', mobile],
    enabled: Boolean(mobile),
    queryFn: async () => {
      const res = await axiosClient.get<UtilityAccountDTO[]>(endpoints.billing.accounts, {
        params: { mobile },
      })
      return res.data
    },
  })
}

export function useBills(accountId: string | null) {
  return useQuery({
    queryKey: ['billing', 'bills', accountId],
    enabled: Boolean(accountId),
    queryFn: async () => {
      const res = await axiosClient.get<BillDTO[]>(endpoints.billing.bills(accountId!))
      return res.data
    },
  })
}

export function useBill(billId: string | null) {
  return useQuery({
    queryKey: ['billing', 'bill', billId],
    enabled: Boolean(billId),
    queryFn: async () => {
      const res = await axiosClient.get<BillDTO>(endpoints.billing.bill(billId!))
      return res.data
    },
  })
}

export type PayBillResponse = PaymentOrderDTO | { status: 'PAID' }

export function usePayBill() {
  return useMutation({
    mutationFn: async (body: { billIds: string[]; paymentMode: PaymentMode }) => {
      const res = await axiosClient.post<PayBillResponse>(endpoints.billing.pay, body)
      return res.data
    },
  })
}

export function useConfirmPayment() {
  return useMutation({
    mutationFn: async (body: { orderId: string; paymentId: string; signature: string; idempotencyKey?: string }) => {
      const idempotencyKey = body.idempotencyKey ?? uuidv4()
      const res = await axiosClient.post<PaymentConfirmDTO>(
        endpoints.billing.confirm,
        { orderId: body.orderId, paymentId: body.paymentId, signature: body.signature },
        { headers: { 'Idempotency-Key': idempotencyKey } },
      )
      return res.data
    },
  })
}

export function useReceipt(paymentId: string | null) {
  return useQuery({
    queryKey: ['billing', 'receipt', paymentId],
    enabled: Boolean(paymentId),
    queryFn: async () => {
      const res = await axiosClient.get<{ receiptHtml: string; receiptJson: object }>(
        endpoints.billing.receipt(paymentId!),
      )
      return res.data
    },
  })
}

export function useLinkAccount() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (body: { accountNumber: string; utilityType: string; providerName: string; address: string }) => {
      const res = await axiosClient.post<UtilityAccountDTO>(endpoints.billing.linkAccount, body)
      return res.data
    },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['billing', 'accounts'] })
    },
  })
}
