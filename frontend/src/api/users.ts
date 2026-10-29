import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { Page, Role, User } from './types'

export interface UserFilters {
  search: string
  role: Role | null
  enabled: boolean | null
  /** Zero-based. */
  page: number
}

export interface UserInput {
  email: string
  fullName: string
  role: Role
  departmentId: number | null
}

const PAGE_SIZE = 20

export function useUsers(filters: UserFilters) {
  const params = new URLSearchParams({ page: String(filters.page), size: String(PAGE_SIZE) })
  if (filters.search.trim()) params.set('search', filters.search.trim())
  if (filters.role) params.set('role', filters.role)
  if (filters.enabled !== null) params.set('enabled', String(filters.enabled))
  return useQuery({
    queryKey: ['users', filters],
    queryFn: () => api<Page<User>>(`/users?${params}`),
    placeholderData: keepPreviousData,
  })
}

function useInvalidateUsers() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: ['users'] })
}

export function useCreateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (input: UserInput) =>
      api<{ user: User; temporaryPassword: string }>('/users', { method: 'POST', json: input }),
    onSuccess: invalidate,
  })
}

export function useUpdateUser() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: UserInput }) =>
      api<User>(`/users/${id}`, { method: 'PUT', json: input }),
    onSuccess: invalidate,
  })
}

export function useSetUserEnabled() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      api<User>(`/users/${id}/enabled`, { method: 'PUT', json: { enabled } }),
    onSuccess: invalidate,
  })
}

export function useResetPassword() {
  const invalidate = useInvalidateUsers()
  return useMutation({
    mutationFn: (id: number) =>
      api<{ temporaryPassword: string }>(`/users/${id}/reset-password`, { method: 'POST' }),
    onSuccess: invalidate,
  })
}
