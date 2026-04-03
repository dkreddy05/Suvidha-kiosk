import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../stores/useAuthStore'
import { tokenHasAdminRole } from '../utils/jwt'

export function AuthGuard() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  if (!isAuthenticated) return <Navigate to="/welcome" replace />
  return <Outlet />
}

export function AdminGuard() {
  const { isAuthenticated, token } = useAuthStore()
  if (!isAuthenticated) return <Navigate to="/welcome" replace />
  if (!tokenHasAdminRole(token)) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
