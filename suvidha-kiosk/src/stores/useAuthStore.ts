import { create } from 'zustand'
import type { CitizenDTO } from '../api/types'

export type AuthStore = {
  token: string | null
  citizen: CitizenDTO | null
  isAuthenticated: boolean
  setAuth: (token: string, citizen: CitizenDTO) => void
  clearAuth: () => void
  // Needed because /billing/accounts requires full mobile query.
  mobileRaw: string | null
  setMobileRaw: (mobile: string) => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  token: null,
  citizen: null,
  isAuthenticated: false,
  mobileRaw: null,
  setMobileRaw: (mobile) => set({ mobileRaw: mobile }),
  setAuth: (token, citizen) =>
    set({ token, citizen, isAuthenticated: true }),
  clearAuth: () => set({ token: null, citizen: null, isAuthenticated: false, mobileRaw: null }),
}))
