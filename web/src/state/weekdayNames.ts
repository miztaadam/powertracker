/** The weekday's name in the device's language, as Android shows it. 1 = Monday. */
export function dayName(weekday: number, style: 'long' | 'short' = 'long'): string {
  // 5 January 2026 is a Monday.
  return new Intl.DateTimeFormat(undefined, { weekday: style, timeZone: 'UTC' }).format(new Date(Date.UTC(2026, 0, 4 + weekday)))
}
