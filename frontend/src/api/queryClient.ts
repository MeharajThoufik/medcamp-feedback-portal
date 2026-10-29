import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { ApiError } from './client'

export const meKey = ['me'] as const

/** Any 401 means the session is gone: clearing "me" sends the user to the sign-in page. */
function handleAuthErrors(error: unknown) {
  if (!(error instanceof ApiError)) return
  if (error.status === 401) {
    queryClient.setQueryData(meKey, null)
  } else if (error.code === 'PASSWORD_CHANGE_REQUIRED') {
    void queryClient.invalidateQueries({ queryKey: meKey })
  }
}

export const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError: handleAuthErrors }),
  mutationCache: new MutationCache({ onError: handleAuthErrors }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Retry only network and server failures, never 4xx answers.
      retry: (failureCount, error) =>
        failureCount < 2 &&
        !(error instanceof ApiError && error.status >= 400 && error.status < 500),
    },
  },
})
