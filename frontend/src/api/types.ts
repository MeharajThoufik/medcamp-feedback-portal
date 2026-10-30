export const ROLES = ['SUPER_ADMIN', 'ORGANIZER', 'MEDICAL_STAFF', 'ANALYST'] as const
export type Role = (typeof ROLES)[number]

export interface Me {
  id: number
  email: string
  fullName: string
  role: Role
  departmentId: number | null
  departmentName: string | null
  mustChangePassword: boolean
}

export interface User {
  id: number
  email: string
  fullName: string
  role: Role
  departmentId: number | null
  departmentName: string | null
  enabled: boolean
  locked: boolean
  mustChangePassword: boolean
  lastLoginAt: string | null
  createdAt: string
}

export interface Department {
  id: number
  name: string
  active: boolean
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export const CAMP_STATUSES = ['UPCOMING', 'ACTIVE', 'CLOSED', 'ARCHIVED'] as const
export type CampStatus = (typeof CAMP_STATUSES)[number]

/** Whether attendees can give feedback today, and if not, why. */
export type FeedbackState = 'OPEN' | 'CAMP_NOT_ACTIVE' | 'NOT_STARTED' | 'ENDED'

/** One row in the camp list. Dates are `YYYY-MM-DD`. */
export interface CampSummary {
  id: number
  name: string
  location: string
  startDate: string
  endDate: string
  status: CampStatus
  feedbackState: FeedbackState
  organizerId: number
  organizerName: string
  serviceCount: number
}

export interface CampServiceItem {
  id: number
  departmentId: number
  departmentName: string
  name: string
}

export interface Camp {
  id: number
  name: string
  location: string
  startDate: string
  endDate: string
  status: CampStatus
  feedbackState: FeedbackState
  feedbackOpensOn: string
  feedbackClosesOn: string
  publicToken: string
  /** The link inside the camp's QR code. */
  feedbackUrl: string
  organizerId: number
  organizerName: string
  services: CampServiceItem[]
  /** The server checks the same rules again on every change. */
  canEdit: boolean
  nextStatuses: CampStatus[]
  createdAt: string
  updatedAt: string
}

export interface OrganizerOption {
  id: number
  fullName: string
  email: string
}

/** What the printable poster shows. */
export interface Poster {
  campId: number
  name: string
  location: string
  startDate: string
  endDate: string
  feedbackUrl: string
  /** Null when no camp Wi-Fi is set. */
  wifi: { ssid: string; password: string | null } | null
}

export type WifiSecurity = 'WPA' | 'NONE'

export interface WifiNetwork {
  ssid: string
  /** Null for an open network. */
  password: string | null
  security: WifiSecurity
}

export interface Settings {
  /** Where feedback links point, from `APP_PUBLIC_BASE_URL`. */
  publicBaseUrl: string
  wifi: WifiNetwork | null
}
export interface RatingCategory {
  code: string
  labelEn: string
  labelHi: string
  /** Attendees must answer it. */
  required: boolean
}

/** What the attendee form needs; anyone with the camp's link may see it. */
export interface PublicForm {
  name: string
  location: string
  startDate: string
  endDate: string
  feedbackState: FeedbackState
  services: { id: number; name: string; departmentName: string }[]
  categories: RatingCategory[]
}

export type FormLanguage = 'en' | 'hi'

export interface FeedbackSubmission {
  submissionToken: string
  language: FormLanguage
  /** Null for "general visit / not sure". */
  serviceId: number | null
  /** Category code to a score from 1 to 5; skipped categories are left out. */
  ratings: Record<string, number>
  comment: string | null
  problemReported: boolean
  followUp: { consent: boolean; name: string; phone: string; email: string } | null
  /** Accepted only from signed-in staff filling the form in for an attendee. */
  assisted: boolean
}

/** One feedback entry as staff see it. */
export interface FeedbackItem {
  id: number
  submittedAt: string
  serviceId: number | null
  serviceName: string | null
  departmentName: string | null
  ratings: Record<string, number>
  comment: string | null
  problemReported: boolean
  language: FormLanguage
  anonymous: boolean
  assisted: boolean
  assistedByName: string | null
  /** Only for the camp's managers, and only when the attendee agreed to be contacted. */
  contact: { name: string | null; phone: string | null; email: string | null } | null
}

export interface FeedbackSummary {
  total: number
  categories: (Omit<RatingCategory, 'required'> & { average: number | null; count: number })[]
}
