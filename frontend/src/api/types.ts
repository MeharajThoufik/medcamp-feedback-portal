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
