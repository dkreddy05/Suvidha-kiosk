import axios from "axios";
import { useAdminAuth } from "@/store/auth.store";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

export const adminClient = axios.create({
  baseURL: API_BASE,
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

adminClient.interceptors.request.use((config) => {
  const token = useAdminAuth.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

adminClient.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      useAdminAuth.getState().logout();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export const adminApi = {
  getDashboard: async () => {
    const { data } = await adminClient.get("/admin/metrics/dashboard");
    return data;
  },

  getUserMetrics: async (period = "month") => {
    const { data } = await adminClient.get("/admin/metrics/users", {
      params: { period },
    });
    return data;
  },

  getGrievanceMetrics: async (period = "month") => {
    const { data } = await adminClient.get("/admin/metrics/grievances", {
      params: { period },
    });
    return data;
  },

  getPaymentMetrics: async (period = "month") => {
    const { data } = await adminClient.get("/admin/metrics/payments", {
      params: { period },
    });
    return data;
  },

  getAllGrievances: async (page = 0, size = 10, status?: string) => {
    const { data } = await adminClient.get("/grievances", {
      params: { page, size, ...(status && { status }) },
    });
    return data;
  },

  updateGrievanceStatus: async (refNumber: string, body: { status: string; message: string }) => {
    const { data } = await adminClient.patch(`/grievances/${refNumber}/status`, body);
    return data;
  },

  getAllConnections: async (page = 0, size = 10, status?: string) => {
    const { data } = await adminClient.get("/connections/requests", {
      params: { page, size, ...(status && { status }) },
    });
    return data;
  },

  approveConnection: async (id: string) => {
    const { data } = await adminClient.post(`/connections/requests/${id}/approve`);
    return data;
  },

  rejectConnection: async (id: string, reason: string) => {
    const { data } = await adminClient.post(`/connections/requests/${id}/reject`, { reason });
    return data;
  },

  getUsers: async (page = 0, size = 10) => {
    const { data } = await adminClient.get("/admin/users", {
      params: { page, size },
    });
    return data;
  },
};
