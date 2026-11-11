import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../../db/db'
import { goBack, navigate, useRoute } from '../../router'
import { TODAY, addDays, exerciseNamed, set, todayFixture } from '../../test/todayFixture'
import { LiftHistoryScreen } from './LiftHistoryScreen'
import { ProgressScreen } from './ProgressScreen'

// Ported from Android's ProgressScreenTest. TODAY is Wednesday 11 November 2026.

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

type Logged = [name: string, kg: number, reps: number, rpe: number, failed?: boolean]

async function log(date: string, bodyweightKg: number | null, ...sets: Logged[]) {
  const sessionId = (await db.sessions.add({ date, bodyweightKg, readiness: null })) as number
  for (const [name, kg, reps, rpe, failed = false] of sets) {
    await db.sets.add({ ...set((await exerciseNamed(db, name)).id, kg, { reps, rpe, failed }), sessionId })
  }
}

/** Two weeks of all three lifts; the second squat session beats the first. */
async function seed() {
  await log(addDays(TODAY, -9), 92, ['Squat', 140, 5, 8], ['Bench', 100, 5, 8], ['Deadlift', 180, 5, 8])
  await log(addDays(TODAY, -2), 92.5, ['Squat', 145, 5, 8], ['Squat', 150, 3, 10, true], ['Pause squat', 120, 3, 8], ['Bench', 100, 5, 8], ['Deadlift', 185, 5, 8])
}

/** Progress and lift history wired the way the app shell wires them. */
function Screens() {
  const route = useRoute()
  if ('page' in route && route.page === 'lift') return <LiftHistoryScreen db={db} lift={route.lift} onBack={() => goBack({ tab: 'progress' })} today={() => TODAY} />
  return <ProgressScreen db={db} onOpenLift={(lift) => navigate({ page: 'lift', lift })} today={() => TODAY} />
}

/** Renders the screens and returns once history has loaded. */
async function show(expected = 'Weekly best e1RM') {
  render(<Screens />)
  await screen.findByText(expected)
}

describe('ProgressScreen', () => {
  it('tapLift_opensHistory', async () => {
    await seed()
    await show()

    const entries = window.history.length
    await userEvent.click(screen.getByRole('button', { name: /squat history/i }))

    // The history lists both sessions, newest first, with main and variation sets.
    expect(await screen.findByRole('heading', { level: 1, name: 'Squat history' })).toBeInTheDocument()
    expect(await screen.findByText('2 sessions')).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)).toEqual(['Mon 9 Nov 2026', 'Mon 2 Nov 2026'])
    expect(screen.getByText('Set 1: 145 kg × 5 at RPE 8, personal record, done')).toBeInTheDocument()
    expect(screen.getByText('Set 2: 150 kg × 3 at RPE 10, failed')).toBeInTheDocument()
    expect(screen.getByText('Pause squat')).toBeInTheDocument()
    expect(screen.getByText('Set 1: 120 kg × 3 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('Set 1: 140 kg × 5 at RPE 8, personal record, done')).toBeInTheDocument()
    // PR badges on PR sets only.
    expect(screen.getAllByText('PR')).toHaveLength(2)
    // No other lift's sets leak in.
    expect(screen.queryByText(/185 kg/)).toBeNull()
    expect(screen.getByText('Current e1RM')).toBeInTheDocument()
    expect(screen.getByText('Best of the last 4 weeks')).toBeInTheDocument()
    expect(screen.getByText('Best e1RM')).toBeInTheDocument()
    expect(screen.getByText('9 Nov 2026')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Back' }))

    expect(await screen.findByText('Weekly best e1RM')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
    expect(window.location.hash).toBe('#/progress')
    // Back went one step back in history and added nothing: the lift page is the entry ahead, not behind.
    expect(window.history.length).toBe(entries + 1)
    window.history.forward()
    expect(await screen.findByRole('heading', { level: 1, name: 'Squat history' })).toBeInTheDocument()
  })

  it('back_fromADeepLinkTakesTheLiftPagesPlace', async () => {
    window.location.hash = '#/lift/DEADLIFT'
    const entries = window.history.length
    await show('No deadlift sessions yet')

    await userEvent.click(screen.getByRole('button', { name: 'Back' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
    expect(window.location.hash).toBe('#/progress')
    // Nothing was added, and the lift page is gone from history: going forward leads nowhere.
    expect(window.history.length).toBe(entries)
    window.history.forward()
    await new Promise((done) => setTimeout(done, 20))
    expect(window.location.hash).toBe('#/progress')
  })

  it('progress_showsTotalsPrsAndRatios', async () => {
    await seed()
    await show()

    expect(screen.getByText('Estimated 1RM')).toBeInTheDocument()
    expect(screen.getByText('Total')).toBeInTheDocument()
    expect(screen.getByText('Change over the last 4 weeks. Tap a lift for its history.')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '6 mo' })).toBeChecked()
    expect(screen.getByRole('heading', { name: 'Records' })).toBeInTheDocument()
    // 145 × 5 @ 8 is an e1RM of 178.8, shown to the half kilo.
    expect(screen.getByText('Squat record: estimated 1RM 179 kg, from 145 × 5 @ 8 on 9 Nov')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Weekly volume' })).toBeInTheDocument()
    expect(screen.getByText('Squat volume this week: 725 kg')).toBeInTheDocument()
    expect(screen.getByText('Week of 21 Sep')).toBeInTheDocument()
    expect(screen.getByText('Weight × reps of completed sets, last 8 weeks.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Lift ratios' })).toBeInTheDocument()
    expect(screen.getByText('Squat ÷ Deadlift')).toBeInTheDocument()
    expect(screen.getByText('Bench ÷ Squat')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Bodyweight' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Line chart. Latest: Latest 92.5 kg' })).toBeInTheDocument()
  })

  it('ratio_hiddenWhenALiftHasNoE1rm', async () => {
    await log(addDays(TODAY, -2), null, ['Squat', 145, 5, 8], ['Bench', 100, 5, 8])
    await show()

    expect(screen.getByText('Bench ÷ Squat')).toBeInTheDocument()
    expect(screen.queryByText('Squat ÷ Deadlift')).toBeNull()
    expect(screen.queryByText('Bodyweight')).toBeNull()
    expect(screen.getByText('Log all three lifts to see your total.')).toBeInTheDocument()
    // A lift without an estimate still opens its (empty) history.
    expect(screen.getByRole('button', { name: /deadlift, –, open deadlift history/i })).toBeInTheDocument()
  })

  it('rangeWithNoSessions_keepsTheSelectorAndSaysSo', async () => {
    await log(addDays(TODAY, -70), null, ['Squat', 140, 5, 8])
    await show()

    await userEvent.click(screen.getByRole('radio', { name: '8 wk' }))
    expect(await screen.findByText('No sessions in this range. Try a longer one.')).toBeInTheDocument()

    // The selector is still there, so the lifter can get back to a range with data.
    await userEvent.click(screen.getByRole('radio', { name: 'All' }))
    await waitFor(() => expect(screen.queryByText(/No sessions in this range/)).toBeNull())
  })

  it('emptyState_showsHint', async () => {
    await show('No lifts logged yet')

    expect(screen.getByText('No lifts logged yet')).toBeInTheDocument()
    expect(screen.getByText('Log a squat, bench or deadlift session on Today and your charts will appear here.')).toBeInTheDocument()
    expect(screen.queryByText('Weekly best e1RM')).toBeNull()
    expect(screen.queryByText('Lift ratios')).toBeNull()
  })

  it('liftHistory_emptyState', async () => {
    window.location.hash = '#/lift/DEADLIFT'
    await show('No deadlift sessions yet')

    expect(screen.getByRole('heading', { level: 1, name: 'Deadlift history' })).toBeInTheDocument()
    expect(screen.getByText('0 sessions')).toBeInTheDocument()
    expect(screen.getByText('Every set you log for this lift and its variations will be listed here.')).toBeInTheDocument()
  })
})
