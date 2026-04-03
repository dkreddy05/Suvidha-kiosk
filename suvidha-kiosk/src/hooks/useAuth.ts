import { useMutation, useQuery } from '@tanstack/react-query'
import { axiosClient } from '../api/axiosClient'
import { endpoints } from '../api/endpoints'
import type { CitizenDTO, OtpSendResponseDTO, VerifyOtpResponseDTO } from '../api/types'
import { useAuthStore } from '../stores/useAuthStore'

export function useSendOtp() {
  return useMutation({
    mutationFn: async (body: { mobile: string }) => {
      const res = await axiosClient.post<OtpSendResponseDTO>(endpoints.auth.sendOtp, body)
      return res.data
    },
  })
}

export function useVerifyOtp() {
  const setAuth = useAuthStore((s) => s.setAuth)
  return useMutation({
    mutationFn: async (body: { sessionId: string; otp: string }) => {
      const res = await axiosClient.post<VerifyOtpResponseDTO>(endpoints.auth.verifyOtp, body)
      return res.data
    },
    onSuccess: (data) => {
      setAuth(data.accessToken, data.citizen)
    },
  })
}

export function useProfile() {
  return useQuery({
    queryKey: ['auth', 'profile'],
    queryFn: async () => {
      const res = await axiosClient.get<CitizenDTO>(endpoints.auth.profile)
      return res.data
    },
  })
}

export function useLogout() {
  const clearAuth = useAuthStore((s) => s.clearAuth)
  return useMutation({
    mutationFn: async () => {
      await axiosClient.post(endpoints.auth.logout)
    },
    onSettled: () => {
      clearAuth()
    },
  })
}
