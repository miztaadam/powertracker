import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../db/db'
import { resolveCatalogue } from '../db/catalogue'
import { updateSettings } from '../db/db'
import { advice, insertAdvice, payload, planOf, rx, statuses } from '../test/coachFixture'
import { TODAY, addDays, baseline, exerciseNamed, plan, todayFixture } from '../test/todayFixture'
import { grit } from '../domain'
import { useCoach } from './useCoach'

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

async function coach(cards: number) {
  const hook = renderHook(() => useCoach(db, () => TODAY))
  await waitFor(() => {
    expect(hook.result.current.loading).toBe(false)
    expect(hook.result.current.cards).toHaveLength(cards)
  })
  return hook.result
}

describe('useCoach', () => {
  it('cards_leaveOutTargets_andOrderByTypeThenNewest', async () => {
    await insertAdvice(
      db,
      advice('WEAK_POINT', 'SQUAT', 'Prioritise it.', payload.weakPoint('SQUAT', 'FLOOR'), { severity: 4 }),
      advice('STALL', 'BENCH', 'older', payload.accessories([]), { createdAt: addDays(TODAY, -3) }),
      advice('STALL', 'SQUAT', 'newer', payload.accessories([])),
      advice('TARGET', 'SQUAT', 'target', payload.target(await id('Squat')), { severity: 0 }),
      advice('RE_ENTRY', null, 'back', payload.reEntry(), { severity: 20 }),
      advice('DELOAD', null, 'tired', payload.deload(), { severity: 2 }),
      advice('DELOAD', null, 'done with', payload.deload(), { status: 'APPLIED' }),
    )
    const result = await coach(5)
    expect(result.current.cards.map((c) => [c.advice.type, c.advice.reason])).toEqual([
      ['DELOAD', 'tired'],
      ['RE_ENTRY', 'back'],
      ['STALL', 'newer'],
      ['STALL', 'older'],
      ['WEAK_POINT', 'Prioritise it.'],
    ])
    expect(result.current.cards[0]?.detail).toEqual({ kind: 'deload', volumePct: 60, loadPct: 90 })
    expect(result.current.cards[1]?.detail).toEqual({ kind: 'reEntry', loadPct: 85 })
  })

  it('weakPoint_showsRatioAgainstTypicalRange', async () => {
    await baseline(db, await id('Squat'), 190, TODAY)
    await baseline(db, await id('Deadlift'), 250, TODAY)
    await insertAdvice(db, advice('WEAK_POINT', 'SQUAT', 'Prioritise it.', payload.weakPoint('SQUAT', null)))
    const detail = (await coach(1)).current.cards[0]?.detail
    expect(detail).toMatchObject({ kind: 'weakPoint', ratio: { numerator: 'SQUAT', denominator: 'DEADLIFT', valuePct: 76, lowPct: 80, highPct: 90 }, missPoint: null, muscles: [] })
  })

  it('weakPoint_withAMissPoint_showsNoRatioThatIsInRange', async () => {
    await baseline(db, await id('Squat'), 212.5, TODAY)
    await baseline(db, await id('Deadlift'), 250, TODAY)
    await insertAdvice(db, advice('WEAK_POINT', 'SQUAT', 'Prioritise it.', payload.weakPoint('SQUAT', 'FLOOR')))
    const detail = (await coach(1)).current.cards[0]?.detail
    expect(detail).toMatchObject({
      kind: 'weakPoint',
      ratio: null,
      missPoint: 'FLOOR',
      muscles: ['Quads', 'Glutes'],
      additions: [rx('Pause squat', 3, 3, 5), rx('Front squat', 3, 3, 5)],
    })
  })

  it('weakPoint_musclesComeFromTheDomainForEveryStickingPoint', async () => {
    const muscles = (lift: string) => ['FLOOR', 'MID', 'LOCKOUT'].map((miss) => [...grit.accessoryMuscles(lift, miss)])
    expect(muscles('SQUAT')).toEqual([['Quads', 'Glutes'], ['Upper back', 'Trunk'], ['Glutes', 'Hips']])
    expect(muscles('BENCH')).toEqual([['Pecs', 'Front delts'], ['Pecs', 'Triceps'], ['Triceps']])
    expect(muscles('DEADLIFT')).toEqual([['Quads', 'Lats', 'Position'], ['Hamstrings', 'Lats'], ['Glutes', 'Upper back']])
    expect([...grit.accessoryMuscles('SQUAT', null)]).toEqual([])

    await insertAdvice(db, advice('WEAK_POINT', 'DEADLIFT', 'Prioritise it.', payload.weakPoint('DEADLIFT', 'MID')))
    expect((await coach(1)).current.cards[0]?.detail).toMatchObject({ kind: 'weakPoint', muscles: ['Hamstrings', 'Lats'] })
  })

  it('stallApply_withoutTheLiftInThePlan_usesFirstTrainingDay_andSkipsWhatIsThere', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingDays: [6, 2] }))
    await plan(db, await id('JM press'), { weekday: 2, sets: 3, reps: 10, order: 0 })
    const items = [rx('close-grip BENCH', 3, 3, 5), rx('JM press', 3, 8, 10), rx('No such lift', 3, 8, 10)]
    await insertAdvice(db, advice('STALL', 'BENCH', "Bench e1RM hasn't improved for 3 weeks.", payload.accessories(items)))
    const result = await coach(1)

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    await waitFor(() => expect(result.current.cards).toHaveLength(0))
    expect(await statuses(db)).toEqual(['APPLIED'])
    expect(result.current.message?.text).toBe('Added 1 exercise to Tuesday')
    expect(await planOf(db)).toEqual([
      { weekday: 2, exerciseId: await id('JM press'), sets: 3, reps: 10, targetRpe: 8, order: 0 },
      { weekday: 2, exerciseId: await id('Close-grip bench'), sets: 3, reps: 5, targetRpe: 8, order: 1, mode: 'POWERLIFTING' },
    ])
  })

  it('stallApply_whenEverythingIsPlanned_saysSo_andWithNoTrainingDaysUsesMonday', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingDays: [] }))
    await plan(db, await id('JM press'), { weekday: 1, sets: 3, reps: 10, order: 0 })
    await insertAdvice(db, advice('STALL', 'BENCH', 'Stalled.', payload.accessories([rx('JM press', 3, 8, 10)])))
    const result = await coach(1)

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    expect(result.current.message?.text).toBe('Already in your plan on Monday')
    expect(await statuses(db)).toEqual(['APPLIED'])
    expect(await planOf(db)).toHaveLength(1)
  })

  it('apply_namesTheDayInTheDeviceLanguage', async () => {
    // A German phone: whenever no locale is asked for, the device's is used.
    const RealFormat = Intl.DateTimeFormat
    vi.spyOn(Intl, 'DateTimeFormat').mockImplementation(function (locales?: Intl.LocalesArgument, options?: Intl.DateTimeFormatOptions) {
      return new RealFormat(locales ?? 'de-DE', options)
    } as typeof Intl.DateTimeFormat)
    await plan(db, await id('Bench'), { weekday: 7 })
    await insertAdvice(db, advice('STALL', 'BENCH', 'Stalled.', payload.accessories([rx('JM press', 3, 8, 10)])))
    const result = await coach(1)

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    expect(result.current.message?.text).toBe('Added 1 exercise to Sonntag')
  })

  /**
   * Strict press accessories have no seeded rows. Apply finds or adds their catalogue rows in the same transaction
   * as the plan items, instead of silently skipping them.
   */
  it('applyStall_pressAccessories_resolvesCatalogueRows', async () => {
    const names = ['Seated barbell press', 'Incline bench press', 'Landmine press']
    expect((await db.exercises.toArray()).filter((e) => names.includes(e.name))).toEqual([])
    const items = [
      rx('Seated barbell press', 3, 8, 10),
      // An alias or another case still lands on the catalogue name.
      rx('incline BENCH press', 3, 8, 10),
      rx('Landmine press', 3, 8, 10),
      rx('No such lift', 3, 8, 10),
    ]
    await insertAdvice(db, advice('STALL', 'STRICT_PRESS', "Strict press e1RM hasn't improved for 3 weeks.", payload.accessories(items)))
    const result = await coach(1)

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    await waitFor(() => expect(result.current.cards).toHaveLength(0))
    expect(await statuses(db)).toEqual(['APPLIED'])
    expect(result.current.message?.text).toBe('Added 3 exercises to Monday')
    const rows = new Map((await db.exercises.toArray()).map((e) => [e.id, e]))
    const added = await planOf(db)
    expect(added.map((p) => rows.get(p.exerciseId)?.name)).toEqual(names)
    for (const [i, item] of added.entries()) {
      const row = rows.get(item.exerciseId)!
      expect(row).toMatchObject({ isBuiltIn: true, archived: false, muscles: [...grit.findInLibrary(row.name)!.muscles] })
      expect(item).toEqual({ weekday: 1, exerciseId: row.id, sets: 3, reps: 10, targetRpe: 8, order: i, mode: 'POWERLIFTING' })
    }
    expect((await db.exercises.toArray()).filter((e) => e.name === 'No such lift')).toEqual([])
  })

  it('applyStall_whenAPlanWriteFails_addsNoCatalogueRow', async () => {
    await insertAdvice(db, advice('STALL', 'STRICT_PRESS', 'Stalled.', payload.accessories([rx('Landmine press', 3, 8, 10), rx('Seated barbell press', 3, 8, 10)])))
    const before = await db.exercises.count()
    const result = await coach(1)
    let calls = 0
    const add = db.planItems.add.bind(db.planItems)
    // The second plan item fails, after the first catalogue row and plan item were written.
    vi.spyOn(db.planItems, 'add').mockImplementation((...args) => {
      if (++calls === 2) throw new Error('disk full')
      return add(...args)
    })

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    expect(result.current.message?.text).toBe("Couldn't update — try again")
    expect(await db.exercises.count()).toBe(before)
    expect(await planOf(db)).toEqual([])
    expect(await statuses(db)).toEqual(['OPEN'])
  })

  /** A weak point's options come from the catalogue: one without a row is still offered, and Apply adds its row. */
  it('weakPointApply_unseededAccessory_resolvesCatalogueRow', async () => {
    await db.exercises.delete(await id('Front squat'))
    await insertAdvice(db, advice('WEAK_POINT', 'SQUAT', 'Prioritise it.', payload.weakPoint('SQUAT', 'FLOOR')))
    const result = await coach(1)
    expect(result.current.cards[0]?.detail).toMatchObject({ additions: [rx('Pause squat', 3, 3, 5), rx('Front squat', 3, 3, 5)] })

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    expect(result.current.message?.text).toBe('Added 2 exercises to Monday')
    const front = await exerciseNamed(db, 'Front squat')
    expect(front).toMatchObject({ isBuiltIn: true, archived: false, category: 'VARIATION', parentLift: 'SQUAT' })
    expect((await planOf(db)).map((p) => p.exerciseId)).toEqual([await id('Pause squat'), front.id])
  })

  it('dismiss_marksDismissed', async () => {
    await insertAdvice(db, advice('DELOAD', null, 'Signs of fatigue.', payload.deload()))
    const result = await coach(1)
    await act(() => result.current.dismiss(result.current.cards[0]!.advice.id))
    await waitFor(() => expect(result.current.cards).toHaveLength(0))
    expect(await statuses(db)).toEqual(['DISMISSED'])
    expect(result.current.message).toBeNull()
  })

  it('applyOrDismiss_whenStoringFails_restoresTheCardAndSaysSo', async () => {
    await insertAdvice(db, advice('DELOAD', null, 'Signs of fatigue.', payload.deload()))
    vi.spyOn(db.advice, 'update').mockRejectedValue(new Error('disk full'))
    const result = await coach(1)
    const adviceId = result.current.cards[0]!.advice.id

    await act(() => result.current.apply(adviceId))
    expect(result.current.message?.text).toBe("Couldn't update — try again")
    expect(result.current.cards.map((c) => c.advice.id)).toEqual([adviceId])
    const first = result.current.message?.id

    await act(() => result.current.dismiss(adviceId))
    expect(result.current.message?.text).toBe("Couldn't update — try again")
    // A new message each time, so the snackbar shows again.
    expect(result.current.message?.id).not.toBe(first)
    expect(result.current.cards.map((c) => c.advice.id)).toEqual([adviceId])
    expect(await statuses(db)).toEqual(['OPEN'])
  })

  it('apply_tappedTwice_addsToThePlanOnce', async () => {
    await plan(db, await id('Bench'), { weekday: 3 })
    await insertAdvice(db, advice('STALL', 'BENCH', 'Stalled.', payload.accessories([rx('JM press', 3, 8, 10)])))
    const result = await coach(1)
    const adviceId = result.current.cards[0]!.advice.id

    await act(async () => {
      await Promise.all([result.current.apply(adviceId), result.current.apply(adviceId)])
    })

    expect(await planOf(db)).toHaveLength(2)
    expect(await statuses(db)).toEqual(['APPLIED'])
  })

  it('feed_hidesOpenDeloadAndReEntryOlderThanFourteenDays', async () => {
    await insertAdvice(
      db,
      advice('DELOAD', null, 'stale deload', payload.deload(), { createdAt: addDays(TODAY, -15) }),
      advice('RE_ENTRY', null, 'stale re-entry', payload.reEntry(), { createdAt: addDays(TODAY, -14) }),
      advice('DELOAD', null, 'fresh deload', payload.deload(), { createdAt: addDays(TODAY, -13) }),
      advice('STALL', 'BENCH', 'old stall', payload.accessories([]), { createdAt: addDays(TODAY, -40) }),
    )
    const result = await coach(2)
    expect(result.current.cards.map((c) => c.advice.reason)).toEqual(['fresh deload', 'old stall'])
  })

  // ---- Strict press cards (strict press spec §5.3): shown whatever the Progress switch says ----

  /** A strict press miss off the shoulders offers catalogue accessories that have no row yet, and Apply adds them. */
  it('weakPointApply_pressMissOffTheShoulders_resolvesCatalogueRows', async () => {
    await insertAdvice(db, advice('WEAK_POINT', 'STRICT_PRESS', 'Prioritise it.', payload.weakPoint('STRICT_PRESS', 'FLOOR')))
    const result = await coach(1)
    const card = result.current.cards[0]!
    expect(card.advice.lift).toBe('STRICT_PRESS')
    // Strict press is in no ratio: the card shows the sticking point and its muscles only.
    expect(card.detail).toEqual({
      kind: 'weakPoint',
      ratio: null,
      missPoint: 'FLOOR',
      muscles: ['Front delts', 'Upper chest'],
      additions: [rx('Seated barbell press', 3, 8, 10), rx('Incline bench press', 3, 8, 10)],
    })

    await act(() => result.current.apply(card.advice.id))

    await waitFor(() => expect(result.current.cards).toHaveLength(0))
    expect(await statuses(db)).toEqual(['APPLIED'])
    expect(result.current.message?.text).toBe('Added 2 exercises to Monday')
    const rows = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    expect((await planOf(db)).map((p) => rows.get(p.exerciseId))).toEqual(['Seated barbell press', 'Incline bench press'])
  })

  it('pressStall_knowsItsLift_andApplyUsesThePressDay', async () => {
    const press = await resolveCatalogue(db, 'Strict press')
    // Squat on Monday (the first training day), strict press on Thursday and Saturday.
    await plan(db, await id('Squat'), { weekday: 1 })
    await plan(db, press, { weekday: 6 })
    await plan(db, press, { weekday: 4 })
    const session = (await db.sessions.add({ date: addDays(TODAY, -2), bodyweightKg: null, readiness: null })) as number
    await db.sets.add({ sessionId: session, exerciseId: press, weightKg: 60, reps: 1, rpe: 10, failed: false, missPoint: null, isTopSet: false })
    await insertAdvice(db, advice('STALL', 'STRICT_PRESS', "Strict press e1RM hasn't improved for 3 weeks.", payload.accessories([rx('Landmine press', 3, 8, 10)])))
    const result = await coach(1)

    // The card is the press's own: its lift, its e1RM and its trend.
    expect(result.current.cards[0]!.advice.lift).toBe('STRICT_PRESS')
    expect(result.current.cards[0]!.detail).toMatchObject({ kind: 'stall', e1rmKg: 60, trend: [60] })

    await act(() => result.current.apply(result.current.cards[0]!.advice.id))

    expect(result.current.message?.text).toBe('Added 1 exercise to Thursday')
    expect((await planOf(db)).filter((p) => p.weekday === 4).map((p) => p.order)).toEqual([0, 1])
  })

  it('pressCards_showWithTheProgressSwitchOff', async () => {
    expect((await updateSettings(db, (s) => s)).showStrictPress).toBe(false)
    await insertAdvice(db, advice('STALL', 'STRICT_PRESS', 'Stalled.', payload.accessories([rx('Landmine press', 3, 8, 10)])), advice('WEAK_POINT', 'STRICT_PRESS', 'Prioritise it.', payload.weakPoint('STRICT_PRESS', 'LOCKOUT')))
    const result = await coach(2)
    expect(result.current.cards.map((c) => [c.advice.type, c.advice.lift])).toEqual([
      ['STALL', 'STRICT_PRESS'],
      ['WEAK_POINT', 'STRICT_PRESS'],
    ])
  })
})
