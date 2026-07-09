import axios from 'axios';
import { useAuthStore } from '@/store/auth.store';
import { API_BASE_URL } from '@/lib/constants';

// Tokens are managed as HttpOnly cookies — the browser sends them automatically
// on every request. No manual Authorization header injection needed.

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  // Attach the in-memory access token as a Bearer token for gateway requests.
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }

  // Kiosk Mode support (Attach X-Kiosk-Id if configured)
  if (typeof window !== 'undefined') {
    const kioskId = localStorage.getItem('kiosk_id') || localStorage.getItem('kioskId');
    if (kioskId) {
      config.headers['X-Kiosk-Id'] = kioskId;
    }
  } else if (process.env.NEXT_PUBLIC_KIOSK_ID) {
    config.headers['X-Kiosk-Id'] = process.env.NEXT_PUBLIC_KIOSK_ID;
  }

  return config;
});

let isRefreshing = false;
let failedQueue: Array<{
  resolve: () => void;
  reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise<void>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => apiClient(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Send the stored refresh token in the body for cross-origin gateway call.
        const storedRefreshToken = useAuthStore.getState().refreshToken;
        const refreshRes = await axios.post(
          `${API_BASE_URL}/auth/refresh-token`,
          storedRefreshToken ? { refresh_token: storedRefreshToken } : {},
          { withCredentials: true }
        );

        // Store the new tokens in the store so the interceptor picks them up.
        const newAccessToken = refreshRes.data?.accessToken;
        const newRefreshToken = refreshRes.data?.refreshToken;
        if (newAccessToken) {
          useAuthStore.getState().setTokens(newAccessToken, newRefreshToken ?? '');
          // Also persist as session cookie via the BFF
          await fetch('/api/auth/set-cookie', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accessToken: newAccessToken, action: 'set' }),
          });
        }

        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
