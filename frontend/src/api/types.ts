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
