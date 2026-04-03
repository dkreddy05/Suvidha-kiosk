import { QueryCache, QueryClient } from '@tanstack/react-query'
import type { ApiError } from '../api/types'
import { useToastStore } from '../stores/useToastStore'

function isApiError(e: unknown): e is ApiError {
  return Boolean(
    e &&
      typeof e === 'object' &&
      'error' in e &&
      (e as any).error &&
      typeof (e as any).error?.message === 'string',
  )
}

function onAnyError(error: unknown) {
  if (!isApiError(error)) return
  useToastStore.getState().push({ kind: 'error', message: error.error.message })
}

export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: onAnyError,
  }),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: 0,
    },
    mutations: {
      retry: false,
    },
  },
})
