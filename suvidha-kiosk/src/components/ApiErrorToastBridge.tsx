import { useEffect } from 'react'
import type { ApiError } from '../api/types'
import { useToastStore } from '../stores/useToastStore'

export function showApiErrorToast(err: ApiError) {
  useToastStore.getState().push({ kind: 'error', message: err.error.message })
}

export function ApiErrorToastBridge({ error }: { error: ApiError | null }) {
  const push = useToastStore((s) => s.push)

  useEffect(() => {
    if (!error) return
    push({ kind: 'error', message: error.error.message })
  }, [error, push])

  return null
}
