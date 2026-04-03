import { useMutation, useQuery } from '@tanstack/react-query'
import { axiosClient } from '../api/axiosClient'
import { endpoints } from '../api/endpoints'
import type { GrievanceDTO, GrievanceStatus, PageDTO, UtilityType } from '../api/types'

export function useSubmitGrievance() {
  return useMutation({
    mutationFn: async (body: {
      utilityType: UtilityType
      category: string
      description: string
      attachments: { filename: string; base64: string }[]
    }) => {
      const res = await axiosClient.post<GrievanceDTO>(endpoints.grievance.submit, body)
      return res.data
    },
  })
}

export function useTrackGrievance(referenceNumber: string | null) {
  return useQuery({
    queryKey: ['grievance', 'track', referenceNumber],
    enabled: Boolean(referenceNumber),
    queryFn: async () => {
      const res = await axiosClient.get<GrievanceDTO>(endpoints.grievance.track(referenceNumber!))
      return res.data
    },
  })
}

export function useMyGrievances(params: {
  status?: GrievanceStatus
  utilityType?: UtilityType
  page: number
  pageSize: number
}) {
  return useQuery({
    queryKey: ['grievance', 'mine', params],
    queryFn: async () => {
      const res = await axiosClient.get<PageDTO<GrievanceDTO>>(endpoints.grievance.mine, {
        params,
      })
      return res.data
    },
  })
}

export function useGrievanceCategories() {
  return useQuery({
    queryKey: ['grievance', 'categories'],
    queryFn: async () => {
      const res = await axiosClient.get<{ ELECTRICITY: string[]; GAS: string[]; WATER: string[] }>(
        endpoints.grievance.categories,
      )
      return res.data
    },
  })
}
