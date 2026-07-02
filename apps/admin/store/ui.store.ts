import { create } from "zustand";

type Theme = "light" | "dark" | "system";

interface AdminUIState {
  sidebarOpen: boolean;
  theme: Theme;
  toggleSidebar: () => void;
  setTheme: (theme: Theme) => void;
}

export const useAdminUI = create<AdminUIState>((set) => ({
  sidebarOpen: true,
  theme: "system",

  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),

  setTheme: (theme) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("adminTheme", theme);
    }
    set({ theme });
  },
}));
