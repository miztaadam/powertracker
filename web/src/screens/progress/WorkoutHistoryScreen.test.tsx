import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { updateSettings, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
import { goBack, navigate, useRoute } from '../../router'
import type { Workout } from '../../state/useWorkoutHistory'
import { TODAY, addDays, exerciseNamed, set, todayFixture } from '../../test/todayFixture'
import { ProgressScreen } from './ProgressScreen'
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
  if ('page' in route && route.page === 'history') return <WorkoutHistoryScreen db={db} onBack={() => goBack({ tab: 'progress' })} />
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
    // Read-only: Back is the screen's one button.
    expect(screen.getAllByRole('button').map((b) => b.getAttribute('aria-label') ?? b.textContent)).toEqual(['Back'])

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
    render(<WorkoutHistoryContent state={{ loading: false, workouts }} onBack={() => {}} />)
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
    render(<WorkoutHistoryContent state={{ loading: false, workouts: [one] }} onBack={() => {}} />)
    expect(screen.getByText('1 workout')).toBeInTheDocument()
  })

  it('whileLoading_showsNeitherACountNorTheEmptyState', () => {
    render(<WorkoutHistoryContent state={{ loading: true, workouts: [] }} onBack={() => {}} />)
    expect(screen.getByRole('heading', { level: 1, name: 'History' })).toBeInTheDocument()
    expect(screen.queryByText('No workouts yet')).toBeNull()
    expect(screen.queryByText(/workouts?$/)).toBeNull()
  })
})
