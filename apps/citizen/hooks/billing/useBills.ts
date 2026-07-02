import { useQuery } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/billing';
import { queryKeys } from '@/lib/query-keys';
import { STALE_TIMES } from '@/lib/constants';

export function useBills(accountId: string, page?: number) {
  return useQuery({
    queryKey: [...queryKeys.billing.bills(accountId), page],
    queryFn: () => billingApi.getBills(accountId, page),
    staleTime: STALE_TIMES.BILLS,
    enabled: !!accountId,
  });
}

export function useBillById(accountId: string, billId: string) {
  return useQuery({
    queryKey: [...queryKeys.billing.bills(accountId), billId],
    queryFn: () => billingApi.getBillById(accountId, billId),
    enabled: !!accountId && !!billId,
  });
}
