import { useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/billing';
import { queryKeys } from '@/lib/query-keys';
import { generateIdempotencyKey } from '@/lib/utils';
import type { PaymentRequest, PaymentMethod } from '@/types/billing';

export function useProcessPayment(accountId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PaymentRequest) =>
      billingApi.processPayment(accountId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.billing.bills(accountId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.billing.accounts(),
      });
    },
  });
}

export function useConfirmPayment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: {
      razorpayOrderId: string;
      razorpayPaymentId: string;
      razorpaySignature: string;
      idempotencyKey: string;
    }) => billingApi.confirmPayment(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.billing.accounts(),
      });
    },
  });
}

export function usePayBill(accountId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: {
      billIds: string[];
      amount: number;
      method: PaymentMethod;
    }) => {
      const idempotencyKey = generateIdempotencyKey();
      return billingApi.processPayment(accountId, {
        ...data,
        idempotencyKey,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.billing.bills(accountId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.billing.accounts(),
      });
    },
  });
}
