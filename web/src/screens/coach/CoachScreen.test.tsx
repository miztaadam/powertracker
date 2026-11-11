import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { updateSettings } from '../../db/db'
import { advice, insertAdvice, payload, planOf, rx, statuses } from '../../test/coachFixture'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
import { TODAY, addDays, exerciseNamed, logSet, plan, set, startSession, todayFixture } from '../../test/todayFixture'
import { CoachScreen } from './CoachScreen'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes: Vitest runs this before the cleanup registered in the test setup.
afterEach(async () => {
  cleanup()
  vi.restoreAllMocks()
  await fx.dispose()
})

const id = async (name: string) => (await exerciseNamed(db, name)).id
const show = () => render(<CoachScreen db={db} today={() => TODAY} />)
const tap = (name: string) => userEvent.click(screen.getByRole('button', { name }))

const stallReason = "Bench e1RM hasn't improved for 3 weeks. You've mostly missed at lockout — this works the triceps."

async function insertStall() {
  // Bench is trained on Wednesday (with a row already there) and again on Friday.
  await plan(db, await id('Bench'), { weekday: 5, sets: 3, reps: 5, order: 0 })
  await plan(db, await id('Bench'), { weekday: 3, sets: 4, reps: 6, order: 0 })
  await plan(db, await id('Squat'), { weekday: 3, sets: 5, reps: 5, order: 1 })
  await logSet(db, await startSession(db, addDays(TODAY, -2)), set(await id('Bench'), 105, { rpe: 9 }))
  await insertAdvice(db, advice('STALL', 'BENCH', stallReason, payload.accessories([rx('Close-grip bench', 3, 3, 5), rx('JM press', 3, 8, 10)])))
}

describe('CoachScreen', () => {
  it('stallCard_applyAddsAccessoriesToPlan', async () => {
    await insertStall()
    show()
    expect(await screen.findByRole('heading', { name: 'Bench has stalled' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Coach' })).toBeInTheDocument()
    expect(screen.getByText('1 open suggestion')).toBeInTheDocument()
    expect(screen.getByText('Stall · Bench')).toBeInTheDocument()
    expect(screen.getByText(stallReason)).toBeInTheDocument()
    expect(screen.getByText('Bench e1RM')).toBeInTheDocument()
    expect(screen.getByText('Close-grip bench')).toBeInTheDocument()
    expect(screen.getByText('3 × 3–5')).toBeInTheDocument()
    expect(screen.getByText('3 × 8–10')).toBeInTheDocument()
    // Nothing changes until Apply is tapped.
    expect(await planOf(db)).toHaveLength(3)

    await tap('Add to plan')

    await waitFor(async () => expect(await statuses(db)).toEqual(['APPLIED']))
    const added = (await planOf(db)).filter((p) => p.order >= 2)
    expect(added).toEqual([
      { weekday: 3, exerciseId: await id('Close-grip bench'), sets: 3, reps: 5, targetRpe: 8, order: 2 },
      { weekday: 3, exerciseId: await id('JM press'), sets: 3, reps: 10, targetRpe: 8, order: 3 },
    ])
    expect(await planOf(db)).toHaveLength(5)
    expect(await screen.findByText('Added 2 exercises to Wednesday')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Bench has stalled')).toBeNull())
  })

  it('infoButton_opensGuide_fromEveryPlace', async () => {
    await insertStall()
    show()
    await screen.findByText('Bench has stalled')

    // Every accessory the coach names has its guide beside it.
    await checkInfoButtonOpensGuide('Close-grip bench')
    await checkInfoButtonOpensGuide('JM press')

    // Reading a guide applied nothing.
    expect(await statuses(db)).toEqual(['OPEN'])
    expect(await planOf(db)).toHaveLength(3)
  })

  it('dismiss_removesCard', async () => {
    await insertStall()
    show()
    await screen.findByText('Bench has stalled')

    await tap('Dismiss')

    await waitFor(() => expect(screen.queryByText('Bench has stalled')).toBeNull())
    await waitFor(async () => expect(await statuses(db)).toEqual(['DISMISSED']))
    expect(await planOf(db)).toHaveLength(3)
    expect(await screen.findByText('No advice right now. Keep logging.')).toBeInTheDocument()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })

  it('emptyState_showsMessage', async () => {
    // A TARGET belongs to Today, not to the feed.
    await insertAdvice(db, advice('TARGET', 'SQUAT', 'Coach suggests 155 × 5 @ 8.', payload.target(await id('Squat')), { severity: 0 }))
    show()
    expect(await screen.findByText('No advice right now. Keep logging.')).toBeInTheDocument()
    expect(screen.getByText('The coach looks at your training each time you finish a session.')).toBeInTheDocument()
    expect(screen.getByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText(/Coach suggests/)).toBeNull()
  })

  it('weakPointApply_addsTwoOptions', async () => {
    await plan(db, await id('Squat'), { weekday: 1, sets: 5, reps: 5, order: 0 })
    // Pause squat is the first option for squat misses out of the hole, and is already planned.
    await plan(db, await id('Pause squat'), { weekday: 5, sets: 3, reps: 4, targetRpe: 7.5, order: 0 })
    await updateSettings(db, (s) => ({ ...s, defaultRpe: 7.5 }))
    await insertAdvice(db, advice('WEAK_POINT', 'SQUAT', 'Prioritise it.', payload.weakPoint('SQUAT', 'FLOOR'), { severity: 4 }))
    show()
    expect(await screen.findByRole('heading', { name: 'Squat is lagging' })).toBeInTheDocument()
    expect(screen.getByText('Weak point · Squat')).toBeInTheDocument()
    expect(screen.getByText('Sticking point')).toBeInTheDocument()
    expect(screen.getByText('Out of the hole')).toBeInTheDocument()
    expect(screen.getByText('Works quads, glutes')).toBeInTheDocument()
    expect(screen.getByText('Adds to your plan')).toBeInTheDocument()
    expect(screen.getByText('Front squat')).toBeInTheDocument()
    expect(screen.getByText('Bulgarian split squat')).toBeInTheDocument()
    expect(screen.queryByText('Pause squat')).toBeNull()

    await tap('Prioritise squat')

    await waitFor(async () => expect(await statuses(db)).toEqual(['APPLIED']))
    expect((await planOf(db)).filter((p) => p.weekday === 1)).toEqual([
      { weekday: 1, exerciseId: await id('Squat'), sets: 5, reps: 5, targetRpe: 8, order: 0 },
      { weekday: 1, exerciseId: await id('Front squat'), sets: 3, reps: 5, targetRpe: 7.5, order: 1 },
      { weekday: 1, exerciseId: await id('Bulgarian split squat'), sets: 3, reps: 10, targetRpe: 7.5, order: 2 },
    ])
    expect(await planOf(db)).toHaveLength(4)
    expect(await screen.findByText('Added 2 exercises to Monday')).toBeInTheDocument()
  })

  it('deloadApply_statusOnly', async () => {
    await plan(db, await id('Squat'), { weekday: 1 })
    await insertAdvice(
      db,
      advice('DELOAD', null, 'Signs of fatigue.', payload.deload(), { severity: 2 }),
      advice('RE_ENTRY', null, 'Welcome back after a break.', payload.reEntry(), { severity: 20 }),
    )
    show()
    expect(await screen.findByRole('heading', { name: 'Take a deload week' })).toBeInTheDocument()
    expect(screen.getByText('2 open suggestions')).toBeInTheDocument()
    expect(screen.getByText('Deload · All lifts')).toBeInTheDocument()
    expect(screen.getByText('of your usual sets')).toBeInTheDocument()
    expect(screen.getByText('of your last top sets, for 7 days')).toBeInTheDocument()

    await tap('Apply deload')
    await waitFor(async () => expect(await statuses(db)).toEqual(['APPLIED', 'OPEN']))
    expect(await screen.findByText('Deload week on — targets lightened for 7 days')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Take a deload week')).toBeNull())

    await tap('Ease back in')
    await waitFor(async () => expect(await statuses(db)).toEqual(['APPLIED', 'APPLIED']))
    expect(await screen.findByText('Easing back in for 7 days')).toBeInTheDocument()
    expect(await planOf(db)).toHaveLength(1)
    expect(await screen.findByText('No advice right now. Keep logging.')).toBeInTheDocument()
  })

  it('failedWrite_showsMessageAndKeepsCard', async () => {
    await insertStall()
    vi.spyOn(db.advice, 'update').mockRejectedValue(new Error('disk full'))
    show()
    await screen.findByText('Bench has stalled')

    await tap('Add to plan')
    expect(await screen.findByText("Couldn't update — try again")).toBeInTheDocument()
    expect(await screen.findByText('Bench has stalled')).toBeInTheDocument()
    // Half an apply is no apply: the accessories did not stay in the plan either.
    expect(await planOf(db)).toHaveLength(3)

    await tap('Dismiss')
    await waitFor(() => expect(db.advice.update).toHaveBeenCalledTimes(2))
    expect(await screen.findByText("Couldn't update — try again")).toBeInTheDocument()
    expect(await screen.findByText('Bench has stalled')).toBeInTheDocument()
    expect(await statuses(db)).toEqual(['OPEN'])
  })
})
