import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { axiosClient } from '../api/axiosClient'
import { endpoints } from '../api/endpoints'
import type { GrievanceDTO, PageDTO, UtilityType, GrievanceStatus } from '../api/types'

export function useAnalyticsSummary() {
  return useQuery({
    queryKey: ['admin', 'analytics', 'summary'],
    queryFn: async () => {
      const res = await axiosClient.get<{
        citizensToday: number
        billsPaidToday: number
        grievancesResolvedToday: number
        activeSessions: number
      }>(endpoints.admin.summary)
      return res.data
    },
    refetchInterval: 30000,
  })
}

export function useAdminGrievances(params: {
  status?: GrievanceStatus
  utilityType?: UtilityType
  page: number
  pageSize: number
}) {
  return useQuery({
    queryKey: ['admin', 'grievances', params],
    queryFn: async () => {
      const res = await axiosClient.get<PageDTO<GrievanceDTO>>(endpoints.admin.grievances, { params })
      return res.data
    },
    refetchInterval: 30000,
  })
}

export function useUpdateGrievanceStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (body: { id: string; status: GrievanceStatus; note: string }) => {
      const res = await axiosClient.put<GrievanceDTO>(endpoints.admin.updateGrievanceStatus(body.id), {
        status: body.status,
        note: body.note,
      })
      return res.data
    },
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: ['admin', 'grievances'] })
    },
  })
}
