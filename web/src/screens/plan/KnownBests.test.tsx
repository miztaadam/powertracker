import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { updateSettings, type GritDb } from '../../db/db'
import { TODAY, addDays, baseline, exerciseNamed, logSet, set, startSession, todayFixture } from '../../test/todayFixture'
import { PlanScreen } from './PlanScreen'

// Known bests (1.16.1): each row's "Best e1RM" line, the higher of the typed best and the best logged
// (`grit.knownBest`), and the strict press row, which the Progress switch being on always brings.

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = () => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} />)
const row = (name: string) => screen.findByRole('group', { name })
const lineOf = async (name: string) => within(await row(name)).queryByText(/^Best e1RM /)?.textContent ?? null
const pressOn = () => updateSettings(db, (s) => ({ ...s, showStrictPress: true }))

async function setBest(name: string, text: string) {
  await userEvent.click(within(await row(name)).getByRole('button', { name: 'Set best' }))
  const dialog = within(screen.getByRole('dialog'))
  await userEvent.type(dialog.getByRole('textbox', { name: 'Estimated 1RM (kg)' }), text)
  await userEvent.click(dialog.getByRole('button', { name: 'Save' }))
}

/** One completed single at RPE 10 (its e1RM is its weight), in a session `daysAgo` before today, on `mode`'s side. */
async function lifted(name: string, kg: number, daysAgo: number, mode: 'POWERLIFTING' | 'BODYBUILDING' = 'POWERLIFTING') {
  const id = (await exerciseNamed(db, name)).id
  const session = (await db.sessions.add({ date: addDays(TODAY, -daysAgo), bodyweightKg: null, readiness: null, mode })) as number
  await logSet(db, session, set(id, kg, { reps: 1, rpe: 10 }))
}

describe('Known bests: the line', () => {
  it('noBest_noLine', async () => {
    show()
    for (const lift of ['Squat', 'Bench', 'Deadlift']) expect(await lineOf(lift)).toBeNull()
  })

  it('setBest_showsItsLine_toTheHalfKilo', async () => {
    show()
    await setBest('Squat', '182,5')
    await waitFor(async () => expect(await lineOf('Squat')).toBe('Best e1RM 182.5 kg'))
    expect(await lineOf('Bench')).toBeNull()
  })

  it('aHeavierLoggedSet_raisesIt', async () => {
    await baseline(db, (await exerciseNamed(db, 'Squat')).id, 180, TODAY)
    show()
    await waitFor(async () => expect(await lineOf('Squat')).toBe('Best e1RM 180 kg'))

    await lifted('Squat', 190, 1)

    await waitFor(async () => expect(await lineOf('Squat')).toBe('Best e1RM 190 kg'))
  })

  it('aLighterLoggedSet_leavesTheTypedBest_andTheBodybuildingSideCountsForNothing', async () => {
    await lifted('Squat', 150, 2)
    await lifted('Squat', 230, 1, 'BODYBUILDING')
    show()
    await waitFor(async () => expect(await lineOf('Squat')).toBe('Best e1RM 150 kg'))

    await setBest('Squat', '200')

    await waitFor(async () => expect(await lineOf('Squat')).toBe('Best e1RM 200 kg'))
  })

  it('anEstimate_isRoundedToTheHalfKilo_asOnProgress', async () => {
    const bench = (await exerciseNamed(db, 'Bench')).id
    const session = await startSession(db, addDays(TODAY, -1))
    await logSet(db, session, set(bench, 100, { reps: 3, rpe: 8 }))
    show()
    await waitFor(async () => expect(await lineOf('Bench')).toMatch(/^Best e1RM \d+(\.5)? kg$/))
  })
})

describe('Known bests: strict press', () => {
  it('pressOff_withNoRow_hasNoPressRow_asBefore', async () => {
    show()
    await row('Squat')
    expect(screen.queryByRole('group', { name: 'Strict press' })).toBeNull()
  })

  it('pressOn_withNoRowYet_showsStrictPress_storingNothing', async () => {
    await pressOn()
    show()
    expect(await lineOf('Strict press')).toBeNull()
    expect((await db.exercises.toArray()).filter((e) => e.category === 'STRICT_PRESS')).toEqual([])
  })

  it('setBest_onIt_makesItsRow_andShowsItsLine', async () => {
    await pressOn()
    show()
    await setBest('Strict press', '70')

    await waitFor(async () => expect(await lineOf('Strict press')).toBe('Best e1RM 70 kg'))
    const press = await exerciseNamed(db, 'Strict press')
    expect(press.category).toBe('STRICT_PRESS')
    expect(await db.baselines.toArray()).toEqual([{ exerciseId: press.id, e1rmKg: 70, date: TODAY }])
    expect(screen.getAllByRole('group', { name: 'Strict press' })).toHaveLength(1)
  })

  it('pressOff_keepsAnExistingRow_andItsBest', async () => {
    await pressOn()
    show()
    await setBest('Strict press', '70')
    await waitFor(async () => expect(await lineOf('Strict press')).toBe('Best e1RM 70 kg'))

    await updateSettings(db, (s) => ({ ...s, showStrictPress: false }))

    // Today's rule stays: a strict press that exists keeps its row whatever the switch says (the coach uses its best).
    // Nothing to wait for but the live read after the switch: give it its turn, then nothing has changed.
    await new Promise((done) => setTimeout(done, 100))
    expect(await lineOf('Strict press')).toBe('Best e1RM 70 kg')
    expect(await db.baselines.count()).toBe(1)
  })

  it('pressOn_withAnArchivedPressUnderAnotherName_addsNoSecondRow', async () => {
    await db.exercises.add({ name: 'OHP', category: 'STRICT_PRESS', parentLift: null, muscles: [], isBuiltIn: false, archived: true })
    await pressOn()
    show()
    await row('OHP')
    expect(screen.queryByRole('group', { name: 'Strict press' })).toBeNull()
  })
})
