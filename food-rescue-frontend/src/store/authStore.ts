import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '@/types/auth';

interface AuthState {
  // token: string | null; ⬅️ Retiré (géré par le cookie HttpOnly)
  email: string | null;
  role: Role | null;
  firstName: string | null;
  lastName: string | null;
  isAuthenticated: boolean;

  setAuth: (data: {
    // token: string; ⬅️ Retiré
    email: string;
    role: Role;
    firstName: string;
    lastName: string;
  }) => void;

  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      // token: null,
      email: null,
      role: null,
      firstName: null,
      lastName: null,
      isAuthenticated: false,

      setAuth: (data) =>
        set({
          // token: data.token,
          email: data.email,
          role: data.role,
          firstName: data.firstName,
          lastName: data.lastName,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          // token: null,
          email: null,
          role: null,
          firstName: null,
          lastName: null,
          isAuthenticated: false,
        }),
    }),
    { name: 'food-rescue-auth' }
  )
);