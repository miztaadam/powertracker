import { daysBetween } from '../../state/isoDate'
import { formatDayMonth, formatMonthYear } from './dates'

/**
 * Axis labels for evenly spaced `dates`: the first, the middle and the last, blank elsewhere.
 * Past a year the day stops mattering, so the label is the month and year.
 */
export function axisLabels(dates: readonly string[]): string[] {
  const last = dates.length - 1
  if (last < 0) return []
  const format = daysBetween(dates[0]!, dates[last]!) > 366 ? formatMonthYear : formatDayMonth
  const shown = dates.length >= 5 ? [0, Math.floor(last / 2), last] : [0, last]
  return dates.map((date, i) => (shown.includes(i) ? format(date) : ''))
}
