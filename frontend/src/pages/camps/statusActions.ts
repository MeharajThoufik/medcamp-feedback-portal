import {
  IconArchive,
  IconArchiveOff,
  IconPlayerPlay,
  IconPlayerStop,
  IconRefresh,
  type Icon,
} from '@tabler/icons-react'
import type { CampStatus } from '../../api/types'

export interface StatusAction {
  target: CampStatus
  /** Translation key under `camps.actions`. */
  key: 'start' | 'reopen' | 'close' | 'archive' | 'restore'
  icon: Icon
  /** The main next step for this camp, shown as a filled button. */
  primary: boolean
  /** Ask first, because the change stops or hides something. */
  confirm: boolean
}

function actionFor(current: CampStatus, target: CampStatus): StatusAction {
  switch (target) {
    case 'ACTIVE':
      return current === 'UPCOMING'
        ? { target, key: 'start', icon: IconPlayerPlay, primary: true, confirm: false }
        : { target, key: 'reopen', icon: IconRefresh, primary: false, confirm: false }
    case 'CLOSED':
      return current === 'ARCHIVED'
        ? { target, key: 'restore', icon: IconArchiveOff, primary: true, confirm: false }
        : { target, key: 'close', icon: IconPlayerStop, primary: true, confirm: true }
    case 'ARCHIVED':
      return { target, key: 'archive', icon: IconArchive, primary: false, confirm: true }
    case 'UPCOMING':
      throw new Error('No camp moves back to Upcoming')
  }
}

/** The buttons to offer, main step first, from the statuses the server says are allowed. */
export function statusActions(current: CampStatus, next: CampStatus[]): StatusAction[] {
  return next
    .map((target) => actionFor(current, target))
    .sort((a, b) => Number(b.primary) - Number(a.primary))
}
