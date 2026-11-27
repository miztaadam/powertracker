import { useMemo, useState } from 'react'
import { grit } from '../../domain'
import type { Workout } from '../../state/useWorkoutHistory'
import { IconButton } from '../../ui/components/Button'
import { cx } from '../../ui/components/format'
import { ChevronLeftIcon, ChevronRightIcon } from '../../ui/components/icons'
import { formatSessionDate } from './dates'
import { workoutSummary } from './workoutSummary'
import './progress.css'

/** "2026-11" of an ISO date. */
const monthOf = (iso: string) => iso.slice(0, 7)

/** The month before (-1) or after (+1) `month` ("2026-11"), on the calendar: December follows November, 2027-01 follows 2026-12. */
export function shiftMonth(month: string, by: -1 | 1): string {
  const [year, m] = month.split('-').map(Number) as [number, number]
  const index = year * 12 + (m - 1) + by
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`
}

/**
 * The days of `month` ("2026-11") as ISO dates, in rows of seven from Monday to Sunday, with null before the 1st and
 * after the last day. Worked out in UTC, as `dates.ts` does: a local date turned into text shifts a day in some time zones.
 */
export function monthGrid(month: string): (string | null)[] {
  const [year, m] = month.split('-').map(Number) as [number, number]
  const days = new Date(Date.UTC(year, m, 0)).getUTCDate()
  // 0 = Monday … 6 = Sunday for the 1st.
  const lead = (new Date(Date.UTC(year, m - 1, 1)).getUTCDay() + 6) % 7
  const cells: (string | null)[] = Array.from({ length: lead }, () => null)
  for (let d = 1; d <= days; d++) cells.push(`${month}-${String(d).padStart(2, '0')}`)
  while (cells.length % 7 !== 0) cells.push(null)
  return cells
}

const part = (month: string, options: Intl.DateTimeFormatOptions) => new Intl.DateTimeFormat(undefined, { ...options, timeZone: 'UTC' }).format(new Date(`${month}-01T00:00:00Z`))

/** "November 2026". */
export const monthTitle = (month: string) => `${part(month, { month: 'long' })} ${month.slice(0, 4)}`

/** "M", "T", "W" …: one letter over each column, so the names fit seven columns on any phone. 1 = Monday. */
const weekdayLetter = (weekday: number) => new Intl.DateTimeFormat(undefined, { weekday: 'narrow', timeZone: 'UTC' }).format(new Date(Date.UTC(2026, 0, 4 + weekday)))

const count = (n: number, one: string) => `${n} ${n === 1 ? one : `${one}s`}`

/** "9 workouts in November", and " · 6 weeks in a row" while there is a run of weeks. */
export function calendarSummary(inMonth: number, month: string, weeks: number): string {
  const text = `${count(inMonth, 'workout')} in ${part(month, { month: 'long' })}`
  return weeks > 0 ? `${text} · ${count(weeks, 'week')} in a row` : text
}

export type WorkoutCalendarProps = {
  /** Every workout History lists, newest first. */
  workouts: readonly Workout[]
  /** The day it is, `YYYY-MM-DD`: its month opens first, it is ringed, and the weeks in a row are counted back from it. */
  today: string
  /** A day with a workout was tapped: History shows that workout. */
  onOpen: (workout: Workout) => void
}

/**
 * History's calendar (1.22): one month at a time, Monday to Sunday, a dot on every day with a workout and a ring on
 * today. A day with a workout is a button that brings its workout up in the list below; the other days are just
 * numbers. The arrows go back as far as the month of the first workout and forward to this month. Under the grid:
 * how many workouts the month on screen has, and how many weeks in a row had one (the domain's `weeksInARow`).
 */
export function WorkoutCalendar({ workouts, today, onOpen }: WorkoutCalendarProps) {
  const [month, setMonth] = useState(monthOf(today))
  const byDay = useMemo(() => new Map(workouts.map((w) => [w.date, w])), [workouts])
  const weeks = useMemo(() => grit.weeksInARow(workouts.map((w) => w.date), today), [workouts, today])
  // The oldest workout's month, and never after this month (a workout dated ahead does not open months to come).
  const first = workouts.reduce((min, w) => (monthOf(w.date) < min ? monthOf(w.date) : min), monthOf(today))
  const last = monthOf(today)
  const cells = monthGrid(month)
  const inMonth = workouts.filter((w) => monthOf(w.date) === month).length
  return (
    <section className="workout-calendar" aria-label="Calendar">
      <div className="workout-calendar-head">
        <IconButton label="Previous month" icon={<ChevronLeftIcon />} enabled={month > first} onClick={() => setMonth((m) => shiftMonth(m, -1))} />
        {/* Not a heading: the workouts below are the screen's headings. Said again when the arrows change it. */}
        <p className="workout-calendar-month title-medium" aria-live="polite">
          {monthTitle(month)}
        </p>
        <IconButton label="Next month" icon={<ChevronRightIcon />} enabled={month < last} onClick={() => setMonth((m) => shiftMonth(m, 1))} />
      </div>
      <div className="workout-calendar-grid">
        {[1, 2, 3, 4, 5, 6, 7].map((weekday) => (
          <span key={`name-${weekday}`} className="workout-calendar-weekday eyebrow" aria-hidden="true">
            {weekdayLetter(weekday)}
          </span>
        ))}
        {cells.map((day, i) => {
          if (day == null) return <span key={`blank-${i}`} aria-hidden="true" />
          const workout = byDay.get(day)
          const classes = cx('workout-day body-large', day === today && 'workout-day-today', workout != null && 'workout-day-trained')
          const number = Number(day.slice(8))
          return workout != null ? (
            <button key={day} type="button" className={classes} aria-label={`${formatSessionDate(day)}: ${workoutSummary(workout)}. Show it`} onClick={() => onOpen(workout)}>
              <span className="workout-day-number">{number}</span>
              <span className="workout-day-dot" />
            </button>
          ) : (
            // A day with nothing to open is not a control: its number is for the eye, to place the others.
            <span key={day} className={classes} aria-hidden="true">
              <span className="workout-day-number">{number}</span>
            </span>
          )
        })}
      </div>
      <p className="workout-calendar-summary body-medium">{calendarSummary(inMonth, month, weeks)}</p>
    </section>
  )
}
