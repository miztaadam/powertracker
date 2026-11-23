import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { resolveCatalogue } from '../../db/catalogue'
import { updateSettings, type GritDb } from '../../db/db'
import { goBack, navigate, useRoute } from '../../router'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
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

/** `seed`, with a strict press top set in both sessions. */
async function seedWithPress() {
  // A new install has no strict press row until the lift is first used.
  await resolveCatalogue(db, 'Strict press')
  await log(addDays(TODAY, -9), 92, ['Squat', 140, 5, 8], ['Bench', 100, 5, 8], ['Deadlift', 180, 5, 8], ['Strict press', 60, 5, 8])
  await log(addDays(TODAY, -2), 92.5, ['Squat', 145, 5, 8], ['Bench', 100, 5, 8], ['Deadlift', 185, 5, 8], ['Strict press', 62.5, 5, 8])
}

const showStrictPress = (on: boolean) => updateSettings(db, (s) => ({ ...s, showStrictPress: on }))

/** How often `text` is on the screen, in any case, as text or as a spoken name. */
function count(text: string): number {
  const needle = text.toLowerCase()
  const named = Array.from(document.body.querySelectorAll('[aria-label]')).filter((el) => el.getAttribute('aria-label')!.toLowerCase().includes(needle)).length
  return screen.queryAllByText((content) => content.toLowerCase().includes(needle)).length + named
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
    // The total always says what it adds up.
    expect(screen.getByText('Total (S+B+D)')).toBeInTheDocument()
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

  it('charts_fillInOnProgress_andDrawAgainOnlyForANewRange', async () => {
    await seed()
    await show()
    const { container } = { container: document.body }
    // The total sparkline, the trend and bodyweight lines, every volume strip and every ratio marker fill in.
    expect(container.querySelector('.progress-total-spark')).toHaveClass('gr-fill-in')
    expect(container.querySelectorAll('.gr-linechart.gr-fill-in')).toHaveLength(2)
    expect(container.querySelectorAll('.gr-bars.gr-fill-in')).toHaveLength(3)
    expect(container.querySelectorAll('.gr-ratio.gr-fill-in')).toHaveLength(2)
    expect(container.querySelectorAll('.gr-fill-in')).toHaveLength(8)
    const trend = () => screen.getByText('Weekly best e1RM').parentElement!.querySelector('.gr-linechart')
    const before = trend()
    const bars = container.querySelector('.gr-bars')

    // New data while the screen is open (a sync, a set logged) keeps the same charts: nothing draws again.
    await log(addDays(TODAY, -1), 93, ['Squat', 150, 5, 8])
    await screen.findByText('Squat volume this week: 1,475 kg')
    expect(trend()).toBe(before)
    expect(container.querySelector('.gr-bars')).toBe(bars)

    // A new range is a new line, so it draws in again.
    await userEvent.click(screen.getByRole('radio', { name: '1 yr' }))
    await waitFor(() => expect(trend()).not.toBe(before))
    expect(trend()).toHaveClass('gr-fill-in')
    expect(container.querySelector('.gr-bars')).toBe(bars)
  })

  it('emptyState_showsHint', async () => {
    await show('No lifts logged yet')

    expect(screen.getByText('No lifts logged yet')).toBeInTheDocument()
    expect(screen.getByText('Log a squat, bench or deadlift session on Today and your charts will appear here.')).toBeInTheDocument()
    expect(screen.queryByText('Weekly best e1RM')).toBeNull()
    expect(screen.queryByText('Lift ratios')).toBeNull()
  })

  it('infoButton_opensGuide_fromEveryPlace', async () => {
    // The history screens are named as the catalogue names the lifts, so each header opens that lift's guide.
    for (const lift of ['SQUAT', 'BENCH', 'DEADLIFT'] as const) {
      const name = lift[0] + lift.slice(1).toLowerCase()
      const { unmount } = render(<LiftHistoryScreen db={db} lift={lift} onBack={() => {}} today={() => TODAY} />)
      await screen.findByRole('heading', { level: 1, name: `${name} history` })
      await checkInfoButtonOpensGuide(name)
      expect(screen.getByRole('heading', { level: 1, name: `${name} history` })).toBeInTheDocument()
      unmount()
    }
  })

  it('liftHistory_emptyState', async () => {
    window.location.hash = '#/lift/DEADLIFT'
    await show('No deadlift sessions yet')

    expect(screen.getByRole('heading', { level: 1, name: 'Deadlift history' })).toBeInTheDocument()
    expect(screen.getByText('0 sessions')).toBeInTheDocument()
    expect(screen.getByText('Every set you log for this lift and its variations will be listed here.')).toBeInTheDocument()
  })

  // ---- Strict press behind its setting (strict press spec §5.2) ----

  it('switchOff_progressUnchanged', async () => {
    await seedWithPress()
    await show()

    // Logged strict press sets show nowhere: not as a tile, a line, a record or a volume row.
    expect(count('Strict press')).toBe(0)
    expect(screen.getByText('Total (S+B+D)')).toBeInTheDocument()
    expect(count('S+B+D+P')).toBe(0)
    for (const lift of ['squat', 'bench', 'deadlift']) expect(screen.getByRole('button', { name: new RegExp(`open ${lift} history`, 'i') })).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /history/i })).toHaveLength(3)
  })

  it('switchOn_showsStrictPressEverywhere', async () => {
    await seedWithPress()
    await showStrictPress(true)
    await show()

    // The tile, the chart legend, the record and the volume row.
    const tile = screen.getByRole('button', { name: /strict press, 77 kg.*open strict press history/i })
    expect(screen.getByRole('img', { name: /Line chart. Latest: Squat .* kg, Bench .* kg, Deadlift .* kg, Strict press 77 kg/ })).toBeInTheDocument()
    expect(screen.getByText('Strict press record: estimated 1RM 77 kg, from 62.5 × 5 @ 8 on 9 Nov')).toBeInTheDocument()
    expect(screen.getByText('Strict press volume this week: 313 kg')).toBeInTheDocument()
    // It is in no ratio.
    expect(screen.queryByText(/Strict press ÷|÷ Strict press/)).toBeNull()

    // Its tile opens its own history page.
    await userEvent.click(tile)
    expect(await screen.findByRole('heading', { level: 1, name: 'Strict press history' })).toBeInTheDocument()
    expect(window.location.hash).toBe('#/lift/STRICT_PRESS')
    expect(await screen.findByText('Set 1: 62.5 kg × 5 at RPE 8, personal record, done')).toBeInTheDocument()
    expect(screen.getByText('Set 1: 60 kg × 5 at RPE 8, personal record, done')).toBeInTheDocument()
    expect(screen.queryByText(/185 kg/)).toBeNull()
    // The header's info button opens the strict press guide.
    await checkInfoButtonOpensGuide('Strict press')
  })

  it('totalLabel_bothWays', async () => {
    await seedWithPress()
    await show()
    expect(screen.getByText('Total (S+B+D)')).toBeInTheDocument()

    // The label follows the switch while the screen is open.
    await showStrictPress(true)
    expect(await screen.findByText('Total (S+B+D+P)')).toBeInTheDocument()
    expect(screen.queryByText('Total (S+B+D)')).toBeNull()

    await showStrictPress(false)
    expect(await screen.findByText('Total (S+B+D)')).toBeInTheDocument()
    expect(count('S+B+D+P')).toBe(0)
  })

  it('switchOn_noPressData_totalExplains', async () => {
    await seed()
    await showStrictPress(true)
    await show()

    // No total without every included lift: the note says which lift it is waiting for.
    expect(screen.getByText('Total (S+B+D+P)')).toBeInTheDocument()
    expect(screen.getByText('Log all four lifts, strict press included, to see your total.')).toBeInTheDocument()
    // The empty lift has its tile and volume row, and nothing crashes on its empty series.
    const tile = screen.getByRole('button', { name: /strict press, –, open strict press history/i })
    expect(screen.getByText('Strict press volume this week: 0 kg')).toBeInTheDocument()
    expect(screen.queryByText(/Strict press record:/)).toBeNull()
    expect(screen.getByText('Squat ÷ Deadlift')).toBeInTheDocument()

    // Its history page opens empty.
    await userEvent.click(tile)
    expect(await screen.findByText('No strict press sessions yet')).toBeInTheDocument()

    // Switched off again, the three-lift total is back.
    await userEvent.click(screen.getByRole('button', { name: 'Back' }))
    await showStrictPress(false)
    await waitFor(() => expect(screen.queryByText(/Log all four lifts/)).toBeNull())
    expect(await screen.findByText('Total (S+B+D)')).toBeInTheDocument()
  })

  it('switchOff_noDeadliftData_keepsTheThreeLiftNote', async () => {
    await log(addDays(TODAY, -2), null, ['Squat', 145, 5, 8], ['Bench', 100, 5, 8])
    await show()

    expect(screen.getByText('Total (S+B+D)')).toBeInTheDocument()
    expect(screen.getByText('Log all three lifts to see your total.')).toBeInTheDocument()
  })

  it('emptyState_namesStrictPress_whenTheSwitchIsOn', async () => {
    await showStrictPress(true)
    await show('No lifts logged yet')

    expect(screen.getByText('Log a squat, bench, deadlift or strict press session on Today and your charts will appear here.')).toBeInTheDocument()
  })
})
