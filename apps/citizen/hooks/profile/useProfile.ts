import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { citizenApi } from '@/lib/api/auth';
import { useAuthStore } from '@/store/auth.store';
import { queryKeys } from '@/lib/query-keys';

export function useCitizenData() {
  return useQuery({
    queryKey: queryKeys.citizen.data(),
    queryFn: () => citizenApi.getData(),
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { name?: string; languagePreference?: string }) =>
      citizenApi.updateProfile(data),
    onSuccess: (citizen) => {
      useAuthStore.getState().setCitizen(citizen);
      queryClient.invalidateQueries({
        queryKey: queryKeys.auth.profile(),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.citizen.data(),
      });
    },
  });
}

export function useExportData() {
  return useQuery({
    queryKey: ['citizen', 'export'],
    queryFn: () => citizenApi.exportData(),
    enabled: false,
  });
}

export function useDeleteAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => citizenApi.deleteAccount(),
    onSuccess: () => {
      useAuthStore.getState().logout();
      queryClient.clear();
    },
  });
}

export function useAuditLogs(page?: number) {
  return useQuery({
    queryKey: queryKeys.citizen.auditLog(page ?? 0),
    queryFn: () => citizenApi.getAuditLogs(page),
  });
}
