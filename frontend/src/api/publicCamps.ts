import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { PublicCamp } from './types'

/** For attendees: no sign-in needed. */
export function usePublicCamp(token: string) {
  return useQuery({
    queryKey: ['public', 'camp', token],
    queryFn: () => api<PublicCamp>(`/public/camps/${encodeURIComponent(token)}`),
  })
}
