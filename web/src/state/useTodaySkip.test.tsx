import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../db/db'
import { loadHistory } from '../db/db'
import { adviceToJs, planItemToJs } from '../db/mapping'
import type { AdviceRow, TrainingMode } from '../db/schema'
import { skip as storeSkip, skips as storedSkips } from '../db/skips'
import { grit } from '../domain'
import { TODAY, addDays, baseline, exerciseNamed, logSet, set, startSession, todayFixture } from '../test/todayFixture'
import { useToday } from './useToday'
import { useCoach } from './useCoach'

/** 1.13: skipping a planned exercise on Today (skip-a-planned-exercise plan, Task 5), as Android's `SkipTodayViewModelTest`. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squat: number
let bench: number
let row: number
let curl: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squat = (await exerciseNamed(db, 'Squat')).id
  bench = (await exerciseNamed(db, 'Bench')).id
  row = (await exerciseNamed(db, 'Row')).id
  curl = (await exerciseNamed(db, 'Curl')).id
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = (today: () => string = () => TODAY) => renderHook(() => useToday(db, today)).result

/** Plans `exerciseId` on Wednesday (TODAY's weekday) on `mode`'s side. */
async function plan(exerciseId: number, order: number, mode: TrainingMode = 'POWERLIFTING', over: { sets?: number; reps?: number; repsHigh?: number; weekday?: number } = {}): Promise<number> {
  return (await db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: 5, targetRpe: 8, order, mode, ...(mode === 'BODYBUILDING' ? { repsHigh: 12, reps: 8 } : {}), ...over })) as number
}
const onSide = (mode: TrainingMode) => db.settings.put({ key: 'trainingMode', value: mode })
const threePlanned = async () => [await plan(squat, 0), await plan(bench, 1), await plan(row, 2)]
const ids = (vm: { current: { items: { exercise: { id: number } }[] } }) => vm.current.items.map((i) => i.exercise.id)
const openSkipCards = async () =>
  (await db.advice.toArray()).filter((a) => a.type === 'SKIP' && a.status === 'OPEN').map((a) => grit.advicePayload(a.payloadJson)!)
/** Today's OPEN target for `exerciseId`, as stored. */
async function todaysTarget(exerciseId: number) {
  const rows = (await db.advice.toArray()).filter((a) => a.type === 'TARGET' && a.createdAt === TODAY && a.status === 'OPEN')
  return rows.map((a) => grit.advicePayload(a.payloadJson)!).find((p) => p.exerciseId === exerciseId) ?? null
}
/** Squat planned 3 × 5 @ 8, with last week's 150 × 5 @ 7: today's target goes up to 155. */
async function squatWithHistory(): Promise<number> {
  const id = await plan(squat, 0)
  await baseline(db, squat, 190, addDays(TODAY, -30))
  await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squat, 150, { rpe: 7 }))
  return id
}

describe('skipping on Today', () => {
  it('skip_eachReason_storedAndRowLeaves', async () => {
    await plan(squat, 0)
    await plan(bench, 1)
    await plan(row, 2)
    await plan(curl, 3)
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row, curl]))
    const reasons = [['BUSY', squat], ['PAIN', bench], ['TIME', row], [null, curl]] as const
    for (const [reason, id] of reasons) {
      const item = vm.current.items.find((i) => i.exercise.id === id)!
      let done: Awaited<ReturnType<typeof vm.current.skip>> = null
      await act(async () => {
        done = await vm.current.skip(id, item.planned!.id, reason)
      })
      expect(done).toMatchObject({ name: item.exercise.name })
      await waitFor(() => expect(ids(vm)).not.toContain(id))
    }
    expect((await storedSkips(db)).map((s) => [s.exerciseId, s.reason, s.mode, s.date])).toEqual(reasons.map(([reason, id]) => [id, reason, 'POWERLIFTING', TODAY]))
    expect(vm.current.skippedToday.map((s) => s.name)).toEqual(['Squat', 'Bench', 'Row', 'Curl'])
  })

  it('skip_withoutSession_works', async () => {
    const [item] = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    expect(vm.current.sessionId).toBeNull()
    await act(() => vm.current.skip(squat, item!, null))
    await waitFor(() => expect(ids(vm)).toEqual([bench, row]))
    expect(await db.sessions.count()).toBe(0)
    expect(await storedSkips(db)).toHaveLength(1)
  })

  it('skip_refusedForARowWithSets_orNotPlanned', async () => {
    const [item] = await threePlanned()
    await logSet(db, await startSession(db, TODAY), set(squat, 100))
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    let done: unknown = 'not yet'
    await act(async () => {
      done = await vm.current.skip(squat, item!, 'BUSY')
    })
    expect(done).toBeNull()
    act(() => vm.current.addExercise(curl))
    await act(async () => {
      done = await vm.current.skip(curl, null, 'BUSY')
    })
    expect(done).toBeNull()
    expect(await storedSkips(db)).toEqual([])
  })

  it('skip_undo_removesSkipAndOpenCard', async () => {
    // A pain skip of bench (a main lift) gives the hold card.
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    let done: Awaited<ReturnType<typeof vm.current.skip>> = null
    await act(async () => {
      done = await vm.current.skip(bench, items[1]!, 'PAIN')
    })
    expect(await openSkipCards()).toHaveLength(1)
    await act(() => vm.current.undoSkip(done!.skipId))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect(await storedSkips(db)).toEqual([])
    expect(await openSkipCards()).toEqual([])
    expect(vm.current.skippedToday).toEqual([])
  })

  it('skip_undo_afterTheCardWasDismissed_keepsTheDismissal', async () => {
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    let done: Awaited<ReturnType<typeof vm.current.skip>> = null
    await act(async () => {
      done = await vm.current.skip(bench, items[1]!, 'PAIN')
    })
    const card = (await db.advice.where('type').equals('SKIP').first())!
    await db.advice.update(card.id!, { status: 'DISMISSED' })
    const planBefore = await db.planItems.toArray()
    await act(() => vm.current.undoSkip(done!.skipId))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect((await db.advice.get(card.id!))?.status).toBe('DISMISSED')
    expect(await db.planItems.toArray()).toEqual(planBefore)
  })

  it('skip_bringBack_returnsRowInPlace', async () => {
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    await act(() => vm.current.skip(bench, items[1]!, 'BUSY'))
    await waitFor(() => expect(ids(vm)).toEqual([squat, row]))
    expect(vm.current.skippedToday).toMatchObject([{ exerciseId: bench, name: 'Bench' }])
    await act(() => vm.current.bringBack(bench))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect(vm.current.skippedToday).toEqual([])
    expect(await storedSkips(db)).toEqual([])
  })

  it('skip_sameExerciseTwiceOnDay_hidesBoth', async () => {
    // Squat on two plan lines today: one card, and one skip hides it; Bring back returns it.
    const first = await plan(squat, 0)
    await plan(bench, 1)
    await plan(squat, 2, 'POWERLIFTING', { sets: 2, reps: 8 })
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench]))
    await act(() => vm.current.skip(squat, first, null))
    await waitFor(() => expect(ids(vm)).toEqual([bench]))
    expect(vm.current.skippedToday.map((s) => s.exerciseId)).toEqual([squat])
    await act(() => vm.current.bringBack(squat))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench]))
  })

  it('skip_otherSideUnaffected', async () => {
    await plan(squat, 0)
    await plan(squat, 0, 'BODYBUILDING')
    // Skipped on the bodybuilding side today.
    await storeSkip(db, { date: TODAY, mode: 'BODYBUILDING', exerciseId: squat, planItemId: null, reason: 'BUSY' }, () => [])
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.side).toBe('POWERLIFTING')
    expect(ids(vm)).toEqual([squat])
    expect(vm.current.skippedToday).toEqual([])

    // And the bodybuilding side's Today hides it.
    await onSide('BODYBUILDING')
    await waitFor(() => expect(vm.current.side).toBe('BODYBUILDING'))
    await waitFor(() => expect(ids(vm)).toEqual([]))
    expect(vm.current.skippedToday.map((s) => s.exerciseId)).toEqual([squat])
  })

  it('skip_otherSideUnaffected_aPowerliftingSkipLeavesBodybuilding', async () => {
    await plan(squat, 0)
    await plan(squat, 0, 'BODYBUILDING')
    await onSide('BODYBUILDING')
    // Skipped on the powerlifting side today.
    await storeSkip(db, { date: TODAY, mode: 'POWERLIFTING', exerciseId: squat, planItemId: null, reason: 'BUSY' }, () => [])
    const vm = show()
    await waitFor(() => expect(vm.current.side).toBe('BODYBUILDING'))
    await waitFor(() => expect(ids(vm)).toEqual([squat]))
    expect(vm.current.skippedToday).toEqual([])

    // And the powerlifting side's Today hides it.
    await onSide('POWERLIFTING')
    await waitFor(() => expect(vm.current.side).toBe('POWERLIFTING'))
    await waitFor(() => expect(ids(vm)).toEqual([]))
    expect(vm.current.skippedToday.map((s) => s.exerciseId)).toEqual([squat])
  })

  it('skip_onTheBodybuildingSide_isStampedWithIt', async () => {
    await onSide('BODYBUILDING')
    const item = await plan(curl, 0, 'BODYBUILDING')
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([curl]))
    await act(() => vm.current.skip(curl, item, 'TIME'))
    expect((await storedSkips(db)).map((s) => [s.mode, s.planItemId])).toEqual([['BODYBUILDING', item]])
  })

  it('addSkippedExerciseFromPicker_unskipsIt', async () => {
    // Adding a skipped exercise again, from the picker, by row or by catalogue name, or from the Exercises tab, takes
    // its skip back the way Bring back does: it is its planned row again, with its menu, and not an added one.
    const items = await threePlanned()
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    await act(() => vm.current.skip(bench, items[1]!, 'PAIN'))
    await waitFor(() => expect(ids(vm)).toEqual([squat, row]))
    expect(await openSkipCards()).toHaveLength(1)

    act(() => vm.current.addExercise(bench))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect(vm.current.items[1]!.planned?.id).toBe(items[1])
    expect(vm.current.skippedToday).toEqual([])
    expect(await storedSkips(db)).toEqual([])
    // As Bring back: the skip's open card goes with it.
    expect(await openSkipCards()).toEqual([])

    // By catalogue name too.
    await act(() => vm.current.skip(row, items[2]!, null))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench]))
    act(() => void vm.current.addFromCatalogue('Row'))
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect(vm.current.items[2]!.planned?.id).toBe(items[2])

    // And from the Exercises tab's "Add to today".
    await act(() => vm.current.skip(squat, items[0]!, 'TIME'))
    await waitFor(() => expect(ids(vm)).toEqual([bench, row]))
    let added: unknown = null
    await act(async () => {
      added = await vm.current.addToOpenSession(null, squat)
    })
    expect(added).toBe('ADDED')
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, row]))
    expect(vm.current.items[0]!.planned?.id).toBe(items[0])
    expect(await storedSkips(db)).toEqual([])
  })

  it('skip_yesterdaysSkip_notHiddenToday', async () => {
    await plan(squat, 0)
    await storeSkip(db, { date: addDays(TODAY, -1), mode: 'POWERLIFTING', exerciseId: squat, planItemId: null, reason: null }, () => [])
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([squat]))
    expect(vm.current.skippedToday).toEqual([])
  })

  it('finish_countsSkipped', async () => {
    const items = await threePlanned()
    await logSet(db, await startSession(db, TODAY), set(squat, 100))
    // Yesterday's and the other side's skips are not today's.
    await storeSkip(db, { date: addDays(TODAY, -1), mode: 'POWERLIFTING', exerciseId: bench, planItemId: null, reason: null }, () => [])
    await storeSkip(db, { date: TODAY, mode: 'BODYBUILDING', exerciseId: row, planItemId: null, reason: null }, () => [])
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    await act(() => vm.current.skip(bench, items[1]!, 'TIME'))
    await waitFor(() => expect(ids(vm)).toEqual([squat, row]))
    await act(() => vm.current.skip(row, items[2]!, null))
    await waitFor(() => expect(ids(vm)).toEqual([squat]))
    await act(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).not.toBeNull())
    expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 0, skipped: 2 })
  })

  it('finish_withNoSkips_countsNone', async () => {
    await plan(squat, 0)
    await logSet(db, await startSession(db, TODAY), set(squat, 100))
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(1))
    await act(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).not.toBeNull())
    // Unchanged from 1.12: no `skipped` at all.
    expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 0 })
  })

  it('skip_doubleCall_storesOneSkip', async () => {
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    const results: unknown[] = []
    await act(async () => {
      results.push(...(await Promise.all([vm.current.skip(squat, items[0]!, 'BUSY'), vm.current.skip(squat, items[0]!, 'PAIN')])))
    })
    expect(results.filter((r) => r != null)).toHaveLength(1)
    expect((await storedSkips(db)).map((s) => s.reason)).toEqual(['BUSY'])
  })
})

describe('skips and the coach on Today', () => {
  it('noSkip_targetGoesUp', async () => {
    await squatWithHistory()
    show()
    await waitFor(async () => expect((await todaysTarget(squat))?.weightKg).toBe(155))
  })

  it('painSkip_nextTodayTarget_isHeld', async () => {
    const item = await squatWithHistory()
    // A pain skip since, on either side: the weight is held until squat is logged again.
    await storeSkip(db, { date: addDays(TODAY, -3), mode: 'BODYBUILDING', exerciseId: squat, planItemId: null, reason: 'PAIN' }, () => [])
    const vm = show()
    await waitFor(async () => expect(await todaysTarget(squat)).not.toBeNull())
    expect((await todaysTarget(squat))?.weightKg).toBe(150)
    expect(vm.current.items.map((i) => i.planned?.id)).toEqual([item])
  })

  it('painSkipToday_thenUndo_targetIsWorkedOutAgainWithoutTheHold', async () => {
    const item = await squatWithHistory()
    const vm = show()
    await waitFor(async () => expect((await todaysTarget(squat))?.weightKg).toBe(155))
    let done: Awaited<ReturnType<typeof vm.current.skip>> = null
    await act(async () => {
      done = await vm.current.skip(squat, item, 'PAIN')
    })
    // The pain skip deletes today's target; the coach works it out again, held.
    await waitFor(async () => expect((await todaysTarget(squat))?.weightKg).toBe(150))
    await act(() => vm.current.undoSkip(done!.skipId))
    await waitFor(async () => expect((await todaysTarget(squat))?.weightKg).toBe(155))
    await waitFor(() => expect(vm.current.items[0]?.suggestion).not.toBeNull())
  })

  it('painSkip_heldRangeTarget_appliesTheHeldRepsAim', async () => {
    // Curl 20 × 12, 12, 12 on the bodybuilding side last week: every set at the top of 8–12.
    await onSide('BODYBUILDING')
    const item = await plan(curl, 0, 'BODYBUILDING')
    const last = (await db.sessions.add({ date: addDays(TODAY, -7), bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })) as number
    for (let i = 0; i < 3; i++) await logSet(db, last, set(curl, 20, { reps: 12 }))
    await storeSkip(db, { date: addDays(TODAY, -3), mode: 'POWERLIFTING', exerciseId: curl, planItemId: null, reason: 'PAIN' }, () => [])
    await db.sessions.add({ date: TODAY, bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })
    const vm = show()
    await waitFor(() => expect(vm.current.items[0]?.suggestion).toBeTruthy())
    const suggestion = vm.current.items[0]!.suggestion!
    expect(suggestion.target.weightKg).toBe(20)
    expect(vm.current.items[0]!.planned?.id).toBe(item)
    // Held, the reps aim is last time's top, not the low end an increase would reset to.
    expect(suggestion.applyReps).toBe(12)
  })

  it('skipEveryPlannedExercise_listIsEmpty_skippedLineHasThemAll', async () => {
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    await act(() => vm.current.skip(squat, items[0]!, null))
    await waitFor(() => expect(ids(vm)).toEqual([bench, row]))
    await act(() => vm.current.skip(bench, items[1]!, null))
    await waitFor(() => expect(ids(vm)).toEqual([row]))
    await act(() => vm.current.skip(row, items[2]!, null))
    await waitFor(() => expect(ids(vm)).toEqual([]))
    expect(vm.current.skippedToday.map((s) => s.name)).toEqual(['Squat', 'Bench', 'Row'])
  })

  it('afterSwapApplied_skippedLine_dropsSwappedName', async () => {
    const items = await threePlanned()
    const vm = show()
    await waitFor(() => expect(ids(vm)).toHaveLength(3))
    await act(() => vm.current.skip(row, items[2]!, 'BUSY'))
    await waitFor(() => expect(vm.current.skippedToday.map((s) => s.exerciseId)).toEqual([row]))

    // The coach's swap puts another exercise in the row's place: there is nothing left to bring back.
    await db.planItems.update(items[2]!, { exerciseId: curl })
    await waitFor(() => expect(ids(vm)).toEqual([squat, bench, curl]))
    expect(vm.current.skippedToday).toEqual([])
    expect(await storedSkips(db)).toHaveLength(1)
  })

  it('reskip_newReason_replacesCard_notBlocked', async () => {
    // Curl on the bodybuilding side, busy a week ago: today's busy skip is the second, and gives a card.
    await onSide('BODYBUILDING')
    const item = await plan(curl, 0, 'BODYBUILDING')
    await storeSkip(db, { date: addDays(TODAY, -7), mode: 'BODYBUILDING', exerciseId: curl, planItemId: item, reason: 'BUSY' }, () => [])
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([curl]))
    await act(() => vm.current.skip(curl, item, 'BUSY'))
    await waitFor(async () => expect((await openSkipCards()).map((p) => p.skipReason)).toEqual(['BUSY']))
    await waitFor(() => expect(vm.current.skippedToday.map((s) => s.exerciseId)).toEqual([curl]))

    // Skipping it again today with another reason replaces the skip's card: the old one does not block the new.
    await act(() => vm.current.skip(curl, item, 'PAIN'))
    await waitFor(async () => expect((await openSkipCards()).map((p) => p.skipReason)).toEqual(['PAIN']))
    expect(await storedSkips(db)).toHaveLength(2)
  })
})

describe('a pain skip always tells you (final review, 1.13)', () => {
  it('painSkip_afterAnEarlierDaysBusyCard_replacesIt', async () => {
    // Curl on the bodybuilding side, busy 7 and 2 days ago: the second gives the real busy card, still open.
    await onSide('BODYBUILDING')
    const item = await plan(curl, 0, 'BODYBUILDING')
    await storeSkip(db, { date: addDays(TODAY, -7), mode: 'BODYBUILDING', exerciseId: curl, planItemId: item, reason: 'BUSY' }, () => [])
    const twoDaysAgo = addDays(TODAY, -2)
    await storeSkip(db, { date: twoDaysAgo, mode: 'BODYBUILDING', exerciseId: curl, planItemId: item, reason: 'BUSY' }, async (stored, all) => {
      const [history, plan, advice] = await Promise.all([loadHistory(db), db.planItems.toArray(), db.advice.toArray()])
      return grit.coachingAfterSkip(stored, all, history, plan.map((p) => planItemToJs(p as typeof p & { id: number })), twoDaysAgo, advice.map((a) => adviceToJs(a as AdviceRow & { id: number })))
    })
    expect((await openSkipCards()).map((p) => p.skipReason)).toEqual(['BUSY'])

    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([curl]))
    await act(() => vm.current.skip(curl, item, 'PAIN'))

    await waitFor(async () => expect((await openSkipCards()).map((p) => p.skipReason)).toEqual(['PAIN']))
    cleanup()
    const coach = renderHook(() => useCoach(db, () => TODAY)).result
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    expect(coach.current.cards[0]!.detail).toMatchObject({ kind: 'skip', exerciseId: curl })
    const stored = (await db.advice.where('type').equals('SKIP').toArray()).filter((a) => a.status === 'OPEN')
    expect(stored.map((a) => a.reason)).toEqual([expect.stringContaining('If the pain persists or gets worse, see a physician.')])
  })
})

describe('the SKIP card on Coach', () => {
  const showCoach = () => renderHook(() => useCoach(db, () => TODAY)).result

  async function painSkipCard(exerciseId: number, mode: TrainingMode) {
    await onSide(mode)
    const item = await plan(exerciseId, 2, mode)
    await plan(squat, 0, mode, { weekday: 1 })
    await plan(bench, 1, mode, { weekday: 1 })
    const vm = show()
    await waitFor(() => expect(ids(vm)).toEqual([exerciseId]))
    await act(() => vm.current.skip(exerciseId, item, 'PAIN'))
    cleanup()
    const card = (await db.advice.where('type').equals('SKIP').toArray()).find((a) => a.status === 'OPEN')!
    return { card, payload: grit.advicePayload(card.payloadJson)! }
  }

  for (const mode of ['BODYBUILDING', 'POWERLIFTING'] as const) {
    it(`coach_skipCard_swapApplies_${mode.toLowerCase()}`, async () => {
      const { card, payload } = await painSkipCard(curl, mode)
      expect(payload.alternatives.length).toBeGreaterThan(0)
      expect(card.mode).toBe(mode)
      const coach = showCoach()
      await waitFor(() => expect(coach.current.cards).toHaveLength(1))
      expect(coach.current.cards[0]!.detail).toEqual({ kind: 'skip', exerciseId: curl, name: 'Curl', alternatives: [...payload.alternatives] })

      const choice = payload.alternatives[0]!
      await act(() => coach.current.apply(card.id!, choice))

      await waitFor(async () => expect((await db.advice.get(card.id!))?.status).toBe('APPLIED'))
      expect(coach.current.message?.text).toBe(`Swapped Curl for ${choice}`)
      const names = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
      const side = (await db.planItems.toArray()).filter((p) => (p.mode ?? 'POWERLIFTING') === mode).map((p) => names.get(p.exerciseId))
      expect(side).toContain(choice)
      expect(side).not.toContain('Curl')
      await waitFor(() => expect(coach.current.cards).toEqual([]))
    })
  }

  it('undoAfterTheSwapWasApplied_keepsTheSwap_andThePlan', async () => {
    const { card, payload } = await painSkipCard(curl, 'BODYBUILDING')
    const skipId = payload.skipId!
    const coach = showCoach()
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    await act(() => coach.current.apply(card.id!, payload.alternatives[0]!))
    await waitFor(async () => expect((await db.advice.get(card.id!))?.status).toBe('APPLIED'))
    const planAfterSwap = await db.planItems.toArray()

    // Undo on Today afterwards: the skip goes, the applied swap and the plan stay as they are.
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    await act(() => vm.current.undoSkip(skipId))
    expect(await storedSkips(db)).toEqual([])
    expect((await db.advice.get(card.id!))?.status).toBe('APPLIED')
    expect(await db.planItems.toArray()).toEqual(planAfterSwap)
    expect(planAfterSwap.map((p) => p.exerciseId)).not.toContain(curl)
  })

  it('coach_skipCard_swapLeavesTheOtherSide', async () => {
    await db.planItems.add({ weekday: 5, exerciseId: curl, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    const { card, payload } = await painSkipCard(curl, 'POWERLIFTING')
    const coach = showCoach()
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    await act(() => coach.current.apply(card.id!, payload.alternatives[0]!))
    await waitFor(async () => expect((await db.advice.get(card.id!))?.status).toBe('APPLIED'))
    expect((await db.planItems.toArray()).filter((p) => p.mode === 'BODYBUILDING').map((p) => p.exerciseId)).toEqual([curl])
  })

  it('coach_skipCard_anUnknownChoice_appliesNothing', async () => {
    const { card } = await painSkipCard(curl, 'BODYBUILDING')
    const coach = showCoach()
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    const before = await db.planItems.toArray()
    await act(() => coach.current.apply(card.id!, 'Tyre flip'))
    expect((await db.advice.get(card.id!))?.status).toBe('OPEN')
    expect(await db.planItems.toArray()).toEqual(before)
  })

  it('coach_mainLiftPainCard_dismissOnly', async () => {
    const { card, payload } = await painSkipCard(squat, 'POWERLIFTING')
    expect(payload.alternatives).toEqual([])
    expect(card.lift).toBe('SQUAT')
    const coach = showCoach()
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    expect(coach.current.cards[0]!.detail).toEqual({ kind: 'skip', exerciseId: squat, name: 'Squat', alternatives: [] })
    await act(() => coach.current.dismiss(card.id!))
    await waitFor(async () => expect((await db.advice.get(card.id!))?.status).toBe('DISMISSED'))
  })

  it('coach_skipCard_hiddenOnceItsExerciseLeftThatSidesPlan', async () => {
    const { card } = await painSkipCard(squat, 'POWERLIFTING')
    const coach = showCoach()
    await waitFor(() => expect(coach.current.cards).toHaveLength(1))
    await db.planItems.filter((p) => p.exerciseId === squat).delete()
    await waitFor(() => expect(coach.current.cards).toEqual([]))
    expect((await db.advice.get(card.id!))?.status).toBe('OPEN')
  })

  it('coach_skipCard_onlyOnItsOwnSide', async () => {
    await painSkipCard(squat, 'POWERLIFTING')
    await plan(squat, 0, 'BODYBUILDING', { weekday: 1 })
    await onSide('BODYBUILDING')
    const coach = showCoach()
    await waitFor(() => expect(coach.current.loading).toBe(false))
    await waitFor(() => expect(coach.current.side).toBe('BODYBUILDING'))
    expect(coach.current.cards).toEqual([])
  })
})
