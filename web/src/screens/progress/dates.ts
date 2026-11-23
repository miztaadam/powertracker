/** Dates as the Progress screens write them. Month and day names follow the device's language, as on Android. */

const part = (iso: string, options: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat(undefined, { ...options, timeZone: 'UTC' }).format(new Date(`${iso}T00:00:00Z`))

const day = (iso: string) => Number(iso.slice(8, 10))
const month = (iso: string) => part(iso, { month: 'short' })
const year = (iso: string) => iso.slice(0, 4)

/** "9 Nov". */
export const formatDayMonth = (iso: string) => `${day(iso)} ${month(iso)}`

/** "9 Nov 2026". */
export const formatDate = (iso: string) => `${formatDayMonth(iso)} ${year(iso)}`

/** "Mon 9 Nov 2026". */
export const formatSessionDate = (iso: string) => `${part(iso, { weekday: 'short' })} ${formatDate(iso)}`

/** "Nov 26". */
export const formatMonthYear = (iso: string) => `${month(iso)} ${year(iso).slice(2)}`
