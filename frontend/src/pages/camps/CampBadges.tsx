import { Badge, type MantineColor } from '@mantine/core'
import { useTranslation } from 'react-i18next'
import type { CampStatus, FeedbackState } from '../../api/types'

const STATUS_COLORS: Record<CampStatus, MantineColor> = {
  UPCOMING: 'blue',
  ACTIVE: 'teal',
  CLOSED: 'gray',
  ARCHIVED: 'dark',
}

export function CampStatusBadge({ status }: { status: CampStatus }) {
  const { t } = useTranslation()
  return (
    <Badge variant="light" color={STATUS_COLORS[status]}>
      {t(`camps.status.${status}`)}
    </Badge>
  )
}

/** Shown only when it adds something: an Active camp whose form is or is not open yet. */
export function FeedbackStateBadge({ state }: { state: FeedbackState }) {
  const { t } = useTranslation()
  if (state === 'CAMP_NOT_ACTIVE') return null
  return (
    <Badge variant="dot" color={state === 'OPEN' ? 'teal' : 'orange'}>
      {t(`camps.feedbackState.${state}`)}
    </Badge>
  )
}
