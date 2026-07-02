import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/client';

export function useAdminConnections(page = 0, size = 10, status?: string) {
  return useQuery({
    queryKey: ['admin', 'connections', { page, size, status }],
    queryFn: () => adminApi.getAllConnections(page, size, status),
  });
}

export function useApproveConnection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.approveConnection(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'connections'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'connections', id] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });
}

export function useRejectConnection() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      adminApi.rejectConnection(id, reason),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'connections'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'connections', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
    },
  });
}
