import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { FeedbackItem, FeedbackSummary, Page } from './types'

const PAGE_SIZE = 20

export function useCampFeedback(campId: number, page: number, enabled = true) {
  return useQuery({
    queryKey: ['feedback', campId, 'list', page],
    queryFn: () =>
      api<Page<FeedbackItem>>(`/camps/${campId}/feedback?page=${page}&size=${PAGE_SIZE}`),
    placeholderData: keepPreviousData,
    enabled,
  })
}

export function useFeedbackSummary(campId: number, enabled = true) {
  return useQuery({
    queryKey: ['feedback', campId, 'summary'],
    queryFn: () => api<FeedbackSummary>(`/camps/${campId}/feedback/summary`),
    enabled,
  })
}
