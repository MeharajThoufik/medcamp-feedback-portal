import { useMutation, useQuery } from '@tanstack/react-query'
import { ApiError, api } from './client'
import type { FeedbackSubmission, PublicComplaintStatus, PublicForm } from './types'

/** For attendees: no sign-in needed. */
export function publicFormKey(token: string) {
  return ['public', 'form', token] as const
}

export function usePublicForm(token: string) {
  return useQuery({
    queryKey: publicFormKey(token),
    queryFn: () => api<PublicForm>(`/public/camps/${encodeURIComponent(token)}`),
  })
}

async function fetchFormToken(token: string) {
  const result = await api<{ submissionToken: string }>(
    `/public/camps/${encodeURIComponent(token)}/form-token`,
  )
  return result.submissionToken
}

export interface SubmissionResult {
  submittedAt: string
  /** Set when a problem was reported. */
  complaintReference: string | null
}

/** Looks up a complaint by its reference; for attendees, no sign-in needed. */
export function useComplaintStatus(reference: string | null) {
  return useQuery({
    queryKey: ['public', 'complaint', reference],
    queryFn: () =>
      api<PublicComplaintStatus>(`/public/complaints/${encodeURIComponent(reference ?? '')}`),
    enabled: reference !== null,
    // Every lookup counts towards the per-phone limit, so do not repeat them by accident.
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  })
}

/**
 * Gets a one-time token and submits. If the token was rejected (for example after the server
 * restarted), it gets a new one and tries once more. "Already submitted" counts as success: the
 * answers did arrive, the confirmation was lost on the way back.
 */
export function useSubmitFeedback(token: string) {
  return useMutation({
    mutationFn: async (answers: Omit<FeedbackSubmission, 'submissionToken'>) => {
      const path = `/public/camps/${encodeURIComponent(token)}/feedback`
      const send = async () =>
        api<SubmissionResult>(path, {
          method: 'POST',
          json: { ...answers, submissionToken: await fetchFormToken(token) },
        })
      try {
        return await send()
      } catch (error) {
        if (
          error instanceof ApiError &&
          (error.code === 'FORM_TOKEN_INVALID' || error.code === 'FORM_TOKEN_EXPIRED')
        )
          return await send()
        throw error
      }
    },
  })
}
