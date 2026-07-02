import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { connectionsApi } from '@/lib/api/connections';
import { queryKeys } from '@/lib/query-keys';
import { STALE_TIMES } from '@/lib/constants';
import type { NewConnectionRequest } from '@/types/connections';

export function useConnections(page?: number) {
  return useQuery({
    queryKey: [...queryKeys.connections.list(), page],
    queryFn: () => connectionsApi.getRequests(page),
    staleTime: STALE_TIMES.CONNECTIONS,
  });
}

export function useConnectionDetail(requestId: string) {
  return useQuery({
    queryKey: queryKeys.connections.detail(requestId),
    queryFn: () => connectionsApi.getRequestById(requestId),
    enabled: !!requestId,
    staleTime: STALE_TIMES.CONNECTIONS,
  });
}

export function useCreateConnectionRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: NewConnectionRequest) =>
      connectionsApi.createRequest(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.connections.list(),
      });
    },
  });
}
