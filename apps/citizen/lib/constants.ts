export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export const OTP_LENGTH = 6;
export const OTP_RESEND_SECONDS = 30;

export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  MAX_PAGE_SIZE: 100,
} as const;

export const STALE_TIMES = {
  PROFILE: 5 * 60 * 1000,
  ACCOUNTS: 5 * 60 * 1000,
  BILLS: 2 * 60 * 1000,
  GRIEVANCES: 30 * 1000,
  CONNECTIONS: 30 * 1000,
  NOTIFICATIONS: 60 * 1000,
} as const;

export const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'hi', label: 'हिन्दी' },
  { code: 'te', label: 'తెలుగు' },
  { code: 'ta', label: 'தமிழ்' },
] as const;

export const ROLES = {
  USER: 'USER',
  EMPLOYEE: 'EMPLOYEE',
  ADMIN: 'ADMIN',
} as const;
