import type { TFunction } from 'i18next'
import i18n from './index'
import { ApiError } from '../api/client'

/**
 * Picks the best message for an error: a translation for this screen (`{scope}.errors.{code}`),
 * then a shared one (`errors.{code}`), then the server's own English text.
 */
export function errorMessage(t: TFunction, error: unknown, scope?: string): string {
  if (error instanceof ApiError) {
    if (scope && i18n.exists(`${scope}.errors.${error.code}`))
      return t(`${scope}.errors.${error.code}`)
    if (i18n.exists(`errors.${error.code}`)) return t(`errors.${error.code}`)
    if (error.status > 0 && error.status < 500 && error.message) return error.message
  }
  return t('common.somethingWrong')
}
