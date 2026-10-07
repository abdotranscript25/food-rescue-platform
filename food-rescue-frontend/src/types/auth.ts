export type Role = 'CONSUMER' | 'MERCHANT' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
  role: Role;
  address?: string;
  city?: string;
}

export interface AuthResponse {
  token?: string;
  partialToken?: string;
  email: string;
  role: Role;
  firstName: string;
  lastName: string;
  mfaRequired: boolean;
}

export interface MfaValidateRequest {
  partialToken: string;
  code: string;
}

export interface MfaVerifyRequest {
  code: string;
}

export interface MfaSetupResponse {
  qrCodeImage: string;
  otpauthUri: string;
  secret: string;
}