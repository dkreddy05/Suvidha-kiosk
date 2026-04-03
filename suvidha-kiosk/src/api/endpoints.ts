export const endpoints = {
  auth: {
    sendOtp: '/auth/send-otp',
    verifyOtp: '/auth/verify-otp',
    refreshToken: '/auth/refresh-token',
    profile: '/auth/profile',
    logout: '/auth/logout',
  },
  billing: {
    accounts: '/billing/accounts',
    bills: (accountId: string) => `/billing/bills/${accountId}`,
    bill: (billId: string) => `/billing/bill/${billId}`,
    pay: '/billing/pay',
    confirm: '/billing/pay/confirm',
    receipt: (paymentId: string) => `/billing/receipt/${paymentId}`,
    linkAccount: '/billing/accounts/link',
  },
  grievance: {
    submit: '/grievance/submit',
    track: (referenceNumber: string) => `/grievance/track/${referenceNumber}`,
    mine: '/grievance/my-grievances',
    categories: '/grievance/categories',
  },
  connections: {
    request: '/connections/request',
    mine: '/connections/my-requests',
  },
  admin: {
    summary: '/admin/analytics/summary',
    grievances: '/admin/grievances',
    updateGrievanceStatus: (id: string) => `/admin/grievances/${id}/status`,
  },
} as const
