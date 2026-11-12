import type Dexie from 'dexie'
import { encodeBackup, exportBackup } from '../backup/backup'

export const SAFETY_COPY_KEY = 'safetyCopy'

export type SafetyCopy = { kind: 'backup' | 'raw'; json: string }

/**
 * The safety copy is normally a full Android-compatible backup JSON, so recovery can restore it
 * with `decodeBackup` + `restoreBackup`. It runs against a database still on its old schema, so the
 * backup kind is only valid while the stored shape matches the exporter: future renames must keep
 * old field names readable, or rely on the raw fallback below.
 *
 * It must never block startup: if the export throws (for example a malformed stored date), it falls
 * back to `{ kind: 'raw', schemaVersion, tables }`, a plain dump of every table that recovery can
 * only offer to export. Both kinds carry when they were made (`exportedAt`, `savedAt`).
 */
export async function snapshotForSafety(db: Dexie): Promise<SafetyCopy> {
  try {
    const file = await exportBackup(db, Date.now())
    // Data still on schema 1 has not had the strict press upgrade: label it as the version-1 backup it is, so
    // restoring it applies the upgrade, as a restored version-1 file does.
    if (db.verno < 2) file.version = 1
    return { kind: 'backup', json: encodeBackup(file) }
  } catch {
    const tables: Record<string, unknown[]> = {}
    await db.transaction('r', db.tables, async () => {
      for (const table of db.tables) {
        const rows = await table.toArray()
        tables[table.name] =
          table.name === 'meta' ? rows.filter((r: { key: string }) => r.key !== SAFETY_COPY_KEY) : rows
      }
    })
    return { kind: 'raw', json: JSON.stringify({ kind: 'raw', schemaVersion: db.verno, savedAt: Date.now(), tables }) }
  }
}

/** Stores a snapshot in `meta` (its own transaction, so it is committed before any upgrade). */
export async function writeSafetyCopy(db: Dexie): Promise<void> {
  const copy = await snapshotForSafety(db)
  await db.table('meta').put({ key: SAFETY_COPY_KEY, value: copy.json })
}

export async function readSafetyCopy(db: Dexie): Promise<SafetyCopy | null> {
  const row = await db.table('meta').get(SAFETY_COPY_KEY)
  if (typeof row?.value !== 'string') return null
  let kind: SafetyCopy['kind'] = 'backup'
  try {
    if ((JSON.parse(row.value) as { kind?: unknown }).kind === 'raw') kind = 'raw'
  } catch {
    kind = 'raw'
  }
  return { kind, json: row.value }
}

/** The device's calendar date (`YYYY-MM-DD`) on which a safety copy was made; null when it does not say. */
export function safetyCopyDate(copy: SafetyCopy): string | null {
  try {
    const parsed = JSON.parse(copy.json) as { exportedAt?: unknown; savedAt?: unknown }
    const at = copy.kind === 'backup' ? parsed.exportedAt : parsed.savedAt
    if (typeof at !== 'number' || !Number.isFinite(at)) return null
    const d = new Date(at)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return null
  }
}
