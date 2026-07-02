export const queryKeys = {
  auth: {
    profile: () => ['auth', 'profile'] as const,
  },
  billing: {
    accounts: () => ['billing', 'accounts'] as const,
    bills: (accountId: string) => ['billing', 'bills', accountId] as const,
    payments: (accountId: string) => ['billing', 'payments', accountId] as const,
    receipt: (paymentId: string) => ['billing', 'receipt', paymentId] as const,
    providers: () => ['billing', 'providers'] as const,
  },
  grievances: {
    list: (filters?: Record<string, unknown>) => ['grievances', 'list', filters] as const,
    detail: (ref: string) => ['grievances', 'detail', ref] as const,
    categories: () => ['grievances', 'categories'] as const,
  },
  connections: {
    list: () => ['connections', 'list'] as const,
    detail: (id: string) => ['connections', 'detail', id] as const,
  },
  citizen: {
    data: () => ['citizen', 'data'] as const,
    auditLog: (page: number) => ['citizen', 'audit-log', page] as const,
  },
  notifications: {
    history: (citizenId: string) => ['notifications', 'history', citizenId] as const,
  },
} as const;
