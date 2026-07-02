import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '@/lib/api/billing';
import { queryKeys } from '@/lib/query-keys';
import { STALE_TIMES } from '@/lib/constants';

export function useAccounts() {
  return useQuery({
    queryKey: queryKeys.billing.accounts(),
    queryFn: () => billingApi.getAccounts(),
    staleTime: STALE_TIMES.ACCOUNTS,
  });
}

export function useLinkAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { accountNumber: string; provider: string }) =>
      billingApi.linkAccount(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.billing.accounts() });
    },
  });
}
