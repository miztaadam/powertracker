import { grit } from '../domain'
import type { GritDb } from './db'
import type { PlanItemRow, SetRow } from './schema'

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

/** The day's plan items as stored now, in order. */
export async function planItemsOn(db: GritDb, weekday: number): Promise<(PlanItemRow & { id: number })[]> {
  const items = (await db.planItems.where('weekday').equals(weekday).toArray()) as (PlanItemRow & { id: number })[]
  return items.sort((a, b) => a.order - b.order || a.id - b.id)
}

type Unresolved<T extends { exerciseId: number }> = Omit<T, 'exerciseId'> & { exerciseId: number | null }

/**
 * Adds a plan item at the end of its day; resolves to its id. With a `catalogueName` the exercise
 * row is found, restored or inserted in the same transaction, and `exerciseId` is not used.
 */
export function addPlanItem(db: GritDb, item: Unresolved<Omit<PlanItemRow, 'id' | 'order'>>, catalogueName: string | null): Promise<number> {
  return db.transaction('rw', db.exercises, db.planItems, async () => {
    const exerciseId = await exerciseFor(db, item.exerciseId, catalogueName)
    const last = (await planItemsOn(db, item.weekday)).at(-1)
    return (await db.planItems.add({ ...item, exerciseId, order: (last?.order ?? -1) + 1 })) as number
  })
}

/** Changes a plan item in place. With a `catalogueName` its exercise becomes that catalogue exercise, resolved in the same transaction. */
export function updatePlanItem(db: GritDb, id: number, change: Unresolved<Pick<PlanItemRow, 'exerciseId' | 'sets' | 'reps' | 'targetRpe'>>, catalogueName: string | null): Promise<void> {
  return db.transaction('rw', db.exercises, db.planItems, async () => {
    const exerciseId = await exerciseFor(db, change.exerciseId, catalogueName)
    await db.planItems.update(id, { ...change, exerciseId })
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
