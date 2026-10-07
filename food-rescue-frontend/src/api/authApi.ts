import axiosClient from './axiosClient';
import type {
  AuthResponse,
  LoginRequest,
  MfaSetupResponse,
  MfaValidateRequest,
  MfaVerifyRequest,
  RegisterRequest,
  User, // Assurez-vous d'importer le type User si vous l'avez dans vos types
} from '@/types/auth';

export const authApi = {
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await axiosClient.post<AuthResponse>('/api/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await axiosClient.post<AuthResponse>('/api/auth/login', data);
    return response.data;
  },

  mfaValidate: async (data: MfaValidateRequest): Promise<AuthResponse> => {
    const response = await axiosClient.post<AuthResponse>('/api/auth/mfa/validate', data);
    return response.data;
  },

  mfaSetup: async (): Promise<MfaSetupResponse> => {
    const response = await axiosClient.post<MfaSetupResponse>('/api/auth/mfa/setup');
    return response.data;
  },

  mfaVerify: async (data: MfaVerifyRequest): Promise<void> => {
    await axiosClient.post('/api/auth/mfa/verify', data);
  },

  // AJOUT : Récupérer le profil/état de l'utilisateur connecté (incluant mfaEnabled)
  getCurrentUser: async (): Promise<any> => {
    const response = await axiosClient.get('/api/auth/me'); // Ajustez l'endpoint selon votre route backend exacte (ex: /api/users/me ou /api/auth/me)
    return response.data;
  },
};