/** Calendar arithmetic on ISO `YYYY-MM-DD` dates. Everything runs in UTC, so no time zone can shift a day. */

const utc = (iso: string) => new Date(`${iso}T00:00:00Z`)

export function addDays(iso: string, days: number): string {
  const d = utc(iso)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

/** Whole days from `from` to `to`; negative when `to` is earlier. */
export function daysBetween(from: string, to: string): number {
  return Math.round((utc(to).getTime() - utc(from).getTime()) / 86_400_000)
}

/** The Monday of the ISO week `iso` falls in. */
export function weekStart(iso: string): string {
  const weekday = utc(iso).getUTCDay() || 7
  return addDays(iso, 1 - weekday)
}
