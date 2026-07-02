import apiClient from './client';
import type {
  SendOtpRequest,
  SendOtpResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
  RegisterRequest,
  RefreshTokenResponse,
  ProfileUpdateRequest,
  CitizenDTO,
  CitizenDataResponse,
  ConsentRecord,
} from '@/types/auth';
import type { PaginatedResponse, AuditLogEntry } from '@/types/api';

export const authApi = {
  sendOtp: async (data: SendOtpRequest): Promise<SendOtpResponse> => {
    const response = await apiClient.post('/auth/send-otp', data);
    return response.data;
  },

  verifyOtp: async (data: VerifyOtpRequest): Promise<VerifyOtpResponse> => {
    const response = await apiClient.post('/auth/verify-otp', data);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<VerifyOtpResponse> => {
    const response = await apiClient.post('/auth/register', data);
    return response.data;
  },

  refreshToken: async (refreshToken: string): Promise<RefreshTokenResponse> => {
    const response = await apiClient.post('/auth/refresh-token', { refreshToken });
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  getProfile: async (): Promise<CitizenDTO> => {
    const response = await apiClient.get('/auth/profile');
    return response.data;
  },

  getPublicKey: async (): Promise<unknown> => {
    const response = await apiClient.get('/auth/public-key');
    return response.data;
  },
};

export const citizenApi = {
  getData: async (): Promise<CitizenDataResponse> => {
    const response = await apiClient.get('/citizen/data');
    return response.data;
  },

  exportData: async (): Promise<Blob> => {
    const response = await apiClient.get('/citizen/export', { responseType: 'blob' });
    return response.data;
  },

  updateProfile: async (data: ProfileUpdateRequest): Promise<CitizenDTO> => {
    const response = await apiClient.put('/citizen/profile', data);
    return response.data;
  },

  deleteAccount: async (): Promise<void> => {
    await apiClient.delete('/citizen/account');
  },

  recordConsent: async (consentType: string): Promise<ConsentRecord> => {
    const response = await apiClient.post('/citizen/consent', { consentType });
    return response.data;
  },

  getAuditLogs: async (page = 0, size = 10): Promise<PaginatedResponse<AuditLogEntry>> => {
    const response = await apiClient.get('/citizen/audit-logs', { params: { page, size } });
    return response.data;
  },
};
