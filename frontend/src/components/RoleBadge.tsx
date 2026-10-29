import { Badge, type MantineColor } from '@mantine/core'
import { useTranslation } from 'react-i18next'
import type { Role } from '../api/types'

const ROLE_COLORS: Record<Role, MantineColor> = {
  SUPER_ADMIN: 'grape',
  ORGANIZER: 'blue',
  MEDICAL_STAFF: 'teal',
  ANALYST: 'orange',
}

export function RoleBadge({ role }: { role: Role }) {
  const { t } = useTranslation()
  return (
    <Badge variant="light" color={ROLE_COLORS[role]}>
      {t(`roles.${role}`)}
    </Badge>
  )
}
