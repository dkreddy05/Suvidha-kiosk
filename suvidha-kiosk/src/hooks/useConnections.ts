import { useMutation, useQuery } from '@tanstack/react-query'
import { axiosClient } from '../api/axiosClient'
import { endpoints } from '../api/endpoints'
import type { ConnectionRequestDTO, PageDTO, RequestType, UtilityType } from '../api/types'

export function useRequestConnection() {
  return useMutation({
    mutationFn: async (body: {
      utilityType: UtilityType
      requestType: RequestType
      address: string
      documents: { type: 'ID_PROOF'; base64: string }[]
    }) => {
      const res = await axiosClient.post<ConnectionRequestDTO>(endpoints.connections.request, body)
      return res.data
    },
  })
}

export function useMyConnectionRequests(params: { page: number; pageSize: number }) {
  return useQuery({
    queryKey: ['connections', 'mine', params],
    queryFn: async () => {
      const res = await axiosClient.get<PageDTO<ConnectionRequestDTO>>(endpoints.connections.mine, { params })
      return res.data
    },
  })
}
