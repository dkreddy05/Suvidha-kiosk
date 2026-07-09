import apiClient from './client';
import type { PaginatedResponse } from '@/types/api';

export interface Notification {
  id: string;
  citizenId: string;
  mobile: string;
  messageType: string;
  status: 'SENT' | 'FAILED' | 'PENDING';
  sentAt: string;
  messageContent?: string;
}

export const notificationsApi = {
  getHistory: async (
    citizenId: string,
    page = 0,
    size = 10
  ): Promise<PaginatedResponse<Notification>> => {
    const response = await apiClient.get(`/notifications/history/${citizenId}`, {
      params: { page, size },
    });
    const data = response.data;
    const notifications = (data.notifications || []).map((n: any) => ({
      id: n.notification_id,
      citizenId,
      mobile: n.phone_number,
      messageType: n.message_type,
      status: n.status as any,
      sentAt: n.sent_at || n.created_at,
      messageContent: n.message_content,
    }));
    return {
      content: notifications,
      totalElements: data.totalElements || notifications.length,
      totalPages: data.totalPages || 1,
      size,
      number: data.currentPage || page,
      first: (data.currentPage || page) === 0,
      last: (data.currentPage || page) >= (data.totalPages || 1) - 1,
    };
  },
};
