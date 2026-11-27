import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { updateSettings, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
import { goBack, navigate, useRoute } from '../../router'
import type { Workout } from '../../state/useWorkoutHistory'
import { TODAY, addDays, exerciseNamed, set, todayFixture } from '../../test/todayFixture'
import { ProgressScreen } from './ProgressScreen'
import { calendarSummary, monthGrid, monthTitle, shiftMonth } from './WorkoutCalendar'
import { WORKOUTS_PER_PAGE, WorkoutHistoryContent, WorkoutHistoryScreen, workoutSummary } from './WorkoutHistoryScreen'

// 1.19: every past workout, opened from the History button at the top right of Progress. TODAY is Wednesday 11 November 2026.

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  window.location.hash = '#/progress'
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes: Vitest runs this before the cleanup registered in the test setup.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

type Logged = [name: string, kg: number, reps: number, rpe: number | null, failed?: boolean]

async function log(date: string, mode: TrainingMode, bodyweightKg: number | null, ...sets: Logged[]) {
  const sessionId = (await db.sessions.add({ date, bodyweightKg, readiness: null, mode })) as number
  for (const [name, kg, reps, rpe, failed = false] of sets) {
    await db.sets.add({ ...set((await exerciseNamed(db, name)).id, kg, { reps, rpe, failed }), sessionId })
  }
}

/** Progress and History wired the way the app shell wires them. */
function Screens() {
  const route = useRoute()
  if ('page' in route && route.page === 'history') return <WorkoutHistoryScreen db={db} onBack={() => goBack({ tab: 'progress' })} today={() => TODAY} />
  return <ProgressScreen db={db} onOpenLift={() => {}} onOpenHistory={() => navigate({ page: 'history' })} today={() => TODAY} />
}

/** What a screen reader reads for an element: its text, without what is hidden from it. */
const spoken = (el: Element): string =>
  Array.from(el.childNodes)
    .map((n) => (n.nodeType === Node.TEXT_NODE ? (n.textContent ?? '') : n instanceof Element && n.getAttribute('aria-hidden') !== 'true' ? spoken(n) : ''))
    .join('')

const cards = () => Array.from(document.querySelectorAll<HTMLElement>('.workout'))
const dates = () => screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)

describe('workout history', () => {
  it('historyButton_opensEveryWorkout_newestFirst_fromBothSides_andBackReturns', async () => {
    await log(addDays(TODAY, -9), 'POWERLIFTING', null, ['Squat', 140, 5, 8], ['Bench', 100, 5, 8])
    await log(addDays(TODAY, -5), 'BODYBUILDING', null, ['Curl', 20, 12, 8], ['Curl', 20, 11, null])
    // Squat, then a variation, then squat again: one Squat group, in the order first logged.
    await log(addDays(TODAY, -2), 'POWERLIFTING', 92.5, ['Squat', 145, 5, 8], ['Pause squat', 120, 3, 8], ['Squat', 150, 3, 10, true])
    // Started and nothing logged: no workout.
    await db.sessions.add({ date: TODAY, bodyweightKg: null, readiness: null })
    render(<Screens />)
    await screen.findByText('Weekly best e1RM')

    const entries = window.history.length
    await userEvent.click(screen.getByRole('button', { name: 'History' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'History' })).toBeInTheDocument()
    expect(await screen.findByText('3 workouts')).toBeInTheDocument()
    expect(window.location.hash).toBe('#/history')
    expect(dates()).toEqual(['Mon 9 Nov 2026', 'Fri 6 Nov 2026', 'Mon 2 Nov 2026'])
    const [latest, bodybuilding, first] = cards()
    expect(within(latest!).getByText('Powerlifting · 2 exercises · 3 sets · 92.5 kg bodyweight')).toHaveClass('eyebrow')
    expect(within(latest!).getAllByRole('heading', { level: 3 }).map((h) => h.textContent)).toEqual(['Squat', 'Pause squat'])
    expect(within(latest!).getAllByRole('listitem').map(spoken)).toEqual(['Set 1: 145 kg × 5 at RPE 8', 'Set 2: 150 kg × 3 at RPE 10, missed', 'Set 1: 120 kg × 3 at RPE 8'])
    expect(within(latest!).getByText('Missed')).toBeInTheDocument()
    expect(within(bodybuilding!).getByText('Bodybuilding · 1 exercise · 2 sets')).toBeInTheDocument()
    expect(within(bodybuilding!).getAllByRole('listitem').map(spoken)).toEqual(['Set 1: 20 kg × 12 at RPE 8', 'Set 2: 20 kg × 11'])
    expect(within(first!).getByText('Powerlifting · 2 exercises · 2 sets')).toBeInTheDocument()
    // Read-only: no workout can be tapped. The buttons are Back and the calendar's (1.22): its two arrows and a day for each workout of the month.
    expect(screen.getAllByRole('button').map((b) => b.getAttribute('aria-label') ?? b.textContent)).toEqual([
      'Back',
      'Previous month',
      'Next month',
      'Mon 2 Nov 2026: Powerlifting · 2 exercises · 2 sets. Show it',
      'Fri 6 Nov 2026: Bodybuilding · 1 exercise · 2 sets. Show it',
      'Mon 9 Nov 2026: Powerlifting · 2 exercises · 3 sets · 92.5 kg bodyweight. Show it',
    ])
    expect(cards().flatMap((card) => within(card).queryAllByRole('button'))).toEqual([])

    await userEvent.click(screen.getByRole('button', { name: 'Back' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
    expect(window.location.hash).toBe('#/progress')
    // Back went one step back in history and added nothing: History is the entry ahead, not behind.
    expect(window.history.length).toBe(entries + 1)
    window.history.forward()
    expect(await screen.findByRole('heading', { level: 1, name: 'History' })).toBeInTheDocument()
  })

  it('historyButton_isOnBothSides_alsoWithNothingLogged', async () => {
    const view = render(<Screens />)
    await screen.findByText('No lifts logged yet')
    expect(screen.getByRole('button', { name: 'History' })).toBeInTheDocument()
    view.unmount()

    await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING' }))
    render(<Screens />)
    await screen.findByText('No exercises in your plan yet')
    await userEvent.click(screen.getByRole('button', { name: 'History' }))

    expect(await screen.findByText('No workouts yet')).toBeInTheDocument()
    expect(screen.getByText('0 workouts')).toBeInTheDocument()
    expect(cards()).toEqual([])
    expect(screen.queryByRole('button', { name: 'Show older workouts' })).toBeNull()
  })

  it('back_fromADeepLinkTakesHistorysPlace', async () => {
    window.location.hash = '#/history'
    const entries = window.history.length
    render(<Screens />)
    await screen.findByText('No workouts yet')

    await userEvent.click(screen.getByRole('button', { name: 'Back' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
    expect(window.location.hash).toBe('#/progress')
    // Nothing was added, and History is gone from history: going forward leads nowhere.
    expect(window.history.length).toBe(entries)
    window.history.forward()
    await new Promise((done) => setTimeout(done, 20))
    expect(window.location.hash).toBe('#/progress')
  })

  it('aLongLog_isDrawnTwentyWorkoutsAtATime', async () => {
    expect(WORKOUTS_PER_PAGE).toBe(20)
    const workouts = Array.from({ length: 45 }, (_, i): Workout => ({
      sessionId: 45 - i,
      date: addDays(TODAY, -i),
      side: 'POWERLIFTING',
      bodyweightKg: null,
      exercises: [{ exerciseId: 1, name: 'Squat', sets: [{ weightKg: 100 + i, reps: 5, rpe: 8, failed: false }] }],
      setCount: 1,
    }))
    render(<WorkoutHistoryContent state={{ loading: false, workouts }} onBack={() => {}} today={TODAY} />)
    // The count is of the whole log, whatever is drawn.
    expect(screen.getByText('45 workouts')).toBeInTheDocument()
    expect(cards()).toHaveLength(20)
    expect(dates()[19]).toBe('Fri 23 Oct 2026')

    await userEvent.click(screen.getByRole('button', { name: 'Show older workouts' }))
    expect(cards()).toHaveLength(40)
    await userEvent.click(screen.getByRole('button', { name: 'Show older workouts' }))
    expect(cards()).toHaveLength(45)
    expect(dates().at(-1)).toBe('Mon 28 Sep 2026')
    expect(screen.queryByRole('button', { name: 'Show older workouts' })).toBeNull()
  })

  it('theSummary_countsOneOfEachInTheSingular', () => {
    const one: Workout = { sessionId: 1, date: TODAY, side: 'BODYBUILDING', bodyweightKg: null, exercises: [{ exerciseId: 1, name: 'Curl', sets: [{ weightKg: 20, reps: 12, rpe: 8, failed: false }] }], setCount: 1 }
    expect(workoutSummary(one)).toBe('Bodybuilding · 1 exercise · 1 set')
    render(<WorkoutHistoryContent state={{ loading: false, workouts: [one] }} onBack={() => {}} today={TODAY} />)
    expect(screen.getByText('1 workout')).toBeInTheDocument()
  })

  // ---- 1.22: the calendar over the list ----

  /** A workout on each of `dates`, one squat set each, newest first as History lists them. */
  const workoutsOn = (...dates: string[]): Workout[] =>
    [...dates]
      .sort()
      .reverse()
      .map((date, i, all) => ({ sessionId: all.length - i, date, side: 'POWERLIFTING' as const, bodyweightKg: null, exercises: [{ exerciseId: 1, name: 'Squat', sets: [{ weightKg: 100, reps: 5, rpe: 8, failed: false }] }], setCount: 1 }))
  const calendar = () => screen.getByRole('region', { name: 'Calendar' })
  const dayButtons = () => within(calendar()).getAllByRole('button').filter((b) => b.classList.contains('workout-day'))
  const arrow = (name: 'Previous month' | 'Next month') => within(calendar()).getByRole('button', { name })

  it('calendar_opensOnThisMonth_withADotOnEachWorkoutDay_aRingOnToday_andTheCounts', () => {
    // Wednesday 11 November: this week (Mon 9), last week (Tue 3, Fri 6) and the week before (Wed 28 Oct), then a gap.
    const workouts = workoutsOn('2026-11-09', '2026-11-06', '2026-11-03', '2026-10-28', '2026-10-07')
    render(<WorkoutHistoryContent state={{ loading: false, workouts }} onBack={() => {}} today={TODAY} />)
    expect(within(calendar()).getByText('November 2026')).toBeInTheDocument()
    // Not a heading: the headings on the screen are the workouts'.
    expect(within(calendar()).queryByRole('heading')).toBeNull()
    expect(dayButtons().map((b) => b.getAttribute('aria-label'))).toEqual([
      'Tue 3 Nov 2026: Powerlifting · 1 exercise · 1 set. Show it',
      'Fri 6 Nov 2026: Powerlifting · 1 exercise · 1 set. Show it',
      'Mon 9 Nov 2026: Powerlifting · 1 exercise · 1 set. Show it',
    ])
    expect(calendar().querySelectorAll('.workout-day-dot')).toHaveLength(3)
    // Thirty days, Monday to Sunday: 1 November 2026 is a Sunday, so six blanks lead.
    const days = Array.from(calendar().querySelectorAll('.workout-day'))
    expect(days.map((d) => d.textContent)).toEqual(Array.from({ length: 30 }, (_, i) => String(i + 1)))
    expect(Array.from(calendar().querySelectorAll('.workout-calendar-weekday')).map((d) => d.textContent)).toEqual(['M', 'T', 'W', 'T', 'F', 'S', 'S'])
    // Today is ringed, and is no button: nothing was logged on it.
    const today = calendar().querySelectorAll('.workout-day-today')
    expect(today).toHaveLength(1)
    expect(today[0]).toHaveTextContent('11')
    expect(today[0]!.tagName).toBe('SPAN')
    expect(within(calendar()).getByText('3 workouts in November · 3 weeks in a row')).toBeInTheDocument()
  })

  it('calendar_todayWithAWorkout_isAButtonWithTheRingAndTheDot', () => {
    // The usual sight: train, then open History.
    render(<WorkoutHistoryContent state={{ loading: false, workouts: workoutsOn(TODAY, '2026-11-09') }} onBack={() => {}} today={TODAY} />)
    const today = calendar().querySelectorAll('.workout-day-today')
    expect(today).toHaveLength(1)
    expect(today[0]!.tagName).toBe('BUTTON')
    expect(today[0]).toHaveClass('workout-day-trained')
    expect(today[0]).toHaveAccessibleName('Wed 11 Nov 2026: Powerlifting · 1 exercise · 1 set. Show it')
    expect(today[0]!.querySelector('.workout-day-number')).toHaveTextContent('11')
    expect(today[0]!.querySelectorAll('.workout-day-dot')).toHaveLength(1)
    expect(within(calendar()).getByText('2 workouts in November · 1 week in a row')).toBeInTheDocument()
  })

  it('calendar_arrows_goBackToTheFirstWorkoutsMonth_andForwardToThisMonth_only', async () => {
    const workouts = workoutsOn('2026-11-09', '2026-10-28', '2026-09-30')
    render(<WorkoutHistoryContent state={{ loading: false, workouts }} onBack={() => {}} today={TODAY} />)
    expect(arrow('Next month')).toBeDisabled()
    expect(arrow('Previous month')).toBeEnabled()

    await userEvent.click(arrow('Previous month'))
    expect(within(calendar()).getByText('October 2026')).toBeInTheDocument()
    // The counts are the month on screen's; the weeks in a row are counted from today whatever month shows.
    // This week (Mon 9), not last week: a run of one.
    expect(within(calendar()).getByText('1 workout in October · 1 week in a row')).toBeInTheDocument()
    expect(dayButtons().map((b) => b.getAttribute('aria-label'))).toEqual(['Wed 28 Oct 2026: Powerlifting · 1 exercise · 1 set. Show it'])
    // No day is ringed in a month that is not this one.
    expect(calendar().querySelectorAll('.workout-day-today')).toHaveLength(0)
    expect(arrow('Next month')).toBeEnabled()

    await userEvent.click(arrow('Previous month'))
    expect(within(calendar()).getByText('September 2026')).toBeInTheDocument()
    expect(arrow('Previous month')).toBeDisabled()
    await userEvent.click(arrow('Next month'))
    await userEvent.click(arrow('Next month'))
    expect(within(calendar()).getByText('November 2026')).toBeInTheDocument()
    expect(arrow('Next month')).toBeDisabled()
  })

  it('calendar_noRunOfWeeks_saysOnlyTheMonthsCount', () => {
    // The last workout was three weeks ago: neither this week nor last has one.
    render(<WorkoutHistoryContent state={{ loading: false, workouts: workoutsOn('2026-10-20') }} onBack={() => {}} today={TODAY} />)
    expect(within(calendar()).getByText('0 workouts in November')).toBeInTheDocument()
    expect(calendarSummary(1, '2026-11', 0)).toBe('1 workout in November')
    expect(calendarSummary(12, '2026-01', 1)).toBe('12 workouts in January · 1 week in a row')
  })

  it('calendar_tapADay_bringsItsWorkoutUp_alsoOneFarDownTheLog', async () => {
    const scrolled: Element[] = []
    const real = Element.prototype.scrollIntoView
    // jsdom has no layout: note what is asked to come into view.
    Element.prototype.scrollIntoView = function (this: Element) {
      scrolled.push(this)
    }
    try {
      // Sixty days in a row back from yesterday: 10 in November, 31 in October, 19 in September.
      const workouts = workoutsOn(...Array.from({ length: 60 }, (_, i) => addDays(TODAY, -(i + 1))))
      render(<WorkoutHistoryContent state={{ loading: false, workouts }} onBack={() => {}} today={TODAY} />)
      expect(cards()).toHaveLength(20)

      // A day of this month, already drawn: its card comes to the top and its date takes the focus.
      await userEvent.click(within(calendar()).getByRole('button', { name: /^Thu 5 Nov 2026/ }))
      const fifth = screen.getByRole('heading', { level: 2, name: 'Thu 5 Nov 2026' })
      expect(fifth).toHaveFocus()
      expect(scrolled.at(-1)).toBe(fifth.closest('.workout'))
      expect(cards()).toHaveLength(20)

      // Two months back, the 50th workout down: the pages down to it are drawn, then it comes up.
      await userEvent.click(arrow('Previous month'))
      await userEvent.click(arrow('Previous month'))
      await userEvent.click(within(calendar()).getByRole('button', { name: /^Tue 22 Sep 2026/ }))
      const far = await screen.findByRole('heading', { level: 2, name: 'Tue 22 Sep 2026' })
      await waitFor(() => expect(far).toHaveFocus())
      expect(scrolled.at(-1)).toBe(far.closest('.workout'))
      expect(cards()).toHaveLength(60)
      // The same day again: brought up again.
      const before = scrolled.length
      await userEvent.click(within(calendar()).getByRole('button', { name: /^Tue 22 Sep 2026/ }))
      await waitFor(() => expect(scrolled.length).toBe(before + 1))
    } finally {
      Element.prototype.scrollIntoView = real
    }
  })

  it('calendar_monthMaths', () => {
    // November 2026 starts on a Sunday and has 30 days: six blanks, then 30 days, to 36 cells and a full last row of 42.
    const november = monthGrid('2026-11')
    expect(november).toHaveLength(42)
    expect(november.slice(0, 7)).toEqual([null, null, null, null, null, null, '2026-11-01'])
    expect(november[35]).toBe('2026-11-30')
    expect(november.slice(36)).toEqual([null, null, null, null, null, null])
    // February 2027 starts on a Monday and has 28 days: exactly four rows.
    expect(monthGrid('2027-02')).toEqual(Array.from({ length: 28 }, (_, i) => `2027-02-${String(i + 1).padStart(2, '0')}`))
    // A leap February.
    expect(monthGrid('2028-02').filter((d) => d != null)).toHaveLength(29)
    expect(shiftMonth('2026-12', 1)).toBe('2027-01')
    expect(shiftMonth('2027-01', -1)).toBe('2026-12')
    expect(shiftMonth('2026-11', -1)).toBe('2026-10')
    expect(monthTitle('2027-01')).toBe('January 2027')
  })

  it('whileLoading_showsNeitherACountNorTheEmptyState', () => {
    render(<WorkoutHistoryContent state={{ loading: true, workouts: [] }} onBack={() => {}} today={TODAY} />)
    expect(screen.getByRole('heading', { level: 1, name: 'History' })).toBeInTheDocument()
    expect(screen.queryByText('No workouts yet')).toBeNull()
    expect(screen.queryByText(/workouts?$/)).toBeNull()
    // Nothing to mark yet: no calendar, as with an empty log.
    expect(screen.queryByRole('region', { name: 'Calendar' })).toBeNull()
  })
})
