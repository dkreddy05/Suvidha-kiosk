import { create } from 'zustand';
import type { CitizenDTO } from '@/types/auth';

interface AuthState {
  citizen: CitizenDTO | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isNewUser: boolean;
  login: (accessToken: string, refreshToken: string, citizen: CitizenDTO, isNewUser: boolean) => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string) => Promise<void>;
  setCitizen: (citizen: CitizenDTO) => void;
  setNewUser: (isNewUser: boolean) => void;
  logout: () => Promise<void>;
  hydrate: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  citizen: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isNewUser: false,

  login: async (accessToken, refreshToken, citizen, isNewUser) => {
    if (typeof window !== 'undefined') {
      sessionStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('citizen', JSON.stringify(citizen));
      try {
        await fetch('/api/auth/set-cookie', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ accessToken }),
        });
      } catch (err) {
        console.error('Failed to set session cookie:', err);
      }
    }
    set({ accessToken, refreshToken, citizen, isAuthenticated: true, isNewUser });
  },

  setTokens: async (accessToken, refreshToken) => {
    if (typeof window !== 'undefined') {
      sessionStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      try {
        await fetch('/api/auth/set-cookie', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ accessToken }),
        });
      } catch (err) {
        console.error('Failed to update session cookie:', err);
      }
    }
    set({ accessToken, refreshToken });
  },

  setCitizen: (citizen) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('citizen', JSON.stringify(citizen));
    }
    set({ citizen });
  },

  setNewUser: (isNewUser) => set({ isNewUser }),

  logout: async () => {
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('citizen');
      try {
        await fetch('/api/auth/set-cookie', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ action: 'clear' }),
        });
      } catch (err) {
        console.error('Failed to clear session cookie:', err);
      }
    }
    set({ citizen: null, accessToken: null, refreshToken: null, isAuthenticated: false, isNewUser: false });
  },

  hydrate: () => {
    if (typeof window !== 'undefined') {
      const accessToken = sessionStorage.getItem('accessToken');
      const refreshToken = localStorage.getItem('refreshToken');
      const citizenRaw = localStorage.getItem('citizen');
      const citizen = citizenRaw ? JSON.parse(citizenRaw) : null;
      set({
        accessToken,
        refreshToken,
        citizen,
        isAuthenticated: !!accessToken && !!citizen,
      });
    }
  },
}));
