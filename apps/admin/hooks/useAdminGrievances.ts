import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/client';

export function useAdminGrievances(page = 0, size = 10, status?: string) {
  return useQuery({
    queryKey: ['admin', 'grievances', { page, size, status }],
    queryFn: () => adminApi.getAllGrievances(page, size, status),
  });
}

export function useUpdateGrievanceStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      refNumber,
      status,
      message,
    }: {
      refNumber: string;
      status: string;
      message: string;
    }) => adminApi.updateGrievanceStatus(refNumber, { status, message }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'grievances'] });
      queryClient.invalidateQueries({
        queryKey: ['admin', 'grievances', variables.refNumber],
      });
      // Also invalidate general metrics since status changed
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'metrics', 'grievances'] });
    },
  });
}
