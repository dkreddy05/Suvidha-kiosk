import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../stores/useAuthStore'
import { useSessionStore } from '../stores/useSessionStore'
import { formatMmSs } from '../utils/format'

const IDLE_MS = 2 * 60 * 1000
const WARNING_MS = 60 * 1000

export function useIdleSession() {
  const navigate = useNavigate()
  const clearAuth = useAuthStore((s) => s.clearAuth)
  const lastActivity = useSessionStore((s) => s.lastActivity)
  const updateActivity = useSessionStore((s) => s.updateActivity)

  const [now, setNow] = useState(() => Date.now())
  const [warningOpen, setWarningOpen] = useState(false)

  useEffect(() => {
    const onAny = () => updateActivity()
    window.addEventListener('pointerdown', onAny, { passive: true })
    window.addEventListener('keydown', onAny)
    window.addEventListener('touchstart', onAny, { passive: true })
    return () => {
      window.removeEventListener('pointerdown', onAny)
      window.removeEventListener('keydown', onAny)
      window.removeEventListener('touchstart', onAny)
    }
  }, [updateActivity])

  useEffect(() => {
    const t = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(t)
  }, [])

  const remainingMs = Math.max(0, IDLE_MS - (now - lastActivity))
  const remainingSec = Math.ceil(remainingMs / 1000)

  useEffect(() => {
    if (remainingMs === 0) {
      clearAuth()
      navigate('/welcome', { replace: true })
    }
    if (remainingMs > 0 && remainingMs <= WARNING_MS) setWarningOpen(true)
    if (remainingMs > WARNING_MS) setWarningOpen(false)
  }, [remainingMs, clearAuth, navigate])

  const remainingLabel = useMemo(() => formatMmSs(remainingSec), [remainingSec])

  return {
    remainingSec,
    remainingLabel,
    warningOpen,
    warningLabel: formatMmSs(Math.ceil(remainingMs / 1000)),
    onContinue: () => updateActivity(),
    onEnd: () => {
      clearAuth()
      navigate('/welcome', { replace: true })
    },
  }
}
