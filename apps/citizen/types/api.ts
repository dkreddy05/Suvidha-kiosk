export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
  requestId?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details?: Record<string, string[]>;
  timestamp: string;
  requestId: string;
  path?: string;
}

export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface AuditLogEntry {
  id: string;
  citizenId: string;
  action: string;
  details: string;
  ip: string;
  createdAt: string;
}
