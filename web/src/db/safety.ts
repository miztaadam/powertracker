import type Dexie from 'dexie'

export const SAFETY_COPY_KEY = 'safetyCopy'

/**
 * Serialises every table to JSON. Temporary: W6 swaps this body for the real
 * backup export (`exportBackup`), and nothing else here needs to change.
 */
export async function snapshotForSafety(db: Dexie): Promise<string> {
  const tables: Record<string, unknown[]> = {}
  for (const table of db.tables) {
    const rows = await table.toArray()
    tables[table.name] =
      table.name === 'meta'
        ? rows.filter((r: { key: string }) => r.key !== SAFETY_COPY_KEY)
        : rows
  }
  return JSON.stringify({ kind: 'grit-safety-copy', version: db.verno, tables })
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
