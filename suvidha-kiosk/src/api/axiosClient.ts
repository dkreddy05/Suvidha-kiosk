import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'
import type { ApiError } from './types'
import { useAuthStore } from '../stores/useAuthStore'
import { getNavigator } from '../router/navigation'
import i18n from '../i18n'
import { useToastStore } from '../stores/useToastStore'

const API_PREFIX = '/api/v1'

function getBaseUrl() {
  const raw = String(import.meta.env.VITE_API_BASE_URL ?? '')
  const trimmed = raw.replace(/\/+$/g, '')
  return trimmed
}

export const axiosClient = axios.create({
  baseURL: `${getBaseUrl()}${API_PREFIX}`,
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
})

function normalizeApiError(data: unknown, fallbackRequestId: string): ApiError {
  if (
    data &&
    typeof data === 'object' &&
    'error' in data &&
    (data as any).error &&
    typeof (data as any).error === 'object'
  ) {
    const err = (data as any).error
    return {
      error: {
        code: String(err.code ?? 'UNKNOWN'),
        message: String(err.message ?? 'Unknown error'),
        fields: err.fields && typeof err.fields === 'object' ? err.fields : undefined,
        requestId: String(err.requestId ?? fallbackRequestId),
      },
    }
  }

  return {
    error: {
      code: 'UNKNOWN',
      message: i18n.t('common.error'),
      requestId: fallbackRequestId,
    },
  }
}

axiosClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  const requestId = uuidv4()

  config.headers = config.headers ?? {}
  ;(config.headers as any)['X-Request-Id'] = requestId
  if (token) (config.headers as any)['Authorization'] = `Bearer ${token}`

  // Ensure JSON content type unless explicitly overridden (e.g. file upload).
  if (!(config.headers as any)['Content-Type'] && !(config.headers as any)['content-type']) {
    ;(config.headers as any)['Content-Type'] = 'application/json'
  }

  ;(config as any)._requestId = requestId
  return config
})

axiosClient.interceptors.response.use(
  (res) => res,
  (error) => {
    const status: number | undefined = error?.response?.status
    const requestId: string =
      error?.response?.headers?.['x-request-id'] || error?.config?._requestId || uuidv4()

    if (status === 401) {
      useAuthStore.getState().clearAuth()
      getNavigator()?.('/welcome', { replace: true })
    }

    if (status === 403) {
      useToastStore.getState().push({ kind: 'error', message: i18n.t('common.accessDenied') })
    }

    if (status === 429) {
      const retryAfterRaw = error?.response?.headers?.['retry-after']
      const retryAfterSec = Math.max(1, Number.parseInt(String(retryAfterRaw ?? '60'), 10) || 60)
      const toastId = useToastStore
        .getState()
        .pushSticky({ kind: 'error', message: i18n.t('rateLimit.retryIn', { sec: retryAfterSec }) })

      let remaining = retryAfterSec
      const timer = window.setInterval(() => {
        remaining -= 1
        if (remaining <= 0) {
          window.clearInterval(timer)
          useToastStore.getState().remove(toastId)
          return
        }
        useToastStore.getState().update(toastId, i18n.t('rateLimit.retryIn', { sec: remaining }))
      }, 1000)
    }

    const apiError = normalizeApiError(error?.response?.data, requestId)
    ;(apiError as any)._httpStatus = status
    return Promise.reject(apiError)
  },
)
