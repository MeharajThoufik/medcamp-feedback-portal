import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { Camp, CampStatus, CampSummary, OrganizerOption, Page } from './types'

export interface CampFilters {
  search: string
  /** Null means every status except Archived. */
  status: CampStatus | null
  /** `YYYY-MM-DD`; a camp matches when any of its days is inside the range. */
  from: string | null
  to: string | null
  organizerId: number | null
  /** Zero-based. */
  page: number
}

export interface CampServiceInput {
  /** Set for an existing service, left out for a new one. */
  id?: number
  departmentId: number
  name: string
}

export interface CampInput {
  name: string
  location: string
  startDate: string
  endDate: string
  feedbackOpensOn: string
  feedbackClosesOn: string
  /** Read only from Super Admins; an organizer always runs their own camps. */
  organizerId: number | null
  /** The full list in display order. Existing services left out are removed. */
  services: CampServiceInput[]
}

const PAGE_SIZE = 20

export function useCamps(filters: CampFilters) {
  const params = new URLSearchParams({ page: String(filters.page), size: String(PAGE_SIZE) })
  if (filters.search.trim()) params.set('search', filters.search.trim())
  if (filters.status) params.set('status', filters.status)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  if (filters.organizerId !== null) params.set('organizerId', String(filters.organizerId))
  return useQuery({
    queryKey: ['camps', 'list', filters],
    queryFn: () => api<Page<CampSummary>>(`/camps?${params}`),
    placeholderData: keepPreviousData,
  })
}

export function campKey(id: number) {
  return ['camps', 'detail', id] as const
}

export function useCamp(id: number) {
  return useQuery({
    queryKey: campKey(id),
    queryFn: () => api<Camp>(`/camps/${id}`),
  })
}

/** Enabled organizers; only Super Admins can load this list. */
export function useOrganizers(enabled: boolean) {
  return useQuery({
    queryKey: ['users', 'organizers'],
    queryFn: () => api<OrganizerOption[]>('/users/organizers'),
    enabled,
  })
}

function useStoreCamp() {
  const queryClient = useQueryClient()
  return (camp: Camp) => {
    queryClient.setQueryData(campKey(camp.id), camp)
    void queryClient.invalidateQueries({ queryKey: ['camps', 'list'] })
  }
}

export function useCreateCamp() {
  const store = useStoreCamp()
  return useMutation({
    mutationFn: (input: CampInput) => api<Camp>('/camps', { method: 'POST', json: input }),
    onSuccess: store,
  })
}

export function useUpdateCamp() {
  const store = useStoreCamp()
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: CampInput }) =>
      api<Camp>(`/camps/${id}`, { method: 'PUT', json: input }),
    onSuccess: store,
  })
}

export function useChangeCampStatus() {
  const store = useStoreCamp()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: CampStatus }) =>
      api<Camp>(`/camps/${id}/status`, { method: 'PUT', json: { status } }),
    onSuccess: store,
  })
}
