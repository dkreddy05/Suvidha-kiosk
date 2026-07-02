import type { ServiceType } from './billing';

export type ConnectionStatus = 'PENDING' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED';

export interface ConnectionDocumentRequest {
  type: string;
  base64: string;
}

export interface ConnectionRequest {
  id: string;
  citizenId: string;
  serviceType: ServiceType;
  address: string;
  documents: any[];
  status: ConnectionStatus;
  referenceNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConnectionTimelineEntry {
  status: ConnectionStatus;
  message: string;
  timestamp: string;
  updatedBy?: string;
}

export interface ConnectionDetail extends ConnectionRequest {
  timeline: ConnectionTimelineEntry[];
  slaEndDate?: string;
}

export interface NewConnectionRequest {
  serviceType: ServiceType;
  address: string;
  documents?: ConnectionDocumentRequest[];
}

export const CONNECTION_STATUS_LABELS: Record<ConnectionStatus, string> = {
  PENDING: 'Pending',
  UNDER_REVIEW: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};
