import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '@/types/auth';

interface AuthState {
  id: string | null; // 👈 Strictement string (UUID) ou null
  email: string | null;
  role: Role | null;
  firstName: string | null;
  lastName: string | null;
  mfaEnabled: boolean;
  isAuthenticated: boolean;

  setAuth: (data: {
    id: string; // 👈 String (UUID)
    email: string;
    role: Role;
    firstName: string;
    lastName: string;
    mfaEnabled?: boolean;
  }) => void;

  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      id: null,
      email: null,
      role: null,
      firstName: null,
      lastName: null,
      mfaEnabled: false,
      isAuthenticated: false,

      setAuth: (data) => {
        set({
          id: data.id,
          email: data.email,
          role: data.role,
          firstName: data.firstName,
          lastName: data.lastName,
          mfaEnabled: data.mfaEnabled ?? false,
          isAuthenticated: true,
        });
      },

      logout: () =>
        set({
          id: null,
          email: null,
          role: null,
          firstName: null,
          lastName: null,
          mfaEnabled: false,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'food-rescue-auth',
      partialize: (state) => ({
        id: state.id,
        email: state.email,
        role: state.role,
        firstName: state.firstName,
        lastName: state.lastName,
        mfaEnabled: state.mfaEnabled,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);