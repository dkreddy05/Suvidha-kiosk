import apiClient from './client';
import type { ConnectionRequest, ConnectionDetail, NewConnectionRequest, ConnectionStatus } from '@/types/connections';
import type { PaginatedResponse } from '@/types/api';
import type { ServiceType } from '@/types/billing';

export const connectionsApi = {
  getRequests: async (page = 0, size = 10): Promise<PaginatedResponse<ConnectionRequest>> => {
    const response = await apiClient.get('/connections/requests', {
      params: { page, size },
    });
    const rawList = Array.isArray(response.data) ? response.data : [];
    const mapped = rawList.map((item: any) => ({
      id: item.requestId,
      citizenId: '',
      serviceType: item.serviceType as ServiceType,
      address: item.address,
      documents: [],
      status: item.status === 'SUBMITTED' ? 'PENDING' : (item.status || 'PENDING') as ConnectionStatus,
      referenceNumber: item.requestId,
      createdAt: item.submittedAt,
      updatedAt: item.submittedAt,
    }));
    return {
      content: mapped,
      totalElements: mapped.length,
      totalPages: 1,
      size: 10,
      number: page,
      first: true,
      last: true,
    };
  },

  getRequestById: async (requestId: string): Promise<ConnectionDetail> => {
    const response = await apiClient.get(`/connections/requests/${requestId}`);
    const item = response.data;
    const timeline = (item.timeline || []).map((t: any) => ({
      status: t.status === 'SUBMITTED' ? 'PENDING' : (t.status || 'PENDING') as ConnectionStatus,
      message: t.message || 'Status updated',
      timestamp: t.updatedAt,
    }));
    return {
      id: item.requestId,
      citizenId: '',
      serviceType: (item.serviceType || 'ELECTRICITY') as ServiceType,
      address: item.address || 'Address not available',
      documents: [],
      status: item.status === 'SUBMITTED' ? 'PENDING' : (item.status || 'PENDING') as ConnectionStatus,
      referenceNumber: item.requestId,
      createdAt: timeline[0]?.timestamp || new Date().toISOString(),
      updatedAt: timeline[timeline.length - 1]?.timestamp || new Date().toISOString(),
      timeline,
    };
  },

  createRequest: async (data: NewConnectionRequest): Promise<ConnectionRequest> => {
    const response = await apiClient.post('/connections/requests', data);
    const item = response.data;
    return {
      id: item.requestId,
      citizenId: '',
      serviceType: data.serviceType,
      address: data.address,
      documents: data.documents || [],
      status: 'PENDING',
      referenceNumber: item.requestId,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  },

  linkAccount: async (data: { accountNumber: string; provider: string }): Promise<any> => {
    const body = {
      accountNumber: data.accountNumber,
      utilityType: data.provider,
    };
    const response = await apiClient.post('/connections/accounts/link', body);
    return response.data;
  },
};
