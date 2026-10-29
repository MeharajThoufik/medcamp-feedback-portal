import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { Department } from './types'

export function useDepartments(includeInactive = false) {
  return useQuery({
    queryKey: ['departments', { includeInactive }],
    queryFn: () => api<Department[]>(`/departments?includeInactive=${includeInactive}`),
  })
}

export function useSaveDepartment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, name, active }: { id?: number; name: string; active?: boolean }) =>
      id === undefined
        ? api<Department>('/departments', { method: 'POST', json: { name, active } })
        : api<Department>(`/departments/${id}`, { method: 'PUT', json: { name, active } }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['departments'] })
      // User rows show department names.
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}
