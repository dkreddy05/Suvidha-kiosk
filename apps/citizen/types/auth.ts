export type Role = 'USER' | 'EMPLOYEE' | 'ADMIN';
export type Language = 'en' | 'hi' | 'te' | 'ta';

export interface CitizenDTO {
  id: string;
  mobile: string;
  name?: string;
  aadhaarLast4?: string;
  languagePref: Language;
  role: Role;
  createdAt: string;
  consumerId?: string;
}

export interface SendOtpRequest {
  mobile: string;
}

export interface SendOtpResponse {
  sessionId: string;
  message: string;
}

export interface VerifyOtpRequest {
  sessionId: string;
  otp: string;
}

export interface VerifyOtpResponse {
  accessToken: string;
  refreshToken: string;
  citizen: CitizenDTO;
  isNewUser: boolean;
}

export interface RegisterRequest {
  sessionId: string;
  mobile: string;
  name: string;
  aadhar?: string;
  languagePreference?: Language;
  role?: Role;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface ConsentRecord {
  id: string;
  citizenId: string;
  consentType: string;
  grantedAt: string;
  expiresAt: string;
  ip: string;
  userAgent: string;
}

export interface CitizenDataResponse {
  citizen: CitizenDTO;
  consents: ConsentRecord[];
}

export interface ProfileUpdateRequest {
  name?: string;
  languagePreference?: string;
}
