import {
  IconBuildingHospital,
  IconHome,
  IconSettings,
  IconTent,
  IconUsers,
  type Icon,
} from '@tabler/icons-react'
import { ROLES, type Role } from '../api/types'

export interface NavItem {
  to: string
  labelKey: string
  icon: Icon
  roles: readonly Role[]
}

export interface NavSection {
  /** Omitted for the top section. */
  titleKey?: string
  items: NavItem[]
}

/** Menu entries per role. Later phases add Complaints and Dashboards here. */
export const NAVIGATION: NavSection[] = [
  {
    items: [
      { to: '/', labelKey: 'nav.home', icon: IconHome, roles: ROLES },
      // Every role sees camps; the server limits which ones (see CampManager).
      { to: '/camps', labelKey: 'nav.camps', icon: IconTent, roles: ROLES },
    ],
  },
  {
    titleKey: 'nav.administration',
    items: [
      { to: '/users', labelKey: 'nav.users', icon: IconUsers, roles: ['SUPER_ADMIN'] },
      {
        to: '/departments',
        labelKey: 'nav.departments',
        icon: IconBuildingHospital,
        roles: ['SUPER_ADMIN'],
      },
      { to: '/settings', labelKey: 'nav.settings', icon: IconSettings, roles: ['SUPER_ADMIN'] },
    ],
  },
]

export function navigationFor(role: Role): NavSection[] {
  return NAVIGATION.map((section) => ({
    ...section,
    items: section.items.filter((item) => item.roles.includes(role)),
  })).filter((section) => section.items.length > 0)
}
