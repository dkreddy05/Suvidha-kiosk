import { create } from 'zustand';
import type { CitizenDTO } from '@/types/auth';

interface AuthState {
  citizen: CitizenDTO | null;
  isAuthenticated: boolean;
  isNewUser: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  login: (accessToken: string, refreshToken: string, citizen: CitizenDTO, isNewUser: boolean) => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string) => Promise<void>;
  setCitizen: (citizen: CitizenDTO) => void;
  setNewUser: (isNewUser: boolean) => void;
  logout: () => Promise<void>;
  hydrate: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  citizen: null,
  isAuthenticated: false,
  isNewUser: false,
  accessToken: null,
  refreshToken: null,

  login: async (accessToken, refreshToken, citizen, isNewUser) => {
    try {
      await fetch('/api/auth/set-cookie', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accessToken, action: 'set' }),
      });
    } catch (e) {
      console.error('Failed to set session cookie:', e);
    }
    set({ citizen, isAuthenticated: true, isNewUser, accessToken, refreshToken });
  },

  setTokens: async (accessToken, refreshToken) => {
    set({ accessToken, refreshToken });
  },

  setCitizen: (citizen) => {
    set({ citizen });
  },

  setNewUser: (isNewUser) => set({ isNewUser }),

  logout: async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } catch {
      // swallow — we clear state regardless
    }
    set({ citizen: null, isAuthenticated: false, isNewUser: false, accessToken: null, refreshToken: null });
  },

  hydrate: async () => {
    try {
      const res = await fetch('/api/auth/me');
      if (res.ok) {
        const data = await res.json();
        // /api/auth/me returns the citizen profile merged with the accessToken
        const { accessToken, ...citizen } = data;
        set({ citizen, isAuthenticated: true, accessToken: accessToken ?? null });
      } else {
        set({ citizen: null, isAuthenticated: false, accessToken: null });
      }
    } catch {
      set({ citizen: null, isAuthenticated: false, accessToken: null });
    }
  },
}));
