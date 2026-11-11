import type Dexie from 'dexie'
import { encodeBackup, exportBackup } from '../backup/backup'

export const SAFETY_COPY_KEY = 'safetyCopy'

/**
 * The safety copy is a full Android-compatible backup JSON, so recovery can restore it with
 * `decodeBackup` + `restoreBackup`. Takes any Dexie: it also runs against a database still on its
 * old schema, just before an upgrade.
 */
export async function snapshotForSafety(db: Dexie): Promise<string> {
  return encodeBackup(await exportBackup(db, Date.now()))
}

/** Stores a snapshot in `meta` (its own transaction, so it is committed before any upgrade). */
export async function writeSafetyCopy(db: Dexie): Promise<void> {
  const json = await snapshotForSafety(db)
  await db.table('meta').put({ key: SAFETY_COPY_KEY, value: json })
}

export async function readSafetyCopy(db: Dexie): Promise<string | null> {
  const row = await db.table('meta').get(SAFETY_COPY_KEY)
  return typeof row?.value === 'string' ? row.value : null
}
