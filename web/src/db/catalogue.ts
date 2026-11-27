import { grit, type JsPlanChange } from '../domain'
import type { GritDb } from './db'
import { planItemFromJs } from './mapping'
import { sideOf, type ExerciseRow, type PlateCount, type PlanItemRow, type SetRow, type TrainingMode } from './schema'
import { getSettings, updateSettings } from './settings'
import { forgetPlanItems } from './skips'
import { keepingLinks } from './supersets'

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

/**
 * Sets an exercise's plate setting (1.17), in one transaction; resolves to the row's id. With a `catalogueName` the
 * row is found, restored or inserted first (a Today stand-in gets its row here, as its first set would give it), and
 * `exerciseId` is not used. `OFF` removes the key from the row, so it reads exactly as before plates: it is never
 * written. Only that row changes: no set, target or other row is touched, so history is never rewritten.
 */
export function setPlateCount(db: GritDb, exerciseId: number | null, catalogueName: string | null, value: PlateCount): Promise<number> {
  return db.transaction('rw', db.exercises, async () => {
    const id = await exerciseFor(db, exerciseId, catalogueName)
    if ((await db.exercises.get(id)) == null) throw new Error(`There is no exercise ${id}.`)
    await db.exercises
      .where(':id')
      .equals(id)
      .modify((row: ExerciseRow) => {
        if (value === 'OFF') delete row.plateCount
        else row.plateCount = value
      })
    return id
  })
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
    const item = await db.planItems.get(id)
    if (item == null) return
    // The item under it, if linked to it as a superset, is unlinked (1.24).
    await keepingLinks(db, sideOf(item), async () => {
      await forgetPlanItems(db, [id])
      await db.planItems.delete(id)
    })
  })
}

/**
 * Moves a plan item one place up (-1) or down (1) on its day, as stored now; nothing when it is already at that end.
 * The day is renumbered rather than two orders swapped, so two items that share an order still change places. A
 * superset the move breaks is unlinked (1.24): the moved item's own, and one it was moved into.
 */
export function movePlanItem(db: GritDb, item: PlanItemRow & { id: number }, direction: -1 | 1): Promise<void> {
  // The scope is itself an async function: Dexie follows the awaits of one, and not of a plain function that hands
  // back a promise (a move that changed nothing then failed with PrematureCommitError).
  return db.transaction('rw', db.planItems, async () => {
    await keepingLinks(db, sideOf(item), async () => {
      const items = await planItemsOn(db, item.weekday, sideOf(item))
      const from = items.findIndex((p) => p.id === item.id)
      const moved = items[from]
      const neighbour = items[from + direction]
      if (moved == null || neighbour == null) return
      items[from + direction] = moved
      items[from] = neighbour
      await Promise.all(items.map((p, order) => (p.order === order ? null : db.planItems.update(p.id, { order }))))
    })
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
export function replacePlan(db: GritDb, mode: TrainingMode, items: readonly NewPlanItem[]): Promise<number[]> {
  return db.transaction('rw', db.exercises, db.planItems, db.skips, async () => {
    const old = (await planFor(db, mode)).map((p) => p.id)
    await forgetPlanItems(db, old)
    await db.planItems.bulkDelete(old)
    const orders = new Map<number, number>()
    const added: number[] = []
    for (const { item, catalogueName } of items) {
      const exerciseId = await exerciseFor(db, item.exerciseId, catalogueName)
      const order = orders.get(item.weekday) ?? 0
      orders.set(item.weekday, order + 1)
      added.push((await db.planItems.add({ ...item, exerciseId, order, mode })) as number)
    }
    return added
  })
}

/**
 * A side's plan as it was before a whole-plan replace (1.14.1), for Undo: its items as stored (ids, days, order and
 * numbers), the skips that pointed at them, and the side's training days; `replacedBy` is the ids the replace added.
 */
export type PlanSnapshot = {
  mode: TrainingMode
  items: (PlanItemRow & { id: number })[]
  skipLinks: { skipId: number; planItemId: number }[]
  days: number[]
  replacedBy: number[]
}

/** The side's training days as stored. */
const daysOf = (settings: { trainingDays: number[]; bodybuildingTrainingDays: number[] }, mode: TrainingMode) =>
  mode === 'BODYBUILDING' ? settings.bodybuildingTrainingDays : settings.trainingDays

/**
 * `mode`'s plan as it is now, to undo the replace about to run, or null when the side has no plan items (an empty
 * side gets no Undo). Call it in the save's transaction, before the replace, with `db.planItems`, `db.skips` and
 * `db.settings` in its scope; `replacedBy` is left empty for the caller to fill with what `replacePlan` returns.
 */
export async function planSnapshot(db: GritDb, mode: TrainingMode): Promise<PlanSnapshot | null> {
  return db.transaction('r', db.planItems, db.skips, db.settings, async () => {
    const items = (await planFor(db, mode)).sort((a, b) => a.id - b.id)
    if (items.length === 0) return null
    const skips = await db.skips.where('planItemId').anyOf(items.map((p) => p.id)).toArray()
    const skipLinks = skips.map((s) => ({ skipId: s.id!, planItemId: s.planItemId! }))
    return { mode, items, skipLinks, days: [...daysOf(await getSettings(db), mode)], replacedBy: [] }
  })
}

/**
 * Undo of a whole-plan replace (1.14.1), in one transaction: `mode`'s items that the replace added are deleted (their
 * skips kept, with no plan item), the snapshot's items are added back with their ids, the skips that pointed at them
 * and still have no plan item point at them again, and the side's training days are put back. Sessions, sets, advice,
 * check-ins and skips made since are not touched, nor is the other side.
 *
 * Only the latest replace can be undone: if the side's items are no longer exactly the ones the replace added (an
 * Undo already ran, another plan was saved, or an item was added or deleted), it is "stale" and nothing changes.
 * A failure part-way rolls everything back and rejects.
 */
export function restorePlan(db: GritDb, mode: TrainingMode, snapshot: PlanSnapshot): Promise<'restored' | 'stale'> {
  return db.transaction('rw', db.planItems, db.skips, db.settings, async () => {
    if (snapshot.mode !== mode) return 'stale'
    const current = (await planFor(db, mode)).map((p) => p.id)
    const added = new Set(snapshot.replacedBy)
    if (current.length !== added.size || current.some((id) => !added.has(id))) return 'stale'
    await forgetPlanItems(db, current)
    await db.planItems.bulkDelete(current)
    // Added, not put: an id taken since fails the undo rather than overwriting that row.
    await db.planItems.bulkAdd(snapshot.items.map((p) => ({ ...p })))
    for (const { skipId, planItemId } of snapshot.skipLinks) {
      const skip = await db.skips.get(skipId)
      if (skip != null && skip.planItemId == null) await db.skips.update(skipId, { planItemId })
    }
    const days = [...snapshot.days]
    await updateSettings(db, (s) => (mode === 'BODYBUILDING' ? { ...s, bodybuildingTrainingDays: days } : { ...s, trainingDays: days }))
    return 'restored'
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
    const write = async () => {
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
    }
    // A swap deletes an item and adds another in its place: a superset it was in is unlinked (1.24), on each side touched.
    const sides = [...new Set([...change.updated, ...change.removed, ...change.added.map((a) => a.item)].map((p) => sideOf(planItemFromJs(p))))]
    const [first, second] = sides
    if (first == null) await write()
    else if (second == null) await keepingLinks(db, first, write)
    else await keepingLinks(db, first, () => keepingLinks(db, second, write))
  })
}
