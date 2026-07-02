import { create } from "zustand";

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: "ADMIN" | "EMPLOYEE";
}

interface AdminAuthState {
  user: AdminUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (user: AdminUser, token: string) => void;
  setUser: (user: AdminUser) => void;
  logout: () => void;
}

export const useAdminAuth = create<AdminAuthState>((set) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,

  login: (user, accessToken) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("adminUser", JSON.stringify(user));
      sessionStorage.setItem("adminToken", accessToken);
    }
    set({ user, accessToken, isAuthenticated: true });
  },

  setUser: (user) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("adminUser", JSON.stringify(user));
    }
    set({ user });
  },

  logout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("adminUser");
      sessionStorage.removeItem("adminToken");
    }
    set({ user: null, accessToken: null, isAuthenticated: false });
  },
}));
