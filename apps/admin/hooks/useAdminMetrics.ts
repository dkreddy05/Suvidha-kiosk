import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/client';

export function useAdminDashboardMetrics() {
  return useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () => adminApi.getDashboard(),
    refetchInterval: 60_000, // refresh every minute
  });
}

export function useAdminUserMetrics(period = 'month') {
  return useQuery({
    queryKey: ['admin', 'metrics', 'users', period],
    queryFn: () => adminApi.getUserMetrics(period),
  });
}

export function useAdminGrievanceMetrics(period = 'month') {
  return useQuery({
    queryKey: ['admin', 'metrics', 'grievances', period],
    queryFn: () => adminApi.getGrievanceMetrics(period),
  });
}

export function useAdminPaymentMetrics(period = 'month') {
  return useQuery({
    queryKey: ['admin', 'metrics', 'payments', period],
    queryFn: () => adminApi.getPaymentMetrics(period),
  });
}
