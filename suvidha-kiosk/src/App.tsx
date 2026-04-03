import './i18n'
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './hooks/queryClient'
import { setNavigator } from './router/navigation'
import { ErrorBoundary } from './components/ErrorBoundary'
import { ToastHost } from './components/ToastHost'
import { IdleWarningModal } from './components/IdleWarningModal'
import { useIdleSession } from './hooks/useIdleSession'
import { AuthGuard, AdminGuard } from './router/guards'
import { WelcomeScreen } from './screens/WelcomeScreen'
import { AuthScreen } from './screens/AuthScreen'
import { DashboardScreen } from './screens/DashboardScreen'
import { BillingScreen } from './screens/BillingScreen'
import { GrievanceScreen } from './screens/GrievanceScreen'
import { TrackGrievanceScreen } from './screens/TrackGrievanceScreen'
import { ConnectionScreen } from './screens/ConnectionScreen'
import { StatusTrackingScreen } from './screens/StatusTrackingScreen'
import { AdminDashboard } from './screens/AdminDashboard'
import { useTranslation } from 'react-i18next'

function ErrorFallback() {
  const { t } = useTranslation()
  return <div className="min-h-screen bg-slate-950 p-14 text-[28px]">{t('common.error')}</div>
}

function NavigatorBridge() {
  const navigate = useNavigate()
  setNavigator(navigate)
  return null
}

function AppShell() {
  const idle = useIdleSession()
  return (
    <>
      <NavigatorBridge />
      <IdleWarningModal
        open={idle.warningOpen}
        time={idle.warningLabel}
        onContinue={idle.onContinue}
        onEnd={idle.onEnd}
      />
      <ToastHost />
      <Routes>
        <Route path="/" element={<Navigate to="/welcome" replace />} />
        <Route path="/welcome" element={<WelcomeScreen />} />
        <Route path="/auth" element={<AuthScreen />} />
        <Route element={<AuthGuard />}>
          <Route path="/dashboard" element={<DashboardScreen />} />
          <Route path="/billing" element={<BillingScreen />} />
          <Route path="/grievance" element={<GrievanceScreen />} />
          <Route path="/connections" element={<ConnectionScreen />} />
        </Route>
        <Route path="/grievance/track" element={<TrackGrievanceScreen />} />
        <Route path="/status" element={<StatusTrackingScreen />} />
        <Route element={<AdminGuard />}>
          <Route path="/admin" element={<AdminDashboard />} />
        </Route>
        <Route path="*" element={<Navigate to="/welcome" replace />} />
      </Routes>
    </>
  )
}

export default function App() {
  return (
    <ErrorBoundary fallback={<ErrorFallback />}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AppShell />
        </BrowserRouter>
      </QueryClientProvider>
    </ErrorBoundary>
  )
}
