import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { grievancesApi } from '@/lib/api/grievances';
import { queryKeys } from '@/lib/query-keys';
import { STALE_TIMES } from '@/lib/constants';
import type { GrievanceStatus, GrievanceSubmitRequest } from '@/types/grievances';

export function useGrievances(page?: number, status?: GrievanceStatus) {
  return useQuery({
    queryKey: queryKeys.grievances.list({ page, status }),
    queryFn: () => grievancesApi.getGrievances(page, undefined, status),
    staleTime: STALE_TIMES.GRIEVANCES,
  });
}

export function useGrievanceDetail(refNumber: string) {
  return useQuery({
    queryKey: queryKeys.grievances.detail(refNumber),
    queryFn: () => grievancesApi.getGrievanceByRef(refNumber),
    enabled: !!refNumber,
    staleTime: STALE_TIMES.GRIEVANCES,
  });
}

export function useSubmitGrievance() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: GrievanceSubmitRequest) =>
      grievancesApi.submitGrievance(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['grievances', 'list'] });
    },
  });
}

export function useGrievanceCategories() {
  return useQuery({
    queryKey: queryKeys.grievances.categories(),
    queryFn: () => grievancesApi.getCategories(),
    staleTime: STALE_TIMES.GRIEVANCES,
  });
}
