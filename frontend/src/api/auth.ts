import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from './client'
import { meKey } from './queryClient'
import type { Me } from './types'

/** The signed-in user, or `null` when nobody is signed in. */
async function fetchMe(): Promise<Me | null> {
  try {
    return await api<Me>('/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

/** `enabled: false` skips the call, for pages that only sometimes need it. */
export function useMe(enabled = true) {
  return useQuery({ queryKey: meKey, queryFn: fetchMe, staleTime: 5 * 60_000, enabled })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (credentials: { email: string; password: string }) =>
      api<Me>('/auth/login', { method: 'POST', form: credentials }),
    onSuccess: (me) => {
      queryClient.setQueryData(meKey, me)
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api<void>('/auth/logout', { method: 'POST' }),
    onSettled: () => {
      queryClient.clear()
      queryClient.setQueryData(meKey, null)
    },
  })
}

export function useChangePassword() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { currentPassword: string; newPassword: string }) =>
      api<void>('/auth/change-password', { method: 'POST', json: body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: meKey }),
  })
}
