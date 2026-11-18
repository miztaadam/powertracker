import { grit, JsSkip, type JsAdvice } from '../domain'
import type { GritDb } from './db'
import { adviceFromJs } from './mapping'
import { sideOf, type AdviceRow, type SkipRow } from './schema'

/** A stored skip. */
export type StoredSkip = SkipRow & { id: number }

export function skipToJs(row: StoredSkip): JsSkip {
  return new JsSkip(row.id, row.date, row.mode, row.exerciseId, row.planItemId, row.reason)
}

/** Every stored skip, both sides', oldest first (by date, then id), as Android's `skips()`. */
export async function skips(db: GritDb): Promise<StoredSkip[]> {
  const rows = (await db.skips.toArray()) as StoredSkip[]
  return rows.sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id)
}

/**
 * The coach's cards for a skip just stored: `stored` carries its id, `all` is every stored skip on both sides, `stored`
 * included. It runs inside `skip`'s transaction, so it may read from the database with Dexie (the skips,
 * advice, history, plan and settings tables), but must not await anything else, or the transaction ends.
 */
export type SkipCards = (stored: JsSkip, all: JsSkip[]) => readonly JsAdvice[] | Promise<readonly JsAdvice[]>

/** `skip`'s and `unskip`'s tables: theirs, and what the coach reads to work out a skip's card. */
const scope = (db: GritDb) => [db.skips, db.advice, db.sessions, db.sets, db.exercises, db.baselines, db.planItems, db.settings]

/**
 * Stores `skip` and the coach's cards for it, in one transaction, and resolves to the skip's id. As Android's
 * `TrainingRepository.skip`:
 * - a skip already stored for the same date, side and exercise is updated in place (reason and plan item replaced, id
 *   kept), and its still-OPEN SKIP cards are deleted, so the new reason's card replaces the old one rather than
 *   stacking; an answered card stays;
 * - a PAIN skip deletes that date's OPEN TARGET advice for the exercise on both sides, since the hold crosses sides:
 *   it was worked out without the hold, and Today works a target out again only when none is stored for the day;
 * - a PAIN skip also deletes the exercise's OPEN busy or repeat SKIP cards on its side, whichever skip made them, so
 *   its pain card replaces them (a pain skip always tells you; the domain's `SkipCoach`). An open pain or SWAP card stays;
 * - then `cards` runs, and what it returns is inserted. If it throws, nothing is stored.
 */
export function skip(db: GritDb, skip: Omit<SkipRow, 'id'>, cards: SkipCards): Promise<number> {
  return db.transaction('rw', scope(db), async () => {
    const existing = (await db.skips.where('[date+mode+exerciseId]').equals([skip.date, skip.mode, skip.exerciseId]).first()) as StoredSkip | undefined
    const row: SkipRow = { date: skip.date, mode: skip.mode, exerciseId: skip.exerciseId, planItemId: skip.planItemId, reason: skip.reason }
    let id: number
    if (existing == null) id = (await db.skips.add(row)) as number
    else {
      id = existing.id
      await db.skips.put({ ...row, id })
      await deleteOpenCards(db, id)
    }
    if (skip.reason === 'PAIN') {
      await deleteOpenTargets(db, skip.date, skip.exerciseId)
      await deleteOpenNonPainCards(db, skip.mode, skip.exerciseId)
    }
    const all = (await skips(db)).map(skipToJs)
    const made = await cards(skipToJs({ ...row, id }), all)
    if (made.length > 0) await db.advice.bulkAdd(made.map((a) => withoutId(adviceFromJs(a))))
    return id
  })
}

/**
 * Takes the skip `skipId` back (Undo, Bring back), in one transaction: the skip, its still-OPEN SKIP cards and that
 * day's OPEN TARGET advice for the exercise on both sides are deleted, so the target is worked out again without the
 * hold. A card already applied or dismissed stays. An unknown id changes nothing.
 */
export function unskip(db: GritDb, skipId: number): Promise<void> {
  return db.transaction('rw', scope(db), async () => {
    const stored = await db.skips.get(skipId)
    if (stored == null) return
    await deleteOpenCards(db, skipId)
    await db.skips.delete(skipId)
    await deleteOpenTargets(db, stored.date, stored.exerciseId)
  })
}

/**
 * Keeps the skips of the plan items `planItemIds` with no plan item: what deleting a plan item does to its skips, as
 * Android's SET NULL foreign key. Call it in the transaction that deletes them, with `db.skips` in its scope.
 */
export async function forgetPlanItems(db: GritDb, planItemIds: readonly number[]): Promise<void> {
  if (planItemIds.length === 0) return
  await db.skips.where('planItemId').anyOf([...planItemIds]).modify({ planItemId: null })
}

/** Deletes the OPEN SKIP advice carrying `skipId`; the payloads are read through the domain, never matched by text. */
async function deleteOpenCards(db: GritDb, skipId: number): Promise<void> {
  await db.advice
    .where('type')
    .equals('SKIP')
    .filter((a) => a.status === 'OPEN' && isPayload(a, 'Skipped', (p) => p.skipId === skipId))
    .delete()
}

/** Deletes `mode`'s OPEN SKIP advice for `exerciseId` whose skip was not for pain; the payloads are read through the domain. */
async function deleteOpenNonPainCards(db: GritDb, mode: SkipRow['mode'], exerciseId: number): Promise<void> {
  await db.advice
    .where('type')
    .equals('SKIP')
    .filter((a) => a.status === 'OPEN' && sideOf(a) === mode && isPayload(a, 'Skipped', (p) => p.exerciseId === exerciseId && p.skipReason !== 'PAIN'))
    .delete()
}

/** Deletes `date`'s OPEN TARGET advice for `exerciseId` on both sides; the payloads are read through the domain. */
async function deleteOpenTargets(db: GritDb, date: string, exerciseId: number): Promise<void> {
  await db.advice
    .where('createdAt')
    .equals(date)
    .filter((a) => a.type === 'TARGET' && a.status === 'OPEN' && isPayload(a, 'Target', (p) => p.exerciseId === exerciseId))
    .delete()
}

function isPayload(a: AdviceRow, kind: string, test: (p: NonNullable<ReturnType<typeof grit.advicePayload>>) => boolean): boolean {
  const p = grit.advicePayload(a.payloadJson)
  return p != null && p.kind === kind && test(p)
}

function withoutId(row: AdviceRow & { id: number }): AdviceRow {
  const { id: _new, ...rest } = row
  return rest
}
