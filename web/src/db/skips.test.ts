import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Dexie from 'dexie'
import { grit, JsAdvice, JsPlanChange, type JsSkip } from '../domain'
import { applyPlanChange, deletePlanItem, replacePlan, updatePlanItem } from './catalogue'
import { ensureSeeded, installedVersion, loadHistory, openGritDb, type GritDb } from './db'
import { adviceToJs, planItemToJs } from './mapping'
import { SCHEMA_VERSIONS, type AdviceRow, type SchemaVersion, type TrainingMode } from './schema'
import { skip, skips, skipToJs, unskip } from './skips'

/** 1.13: skipped planned exercises in the web app's storage (schema version 5), as Android's `SkipStorageTest`. */

let n = 0
let name = ''
const open: GritDb[] = []
beforeEach(() => {
  name = `grit-skips-test-${++n}`
})
afterEach(async () => {
  for (const d of open.splice(0)) d.close()
  await Dexie.delete(name)
})
async function openDb(versions?: SchemaVersion[]) {
  const db = await openGritDb(name, versions ? { schemaVersions: versions } : {})
  open.push(db)
  return db
}

const TODAY = '2026-11-18'
const YESTERDAY = '2026-11-17'
const P = 'com.powertracker.domain.model.AdvicePayload.'
const none = () => [] as JsAdvice[]

/** Two exercises, ids 1 and 12, so an id is never matched by its digits. */
async function withExercises(db: GritDb): Promise<void> {
  await db.exercises.bulkAdd([
    { id: 1, name: 'Squat', category: 'SQUAT', parentLift: null, muscles: ['Quads'], isBuiltIn: true, archived: false },
    { id: 12, name: 'Curl', category: 'ACCESSORY', parentLift: null, muscles: ['Biceps'], isBuiltIn: true, archived: false },
  ])
}

const target = (exerciseId: number, mode: TrainingMode, createdAt = TODAY, status = 'OPEN'): Omit<AdviceRow, 'id'> => ({
  type: 'TARGET',
  lift: null,
  createdAt,
  reason: 'Beat it.',
  severity: 1,
  status,
  payloadJson: `{"type":"${P}Target","exerciseId":${exerciseId},"weightKg":100.0,"reps":5,"rpe":8.0,"sets":3}`,
  mode,
})
const cardJson = (skipId: number, exerciseId: number, reason: string | null) =>
  `{"type":"${P}Skipped","skipId":${skipId},"exerciseId":${exerciseId},"name":"Curl","reason":${reason == null ? 'null' : `"${reason}"`},"alternatives":["Cable curl"]}`
const card = (skipId: number, exerciseId: number, reason: string | null, status = 'OPEN', mode: TrainingMode = 'BODYBUILDING'): Omit<AdviceRow, 'id'> => ({
  type: 'SKIP',
  lift: null,
  createdAt: TODAY,
  reason: `Skipped for ${reason}`,
  severity: 3,
  status,
  payloadJson: cardJson(skipId, exerciseId, reason),
  mode,
})
/** What a `cards` callback returns: the advice as the domain gives it, id 0. */
const jsCard = (stored: JsSkip): JsAdvice =>
  new JsAdvice(0, 'SKIP', null, stored.date, `Skipped for ${stored.reason}`, 3, 'OPEN', cardJson(stored.id, stored.exerciseId, stored.reason ?? null), stored.mode)

const curlSkip = (reason: 'BUSY' | 'PAIN' | 'TIME' | null, over: Partial<{ date: string; mode: TrainingMode; exerciseId: number; planItemId: number | null }> = {}) => ({
  date: TODAY,
  mode: 'BODYBUILDING' as TrainingMode,
  exerciseId: 12,
  planItemId: null,
  reason,
  ...over,
})

describe('schema version 5', () => {
  it('upgrade4to5_keepsEveryRow_andAddsAnEmptySkipsStore', async () => {
    const v4 = await openDb(SCHEMA_VERSIONS.filter((v) => v.version <= 4))
    await withExercises(v4)
    await v4.sessions.add({ id: 1, date: '2026-10-27', bodyweightKg: 84.5, readiness: 4, mode: 'POWERLIFTING' })
    await v4.sets.add({ id: 1, sessionId: 1, exerciseId: 1, weightKg: 140, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: true })
    await v4.planItems.add({ id: 1, weekday: 3, exerciseId: 12, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    await v4.advice.add({ ...target(1, 'POWERLIFTING'), id: 1 })
    await v4.baselines.put({ exerciseId: 1, e1rmKg: 172.5, date: '2026-09-01' })
    await v4.checkIns.add({ id: 1, date: '2026-11-11', lagging: ['CHEST'], growingWell: [] })
    await v4.settings.put({ key: 'trainingMode', value: 'BODYBUILDING' })
    const tables = ['exercises', 'sessions', 'sets', 'planItems', 'advice', 'baselines', 'checkIns', 'settings'] as const
    const before = Object.fromEntries(await Promise.all(tables.map(async (t) => [t, await v4.table(t).toArray()])))
    v4.close()

    const db = await openDb()

    expect(await installedVersion(name)).toBe(8)
    for (const t of tables) expect(await db.table(t).toArray(), t).toEqual(before[t])
    expect(await db.skips.toArray()).toEqual([])
    // The upgrade wrote a safety copy of the version 4 data first.
    expect((await db.meta.get('safetyCopy'))?.value).toEqual(expect.any(String))
  })

  it('oneSkipPerExercise_perDate_perSide', async () => {
    const db = await openDb()
    await db.skips.add(curlSkip('BUSY'))
    await expect(db.skips.add(curlSkip('PAIN'))).rejects.toMatchObject({ name: 'ConstraintError' })
    // Another side, date or exercise is another skip.
    await db.skips.add(curlSkip('BUSY', { mode: 'POWERLIFTING' }))
    await db.skips.add(curlSkip('BUSY', { date: YESTERDAY }))
    await db.skips.add(curlSkip('BUSY', { exerciseId: 1 }))
    expect(await db.skips.count()).toBe(4)
  })
})

describe('skips', () => {
  it('skip_isStored_andReadBack_oldestFirst', async () => {
    const db = await openDb()
    await withExercises(db)
    const later = await skip(db, curlSkip('PAIN', { planItemId: 7 }), none)
    const earlier = await skip(db, curlSkip(null, { date: YESTERDAY, mode: 'POWERLIFTING', exerciseId: 1 }), none)
    expect(await skips(db)).toEqual([
      { id: earlier, date: YESTERDAY, mode: 'POWERLIFTING', exerciseId: 1, planItemId: null, reason: null },
      { id: later, date: TODAY, mode: 'BODYBUILDING', exerciseId: 12, planItemId: 7, reason: 'PAIN' },
    ])
    const js = skipToJs((await skips(db))[1]!)
    expect([js.id, js.date, js.mode, js.exerciseId, js.planItemId, js.reason]).toEqual([later, TODAY, 'BODYBUILDING', 12, 7, 'PAIN'])
  })

  it('skipAgain_replacesTheReasonAndPlanItem_andKeepsTheId', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('BUSY', { planItemId: 3 }), none)
    const again = await skip(db, curlSkip('TIME', { planItemId: 4 }), none)
    expect(again).toBe(id)
    expect(await skips(db)).toEqual([{ id, date: TODAY, mode: 'BODYBUILDING', exerciseId: 12, planItemId: 4, reason: 'TIME' }])
  })

  it('skipsOnAnotherSideOrDate_areSeparate', async () => {
    const db = await openDb()
    const a = await skip(db, curlSkip('BUSY'), none)
    const b = await skip(db, curlSkip('BUSY', { mode: 'POWERLIFTING' }), none)
    const c = await skip(db, curlSkip('BUSY', { date: YESTERDAY }), none)
    expect(new Set([a, b, c]).size).toBe(3)
    expect(await db.skips.count()).toBe(3)
  })

  it('cards_getTheStoredSkipWithItsId_andEverySkip_andTheirCardsAreStored', async () => {
    const db = await openDb()
    const other = await skip(db, curlSkip('BUSY', { mode: 'POWERLIFTING', date: YESTERDAY }), none)
    let seen: { stored: JsSkip; all: JsSkip[] } | null = null
    const id = await skip(db, curlSkip('PAIN'), (stored, all) => {
      seen = { stored, all }
      return [jsCard(stored)]
    })
    expect(seen).not.toBeNull()
    const { stored, all } = seen!
    expect([stored.id, stored.date, stored.mode, stored.exerciseId, stored.planItemId, stored.reason]).toEqual([id, TODAY, 'BODYBUILDING', 12, null, 'PAIN'])
    expect(all.map((s) => s.id).sort()).toEqual([other, id].sort())
    const stored2 = await db.advice.toArray()
    expect(stored2).toHaveLength(1)
    expect(stored2[0]).toMatchObject({ type: 'SKIP', status: 'OPEN', mode: 'BODYBUILDING', createdAt: TODAY, payloadJson: cardJson(id, 12, 'PAIN') })
    expect(stored2[0]!.id).toBeGreaterThan(0)
  })

  it('aThrowingCards_storesNothing', async () => {
    const db = await openDb()
    await db.advice.add(target(12, 'BODYBUILDING'))
    const before = await db.advice.toArray()
    await expect(
      skip(db, curlSkip('PAIN'), () => {
        throw new Error('boom')
      }),
    ).rejects.toThrow('boom')
    expect(await db.skips.count()).toBe(0)
    // The target the pain skip would have deleted is still there.
    expect(await db.advice.toArray()).toEqual(before)
  })

  it('painSkip_deletesTodaysOpenTarget_forThatExerciseOnBothSides_beforeCardsRun', async () => {
    const db = await openDb()
    const ids = await db.advice.bulkAdd(
      [
        target(12, 'BODYBUILDING'), // goes
        target(12, 'POWERLIFTING'), // goes: the hold crosses sides
        target(1, 'BODYBUILDING'), // another exercise
        target(12, 'BODYBUILDING', YESTERDAY), // another day
        target(12, 'BODYBUILDING', TODAY, 'APPLIED'), // already used
        card(99, 12, 'PAIN', 'OPEN'), // not a target (a pain card: a pain skip deletes an open busy one, final review 1.13)
      ],
      { allKeys: true },
    )
    let whenCardsRan: number[] = []
    // `cards` runs inside the transaction and may read with Dexie: the targets are already gone.
    await skip(db, curlSkip('PAIN'), async () => {
      whenCardsRan = (await db.advice.toArray()).map((a) => a.id as number)
      return []
    })
    expect(whenCardsRan).toEqual(ids.slice(2))
    expect((await db.advice.toArray()).map((a) => a.id)).toEqual(ids.slice(2))
  })

  it('otherReasons_keepTheTarget', async () => {
    const db = await openDb()
    await db.advice.bulkAdd([target(12, 'BODYBUILDING'), target(12, 'POWERLIFTING')])
    for (const reason of ['BUSY', 'TIME', null] as const) await skip(db, curlSkip(reason), none)
    expect(await db.advice.where('type').equals('TARGET').count()).toBe(2)
  })

  it('exerciseIds_areMatchedExactly', async () => {
    const db = await openDb()
    await db.advice.bulkAdd([target(1, 'BODYBUILDING'), target(12, 'BODYBUILDING')])
    await skip(db, curlSkip('PAIN', { exerciseId: 1 }), none)
    const left = await db.advice.toArray()
    expect(left.map((a) => grit.advicePayload(a.payloadJson)?.exerciseId)).toEqual([12])
  })

  it('reSkip_withANewReason_replacesItsOpenCard', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('PAIN'), (stored) => [jsCard(stored)])
    const dismissed = (await db.advice.add(card(id, 12, 'PAIN', 'DISMISSED'))) as number
    const others = (await db.advice.add(card(id + 100, 12, 'BUSY'))) as number
    let whenCardsRan: [number, string][] = []
    const again = await skip(db, curlSkip('BUSY'), async (stored) => {
      whenCardsRan = (await db.advice.orderBy('id').toArray()).map((a) => [a.id as number, a.status])
      return [jsCard(stored)]
    })
    expect(again).toBe(id)
    // The old OPEN card was gone when `cards` ran, so the coach is not blocked by it; the answered card stays.
    expect(whenCardsRan).toEqual([
      [dismissed, 'DISMISSED'],
      [others, 'OPEN'],
    ])
    const left = await db.advice.orderBy('id').toArray()
    expect(left.map((a) => [a.id === dismissed, a.id === others, a.status, grit.advicePayload(a.payloadJson)?.skipReason])).toEqual([
      [true, false, 'DISMISSED', 'PAIN'],
      [false, true, 'OPEN', 'BUSY'],
      [false, false, 'OPEN', 'BUSY'],
    ])
  })

  it('reSkip_whoseCardsThrow_rollsBackTheUpdatedRowAndTheDeletedCard', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('BUSY', { planItemId: 3 }), (stored) => [jsCard(stored)])
    const [skipsBefore, adviceBefore] = [await db.skips.toArray(), await db.advice.toArray()]
    await expect(
      skip(db, curlSkip('PAIN', { planItemId: 4 }), () => {
        throw new Error('boom')
      }),
    ).rejects.toThrow('boom')
    // The row keeps its old reason and plan item, and its card, deleted before `cards` ran, is back.
    expect(await db.skips.toArray()).toEqual(skipsBefore)
    expect(await db.advice.toArray()).toEqual(adviceBefore)
    expect(grit.advicePayload((await db.advice.toArray())[0]!.payloadJson)?.skipId).toBe(id)
  })

  // Final review, 1.13: a pain skip always tells you, as Android's `painSkip_deletesTheExercisesOpenBusyAndRepeatCards_onThatSideOnly`.
  it('painSkip_deletesTheExercisesOpenBusyAndRepeatCards_onThatSideOnly_beforeCardsRun', async () => {
    const db = await openDb()
    const busy = await skip(db, curlSkip('BUSY', { date: YESTERDAY }), (stored) => [jsCard(stored)])
    const repeat = await skip(db, curlSkip(null, { date: '2026-11-16' }), (stored) => [jsCard(stored)])
    const answered = await skip(db, curlSkip('BUSY', { date: '2026-11-15' }), () => [])
    await db.advice.add(card(answered, 12, 'BUSY', 'APPLIED'))
    const pain = await skip(db, curlSkip('PAIN', { date: '2026-11-14' }), (stored) => [jsCard(stored)])
    const otherSide = await skip(db, curlSkip('BUSY', { date: YESTERDAY, mode: 'POWERLIFTING' }), (stored) => [jsCard(stored)])
    const otherExercise = await skip(db, curlSkip('BUSY', { date: YESTERDAY, exerciseId: 1 }), (stored) => [jsCard(stored)])
    await db.advice.add({
      type: 'SWAP', lift: null, createdAt: YESTERDAY, reason: 'Stuck', severity: 0, status: 'OPEN', mode: 'BODYBUILDING',
      payloadJson: `{"type":"${P}Swap","exerciseId":12,"name":"Curl","group":"BICEPS","since":"${YESTERDAY}","alternatives":["Cable curl"]}`,
    })
    let seen = 0
    const today = await skip(db, curlSkip('PAIN'), async (stored) => {
      seen = await db.advice.count()
      return [jsCard(stored)]
    })
    const skipCards = (await db.advice.orderBy('id').toArray()).map((a) => grit.advicePayload(a.payloadJson)).filter((p) => p?.kind === 'Skipped').map((p) => p!.skipId)
    expect(skipCards).toEqual([answered, pain, otherSide, otherExercise, today])
    expect(skipCards).not.toContain(busy)
    expect(skipCards).not.toContain(repeat)
    expect(await db.advice.where('type').equals('SWAP').count()).toBe(1)
    // Gone before the coach looks.
    expect(seen).toBe(5)
  })

  it('busySkip_keepsAnEarlierSkipsOpenCard', async () => {
    const db = await openDb()
    const busy = await skip(db, curlSkip('BUSY', { date: YESTERDAY }), (stored) => [jsCard(stored)])
    await skip(db, curlSkip('BUSY'), none)
    await skip(db, curlSkip(null, { date: '2026-11-19' }), none)
    expect((await db.advice.toArray()).map((a) => grit.advicePayload(a.payloadJson)?.skipId)).toEqual([busy])
  })

  it('reSkip_toOutOfTime_leavesNoCard', async () => {
    const db = await openDb()
    await skip(db, curlSkip('PAIN'), (stored) => [jsCard(stored)])
    await skip(db, curlSkip('TIME'), (stored) => (stored.reason === 'TIME' ? [] : [jsCard(stored)]))
    expect(await db.advice.where('type').equals('SKIP').count()).toBe(0)
  })

  it('withTheRealCoach_aPainSkipOfBench_givesItsHoldCard', async () => {
    const db = await openDb()
    await ensureSeeded(db)
    const bench = (await db.exercises.toArray()).find((e) => e.name === 'Bench')!.id as number
    const planId = (await db.planItems.add({ weekday: 3, exerciseId: bench, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })) as number
    // What Task 5 will do: read the history, plan and prior advice inside the transaction, then ask the coach.
    const id = await skip(db, { date: TODAY, mode: 'POWERLIFTING', exerciseId: bench, planItemId: planId, reason: 'PAIN' }, async (stored, all) => {
      const [history, plan, advice] = await Promise.all([loadHistory(db), db.planItems.toArray(), db.advice.toArray()])
      return grit.coachingAfterSkip(stored, all, history, plan.map((p) => planItemToJs(p as typeof p & { id: number })), TODAY, advice.map((a) => adviceToJs(a as AdviceRow & { id: number })))
    })
    const cards = await db.advice.where('type').equals('SKIP').toArray()
    expect(cards).toHaveLength(1)
    expect(cards[0]).toMatchObject({ status: 'OPEN', mode: 'POWERLIFTING', lift: 'BENCH', createdAt: TODAY })
    const payload = grit.advicePayload(cards[0]!.payloadJson)!
    expect([payload.kind, payload.skipId, payload.exerciseId, payload.skipReason, [...payload.alternatives]]).toEqual(['Skipped', id, bench, 'PAIN', []])
  })
})

describe('unskip', () => {
  it('deletesTheSkipAndItsOpenCard_keepsAnsweredCards_andOtherSkips', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('PAIN'), (stored) => [jsCard(stored)])
    const other = await skip(db, curlSkip('PAIN', { exerciseId: 1 }), (stored) => [jsCard(stored)])
    await db.advice.bulkAdd([card(id, 12, 'PAIN', 'APPLIED'), card(id, 12, 'PAIN', 'DISMISSED')])

    await unskip(db, id)

    expect((await skips(db)).map((s) => s.id)).toEqual([other])
    const left = await db.advice.orderBy('id').toArray()
    expect(left.map((a) => [grit.advicePayload(a.payloadJson)?.skipId, a.status])).toEqual([
      [other, 'OPEN'],
      [id, 'APPLIED'],
      [id, 'DISMISSED'],
    ])
  })

  it('deletesTodaysOpenTarget_forThatExerciseOnBothSides_whateverTheReason', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('BUSY'), none)
    const ids = await db.advice.bulkAdd(
      [target(12, 'BODYBUILDING'), target(12, 'POWERLIFTING'), target(1, 'BODYBUILDING'), target(12, 'BODYBUILDING', YESTERDAY), target(12, 'POWERLIFTING', TODAY, 'DISMISSED')],
      { allKeys: true },
    )
    await unskip(db, id)
    expect((await db.advice.toArray()).map((a) => a.id)).toEqual(ids.slice(2))
  })

  it('unskip_isAtomic_aFailureHalfwayKeepsTheSkipAndItsCard', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('PAIN'), (stored) => [jsCard(stored)])
    await db.advice.add(target(12, 'BODYBUILDING'))
    const [skipsBefore, adviceBefore] = [await db.skips.toArray(), await db.advice.toArray()]
    // The last step, reading the targets' payloads, fails: the card and the skip, already deleted by then, come back.
    const read = grit.advicePayload.bind(grit)
    const spy = vi.spyOn(grit, 'advicePayload').mockImplementation((json: string) => {
      const p = read(json)
      if (p?.kind === 'Target') throw new Error('boom')
      return p
    })
    try {
      await expect(unskip(db, id)).rejects.toThrow('boom')
    } finally {
      spy.mockRestore()
    }
    expect(await db.skips.toArray()).toEqual(skipsBefore)
    expect(await db.advice.toArray()).toEqual(adviceBefore)
  })

  it('anUnknownId_changesNothing', async () => {
    const db = await openDb()
    const id = await skip(db, curlSkip('PAIN'), (stored) => [jsCard(stored)])
    await db.advice.add(target(12, 'BODYBUILDING'))
    const [before, cards] = [await db.skips.toArray(), await db.advice.toArray()]
    await unskip(db, id + 1)
    expect(await db.skips.toArray()).toEqual(before)
    expect(await db.advice.toArray()).toEqual(cards)
  })
})

describe('skips and plan items', () => {
  async function planned(db: GritDb) {
    await withExercises(db)
    const pl = (await db.planItems.add({ weekday: 3, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })) as number
    const bb = (await db.planItems.add({ weekday: 3, exerciseId: 12, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })) as number
    const plSkip = await skip(db, { date: TODAY, mode: 'POWERLIFTING', exerciseId: 1, planItemId: pl, reason: null }, none)
    const bbSkip = await skip(db, curlSkip('BUSY', { planItemId: bb }), none)
    return { pl, bb, plSkip, bbSkip }
  }
  const planItemOf = async (db: GritDb, id: number) => (await db.skips.get(id))?.planItemId

  it('deletingAPlanItem_keepsItsSkips_withNoPlanItem', async () => {
    const db = await openDb()
    const { pl, bb, plSkip, bbSkip } = await planned(db)
    await deletePlanItem(db, bb)
    expect(await db.planItems.get(bb)).toBeUndefined()
    expect(await planItemOf(db, bbSkip)).toBeNull()
    expect(await planItemOf(db, plSkip)).toBe(pl)
  })

  it('replacingASidesPlan_clearsThatSidesPlanItems_only', async () => {
    const db = await openDb()
    const { pl, plSkip, bbSkip } = await planned(db)
    await replacePlan(db, 'BODYBUILDING', [{ item: { weekday: 1, exerciseId: 12, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8 }, catalogueName: null }])
    expect(await planItemOf(db, bbSkip)).toBeNull()
    expect(await planItemOf(db, plSkip)).toBe(pl)
  })

  it('aCoachsChange_clearsTheRemovedItems_andKeepsTheUpdatedOnes', async () => {
    const db = await openDb()
    const { pl, bb, plSkip, bbSkip } = await planned(db)
    const [plItem, bbItem] = await Promise.all([db.planItems.get(pl), db.planItems.get(bb)])
    const updated = planItemToJs({ ...plItem!, sets: 4 } as typeof plItem & { id: number })
    await applyPlanChange(db, new JsPlanChange([updated], [planItemToJs(bbItem as typeof bbItem & { id: number })], [], 'Swapped', true))
    expect(await planItemOf(db, plSkip)).toBe(pl)
    expect((await db.planItems.get(pl))!.sets).toBe(4)
    expect(await planItemOf(db, bbSkip)).toBeNull()
  })

  it('editingAPlanItem_keepsItsSkipsPlanItem', async () => {
    const db = await openDb()
    const { bb, bbSkip } = await planned(db)
    await updatePlanItem(db, bb, { exerciseId: 12, sets: 5, reps: 8, targetRpe: 8, repsHigh: 12 }, null)
    expect(await planItemOf(db, bbSkip)).toBe(bb)
  })

  it('skips_neverBecomeSessionsOrSets', async () => {
    const db = await openDb()
    await planned(db)
    const history = await loadHistory(db)
    expect(history.sessions).toHaveLength(0)
    expect(await db.sessions.count()).toBe(0)
    expect(await db.sets.count()).toBe(0)
  })
})
