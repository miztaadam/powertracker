import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { SUPERSET_SWITCH_GUARD_MS } from '../../state/useToday'
import { TODAY, addDays, exerciseNamed, logSet, set, setsOn, startSession, todayFixture } from '../../test/todayFixture'
import { TodayScreen } from './TodayScreen'

// Supersets on Today (1.24): the pair's cards hand over to each other, and the rest starts after the second.
let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squat: number
let bench: number
let row: number
let rest: ReturnType<typeof vi.fn<(isMainOrVariation: boolean) => void>>

/** Today's plan: squat and bench as a superset (the link on the bench), then row on its own. Last week each was done. */
async function plan({ squatSets = 3, benchSets = 3 } = {}) {
  const lastWeek = await startSession(db, addDays(TODAY, -7))
  for (const [order, [exerciseId, sets, kg]] of (
    [
      [squat, squatSets, 150],
      [bench, benchSets, 100],
      [row, 3, 60],
    ] as const
  ).entries()) {
    await db.planItems.add({ weekday: 3, exerciseId, sets, reps: 5, targetRpe: 8, order, ...(exerciseId === bench ? { supersetWithAbove: true as const } : {}) })
    await logSet(db, lastWeek, set(exerciseId, kg))
  }
}

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squat = (await exerciseNamed(db, 'Squat')).id
  bench = (await exerciseNamed(db, 'Bench')).id
  row = (await exerciseNamed(db, 'Row')).id
  rest = vi.fn<(isMainOrVariation: boolean) => void>()
})
afterEach(async () => {
  vi.restoreAllMocks()
  cleanup()
  await fx.dispose()
})

const show = () => render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} onSetLogged={rest} />)
/** The open card: the one with the weight entry. Its exercise's name and its eyebrow. */
const openCard = () => {
  const card = screen.getByRole('button', { name: /^Log set \d+$/ }).closest('.gr-card') as HTMLElement
  return { name: within(card).getByRole('heading', { level: 2 }).textContent, eyebrow: card.querySelector('.eyebrow')?.textContent, card }
}
const logButton = () => screen.getByRole('button', { name: /^Log set \d+$/ })
const logged = async () => (await setsOn(db, TODAY)).map((s) => s.exerciseId)
const waitForSets = (count: number) => waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(count))
const opens = (name: string) => waitFor(() => expect(openCard().name).toBe(name))
/** Lets the time in which a tap on the other card is taken for a slip go by. */
function pastTheGuard() {
  const now = performance.now()
  vi.spyOn(performance, 'now').mockReturnValue(now + SUPERSET_SWITCH_GUARD_MS + 1)
}

describe('a superset on Today', () => {
  it('theFirstLeads_bothAreTagged_andLoggingItOpensTheSecondWithNoRest', async () => {
    await plan()
    await startSession(db, TODAY)
    show()
    await screen.findByRole('button', { name: 'Log set 1' })
    expect(openCard()).toMatchObject({ name: 'Squat', eyebrow: 'Exercise 1 of 3 · Superset A' })
    // The folded rows: the bench is tagged, the row is not.
    const benchRow = screen.getByRole('button', { name: /^Bench/ })
    expect(within(benchRow).getByText('Superset B')).toBeInTheDocument()
    expect(within(screen.getByRole('button', { name: /^Row/ })).queryByText(/Superset/)).toBeNull()

    await userEvent.click(logButton())
    await waitForSets(1)
    await opens('Bench')
    expect(openCard().eyebrow).toBe('Exercise 2 of 3 · Superset B')
    expect(await logged()).toEqual([squat])
    // No rest between the two.
    expect(rest).not.toHaveBeenCalled()
    // The squat's row shows its set, still tagged.
    expect(within(screen.getByRole('button', { name: /^Squat/ })).getByText('Superset A')).toBeInTheDocument()
  })

  it('loggingTheSecond_startsTheRest_andOpensTheFirstAgain_roundAfterRound', async () => {
    await plan()
    await startSession(db, TODAY)
    show()
    await screen.findByRole('button', { name: 'Log set 1' })
    for (const round of [1, 2, 3]) {
      expect(openCard().name).toBe('Squat')
      expect(logButton()).toHaveTextContent(`Log set ${round}`)
      await userEvent.click(logButton())
      await waitForSets(round * 2 - 1)
      await opens('Bench')
      expect(rest).toHaveBeenCalledTimes(round - 1)
      pastTheGuard()
      await userEvent.click(logButton())
      await waitForSets(round * 2)
      expect(rest).toHaveBeenCalledTimes(round)
      // A main lift's rest, as after any bench set on this side.
      expect(rest).toHaveBeenLastCalledWith(true)
      if (round < 3) await opens('Squat')
      pastTheGuard()
    }
    // Both done: the bench's card stays, with the way on to the row.
    expect(await logged()).toEqual([squat, bench, squat, bench, squat, bench])
    expect(await screen.findByRole('button', { name: 'Next: Row' })).toBeInTheDocument()
  })

  it('aSecondQuickTap_afterTheHandOver_savesNothingOnTheOtherCard', async () => {
    await plan()
    await startSession(db, TODAY)
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Log set 1' }))
    await waitForSets(1)
    await opens('Bench')
    // The thumb comes down again where Log set was: now the bench's.
    await userEvent.click(logButton())
    await new Promise((done) => setTimeout(done, 80))
    expect(await logged()).toEqual([squat])
    expect(openCard().name).toBe('Bench')
    // A moment later the same button logs the bench's set.
    pastTheGuard()
    await userEvent.click(logButton())
    await waitForSets(2)
    expect(await logged()).toEqual([squat, bench])
  })

  it('openedAgainMidPair_theCardIsTheOneWhoseTurnItIs', async () => {
    await plan()
    // The squat's first set is in the log, the bench's is not: closed and opened here.
    await logSet(db, await startSession(db, TODAY), set(squat, 150))
    show()
    await screen.findByRole('button', { name: /^Log set/ })
    expect(openCard()).toMatchObject({ name: 'Bench', eyebrow: 'Exercise 2 of 3 · Superset B' })
    expect(logButton()).toHaveTextContent('Log set 1')
  })

  it('unevenSets_theOneWithSetsLeftCarriesOnAlone_withARestAfterEachSet', async () => {
    await plan({ squatSets: 3, benchSets: 1 })
    const session = await startSession(db, TODAY)
    // One round done: the bench has its one set.
    await logSet(db, session, set(squat, 150))
    await logSet(db, session, set(bench, 100))
    show()
    await screen.findByRole('button', { name: 'Log set 2' })
    expect(openCard().name).toBe('Squat')
    await userEvent.click(logButton())
    await waitForSets(3)
    // No hand-over: the squat's card stays for its third set, and the rest starts.
    expect(rest).toHaveBeenCalledTimes(1)
    expect(await screen.findByRole('button', { name: 'Log set 3' })).toBeInTheDocument()
    expect(openCard().name).toBe('Squat')
  })

  it('aMissOnTheFirst_keepsItsCardToAskWhereItStalled_andStillStartsNoRest', async () => {
    await plan()
    await startSession(db, TODAY)
    show()
    await screen.findByRole('button', { name: 'Log set 1' })
    await userEvent.click(screen.getByRole('button', { name: 'Missed' }))
    await waitForSets(1)
    // The powerlifting card asks where the rep was missed: it stays open.
    expect(await screen.findByTestId('miss-row-1')).toBeInTheDocument()
    await new Promise((done) => setTimeout(done, 80))
    expect(openCard().name).toBe('Squat')
    expect(rest).not.toHaveBeenCalled()
    // The lifter goes on to the bench by its row; the turn is still the bench's.
    await userEvent.click(screen.getByRole('button', { name: /^Bench/ }))
    await opens('Bench')
  })

  it('withOneOfThePairSkippedToday_theOtherIsAnExerciseOnItsOwn', async () => {
    await plan()
    await startSession(db, TODAY)
    const benchItem = (await db.planItems.where('exerciseId').equals(bench).first())!.id!
    await db.skips.add({ date: TODAY, mode: 'POWERLIFTING', exerciseId: bench, planItemId: benchItem, reason: 'BUSY' })
    show()
    await screen.findByRole('button', { name: 'Log set 1' })
    expect(openCard()).toMatchObject({ name: 'Squat', eyebrow: 'Exercise 1 of 2' })
    await userEvent.click(logButton())
    await waitForSets(1)
    // No hand-over, and the rest as after any set.
    expect(rest).toHaveBeenCalledTimes(1)
    expect(await screen.findByRole('button', { name: 'Log set 2' })).toBeInTheDocument()
    expect(openCard().name).toBe('Squat')
  })
})
