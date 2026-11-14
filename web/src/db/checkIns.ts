import { grit, JsCheckIn } from '../domain'
import type { GritDb } from './db'
import type { CheckInRow } from './schema'

/** `groups` in the domain's `MuscleGroup` order, each once: how a check-in stores its body parts. */
export function inGroupOrder(groups: Iterable<string>): string[] {
  const wanted = new Set(groups)
  return grit.muscleGroups().filter((g) => wanted.has(g))
}

export function checkInToJs(row: CheckInRow & { id: number }): JsCheckIn {
  return new JsCheckIn(row.date, [...row.lagging], [...row.growingWell], row.id)
}

/** Every check-in, oldest first. */
export async function loadCheckIns(db: GritDb): Promise<(CheckInRow & { id: number })[]> {
  return (await db.checkIns.orderBy('date').toArray()) as (CheckInRow & { id: number })[]
}

/**
 * Stores a check-in, replacing one of the same date (one per date, as on Android). Call it inside the `rw`
 * transaction that also stores what the check-in changes, so it lands with them or not at all.
 */
export async function putCheckIn(db: GritDb, date: string, lagging: Iterable<string>, growingWell: Iterable<string>): Promise<number> {
  return db.transaction('rw', db.checkIns, async () => {
    await db.checkIns.where('date').equals(date).delete()
    return (await db.checkIns.add({ date, lagging: inGroupOrder(lagging), growingWell: inGroupOrder(growingWell) })) as number
  })
}
