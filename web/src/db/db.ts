import Dexie, { type EntityTable } from 'dexie'
import { JsHistory } from '../domain'
import { baselineToJs, exerciseToJs, sessionToJs } from './mapping'
import {
  SCHEMA_VERSIONS,
  type AdviceRow,
  type BaselineRow,
  type CheckInRow,
  type ExerciseRow,
  type KeyValueRow,
  type PlanItemRow,
  type SchemaVersion,
  type SessionRow,
  type SetRow,
  type SkipRow,
} from './schema'
import { isQuotaError } from '../platform/storage'
import { writeSafetyCopy } from './safety'

export { ensureSeeded } from './seed'
export { readSafetyCopy, writeSafetyCopy } from './safety'
export {
  activeTrainingDays,
  clearLeftOn,
  DEFAULT_SETTINGS,
  getSettings,
  pickFirstSide,
  setTrainingMode,
  updateSettings,
  withFirstSide,
  withTrainingDayToggled,
  withTrainingMode,
  type AppSettings,
} from './settings'

const versionChangeListeners = new Set<() => void>()

/** Calls `cb` when another tab needs a newer schema and this connection was closed. Returns an unsubscribe. */
export function onDbVersionChange(cb: () => void): () => void {
  versionChangeListeners.add(cb)
  return () => versionChangeListeners.delete(cb)
}

const quotaListeners = new Set<() => void>()

/** Calls `cb` each time a write fails because the phone's storage is full. Returns an unsubscribe. */
export function onQuotaExceeded(cb: () => void): () => void {
  quotaListeners.add(cb)
  return () => quotaListeners.delete(cb)
}

function reportQuota(error: unknown): void {
  if (isQuotaError(error)) quotaListeners.forEach((cb) => cb())
}

/**
 * Thrown by `openGritDb` when a schema upgrade failed. Data is untouched. `safetyCopySaved` says
 * whether the copy of it was written first: false when writing the copy is what failed.
 */
export class UpgradeFailedError extends Error {
  constructor(
    cause: unknown,
    readonly safetyCopySaved: boolean,
  ) {
    super(`Upgrading the Grit database failed. Your data is unchanged${safetyCopySaved ? ' and a safety copy was saved' : ''}.`, { cause })
    this.name = 'UpgradeFailedError'
  }
}

/**
 * Thrown by `openGritDb` when the database was written by a newer Grit than this copy (1.17). Nothing is read from it
 * or written to it: this copy would read plate counts as kilograms, and its backups would drop the plate markers.
 * Dexie does not refuse on its own: it opens a newer database whose stores match. The app shows OlderCopyScreen.
 */
export class OlderCopyError extends Error {
  constructor(
    readonly nativeVersion: number,
    readonly declaredVersion: number,
  ) {
    super(`This copy of Grit (storage version ${declaredVersion}) is older than the stored log (native version ${nativeVersion}).`)
    this.name = 'OlderCopyError'
  }
}

/**
 * Whether a database at IndexedDB version `native` was made by a newer Grit than one that declares `declared`.
 * Dexie stores declared version × 10, and adds 1 each time it patches in a missing index without a new declared
 * version, so 60 to 69 are all version 6 (rounded, 65 would wrongly read as 7). Only 70 and up are newer.
 */
export function isNewerThan(native: number, declared: number): boolean {
  return Math.floor(native / 10) > declared
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
  checkIns!: EntityTable<CheckInRow, 'id'>
  skips!: EntityTable<SkipRow, 'id'>

  constructor(name: string, versions: SchemaVersion[]) {
    super(name)
    for (const v of [...versions].sort((a, b) => a.version - b.version)) {
      const version = this.version(v.version).stores(v.stores)
      if (v.upgrade) version.upgrade(v.upgrade)
    }
    // Every write passes here, so a full phone is noticed whichever screen was saving. The write
    // still fails for its caller, and its transaction rolls back: nothing is half-written.
    this.use({
      stack: 'dbcore',
      name: 'grit-quota',
      create: (down) => ({
        ...down,
        transaction: (stores, mode, options) => {
          const tx = down.transaction(stores, mode, options)
          // The browser may only notice at commit: then the transaction aborts with the error.
          if (mode === 'readwrite') (tx as unknown as IDBTransaction).addEventListener('abort', (event) => reportQuota((event.target as IDBTransaction | null)?.error))
          return tx
        },
        table: (tableName) => {
          const table = down.table(tableName)
          return {
            ...table,
            mutate: (request) =>
              table.mutate(request).then(
                (response) => {
                  if (response.numFailures > 0) Object.values(response.failures).forEach(reportQuota)
                  return response
                },
                (error: unknown) => {
                  reportQuota(error)
                  throw error
                },
              ),
          }
        },
      }),
    })
    // A newer Grit's database, found on the way in (another window moved it on after openGritDb looked, or Dexie
    // reopened without a version after a VersionError): fail the open, so the connection is closed before anything
    // is read. Sticky, so a reopen is checked too. A database with no stores is not a Grit log at all: left to the
    // usual failure.
    this.on(
      'ready',
      (vip) => {
        const idb = (vip as Dexie).backendDB()
        if (idb.objectStoreNames.length > 0 && isNewerThan(idb.version, this.verno)) throw new OlderCopyError(idb.version, this.verno)
      },
      true,
    )
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
  const native = await installedNativeVersion(name)
  return Math.round(native / 10)
}

/** The IndexedDB version of `name` (Dexie's version × 10, plus its patches), or 0 if it does not exist or has no stores. */
async function installedNativeVersion(name: string): Promise<number> {
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
      resolve(existed && objectStoreNames.length > 0 ? version : 0)
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
  const native = await installedNativeVersion(name)
  // Written by a newer Grit: not opened at all, so nothing is read or written (not even a safety copy).
  if (isNewerThan(native, target)) throw new OlderCopyError(native, target)
  const installed = Math.round(native / 10)

  if (installed > 0 && installed < target) {
    // Schema-less open reads the installed schema as-is.
    const current = new Dexie(name)
    await current.open()
    try {
      await writeSafetyCopy(current)
    } catch (e) {
      throw new UpgradeFailedError(e, false)
    } finally {
      current.close()
    }
  }

  const db = new GritDb(name, versions)
  try {
    await db.open()
  } catch (e) {
    db.close()
    if (e instanceof OlderCopyError) throw e
    if (installed > 0 && installed < target) throw new UpgradeFailedError(e, true)
    throw e
  }
  return db
}

export async function loadHistory(db: GritDb, exceptSetId?: number): Promise<JsHistory> {
  const [sessions, allSets, exercises, baselines] = await Promise.all([
    db.sessions.orderBy('date').toArray(),
    db.sets.orderBy('id').toArray(),
    db.exercises.toArray(),
    db.baselines.toArray(),
  ])
  // With `exceptSetId`: the history as it was before that set was logged.
  const sets = exceptSetId == null ? allSets : allSets.filter((s) => s.id !== exceptSetId)
  return historyOf(sessions, sets, exercises, baselines)
}

/**
 * The domain's history from rows already read: what `loadHistory` reads, for a caller that has read them itself.
 * `sessions` in date order, `sets` in id order, as `loadHistory` reads them.
 */
export function historyOf(sessions: readonly SessionRow[], sets: readonly SetRow[], exercises: readonly ExerciseRow[], baselines: readonly BaselineRow[]): JsHistory {
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
