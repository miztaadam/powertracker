import type { Transaction } from 'dexie'
import { grit } from '../domain'
import type { ExerciseRow } from './schema'

/**
 * The one-time strict press upgrade (strict press spec §4.1), run by schema version 2 and when a version-1 backup is
 * restored. The same rule as Android's `StrictPressUpgrade`.
 *
 * The rows that match are the non-archived ones whose name is the catalogue's "Strict press" or any of its aliases
 * ("Overhead press", "OHP", ...), as `grit.findInLibrary` matches them. The oldest (lowest id) becomes the main lift:
 * named "Strict press", category STRICT_PRESS, built in. The others become STRICT_PRESS under their own names. Ids
 * never change, so every set, plan item and baseline stays attached. Running it again changes nothing more.
 */
const STRICT_PRESS = 'STRICT_PRESS'

function entryName(): string {
  const entry = grit.findInLibrary(grit.liftLabel(STRICT_PRESS))
  if (!entry) throw new Error('Strict press is missing from the catalogue')
  return entry.name
}

type Candidate = { id: number; name: string; archived: boolean }

/** The ids of the matching rows, oldest first: the first becomes the main lift, the rest are only re-categorised. */
export function strictPressMatches(rows: readonly Candidate[]): number[] {
  const name = entryName()
  return rows
    .filter((r) => !r.archived && grit.findInLibrary(r.name)?.name === name)
    .map((r) => r.id)
    .sort((a, b) => a - b)
}

/** Applies the rule to restored rows before they are written. Pure: returns new rows. */
export function applyStrictPressUpgrade<T extends ExerciseRow & { id: number }>(rows: readonly T[]): T[] {
  const ids = strictPressMatches(rows)
  const [main] = ids
  const name = entryName()
  return rows.map((row) => {
    if (row.id === main) return { ...row, name, category: STRICT_PRESS, isBuiltIn: true }
    if (ids.includes(row.id)) return { ...row, category: STRICT_PRESS }
    return row
  })
}

/** Applies the rule to the exercises table in place, inside a Dexie upgrade. Data only: no table changes. */
export async function runStrictPressUpgrade(tx: Transaction): Promise<void> {
  const exercises = tx.table<ExerciseRow & { id: number }, number>('exercises')
  const ids = strictPressMatches(await exercises.toArray())
  const name = entryName()
  for (const [i, id] of ids.entries()) {
    await exercises.update(id, i === 0 ? { name, category: STRICT_PRESS, isBuiltIn: true } : { category: STRICT_PRESS })
  }
}
