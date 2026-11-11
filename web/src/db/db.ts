import Dexie, { type EntityTable } from 'dexie'
import { JsHistory } from '../domain'
import { baselineToJs, exerciseToJs, sessionToJs } from './mapping'
import {
  SCHEMA_VERSIONS,
  type AdviceRow,
  type BaselineRow,
  type ExerciseRow,
  type KeyValueRow,
  type PlanItemRow,
  type SchemaVersion,
  type SessionRow,
  type SetRow,
} from './schema'
import { writeSafetyCopy } from './safety'

export { ensureSeeded } from './seed'
export { readSafetyCopy, writeSafetyCopy } from './safety'
export { DEFAULT_SETTINGS, getSettings, updateSettings, type AppSettings } from './settings'

const versionChangeListeners = new Set<() => void>()

/** Calls `cb` when another tab needs a newer schema and this connection was closed. Returns an unsubscribe. */
export function onDbVersionChange(cb: () => void): () => void {
  versionChangeListeners.add(cb)
  return () => versionChangeListeners.delete(cb)
}

/** Thrown by `openGritDb` when a schema upgrade failed. Data is untouched; a safety copy exists. */
export class UpgradeFailedError extends Error {
  constructor(cause: unknown) {
    super('Upgrading the Grit database failed. Your data is unchanged and a safety copy was saved.', { cause })
    this.name = 'UpgradeFailedError'
  }
}

export class GritDb extends Dexie {
  exercises!: EntityTable<ExerciseRow, 'id'>
  sessions!: EntityTable<SessionRow, 'id'>
  sets!: EntityTable<SetRow, 'id'>
  planItems!: EntityTable<PlanItemRow, 'id'>
  advice!: EntityTable<AdviceRow, 'id'>
  baselines!: EntityTable<BaselineRow, 'exerciseId'>
  settings!: EntityTable<KeyValueRow, 'key'>
  meta!: EntityTable<KeyValueRow, 'key'>

  constructor(name: string, versions: SchemaVersion[]) {
    super(name)
    for (const v of [...versions].sort((a, b) => a.version - b.version)) {
      const version = this.version(v.version).stores(v.stores)
      if (v.upgrade) version.upgrade(v.upgrade)
    }
    // Another tab/version wants to upgrade: release the connection and never reopen
    // with this (older) schema. The UI prompts for a reload.
    this.on('versionchange', () => {
      this.close({ disableAutoOpen: true })
      versionChangeListeners.forEach((cb) => cb())
      return false
    })
  }
}

/** Installed Dexie version of `name`, or 0 if the database does not exist (and is not created). */
export async function installedVersion(name: string): Promise<number> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(name)
    let existed = true
    req.onupgradeneeded = () => {
      existed = false
      req.transaction?.abort()
    }
    req.onsuccess = () => {
      const { version, objectStoreNames } = req.result
      req.result.close()
      resolve(existed && objectStoreNames.length > 0 ? Math.round(version / 10) : 0)
    }
    req.onerror = () => (existed ? reject(req.error) : resolve(0))
    req.onblocked = () => reject(new Error('Grit database is blocked by another tab'))
  })
}

/**
 * Opens the database. If it needs a schema upgrade, a safety copy of the current data is
 * written first. `schemaVersions` exists so tests can inject versions.
 */
export async function openGritDb(
  name = 'grit',
  options: { schemaVersions?: SchemaVersion[] } = {},
): Promise<GritDb> {
  const versions = options.schemaVersions ?? SCHEMA_VERSIONS
  const target = Math.max(...versions.map((v) => v.version))
  const installed = await installedVersion(name)

  if (installed > 0 && installed < target) {
    // Schema-less open reads the installed schema as-is.
    const current = new Dexie(name)
    await current.open()
    try {
      await writeSafetyCopy(current)
    } finally {
      current.close()
    }
  }

  const db = new GritDb(name, versions)
  try {
    await db.open()
  } catch (e) {
    db.close()
    if (installed > 0 && installed < target) throw new UpgradeFailedError(e)
    throw e
  }
  return db
}

export async function loadHistory(db: GritDb): Promise<JsHistory> {
  const [sessions, sets, exercises, baselines] = await Promise.all([
    db.sessions.orderBy('date').toArray(),
    db.sets.orderBy('id').toArray(),
    db.exercises.toArray(),
    db.baselines.toArray(),
  ])
  const bySession = new Map<number, SetRow[]>()
  for (const s of sets) {
    const list = bySession.get(s.sessionId) ?? []
    list.push(s)
    bySession.set(s.sessionId, list)
  }
  return new JsHistory(
    sessions.map((s) => sessionToJs(s as SessionRow & { id: number }, bySession.get(s.id as number) ?? [])),
    exercises.map((e) => exerciseToJs(e as ExerciseRow & { id: number })),
    baselines.map(baselineToJs),
  )
}
