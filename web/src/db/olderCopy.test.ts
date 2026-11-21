import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Dexie from 'dexie'
import { GritDb, OlderCopyError, ensureSeeded, openGritDb } from './db'
import { SCHEMA_VERSIONS } from './schema'

// 1.17: a copy of Grit older than the database it finds refuses to open it, before reading or writing anything. An
// older copy reads plate counts as kilograms and its backups drop the plate markers. Dexie itself lets an older
// app open a newer database when the stores match, so the guard is Grit's own.

const TARGET = Math.max(...SCHEMA_VERSIONS.map((v) => v.version))

let n = 0
let name = ''
const opened: Dexie[] = []
beforeEach(() => {
  name = `grit-older-${++n}`
})
afterEach(async () => {
  for (const d of opened.splice(0)) d.close()
  vi.restoreAllMocks()
  await Dexie.delete(name)
})

/** The native IndexedDB version of `name`, read without changing it. */
function nativeVersion(): Promise<number> {
  return new Promise((done, fail) => {
    const req = indexedDB.open(name)
    req.onsuccess = () => {
      const version = req.result.version
      req.result.close()
      done(version)
    }
    req.onerror = () => fail(req.error)
  })
}

/** Grit's database at this version, with a session and a set in it, then moved to `native` with the same stores. */
async function storedAt(native: number) {
  const db = await openGritDb(name)
  await ensureSeeded(db)
  await db.sessions.add({ date: '2026-11-18', mode: 'POWERLIFTING' } as never)
  await db.sets.add({ sessionId: 1, exerciseId: 1, setNumber: 1, reps: 5, weightKg: 4, unit: 'PLATES' } as never)
  db.close()
  if (native === TARGET * 10) return
  await new Promise<void>((done, fail) => {
    const req = indexedDB.open(name, native)
    // Same stores and indexes: only the number changes, as a newer Grit with no store change would leave it.
    req.onupgradeneeded = () => {}
    req.onsuccess = () => {
      req.result.close()
      done()
    }
    req.onerror = () => fail(req.error)
  })
}

/** Every row of every store, read with the raw IndexedDB API. */
async function everything(): Promise<Record<string, unknown[]>> {
  const idb = await new Promise<IDBDatabase>((done, fail) => {
    const req = indexedDB.open(name)
    req.onsuccess = () => done(req.result)
    req.onerror = () => fail(req.error)
  })
  try {
    const out: Record<string, unknown[]> = {}
    for (const store of Array.from(idb.objectStoreNames)) {
      out[store] = await new Promise((done, fail) => {
        const req = idb.transaction(store).objectStore(store).getAll()
        req.onsuccess = () => done(req.result)
        req.onerror = () => fail(req.error)
      })
    }
    return out
  } finally {
    idb.close()
  }
}

describe('an older copy of Grit', () => {
  it('native version 60, the version this copy declares: opens', async () => {
    await storedAt(60)
    const db = await openGritDb(name)
    opened.push(db)
    expect(db.backendDB().version).toBe(60)
    expect(await db.sets.count()).toBe(1)
  })

  it('native version 61, Dexie bumped with the same declared version: opens', async () => {
    // Dexie raises the native number by one when it adds a missing index without a new declared version
    // ("SchemaDiff"), so this very copy can leave 61 behind. It is still declared version 6: refusing it would lock
    // the lifter out of their own log.
    await storedAt(61)
    const db = await openGritDb(name)
    opened.push(db)
    expect(db.backendDB().version).toBe(61)
    expect(await db.sets.count()).toBe(1)
  })

  it('native version 65 is still declared version 6: opens (the comparison does not round 6.5 up to 7)', async () => {
    await storedAt(65)
    const db = await openGritDb(name)
    opened.push(db)
    expect(db.backendDB().version).toBe(65)
  })

  it('native version 70, a newer Grit: refused with OlderCopyError, before anything is read, and nothing is written', async () => {
    await storedAt(70)
    const before = await everything()
    const open = vi.spyOn(indexedDB, 'open')

    const error = await openGritDb(name).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(OlderCopyError)
    // One open, the version probe, which reads no rows; no connection with Grit's schema and no safety copy.
    expect(open).toHaveBeenCalledTimes(1)
    open.mockRestore()

    expect(await nativeVersion()).toBe(70)
    const after = await everything()
    expect(after).toEqual(before)
    expect((after.meta as { key: string }[]).map((r) => r.key)).not.toContain('safetyCopy')
  })

  it('the reopen path: a connection that finds a newer database (Dexie reopens without a version) fails the same way and stays closed', async () => {
    // What happens when another window moves the database on between the probe and the open: Grit's own
    // connection, opened straight away, must not be handed out either.
    await storedAt(70)
    const db = new GritDb(name, SCHEMA_VERSIONS)
    opened.push(db)
    await expect(db.open()).rejects.toBeInstanceOf(OlderCopyError)
    expect(db.isOpen()).toBe(false)
    expect(await nativeVersion()).toBe(70)
  })

  it('a database with no stores at a high version is not taken for a newer Grit: the usual failure, not this one', async () => {
    await new Promise<void>((done, fail) => {
      const req = indexedDB.open(name, 9999)
      req.onsuccess = () => {
        req.result.close()
        done()
      }
      req.onerror = () => fail(req.error)
    })
    const error = await openGritDb(name)
      .then(async (db) => {
        opened.push(db)
        await ensureSeeded(db)
      })
      .catch((e: unknown) => e)
    expect(error).not.toBeInstanceOf(OlderCopyError)
  })
})
