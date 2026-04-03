import { create } from 'zustand'

export type SessionStore = {
  lastActivity: number
  updateActivity: () => void
  resetSession: () => void
}

export const useSessionStore = create<SessionStore>((set) => ({
  lastActivity: Date.now(),
  updateActivity: () => set({ lastActivity: Date.now() }),
  resetSession: () => set({ lastActivity: Date.now() }),
}))
