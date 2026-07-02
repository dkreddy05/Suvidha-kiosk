import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/lib/api/auth';
import { useAuthStore } from '@/store/auth.store';
import { queryKeys } from '@/lib/query-keys';
import { STALE_TIMES } from '@/lib/constants';

export function useProfile() {
  return useQuery({
    queryKey: queryKeys.auth.profile(),
    queryFn: () => authApi.getProfile(),
    staleTime: STALE_TIMES.PROFILE,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { sessionId: string; otp: string }) =>
      authApi.verifyOtp(data),
    onSuccess: async (response) => {
      await useAuthStore.getState().login(
        response.accessToken,
        response.refreshToken,
        response.citizen,
        response.isNewUser,
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.profile() });
    },
  });
}

export function useRegister() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof authApi.register>[0]) =>
      authApi.register(data),
    onSuccess: async (response) => {
      await useAuthStore.getState().login(
        response.accessToken,
        response.refreshToken,
        response.citizen,
        response.isNewUser,
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.auth.profile() });
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: async () => {
      await useAuthStore.getState().logout();
      queryClient.clear();
    },
  });
}
