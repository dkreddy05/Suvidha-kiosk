import apiClient from './client';
import type { Grievance, GrievanceDetail, GrievanceCategory, GrievanceSubmitRequest, GrievanceStatus } from '@/types/grievances';
import type { PaginatedResponse } from '@/types/api';

export const grievancesApi = {
  getGrievances: async (
    page = 0,
    size = 10,
    status?: GrievanceStatus
  ): Promise<PaginatedResponse<Grievance>> => {
    const response = await apiClient.get('/grievances', {
      params: { page, size, ...(status && { status }) },
    });
    const data = response.data;
    const grievances = (data.grievances || []).map((g: any) => ({
      id: g.referenceNumber,
      citizenId: '',
      title: g.category + ' Issue',
      description: '',
      category: g.category,
      status: g.status as GrievanceStatus,
      referenceNumber: g.referenceNumber,
      createdAt: g.submittedAt,
      updatedAt: g.submittedAt,
    }));
    return {
      content: grievances,
      totalElements: data.totalCount || grievances.length,
      totalPages: Math.ceil((data.totalCount || grievances.length) / size),
      size,
      number: data.page || page,
      first: (data.page || page) === 0,
      last: (data.page || page) >= Math.ceil((data.totalCount || grievances.length) / size) - 1,
    };
  },

  getGrievanceByRef: async (referenceNumber: string): Promise<GrievanceDetail> => {
    const response = await apiClient.get(`/grievances/${referenceNumber}`);
    const g = response.data;
    const updates = (g.statusHistory || []).map((h: any) => ({
      grievanceId: g.referenceNumber,
      message: h.notes || 'Status changed to ' + h.status,
      status: h.status as GrievanceStatus,
      timestamp: h.timestamp,
    }));
    return {
      id: g.referenceNumber,
      citizenId: '',
      referenceNumber: g.referenceNumber,
      category: g.category,
      description: g.description,
      photoUrl: g.photoUrl,
      status: g.status as GrievanceStatus,
      submittedAt: g.submittedAt,
      updatedAt: g.updatedAt || g.submittedAt,
      updates,
    };
  },

  submitGrievance: async (data: GrievanceSubmitRequest): Promise<Grievance> => {
    const response = await apiClient.post('/grievances', data);
    const g = response.data;
    return {
      id: g.referenceNumber,
      citizenId: '',
      referenceNumber: g.referenceNumber,
      category: g.category || data.category,
      description: g.description || data.description,
      photoUrl: g.photoUrl || data.photoUrl || '',
      status: g.status as GrievanceStatus,
      submittedAt: g.submittedAt || new Date().toISOString(),
      updatedAt: g.submittedAt || new Date().toISOString(),
    };
  },

  getCategories: async (): Promise<GrievanceCategory[]> => {
    const response = await apiClient.get('/grievances/categories');
    const data = response.data;
    const catList = data && typeof data === 'object' && 'categories' in data && Array.isArray(data.categories)
      ? data.categories
      : Array.isArray(data) ? data : [];

    return catList.map((catName: string) => {
      const formattedName = catName
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
      return {
        id: catName,
        name: formattedName,
      };
    });
  },
};
