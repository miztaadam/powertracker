import { grit, type JsPlanChange } from '../domain'
import type { GritDb } from './db'
import { planItemFromJs } from './mapping'
import { sideOf, type PlanItemRow, type SetRow, type TrainingMode } from './schema'
import { forgetPlanItems } from './skips'

/**
 * The id of the exercise row for the catalogue exercise `name` (a name or an alias). Nothing is
 * inserted up front: the catalogue is browsed as it is, and a row appears only with the plan item
 * or set that needs it. A row with that name, ignoring case, is reused whoever made it; if the only
 * one is archived it is restored; otherwise a built-in row is inserted from the catalogue.
 *
 * Call it inside the `rw` transaction that saves the plan item or set (with `db.exercises` in its
 * scope), so a failure there leaves no row behind. It joins that transaction.
 */
export async function resolveCatalogue(db: GritDb, name: string): Promise<number> {
  const entry = grit.findInLibrary(name)
  if (!entry) throw new Error(`${name} is not in the exercise catalogue`)
  return db.transaction('rw', db.exercises, async () => {
    const wanted = entry.name.toLowerCase()
    // In id order, oldest first: with two rows of one name, the same one is picked every time.
    const same = (await db.exercises.toArray()).filter((e) => e.name.toLowerCase() === wanted)
    const live = same.find((e) => !e.archived)
    if (live?.id != null) return live.id
    const archived = same[0]
    if (archived?.id != null) {
      await db.exercises.update(archived.id, { archived: false })
      return archived.id
    }
    return (await db.exercises.add({
      name: entry.name,
      category: entry.category,
      parentLift: entry.parentLift ?? null,
      muscles: [...entry.muscles],
      isBuiltIn: true,
      archived: false,
    })) as number
  })
}

/** The stored row to use, or the catalogue exercise to find, restore or insert on saving. */
async function exerciseFor(db: GritDb, exerciseId: number | null, catalogueName: string | null): Promise<number> {
  if (catalogueName != null) return resolveCatalogue(db, catalogueName)
  if (exerciseId == null) throw new Error('There is no exercise to save.')
  return exerciseId
}

/** Every plan item of `mode`'s side, in id order. Each side keeps its own plan (bodybuilding mode spec §2). */
export async function planFor(db: GritDb, mode: TrainingMode): Promise<(PlanItemRow & { id: number })[]> {
  // Filtered here rather than by the mode index: a row with no mode is on the powerlifting side (see `sideOf`).
  const items = (await db.planItems.toArray()) as (PlanItemRow & { id: number })[]
  return items.filter((p) => sideOf(p) === mode)
}

/** The day's plan items on `mode`'s side as stored now, in order. */
export async function planItemsOn(db: GritDb, weekday: number, mode: TrainingMode): Promise<(PlanItemRow & { id: number })[]> {
  const items = (await db.planItems.where('weekday').equals(weekday).toArray()) as (PlanItemRow & { id: number })[]
  return items.filter((p) => sideOf(p) === mode).sort((a, b) => a.order - b.order || a.id - b.id)
}

type Unresolved<T extends { exerciseId: number }> = Omit<T, 'exerciseId'> & { exerciseId: number | null }

/**
 * Adds a plan item at the end of its day on `mode`'s side; resolves to its id. With a `catalogueName` the exercise
 * row is found, restored or inserted in the same transaction, and `exerciseId` is not used.
 */
export function addPlanItem(db: GritDb, item: Unresolved<Omit<PlanItemRow, 'id' | 'order' | 'mode'>>, catalogueName: string | null, mode: TrainingMode): Promise<number> {
  return db.transaction('rw', db.exercises, db.planItems, async () => {
    const exerciseId = await exerciseFor(db, item.exerciseId, catalogueName)
    const last = (await planItemsOn(db, item.weekday, mode)).at(-1)
    return (await db.planItems.add({ ...item, exerciseId, order: (last?.order ?? -1) + 1, mode })) as number
  })
}

/** Changes a plan item in place; it stays on its side. With a `catalogueName` its exercise becomes that catalogue exercise, resolved in the same transaction. */
export function updatePlanItem(db: GritDb, id: number, change: Unresolved<Pick<PlanItemRow, 'exerciseId' | 'sets' | 'reps' | 'targetRpe' | 'repsHigh'>>, catalogueName: string | null): Promise<void> {
  return db.transaction('rw', db.exercises, db.planItems, async () => {
    const exerciseId = await exerciseFor(db, change.exerciseId, catalogueName)
    await db.planItems.update(id, { ...change, exerciseId })
  })
}

/** Deletes a plan item; its skips stay, with no plan item (as Android's SET NULL). Joins the caller's transaction when it has these tables. */
export function deletePlanItem(db: GritDb, id: number): Promise<void> {
  return db.transaction('rw', db.planItems, db.skips, async () => {
    await forgetPlanItems(db, [id])
    await db.planItems.delete(id)
  })
}

/** One item of a whole new plan for `replacePlan`: its day and prescription, and its exercise as for `addPlanItem`. */
export type NewPlanItem = { item: Unresolved<Omit<PlanItemRow, 'id' | 'order' | 'mode'>>; catalogueName: string | null }

/**
 * Replaces `mode`'s side's plan with `items`, in one transaction: that side's items are deleted, each catalogue name is
 * found, restored or inserted, and the items are added in the order given (each at the end of its day). A failure
 * part-way leaves the old plan as it was. The other side's plan is never touched. The deleted items' skips stay, with
 * no plan item.
 */
export function replacePlan(db: GritDb, mode: TrainingMode, items: readonly NewPlanItem[]): Promise<void> {
  return db.transaction('rw', db.exercises, db.planItems, db.skips, async () => {
    const old = (await planFor(db, mode)).map((p) => p.id)
    await forgetPlanItems(db, old)
    await db.planItems.bulkDelete(old)
    const orders = new Map<number, number>()
    for (const { item, catalogueName } of items) {
      const exerciseId = await exerciseFor(db, item.exerciseId, catalogueName)
      const order = orders.get(item.weekday) ?? 0
      orders.set(item.weekday, order + 1)
      await db.planItems.add({ ...item, exerciseId, order, mode })
    }
  })
}

/**
 * Saves a logged set; resolves to its id. With a `catalogueName` the exercise row is found,
 * restored or inserted in the same transaction, and `exerciseId` is not used.
 */
export function addSet(db: GritDb, set: Unresolved<Omit<SetRow, 'id'>>, catalogueName: string | null): Promise<number> {
  return db.transaction('rw', db.exercises, db.sets, async () => {
    const exerciseId = await exerciseFor(db, set.exerciseId, catalogueName)
    return (await db.sets.add({ ...set, exerciseId })) as number
  })
}

/**
 * Carries out a coach's change to the plan (a MORE_WORK or SWAP advice applied by the domain), in one transaction:
 * `updated` items are saved, `removed` ones deleted, and each `added` item gets its catalogue exercise found, restored
 * or inserted. A failure part-way leaves the plan as it was. Joins the caller's transaction when it has these tables.
 */
export function applyPlanChange(db: GritDb, change: JsPlanChange): Promise<void> {
  return db.transaction('rw', db.exercises, db.planItems, db.skips, async () => {
    // Updated in place, so an item keeps its id and its skips keep pointing at it.
    for (const item of change.updated) await db.planItems.put(planItemFromJs(item))
    if (change.removed.length > 0) {
      await forgetPlanItems(db, change.removed.map((p) => p.id))
      await db.planItems.bulkDelete(change.removed.map((p) => p.id))
    }
    for (const { item, name } of change.added) {
      const { id: _new, ...row } = planItemFromJs(item)
      await db.planItems.add({ ...row, exerciseId: await resolveCatalogue(db, name) })
    }
  })
}
