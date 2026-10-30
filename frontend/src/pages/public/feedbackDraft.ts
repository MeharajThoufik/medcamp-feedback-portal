/**
 * Unsent answers and the "already sent" mark are kept in this phone's browser storage, so a
 * refresh or a dropped connection does not lose anything. Storage can be blocked (private mode),
 * so every access is allowed to fail.
 */

export const STEPS = ['service', 'ratings', 'comment', 'followUp'] as const
export type Step = 'language' | (typeof STEPS)[number]

export interface Answers {
  /** A service id, 'general' for "general visit / not sure", or null when not chosen yet. */
  serviceId: number | 'general' | null
  ratings: Record<string, number>
  comment: string
  problemReported: boolean
  wantsFollowUp: boolean
  contactName: string
  contactPhone: string
  contactEmail: string
  consent: boolean
}

export interface Draft {
  step: Step
  answers: Answers
}

export const EMPTY_ANSWERS: Answers = {
  serviceId: null,
  ratings: {},
  comment: '',
  problemReported: false,
  wantsFollowUp: false,
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  consent: false,
}

const draftKey = (token: string) => `medcamp.feedbackDraft.${token}`
const sentKey = (token: string) => `medcamp.feedbackSent.${token}`

export function loadDraft(token: string): Draft | null {
  try {
    const raw = localStorage.getItem(draftKey(token))
    if (!raw) return null
    const draft = JSON.parse(raw) as Draft
    return { step: draft.step, answers: { ...EMPTY_ANSWERS, ...draft.answers } }
  } catch {
    return null
  }
}

export function saveDraft(token: string, draft: Draft) {
  try {
    localStorage.setItem(draftKey(token), JSON.stringify(draft))
  } catch {
    // Not saved; the form still works.
  }
}

export function clearDraft(token: string) {
  try {
    localStorage.removeItem(draftKey(token))
  } catch {
    // Nothing to clear.
  }
}

/** A soft check only: it warns, it never blocks. The server has the real limits. */
export function wasSentFromThisPhone(token: string): boolean {
  try {
    return localStorage.getItem(sentKey(token)) !== null
  } catch {
    return false
  }
}

export function markSentFromThisPhone(token: string) {
  try {
    localStorage.setItem(sentKey(token), new Date().toISOString())
  } catch {
    // Not remembered; the server limits still apply.
  }
}

export type StepErrors = Partial<Record<string, string>>

/** Checks one step; returns translation keys for the problems found. */
export function validateStep(
  step: Step,
  answers: Answers,
  requiredCategories: string[],
): StepErrors {
  const errors: StepErrors = {}
  if (step === 'service' && answers.serviceId === null) {
    errors.service = 'feedbackForm.service.required'
  }
  if (step === 'ratings') {
    for (const code of requiredCategories) {
      if (!answers.ratings[code]) errors[code] = 'feedbackForm.ratings.required'
    }
  }
  if (step === 'comment' && answers.comment.trim().length > 1000) {
    errors.comment = 'validation.tooLong'
  }
  if (step === 'followUp' && answers.wantsFollowUp) {
    const phone = answers.contactPhone.trim()
    const email = answers.contactEmail.trim()
    if (!phone && !email) errors.contact = 'feedbackForm.followUp.contactRequired'
    const digits = phone.replace(/\D/g, '')
    if (phone && (!/^\+?[0-9(][0-9 ()-]*$/.test(phone) || digits.length < 7 || digits.length > 15))
      errors.contactPhone = 'feedbackForm.followUp.invalidPhone'
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errors.contactEmail = 'feedbackForm.followUp.invalidEmail'
    if (!answers.consent) errors.consent = 'feedbackForm.followUp.consentRequired'
  }
  return errors
}
