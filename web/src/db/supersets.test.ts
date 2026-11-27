import Dexie from 'dexie'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { grit, JsPlanItem, type JsPlanChange } from '../domain'
import { applyPlanChange, deletePlanItem, movePlanItem, planFor, planItemsOn, planSnapshot, replacePlan, restorePlan, updatePlanItem } from './catalogue'
import { ensureSeeded, installedVersion, openGritDb, type GritDb } from './db'
import { planItemFromJs, planItemToJs } from './mapping'
import { SCHEMA_VERSIONS, type PlanItemRow } from './schema'
import { setSuperset, supersetHalves } from './supersets'

// Supersets in the web app's storage (1.24): the link on a plan item, and what keeps or clears it.

let n = 0
let name = ''
let db: GritDb
beforeEach(async () => {
  name = `grit-supersets-test-${++n}`
  db = await openGritDb(name)
  await ensureSeeded(db)
})
afterEach(async () => {
  db.close()
  await Dexie.delete(name)
})

type Stored = PlanItemRow & { id: number }

/** Four exercises' ids, from the seeded rows. */
async function exerciseIds(): Promise<number[]> {
  return (await db.exercises.orderBy('id').toArray()).slice(0, 4).map((e) => e.id!)
}

/** Wednesday on the powerlifting side: four different exercises in order; resolves to the items' ids, top to bottom. */
async function wednesday(): Promise<number[]> {
  const exercises = await exerciseIds()
  const ids: number[] = []
  for (const [order, exerciseId] of exercises.entries()) ids.push((await db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: 8, targetRpe: 8, order })) as number)
  return ids
}

/** The day's linked item ids, in order. */
const linked = async () => (await planItemsOn(db, 3, 'POWERLIFTING')).filter((p) => p.supersetWithAbove === true).map((p) => p.id)
const order = async () => (await planItemsOn(db, 3, 'POWERLIFTING')).map((p) => p.id)
const stored = async (id: number) => (await db.planItems.get(id)) as Stored

describe('schema version 7', () => {
  it('isDeclaredWithNoIndexAndNoUpgrade_andInstalled', async () => {
    expect(Math.max(...SCHEMA_VERSIONS.map((v) => v.version))).toBe(7)
    expect(SCHEMA_VERSIONS.find((v) => v.version === 7)).toEqual({ version: 7, stores: {} })
    expect(await installedVersion(name)).toBe(7)
  })

  it('anItemNeverLinked_storesNoKey_andMapsBothWays', async () => {
    const [a, b] = await wednesday()
    expect('supersetWithAbove' in (await stored(b!))).toBe(false)
    expect(planItemToJs(await stored(b!)).supersetWithAbove).toBe(false)
    expect(await setSuperset(db, b!, true)).toBe(true)
    expect((await stored(b!)).supersetWithAbove).toBe(true)
    const js = planItemToJs(await stored(b!))
    expect(js.supersetWithAbove).toBe(true)
    // Back from the domain it is the stored row again, with its side spelt out.
    expect(planItemFromJs(js)).toEqual({ ...(await stored(b!)), mode: 'POWERLIFTING' })
    // The one above carries nothing.
    expect('supersetWithAbove' in (await stored(a!))).toBe(false)
    expect('supersetWithAbove' in planItemFromJs(planItemToJs(await stored(a!)))).toBe(false)
  })
})

describe('linking and unlinking', () => {
  it('linksToTheOneAbove_andUnlinksLeavingNoKey', async () => {
    const [a, b] = await wednesday()
    expect(await setSuperset(db, b!, true)).toBe(true)
    expect(await linked()).toEqual([b])
    expect(supersetHalves(await planFor(db, 'POWERLIFTING'))).toEqual(
      new Map([
        [a, { partnerId: b, half: 'FIRST' }],
        [b, { partnerId: a, half: 'SECOND' }],
      ]),
    )
    expect(await setSuperset(db, b!, false)).toBe(true)
    expect(await linked()).toEqual([])
    expect('supersetWithAbove' in (await stored(b!))).toBe(false)
  })

  it('refusesWhereTheRuleSaysNo_changingNothing', async () => {
    const [a, b, c, d] = await wednesday()
    // First on its day.
    expect(await setSuperset(db, a!, true)).toBe(false)
    // No such item.
    expect(await setSuperset(db, 9999, true)).toBe(false)
    expect(await setSuperset(db, b!, true)).toBe(true)
    // The one above is a pair's second.
    expect(await setSuperset(db, c!, true)).toBe(false)
    expect(await setSuperset(db, d!, true)).toBe(true)
    expect(await linked()).toEqual([b, d])
    // The same exercise as the one above.
    await setSuperset(db, d!, false)
    await db.planItems.update(d!, { exerciseId: (await stored(c!)).exerciseId })
    expect(await setSuperset(db, d!, true)).toBe(false)
    expect(await linked()).toEqual([b])
  })
})

describe('what keeps a link and what clears it', () => {
  it('editingAnItemsOwnNumbersOrExercise_keepsIt', async () => {
    const [a, b] = await wednesday()
    await setSuperset(db, b!, true)
    const other = (await db.exercises.orderBy('id').toArray()).at(-1)!.id!
    await updatePlanItem(db, b!, { exerciseId: other, sets: 5, reps: 3, targetRpe: 9 }, null)
    await updatePlanItem(db, a!, { exerciseId: (await stored(a!)).exerciseId, sets: 4, reps: 6, targetRpe: 7 }, null)
    expect(await linked()).toEqual([b])
    expect((await stored(b!)).sets).toBe(5)
  })

  it('deletingTheOneAbove_unlinksTheOneUnderIt_andDeletingTheSecondLeavesNothingBehind', async () => {
    const [a, b, c, d] = await wednesday()
    await setSuperset(db, b!, true)
    await setSuperset(db, d!, true)
    await deletePlanItem(db, a!)
    expect(await linked()).toEqual([d])
    expect('supersetWithAbove' in (await stored(b!))).toBe(false)
    // The second of the other pair deleted: nothing to unlink.
    await deletePlanItem(db, d!)
    expect(await order()).toEqual([b, c])
    expect(await linked()).toEqual([])
  })

  it('deletingTheItemBetween_unlinksTheOneNowUnderAnother', async () => {
    const [a, b, c, d] = await wednesday()
    await setSuperset(db, d!, true)
    await deletePlanItem(db, c!)
    expect(await order()).toEqual([a, b, d])
    expect(await linked()).toEqual([])
  })

  it('aMove_unlinksEveryItemWhoseItemAboveChanged', async () => {
    const [a, b, c, d] = await wednesday()
    await setSuperset(db, b!, true)
    await setSuperset(db, d!, true)
    // The first pair's first moved down: the pair's two change places, and its link is gone. The other pair stands.
    await movePlanItem(db, await stored(a!), 1)
    expect(await order()).toEqual([b, a, c, d])
    expect(await linked()).toEqual([d])
    // c moved down into the second pair: d is now under... c still? No: d goes above c.
    await movePlanItem(db, await stored(c!), 1)
    expect(await order()).toEqual([b, a, d, c])
    expect(await linked()).toEqual([])
  })

  it('aMoveElsewhereOnTheDay_keepsAPairThatDidNotMove', async () => {
    const exercises = await exerciseIds()
    const [a, b, c, d] = await wednesday()
    const e = (await db.planItems.add({ weekday: 3, exerciseId: exercises[0]!, sets: 3, reps: 8, targetRpe: 8, order: 4 })) as number
    await setSuperset(db, b!, true)
    // The last two change places: nothing above b changed. The web renumbers the whole day.
    await movePlanItem(db, await stored(e), -1)
    expect(await order()).toEqual([a, b, c, e, d])
    expect(await linked()).toEqual([b])
    // At the end already: nothing happens.
    await movePlanItem(db, await stored(d!), 1)
    expect(await order()).toEqual([a, b, c, e, d])
  })

  it('theCoachsSwap_unlinksThePairItTouches_andMoreWorkKeepsIt', async () => {
    const [a, b, c, d] = await wednesday()
    await setSuperset(db, b!, true)
    await setSuperset(db, d!, true)
    const plan = (await planFor(db, 'POWERLIFTING')).map(planItemToJs)
    const byId = (id: number) => plan.find((p) => p.id === id)!
    // More work: an item's sets go up, the item handed back whole by the domain. Its link comes back with it.
    const moreWork = { updated: [byId(b!).copy(undefined, undefined, undefined, 4)], removed: [], added: [], message: '', changed: true } as unknown as JsPlanChange
    await applyPlanChange(db, moreWork)
    expect((await stored(b!)).sets).toBe(4)
    expect(await linked()).toEqual([b, d])
    // A swap: the first of the second pair is deleted and another exercise added in its place, as a new item.
    const old = byId(c!)
    const swapped = new JsPlanItem(0, old.weekday, 0, old.sets, old.reps, old.targetRpe, old.order, old.mode, old.repsHigh, old.supersetWithAbove)
    const swap = { updated: [], removed: [old], added: [{ item: swapped, name: grit.library()[0]!.name }], message: '', changed: true } as unknown as JsPlanChange
    await applyPlanChange(db, swap)
    expect((await order()).length).toBe(4)
    expect(await linked()).toEqual([b])
  })

  it('aWholePlanReplaced_thenPutBackByUndo_hasItsLinksAgain', async () => {
    const [, b] = await wednesday()
    await setSuperset(db, b!, true)
    const snapshot = (await planSnapshot(db, 'POWERLIFTING'))!
    const exercises = await exerciseIds()
    snapshot.replacedBy = await replacePlan(db, 'POWERLIFTING', [{ item: { weekday: 1, exerciseId: exercises[0]!, sets: 3, reps: 5, targetRpe: 8 }, catalogueName: null }])
    expect((await planFor(db, 'POWERLIFTING')).some((p) => p.supersetWithAbove === true)).toBe(false)
    expect(await restorePlan(db, 'POWERLIFTING', snapshot)).toBe('restored')
    expect(await linked()).toEqual([b])
  })
})
