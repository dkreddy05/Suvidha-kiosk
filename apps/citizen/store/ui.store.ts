import { create } from 'zustand';

type Theme = 'light' | 'dark' | 'system';
type Language = 'en' | 'hi' | 'te' | 'ta';

interface UIState {
  theme: Theme;
  language: Language;
  sidebarOpen: boolean;
  setTheme: (theme: Theme) => void;
  setLanguage: (language: Language) => void;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  theme: 'system',
  language: 'en',
  sidebarOpen: true,

  setTheme: (theme) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('theme', theme);
    }
    set({ theme });
  },

  setLanguage: (language) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('language', language);
    }
    set({ language });
  },

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),

  setSidebarOpen: (sidebarOpen) => set({ sidebarOpen }),
}));
