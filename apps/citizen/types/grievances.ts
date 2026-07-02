export type GrievanceStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED' | 'CLOSED';

export interface GrievanceCategory {
  id: string;
  name: string;
  description?: string;
}

export interface Grievance {
  id: string;
  citizenId: string;
  referenceNumber: string;
  category: string;
  description: string;
  photoUrl?: string;
  status: GrievanceStatus;
  submittedAt: string;
  updatedAt: string;
}

export interface GrievanceUpdate {
  grievanceId: string;
  message: string;
  status: GrievanceStatus;
  timestamp: string;
  updatedBy?: string;
}

export interface GrievanceDetail extends Grievance {
  updates: GrievanceUpdate[];
}

export interface GrievanceSubmitRequest {
  category: string;
  description: string;
  photoUrl?: string;
}

export const GRIEVANCE_STATUS_CONFIG: Record<GrievanceStatus, { label: string; variant: string }> = {
  OPEN: { label: 'Open', variant: 'open' },
  IN_PROGRESS: { label: 'In Progress', variant: 'in_progress' },
  RESOLVED: { label: 'Resolved', variant: 'resolved' },
  REJECTED: { label: 'Rejected', variant: 'rejected' },
  CLOSED: { label: 'Closed', variant: 'default' },
};
