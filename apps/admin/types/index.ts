export interface DashboardMetrics {
  totalUsers: number;
  activeSessions: number;
  totalGrievances: number;
  openGrievances: number;
  revenue: number;
  paymentSuccessRate: number;
}

export interface UserMetric {
  date: string;
  count: number;
}

export interface GrievanceMetric {
  period: string;
  open: number;
  inProgress: number;
  resolved: number;
}

export interface PaymentMetric {
  date: string;
  volume: number;
  successRate: number;
  revenue: number;
}

export interface AdminGrievance {
  id: string;
  referenceNumber: string;
  citizenId: string;
  category: string;
  description: string;
  status: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED" | "CLOSED";
  submittedAt: string;
  citizenName?: string;
  citizenMobile?: string;
}

export interface AdminConnection {
  id: string;
  referenceNumber: string;
  citizenId: string;
  serviceType: string;
  address: string;
  status: "PENDING" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "COMPLETED";
  createdAt: string;
  citizenName?: string;
  citizenMobile?: string;
}

export interface StatusUpdateRequest {
  status: string;
  message: string;
}
