import dayjs from 'dayjs'
import { useTranslation } from 'react-i18next'

/** Camp dates travel as `YYYY-MM-DD` strings, which also compare correctly as text. */
export function addDays(date: string, days: number): string {
  return dayjs(date).add(days, 'day').format('YYYY-MM-DD')
}

/** By default feedback opens on the first camp day and closes a week after the last one. */
export const FEEDBACK_DAYS_AFTER_CAMP = 7

export function defaultFeedbackWindow(startDate: string, endDate: string): [string, string] {
  return [startDate, addDays(endDate, FEEDBACK_DAYS_AFTER_CAMP)]
}

function toDate(date: string) {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day)
}

/** Formats camp dates for the current language, such as "29 Oct 2026" or "29–31 Oct 2026". */
export function useCampDateFormat() {
  const { i18n } = useTranslation()
  const format = new Intl.DateTimeFormat(i18n.language === 'hi' ? 'hi-IN' : 'en-IN', {
    dateStyle: 'medium',
  })
  return {
    date: (date: string) => format.format(toDate(date)),
    range: (from: string, to: string) => format.formatRange(toDate(from), toDate(to)),
  }
}
