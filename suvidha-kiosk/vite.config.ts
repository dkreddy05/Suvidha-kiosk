import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  // Dev CORS workaround: proxy same-origin `/api/*` to backend.
  // Use `.env.local` with `VITE_API_BASE_URL=/` so axios hits this proxy.
  // Prefer an explicit proxy target for local dev.
  // Example:
  //   VITE_API_BASE_URL=/
  //   VITE_PROXY_TARGET=http://localhost:8080
  const proxyTargetRaw = env.VITE_PROXY_TARGET || env.VITE_API_BASE_URL || 'http://localhost:8080'
  const proxyTarget = proxyTargetRaw.startsWith('http') ? proxyTargetRaw : 'http://localhost:8080'
  const isHttps = proxyTarget.startsWith('https://')

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          // Only relevant for HTTPS targets.
          secure: isHttps,
        },
      },
    },
  }
})
