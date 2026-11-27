import { grit } from '../domain'
import type { GritDb } from './db'
import { planItemToJs } from './mapping'
import { sideOf, type PlanItemRow, type TrainingMode } from './schema'

type Stored = PlanItemRow & { id: number }

/** `mode`'s side's plan as stored now (as `planFor` in catalogue.ts, which uses this file). */
async function planFor(db: GritDb, mode: TrainingMode): Promise<Stored[]> {
  return ((await db.planItems.toArray()) as Stored[]).filter((p) => sideOf(p) === mode)
}

/** Takes the link off these plan items: the key is removed, as on an item never linked. */
async function unlink(db: GritDb, ids: readonly number[]): Promise<void> {
  if (ids.length === 0) return
  await db.planItems
    .where(':id')
    .anyOf([...ids])
    .modify((row: PlanItemRow) => {
      delete row.supersetWithAbove
    })
}

/**
 * Runs `change`, a write to `mode`'s side's plan, and then takes the link off every item whose item above changed by
 * it (1.24; the rule is the domain's `Supersets.broken`): the one above was deleted, moved, or swapped for another
 * exercise by the coach, or something was moved in between. One transaction with the change when the caller's has
 * `planItems`. Every write that can change what is above an item goes through here: delete, move, and the coach's
 * plan changes. Adding at the end of a day, and editing an item's own numbers or exercise, change nothing above anyone.
 */
export async function keepingLinks<T>(db: GritDb, mode: TrainingMode, change: () => Promise<T>): Promise<T> {
  const before = (await planFor(db, mode)).map(planItemToJs)
  const result = await change()
  const after = (await planFor(db, mode)).map(planItemToJs)
  await unlink(db, grit.supersetBroken(before, after))
  return result
}

/**
 * Links the plan item `id` to the one above it, or takes its link off (1.24). Linking is refused, changing nothing and
 * resolving to false, where `Supersets.canLink` says no: nothing above it on its day, the same exercise, or one of the
 * two already in a pair. Unlinking always goes through.
 */
export function setSuperset(db: GritDb, id: number, linked: boolean): Promise<boolean> {
  return db.transaction('rw', db.planItems, async () => {
    const item = (await db.planItems.get(id)) as Stored | undefined
    if (item == null) return false
    if (!linked) {
      await unlink(db, [id])
      return true
    }
    const plan = (await planFor(db, sideOf(item))).map(planItemToJs)
    if (!grit.supersetCanLink(plan, id)) return false
    await db.planItems.update(id, { supersetWithAbove: true })
    return true
  })
}

/** The pairs on `mode`'s side as stored now: for each plan item in one, its partner's id and which half it is. */
export function supersetHalves(plan: readonly Stored[]): Map<number, { partnerId: number; half: 'FIRST' | 'SECOND' }> {
  const out = new Map<number, { partnerId: number; half: 'FIRST' | 'SECOND' }>()
  for (const pair of grit.supersetPairs(plan.map(planItemToJs))) {
    out.set(pair.firstId, { partnerId: pair.secondId, half: 'FIRST' })
    out.set(pair.secondId, { partnerId: pair.firstId, half: 'SECOND' })
  }
  return out
}

/** Takes the link off every item of the stored plan (both sides) whose flag counts for nothing: after a restore. */
export async function clearStaleLinks(db: GritDb): Promise<void> {
  const all = (await db.planItems.toArray()) as Stored[]
  await unlink(db, grit.supersetStale(all.map(planItemToJs)))
}
