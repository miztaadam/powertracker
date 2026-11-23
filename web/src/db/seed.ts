import { grit } from '../domain'
import { exerciseFromJs } from './mapping'
import type { GritDb } from './db'

/** Adds any built-in exercise that is missing (by seed id). Never overwrites user edits. */
export async function ensureSeeded(db: GritDb): Promise<void> {
  await db.transaction('rw', db.exercises, async () => {
    const existing = new Set(await db.exercises.toCollection().primaryKeys())
    const missing = grit
      .seedExercises()
      .filter((e) => !existing.has(e.id))
      .map(exerciseFromJs)
    if (missing.length > 0) await db.exercises.bulkAdd(missing)
  })
}
