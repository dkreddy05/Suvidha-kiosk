import { useQuery } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/billing';
import { queryKeys } from '@/lib/query-keys';

export function useReceipt(paymentId: string) {
  return useQuery({
    queryKey: queryKeys.billing.receipt(paymentId),
    queryFn: () => billingApi.getReceipt(paymentId),
    enabled: !!paymentId,
  });
}
