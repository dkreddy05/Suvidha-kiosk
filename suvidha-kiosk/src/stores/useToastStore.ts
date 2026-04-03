import { create } from 'zustand'
import { v4 as uuidv4 } from 'uuid'

export type Toast = {
  id: string
  message: string
  kind: 'info' | 'success' | 'error'
}

type ToastStore = {
  toasts: Toast[]
  push: (toast: Omit<Toast, 'id'>) => string
  pushSticky: (toast: Omit<Toast, 'id'>) => string
  update: (id: string, message: string) => void
  remove: (id: string) => void
}

export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],
  push: (toast) => {
    const id = uuidv4()
    set({ toasts: [...get().toasts, { ...toast, id }] })
    window.setTimeout(() => get().remove(id), 4000)
    return id
  },
  pushSticky: (toast) => {
    const id = uuidv4()
    set({ toasts: [...get().toasts, { ...toast, id }] })
    return id
  },
  update: (id, message) =>
    set({
      toasts: get().toasts.map((t) => (t.id === id ? { ...t, message } : t)),
    }),
  remove: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
}))
