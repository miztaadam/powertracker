import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { skip as storeSkip, skips as storedSkips } from '../../db/skips'
import { setTapHaptics } from '../../platform/haptics'
import { pastTheTapWindow } from '../../test/tapWindow'
import { TODAY, addDays, exerciseNamed, logSet, set, startSession, todayFixture } from '../../test/todayFixture'
import { grit, JsExercise } from '../../domain'
import { CoachScreen } from '../coach/CoachScreen'
import type { TodayItem } from '../../state/useToday'
import { StartSessionCard } from './SessionCards'
import { TodayScreen } from './TodayScreen'

/** 1.13: skipping a planned exercise on Today: the row menu, the sheet, Undo, Bring back (as Android's `SkipTodayScreenTest`). */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squat: number
let bench: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squat = (await exerciseNamed(db, 'Squat')).id
  bench = (await exerciseNamed(db, 'Bench')).id
  // Planned today (a Wednesday): squat, then bench.
  await db.planItems.add({ weekday: 3, exerciseId: squat, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
  await db.planItems.add({ weekday: 3, exerciseId: bench, sets: 3, reps: 5, targetRpe: 8, order: 1, mode: 'POWERLIFTING' })
})
afterEach(async () => {
  vi.useRealTimers()
  cleanup()
  await fx.dispose()
})

const show = (today = TODAY) => render(<TodayScreen db={db} today={() => today} onOpenCoach={() => {}} />)
const menu = (name: string) => screen.queryByRole('button', { name: `Options for ${name}` })
const sheet = () => screen.queryByRole('dialog', { name: /^Why skip / })
/** Opens `name`'s row menu and taps Skip today: the reason sheet opens. */
async function openSheet(name: string) {
  await userEvent.click(await screen.findByRole('button', { name: `Options for ${name}` }))
  await userEvent.click(screen.getByRole('menuitem', { name: 'Skip today' }))
  return within(await screen.findByRole('dialog', { name: `Why skip ${name}?` }))
}
/** Opens the sheet for `name` and, once taps are taken, chooses `answer`. */
async function skipWith(name: string, answer: string) {
  const s = await openSheet(name)
  pastTheTapWindow()
  await userEvent.click(s.getByRole('button', { name: answer }))
}
const skippedLine = () => screen.queryByRole('region', { name: 'Skipped today' })

describe('Skip today', () => {
  it('skip_menuOnlyOnPlannedRowWithNoSets', async () => {
    await logSet(db, await startSession(db, TODAY), set(squat, 100))
    show()
    expect(await screen.findByRole('button', { name: 'Options for Bench' })).toBeInTheDocument()
    // Squat has a set: the sets count, and there is no Skip today.
    expect(menu('Squat')).toBeNull()
    // An exercise added for the session is taken back with Remove instead.
    await userEvent.click(screen.getByRole('button', { name: 'Add exercise' }))
    const picker = within(screen.getByRole('dialog', { name: 'Add exercise' }))
    await userEvent.type(picker.getByRole('textbox', { name: 'Search exercises' }), 'dead')
    await userEvent.click(within(await picker.findByRole('listitem', { name: 'Deadlift' })).getAllByRole('button')[0]!)
    expect(await screen.findByRole('button', { name: 'Remove' })).toBeInTheDocument()
    expect(menu('Deadlift')).toBeNull()
    expect(menu('Bench')).not.toBeNull()
  })

  it('skip_menuOnTheStartCard_beforeASession', async () => {
    show()
    expect(await screen.findByText('Wednesday · 2 exercises')).toBeInTheDocument()
    expect(menu('Squat')).not.toBeNull()
    expect(menu('Bench')).not.toBeNull()
  })

  it('skip_sheetCancel_storesNothing', async () => {
    show()
    await openSheet('Bench')
    pastTheTapWindow()
    await userEvent.keyboard('{Escape}')
    await waitFor(() => expect(sheet()).toBeNull())
    // A tap outside cancels too.
    await openSheet('Bench')
    pastTheTapWindow()
    await userEvent.click(screen.getByRole('dialog', { name: 'Why skip Bench?' }).parentElement!)
    await waitFor(() => expect(sheet()).toBeNull())
    await new Promise((done) => setTimeout(done, 50))
    expect(await storedSkips(db)).toEqual([])
    expect(menu('Bench')).not.toBeNull()
  })

  it('skip_sheet_hasTheFourAnswers', async () => {
    show()
    const s = await openSheet('Bench')
    expect(s.getAllByRole('button').map((b) => b.textContent)).toEqual(['Busy or no equipment', 'Pain', 'Out of time', 'Just skip'])
  })

  it('skip_withoutSession_rowLeaves_snackbarAndSkippedLine', async () => {
    show()
    await skipWith('Bench', 'Out of time')
    expect(await screen.findByText('Bench skipped.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Undo' })).toBeInTheDocument()
    await waitFor(() => expect(menu('Bench')).toBeNull())
    expect(within(skippedLine()!).getByText('Bench')).toBeInTheDocument()
    expect(screen.getByText('Wednesday · 1 exercise')).toBeInTheDocument()
    expect((await storedSkips(db)).map((s) => [s.exerciseId, s.reason])).toEqual([[bench, 'TIME']])
    expect(await db.sessions.count()).toBe(0)
  })

  it('skip_undo_onTheSnackbar_bringsTheRowBack', async () => {
    show()
    await skipWith('Bench', 'Pain')
    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }))
    await waitFor(() => expect(menu('Bench')).not.toBeNull())
    expect(skippedLine()).toBeNull()
    expect(await storedSkips(db)).toEqual([])
    // Bench's pain card went with it.
    expect(await db.advice.where('type').equals('SKIP').count()).toBe(0)
  })

  it('skip_bringBack_onTheSkippedLine', async () => {
    await startSession(db, TODAY)
    show()
    await skipWith('Squat', 'Busy or no equipment')
    await waitFor(() => expect(skippedLine()).not.toBeNull())
    await userEvent.click(within(skippedLine()!).getByRole('button', { name: 'Bring back Squat' }))
    await waitFor(() => expect(skippedLine()).toBeNull())
    // Back in its place, first.
    expect(screen.getByRole('heading', { level: 2, name: 'Squat' })).toBeInTheDocument()
    expect(await storedSkips(db)).toEqual([])
  })

  it('doubleTapSkipToday_doesNotPickAReason', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    show()
    const s = await openSheet('Bench')
    // The second tap of a double tap lands on an answer just after the sheet opened: nothing happens.
    await userEvent.click(s.getByRole('button', { name: 'Pain' }))
    expect(sheet()).not.toBeNull()
    vi.setSystemTime(Date.now() + 299)
    await userEvent.click(s.getByRole('button', { name: 'Just skip' }))
    expect(sheet()).not.toBeNull()
    await new Promise((done) => setTimeout(done, 50))
    expect(await storedSkips(db)).toEqual([])
    // The keyboard is not held; nor are taps once the moment has passed.
    vi.setSystemTime(Date.now() + 1)
    await userEvent.click(s.getByRole('button', { name: 'Out of time' }))
    await waitFor(async () => expect((await storedSkips(db)).map((x) => x.reason)).toEqual(['TIME']))
    expect(sheet()).toBeNull()
  })

  it('aTapOutside_duringTheMoment_cancelsAtOnce', async () => {
    // Only the answers are held, as on Android: a tap outside the sheet just after it opened cancels it, storing nothing.
    vi.useFakeTimers({ toFake: ['Date'] })
    show()
    await openSheet('Bench')
    await userEvent.click(screen.getByRole('dialog', { name: 'Why skip Bench?' }).parentElement!)
    expect(sheet()).toBeNull()
    await new Promise((done) => setTimeout(done, 50))
    expect(await storedSkips(db)).toEqual([])
    expect(menu('Bench')).not.toBeNull()
  })

  it('doubleTapSkipToday_keyboardIsNotHeld', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    show()
    const s = await openSheet('Bench')
    s.getByRole('button', { name: 'Just skip' }).focus()
    await userEvent.keyboard('{Enter}')
    await waitFor(async () => expect((await storedSkips(db)).map((x) => x.reason)).toEqual([null]))
  })

  it('skipEveryPlannedExercise_noRestDayView_skippedLineShown', async () => {
    show()
    await skipWith('Squat', 'Just skip')
    await waitFor(() => expect(menu('Squat')).toBeNull())
    await skipWith('Bench', 'Just skip')
    await waitFor(() => expect(within(skippedLine()!).getAllByRole('listitem')).toHaveLength(2))

    // Before a session: not the rest day's "Nothing planned".
    expect(screen.queryByText('Nothing planned')).toBeNull()
    expect(screen.queryByText(/There is nothing in your plan/)).toBeNull()
    // 1.13.1: with none left, the eyebrow counts the skipped, not "0 exercises".
    expect(screen.getByText('Wednesday · 2 skipped')).toBeInTheDocument()
    expect(screen.queryByText('Wednesday · 0 exercises')).toBeNull()
    expect(within(skippedLine()!).getAllByRole('button', { name: /^Bring back / })).toHaveLength(2)

    // In the session too, the line stays.
    await userEvent.click(screen.getByRole('button', { name: 'Start session' }))
    expect(await screen.findByRole('button', { name: 'Add exercise' })).toBeInTheDocument()
    expect(within(skippedLine()!).getAllByRole('button', { name: /^Bring back / })).toHaveLength(2)
  })

  /** Both planned exercises skipped before a session. */
  async function skipBoth() {
    show()
    await skipWith('Squat', 'Just skip')
    await waitFor(() => expect(menu('Squat')).toBeNull())
    await skipWith('Bench', 'Just skip')
    expect(await screen.findByText('Wednesday · 2 skipped')).toBeInTheDocument()
  }

  it('startCard_oneLeft_keepsTheExerciseCount', async () => {
    show()
    await skipWith('Bench', 'Just skip')
    await waitFor(() => expect(menu('Bench')).toBeNull())
    expect(screen.getByText('Wednesday · 1 exercise')).toBeInTheDocument()
    expect(screen.queryByText(/ skipped$/)).toBeNull()
  })

  it('startCard_allSkipped_undo_restoresTheCount', async () => {
    await skipBoth()
    expect(await screen.findByText('Bench skipped.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Undo' }))
    await waitFor(() => expect(menu('Bench')).not.toBeNull())
    expect(screen.getByText('Wednesday · 1 exercise')).toBeInTheDocument()
    expect(screen.queryByText(/ skipped$/)).toBeNull()
    expect((await storedSkips(db)).map((s) => s.exerciseId)).toEqual([squat])
  })

  it('startCard_allSkipped_bringBack_restoresTheCount', async () => {
    await skipBoth()
    await userEvent.click(within(skippedLine()!).getByRole('button', { name: 'Bring back Squat' }))
    await waitFor(() => expect(menu('Squat')).not.toBeNull())
    expect(screen.getByText('Wednesday · 1 exercise')).toBeInTheDocument()
    expect(screen.queryByText(/ skipped$/)).toBeNull()
    await userEvent.click(within(skippedLine()!).getByRole('button', { name: 'Bring back Bench' }))
    await waitFor(() => expect(menu('Bench')).not.toBeNull())
    expect(screen.getByText('Wednesday · 2 exercises')).toBeInTheDocument()
    expect(await storedSkips(db)).toEqual([])
  })

  it('startCard_skipsAndAnAddedExercise_countsWhatIsLeft', async () => {
    // An exercise the lifter added (no plan item) is left to do: the normal count, not the skipped one.
    const row = await exerciseNamed(db, 'Deadlift')
    const exercise = new JsExercise(row.id, row.name, row.category, row.parentLift ?? null, [...(row.muscles ?? [])], true, false)
    const added: TodayItem = { exercise, planned: null, suggestion: null, sets: [], editor: { weightKg: null, reps: 5, rpe: 8 }, targetSets: null, isComplete: false, perSide: null, lastWeightKg: null, lastTime: null }
    render(<StartSessionCard items={[added]} dayName="Wednesday" onStart={() => {}} skipped={2} />)
    expect(screen.getByText('Wednesday · 1 exercise')).toBeInTheDocument()
    expect(screen.queryByText(/ skipped$/)).toBeNull()
  })

  it('finish_countsSkipped_inTheSummary', async () => {
    await logSet(db, await startSession(db, TODAY), set(squat, 100))
    show()
    await skipWith('Bench', 'Out of time')
    await waitFor(() => expect(skippedLine()).not.toBeNull())
    await userEvent.click(screen.getByRole('button', { name: 'Finish' }))
    expect(await screen.findByRole('heading', { name: 'Session saved' })).toBeInTheDocument()
    expect(screen.getByText('No new coach notes, 1 skipped')).toBeInTheDocument()
  })

  it('skippedLine_notOnAnotherDay', async () => {
    await storeSkip(db, { date: addDays(TODAY, -7), mode: 'POWERLIFTING', exerciseId: bench, planItemId: null, reason: null }, () => [])
    show()
    expect(await screen.findByRole('button', { name: 'Options for Bench' })).toBeInTheDocument()
    expect(skippedLine()).toBeNull()
  })
})

describe('Skip today haptics', () => {
  let vibrate: ReturnType<typeof vi.fn>
  beforeEach(() => {
    vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
  })
  afterEach(() => {
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('skip_ticksOnce', async () => {
    show()
    await openSheet('Bench')
    expect(vibrate, 'opening the menu and the sheet').not.toHaveBeenCalled()
    pastTheTapWindow()
    await userEvent.keyboard('{Escape}')
    await waitFor(() => expect(sheet()).toBeNull())
    expect(vibrate, 'a cancel').not.toHaveBeenCalled()

    await skipWith('Bench', 'Busy or no equipment')
    await waitFor(() => expect(skippedLine()).not.toBeNull())
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)

    // Undo and Bring back do not buzz.
    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }))
    await waitFor(() => expect(skippedLine()).toBeNull())
    await skipWith('Bench', 'Just skip')
    await waitFor(() => expect(skippedLine()).not.toBeNull())
    await userEvent.click(within(skippedLine()!).getByRole('button', { name: 'Bring back Bench' }))
    await waitFor(() => expect(skippedLine()).toBeNull())
    expect(vibrate).toHaveBeenCalledTimes(2)
  })
})

describe('the SKIP card on Coach', () => {
  const card = async (title: string) => (await screen.findByRole('heading', { name: title })).closest('.gr-card') as HTMLElement

  /** Skips `name`, planned today, on the screen with `reason`, then opens Coach. */
  async function skipThenCoach(name: string, answer: string) {
    const view = show()
    await skipWith(name, answer)
    await waitFor(async () => expect(await db.advice.where('type').equals('SKIP').count()).toBe(1))
    view.unmount()
    render(<CoachScreen db={db} today={() => TODAY} />)
  }

  it('coach_mainLiftPainCard_dismissOnly', async () => {
    await skipThenCoach('Bench', 'Pain')
    const pain = await card('Bench skipped')
    expect(
      within(pain).getByText('You skipped Bench because of pain. Its weight won’t go up until you do it again. If the pain persists or gets worse, see a physician.'.replace('’', "'")),
    ).toBeInTheDocument()
    expect(within(pain).getByText('Skipped')).toBeInTheDocument()
    expect(within(pain).getAllByRole('button').map((b) => b.textContent)).toEqual(['Dismiss'])
    await userEvent.click(within(pain).getByRole('button', { name: 'Dismiss' }))
    await waitFor(async () => expect((await db.advice.where('type').equals('SKIP').first())?.status).toBe('DISMISSED'))
  })

  it('coach_skipCardWithAlternatives_showsASwapButtonForEach_andSwaps', async () => {
    const curl = (await exerciseNamed(db, 'Curl')).id
    await db.planItems.add({ weekday: 3, exerciseId: curl, sets: 3, reps: 8, targetRpe: 8, order: 2, mode: 'POWERLIFTING' })
    await skipThenCoach('Curl', 'Pain')
    const stored = (await db.advice.where('type').equals('SKIP').first())!
    const alternatives = [...grit.advicePayload(stored.payloadJson)!.alternatives]
    expect(alternatives.length).toBeGreaterThan(0)
    const pain = await card('Curl skipped')
    expect(within(pain).getByText(stored.reason)).toBeInTheDocument()
    expect(within(pain).getAllByRole('button').map((b) => b.textContent)).toEqual([...alternatives.map((a) => `Swap for ${a}`), 'Dismiss'])
    await userEvent.click(within(pain).getByRole('button', { name: `Swap for ${alternatives[0]}` }))
    expect(await screen.findByText(`Swapped Curl for ${alternatives[0]}`)).toBeInTheDocument()
    await waitFor(async () => expect((await db.advice.get(stored.id!))?.status).toBe('APPLIED'))
  })
})
