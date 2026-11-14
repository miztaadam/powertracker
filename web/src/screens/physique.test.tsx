import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, replacePlan } from '../db/catalogue'
import { checkInToJs } from '../db/checkIns'
import { loadHistory, updateSettings, type GritDb } from '../db/db'
import { adviceFromJs, planItemToJs } from '../db/mapping'
import type { AdviceRow, TrainingMode } from '../db/schema'
import { grit, JsCheckIn } from '../domain'
import { draftItems } from '../state/usePlanBuilder'
import { TODAY, addDays, exerciseNamed, logSet, set, todayFixture } from '../test/todayFixture'
import { CoachScreen } from './coach/CoachScreen'
import { ProgressScreen } from './progress/ProgressScreen'
import { TodayScreen } from './today/TodayScreen'

/** Bodybuilding mode stage 3 on Coach, Progress and Today (plan Task 2, "What the lifter sees" 1, 3 and 4). */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  await onSide('BODYBUILDING')
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

async function onSide(mode: TrainingMode) {
  await updateSettings(db, (s) => ({ ...s, trainingMode: mode }))
}

/** The stage 2 builder's default three days, saved as the bodybuilding plan (Monday: Leg press 4, DB press 3, ...). */
async function defaultPlan() {
  await replacePlan(db, 'BODYBUILDING', draftItems(grit.planBuilderBuild([1, 3, 5], [], [], 0), []))
}

const id = async (name: string) => (await exerciseNamed(db, name)).id

async function session(daysAgo: number, sets: [string, number, number][], mode: TrainingMode = 'BODYBUILDING') {
  const sessionId = (await db.sessions.add({ date: addDays(TODAY, -daysAgo), bodyweightKg: null, readiness: null, mode })) as number
  for (const [name, kg, reps] of sets) await logSet(db, sessionId, set(await id(name), kg, { reps }))
  return sessionId
}

/** Stores what saving a check-in today would, as the check-in screen does. */
async function checkIn(lagging: string[], growingWell: string[] = [], date = TODAY) {
  const history = await loadHistory(db)
  const plan = (await planFor(db, 'BODYBUILDING')).map(planItemToJs)
  const outcome = grit.physiqueAfterCheckIn(new JsCheckIn(date, lagging, growingWell), history, plan, date, [])
  await db.checkIns.add({ date, lagging, growingWell })
  await db.advice.bulkAdd(outcome.advice.map((a) => withoutId(adviceFromJs(a))))
  return outcome
}

/** Leg press flat for three weeks: 100 × 10 on 15 Oct, 25 Oct and 5 Nov. */
async function stuckLegPress() {
  for (const daysAgo of [27, 17, 6]) await session(daysAgo, [['Leg press', 100, 10]])
}

async function storeStuck() {
  const history = await loadHistory(db)
  const plan = (await planFor(db, 'BODYBUILDING')).map(planItemToJs)
  const advice = grit.physiqueStuck(history, plan, [], [], TODAY)
  await db.advice.bulkAdd(advice.map((a) => withoutId(adviceFromJs(a))))
}

const withoutId = ({ id: _id, ...row }: AdviceRow & { id: number }): AdviceRow => row

const showCoach = (onOpenCheckIn = vi.fn()) => {
  render(<CoachScreen db={db} today={() => TODAY} onOpenCheckIn={onOpenCheckIn} />)
  return onOpenCheckIn
}

const card = async (title: string) => (await screen.findByRole('heading', { name: title })).closest('.gr-card') as HTMLElement

/** The bodybuilding plan as [weekday, name, sets, reps, repsHigh, rpe, order], by day then order. */
async function planRows(): Promise<unknown[][]> {
  const names = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
  return (await planFor(db, 'BODYBUILDING'))
    .sort((a, b) => a.weekday - b.weekday || a.order - b.order)
    .map((p) => [p.weekday, names.get(p.exerciseId), p.sets, p.reps, p.repsHigh, p.targetRpe, p.order])
}

describe('Coach: the check-in card', () => {
  it('showsAt28Days_notAt27_withItsWording_andNoDismiss', async () => {
    await defaultPlan()
    await session(27, [['DB press', 26, 10]])
    showCoach()
    expect(await screen.findByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText('Time for your check-in')).toBeNull()
    cleanup()

    await db.sessions.toCollection().modify({ date: addDays(TODAY, -28) })
    const open = showCoach()
    const due = await card('Time for your check-in')
    expect(within(due).getByText('Four weeks on this plan. Mark the body parts that stand out; everything else stays on track.')).toBeInTheDocument()
    expect(within(due).getAllByRole('button').map((b) => b.textContent)).toEqual(['Check in'])
    await userEvent.click(within(due).getByRole('button', { name: 'Check in' }))
    expect(open).toHaveBeenCalled()
    // It is not a suggestion: the feed still says nothing is open, and shows no empty state under it.
    expect(screen.getByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText('No advice right now. Keep logging.')).toBeNull()
  })

  it('aCheckInInTheLast28Days_putsItOff', async () => {
    await defaultPlan()
    await session(60, [['DB press', 26, 10]])
    await db.checkIns.add({ date: addDays(TODAY, -27), lagging: [], growingWell: [] })
    showCoach()
    expect(await screen.findByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText('Time for your check-in')).toBeNull()
  })

  it('neverOnThePowerliftingSide', async () => {
    await defaultPlan()
    await session(40, [['DB press', 26, 10]])
    await checkIn(['CHEST'])
    await onSide('POWERLIFTING')
    showCoach()
    expect(await screen.findByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText('Time for your check-in')).toBeNull()
    expect(screen.queryByText('Chest is lagging')).toBeNull()
  })
})

describe('Coach: more work', () => {
  it('setsAdded_card_andAddToPlan', async () => {
    await defaultPlan()
    await checkIn(['CHEST'])
    showCoach()
    const more = await card('Chest is lagging')
    expect(within(more).getByText('More work')).toBeInTheDocument()
    expect(within(more).getByText('One more set of DB press on Monday (3 → 4)')).toBeInTheDocument()
    expect(within(more).getByText('One more set of Incline DB press on Wednesday (3 → 4)')).toBeInTheDocument()
    expect(within(more).getByText('Chest: 10 → 12 sets a week')).toBeInTheDocument()
    expect(within(more).getAllByRole('button').map((b) => b.textContent)).toEqual(['Add to plan', 'Dismiss'])
    expect(screen.getByText('1 open suggestion')).toBeInTheDocument()

    const before = await planRows()
    await userEvent.click(within(more).getByRole('button', { name: 'Add to plan' }))
    expect(await screen.findByText('Added a set to 2 exercises')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByText('Chest is lagging')).toBeNull())
    const after = await planRows()
    const changed = after.filter((row, i) => JSON.stringify(row) !== JSON.stringify(before[i]))
    expect(changed).toEqual([
      [1, 'DB press', 4, 8, 12, 8, 1],
      [3, 'Incline DB press', 4, 8, 12, 8, 1],
    ])
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('aNewExercise_card_andAddToPlan_findsItsCatalogueRow', async () => {
    await defaultPlan()
    await checkIn(['ADDUCTORS'])
    showCoach()
    const more = await card('Adductors is lagging')
    expect(within(more).getByText('Add Machine hip adduction on Friday, 3 × 8–12 @ 8')).toBeInTheDocument()
    expect(within(more).getByText('Adductors: 0 → 3 sets a week')).toBeInTheDocument()
    await userEvent.click(within(more).getByRole('button', { name: 'Add to plan' }))
    expect(await screen.findByText('Added Machine hip adduction to Friday')).toBeInTheDocument()
    const friday = (await planRows()).filter((r) => r[0] === 5)
    expect(friday.at(-1)).toEqual([5, 'Machine hip adduction', 3, 8, 12, 8, friday.length - 1])
    expect((await db.planItems.toArray()).every((p) => p.mode === 'BODYBUILDING')).toBe(true)
  })

  it('afterThePlanChanged_appliesOnlyWhatStillMatches_andSaysSo', async () => {
    await defaultPlan()
    await checkIn(['CHEST'])
    // DB press on Monday was edited since; the Wednesday incline is still as it was.
    const press = await id('DB press')
    await db.planItems.where('exerciseId').equals(press).filter((p) => p.weekday === 1).modify({ sets: 2 })
    showCoach()
    await userEvent.click(within(await card('Chest is lagging')).getByRole('button', { name: 'Add to plan' }))
    expect(await screen.findByText('Added a set to 1 exercise; 1 exercise had changed since and was left as it is')).toBeInTheDocument()
    expect((await db.planItems.where('exerciseId').equals(press).filter((p) => p.weekday === 1).first())!.sets).toBe(2)
  })

  it('nothingLeftToApply_saysSo_andTheCardGoes', async () => {
    await defaultPlan()
    await checkIn(['CHEST'])
    const ids = [await id('DB press'), await id('Incline DB press')]
    await db.planItems.where('exerciseId').anyOf(ids).delete()
    const before = await planRows()
    showCoach()
    await userEvent.click(within(await card('Chest is lagging')).getByRole('button', { name: 'Add to plan' }))
    expect(await screen.findByText('Your plan has changed since; nothing was added')).toBeInTheDocument()
    expect(await planRows()).toEqual(before)
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('dismiss_leavesThePlan', async () => {
    await defaultPlan()
    await checkIn(['CHEST'])
    const before = await planRows()
    showCoach()
    await userEvent.click(within(await card('Chest is lagging')).getByRole('button', { name: 'Dismiss' }))
    await waitFor(() => expect(screen.queryByText('Chest is lagging')).toBeNull())
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['DISMISSED'])
    expect(await planRows()).toEqual(before)
  })
})

describe('Coach: a failed write', () => {
  const failing = () => {
    throw new Error('disk full')
  }

  it('moreWork_addToPlan_leavesThePlanAndTheCardAndSaysSo', async () => {
    await defaultPlan()
    await checkIn(['CHEST'])
    const before = await planRows()
    // The set additions are updates of planned items.
    db.planItems.hook('updating', failing)
    try {
      showCoach()
      await userEvent.click(within(await card('Chest is lagging')).getByRole('button', { name: 'Add to plan' }))
      expect(await screen.findByText("Couldn't update — try again")).toBeInTheDocument()
      expect(await card('Chest is lagging')).toBeInTheDocument()
    } finally {
      db.planItems.hook('updating').unsubscribe(failing)
    }
    expect(await planRows()).toEqual(before)
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['OPEN'])
  })

  it('swap_leavesThePlanAndTheCardAndSaysSo', async () => {
    await defaultPlan()
    await stuckLegPress()
    await storeStuck()
    const before = await planRows()
    // The swap's new items are added (its old ones deleted first, in the same transaction).
    db.planItems.hook('creating', failing)
    try {
      showCoach()
      await userEvent.click(within(await card("Leg press hasn't moved in 3 weeks")).getByRole('button', { name: 'Swap for Hack squat' }))
      expect(await screen.findByText("Couldn't update — try again")).toBeInTheDocument()
      expect(await screen.findByRole('button', { name: 'Swap for Hack squat' })).toBeInTheDocument()
    } finally {
      db.planItems.hook('creating').unsubscribe(failing)
    }
    expect(await planRows()).toEqual(before)
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['OPEN'])
    expect((await db.exercises.toArray()).some((e) => e.name === 'Hack squat')).toBe(false)
  })
})

describe('Coach: a stuck exercise', () => {
  it('card_andSwap_replacesItOnEveryPlannedDay_keepingThePrescription', async () => {
    await defaultPlan()
    await stuckLegPress()
    await storeStuck()
    showCoach()
    const stuck = await card("Leg press hasn't moved in 3 weeks")
    expect(within(stuck).getByText('Stuck')).toBeInTheDocument()
    expect(within(stuck).getByText('Same weight and no more reps since 15 Oct. Another exercise for quads can get it moving.')).toBeInTheDocument()
    expect(within(stuck).getAllByRole('button').map((b) => b.textContent)).toEqual(['Swap for Hack squat', 'Swap for DB squat', 'Swap for Single-leg leg press', 'Dismiss'])

    const before = await planRows()
    await userEvent.click(within(stuck).getByRole('button', { name: 'Swap for Hack squat' }))
    expect(await screen.findByText('Swapped Leg press for Hack squat')).toBeInTheDocument()
    const after = await planRows()
    expect(after).toEqual(before.map((row) => (row[1] === 'Leg press' ? [row[0], 'Hack squat', ...row.slice(2)] : row)))
    expect(after.filter((r) => r[1] === 'Hack squat').map((r) => r[0])).toEqual([1, 5])
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('whenTheChoiceIsInThePlanByThen_nothingChanges', async () => {
    await defaultPlan()
    await stuckLegPress()
    await storeStuck()
    const hack = (await db.exercises.add({ name: 'Hack squat', category: 'ACCESSORY', parentLift: null, muscles: ['Quads', 'Glutes'], isBuiltIn: true, archived: false })) as number
    await db.planItems.add({ weekday: 3, exerciseId: hack, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 99, mode: 'BODYBUILDING' })
    const before = await planRows()
    showCoach()
    await userEvent.click(within(await card("Leg press hasn't moved in 3 weeks")).getByRole('button', { name: 'Swap for Hack squat' }))
    expect(await screen.findByText('Your plan has changed since; nothing was swapped')).toBeInTheDocument()
    expect(await planRows()).toEqual(before)
    // The card stays open (coordinator's ruling), so another alternative can be tried.
    expect(await screen.findByRole('button', { name: 'Swap for DB squat' })).toBeInTheDocument()
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['OPEN'])
    await userEvent.click(screen.getByRole('button', { name: 'Swap for DB squat' }))
    expect(await screen.findByText('Swapped Leg press for DB squat')).toBeInTheDocument()
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('theTitle_countsWholeWeeksFromSinceToTheAdvicesDate', async () => {
    await defaultPlan()
    await stuckLegPress()
    await storeStuck()
    // Not beaten since 1 Oct, seen on 11 Nov: 41 days, 5 whole weeks (as the domain's reason and Android count them).
    const json = (await db.advice.toArray())[0]!.payloadJson.replace('"since":"2026-10-15"', '"since":"2026-10-01"')
    await db.advice.toCollection().modify({ payloadJson: json })
    showCoach()
    expect(await screen.findByRole('heading', { name: "Leg press hasn't moved in 5 weeks" })).toBeInTheDocument()
    expect(screen.getByText(/since 1 Oct\./)).toBeInTheDocument()
  })

  it('isNotShown_whenTheExerciseIsNoLongerInThePlan', async () => {
    await defaultPlan()
    await stuckLegPress()
    await storeStuck()
    await db.planItems.where('exerciseId').equals(await id('Leg press')).delete()
    showCoach()
    expect(await screen.findByText('Nothing open')).toBeInTheDocument()
    expect(screen.queryByText(/hasn't moved/)).toBeNull()
  })
})

describe('Today: finishing a session hands the coach the check-ins', () => {
  async function finishToday() {
    await session(0, [['DB press', 26, 10]])
    render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Finish' }))
    await screen.findByText('Session saved')
    return (await db.advice.toArray()).filter((a) => a.type === 'SWAP')
  }

  it('aStuckExercise_getsASwap', async () => {
    await defaultPlan()
    await stuckLegPress()
    expect((await finishToday()).map((a) => [a.mode, grit.advicePayload(a.payloadJson)?.name])).toEqual([['BODYBUILDING', 'Leg press']])
  })

  it('butNotWhenItsBodyPartIsGrowingWell', async () => {
    await defaultPlan()
    await stuckLegPress()
    await db.checkIns.add({ date: addDays(TODAY, -3), lagging: [], growingWell: ['QUADS'] })
    expect(await finishToday()).toEqual([])
  })
})

describe('Progress: body parts', () => {
  async function pressPlan() {
    await db.planItems.add({ weekday: 3, exerciseId: await id('DB press'), sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    await session(10, [['DB press', 26, 10], ['DB press', 26, 10], ['DB press', 26, 10]])
    await session(3, [['DB press', 26, 11], ['DB press', 26, 11], ['DB press', 26, 11]])
  }
  const showProgress = (onCheckIn = vi.fn()) => {
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={() => {}} onOpenExercise={() => {}} onCheckIn={onCheckIn} />)
    return onCheckIn
  }

  it('beforeTheFirstCheckIn_theButtonAndTheLine_andRowsWithoutARating', async () => {
    await pressPlan()
    const onCheckIn = showProgress()
    await userEvent.click(await screen.findByRole('button', { name: 'Check in' }))
    expect(onCheckIn).toHaveBeenCalled()
    expect(screen.getByText('Your first check-in is four weeks after your first bodybuilding session. You can check in any time.')).toBeInTheDocument()
    const parts = screen.getByRole('region', { name: 'Body parts' })
    const rows = within(parts).getAllByRole('listitem')
    // One row per group the plan or the last 7 days work, in the domain's order.
    const expected = grit.muscleProgressRows(await loadHistory(db), (await planFor(db, 'BODYBUILDING')).map(planItemToJs), [], TODAY)
    expect(rows.map((r) => within(r).getByRole('heading').textContent)).toEqual(expected.map((r) => grit.muscleGroupLabel(r.group)))
    const chest = rows[0]!
    expect(within(chest).getByText('3 of 3 planned sets in the last 7 days')).toBeInTheDocument()
    expect(within(chest).getByText('1 of 1 exercise going up')).toBeInTheDocument()
    expect(within(chest).queryByText('On track')).toBeNull()
    // The stage 1 rows stay below.
    expect(screen.getByRole('button', { name: /^DB press/ })).toBeInTheDocument()
  })

  it('afterCheckIns_theLatestRating_andTheEarlierOnesOldestFirst', async () => {
    await pressPlan()
    await db.checkIns.bulkAdd([
      { date: addDays(TODAY, -70), lagging: ['CHEST'], growingWell: [] },
      { date: addDays(TODAY, -40), lagging: [], growingWell: [] },
      { date: addDays(TODAY, -1), lagging: [], growingWell: ['CHEST'] },
    ])
    showProgress()
    const parts = await screen.findByRole('region', { name: 'Body parts' })
    const chest = within(parts).getAllByRole('listitem')[0]!
    expect(within(chest).getByRole('heading').textContent).toBe('Chest')
    expect(within(chest).getByText('Growing well')).toBeInTheDocument()
    expect(within(chest).getByText('Earlier: Lagging · On track')).toBeInTheDocument()
    // DB press works the front delts too: on track at every check-in.
    const delts = within(parts).getAllByRole('listitem').find((r) => within(r).getByRole('heading').textContent === 'Front delts')!
    expect(within(delts).getByText('On track')).toBeInTheDocument()
    expect(within(delts).getByText('Earlier: On track · On track')).toBeInTheDocument()
    expect(screen.queryByText(/Your first check-in/)).toBeNull()
    expect(checkInToJs({ id: 1, date: TODAY, lagging: [], growingWell: [] }).date).toBe(TODAY)
  })

  it('noExerciseThatCanTell_leavesTheGoingUpLineOut', async () => {
    await db.planItems.add({ weekday: 3, exerciseId: await id('Curl'), sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    showProgress()
    const parts = await screen.findByRole('region', { name: 'Body parts' })
    expect(within(parts).getByText('0 of 3 planned sets in the last 7 days')).toBeInTheDocument()
    expect(within(parts).queryByText(/going up/)).toBeNull()
  })

  it('aBodyPartWithNoPlannedSets_showsOnlyWhatWasDone', async () => {
    await pressPlan()
    await session(1, [['Curl', 14, 10]])
    showProgress()
    const parts = await screen.findByRole('region', { name: 'Body parts' })
    const biceps = within(parts).getAllByRole('listitem').find((r) => within(r).getByRole('heading').textContent === 'Biceps')!
    expect(within(biceps).getByText('1 set in the last 7 days')).toBeInTheDocument()
  })

  it('anEmptyPlan_hasNoCheckInButton', async () => {
    await session(3, [['DB press', 26, 10]])
    showProgress()
    expect(await screen.findByText('No exercises in your plan yet')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Check in' })).toBeNull()
    expect(screen.queryByText(/Your first check-in/)).toBeNull()
  })

  it('thePowerliftingSide_hasNoneOfIt', async () => {
    await pressPlan()
    await db.checkIns.add({ date: TODAY, lagging: ['CHEST'], growingWell: [] })
    await onSide('POWERLIFTING')
    showProgress()
    expect(await screen.findByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Check in' })).toBeNull()
    expect(screen.queryByRole('region', { name: 'Body parts' })).toBeNull()
  })
})
