import { useEffect, useState } from 'react'
import { ensureSeeded, openGritDb, type GritDb } from '../db/db'
import type { SchemaVersion } from '../db/schema'

/**
 * Opens the app's database and adds the built-in exercises. Null until it is ready. A failed open
 * is thrown while rendering, for the app's error boundary. `name` and `schemaVersions` are for tests.
 */
export function useGritDb(name?: string, schemaVersions?: SchemaVersion[]): GritDb | null {
  const [db, setDb] = useState<GritDb | null>(null)
  const [failure, setFailure] = useState<unknown>(null)
  useEffect(() => {
    let closed = false
    let opened: GritDb | null = null
    openGritDb(name, { schemaVersions })
      .then(async (d) => {
        opened = d
        if (closed) return d.close()
        await ensureSeeded(d)
        if (!closed) setDb(d)
      })
      .catch((e: unknown) => {
        // Not left open: recovery reads the database through a connection of its own.
        opened?.close()
        if (!closed) setFailure(e)
      })
    return () => {
      closed = true
      opened?.close()
    }
  }, [name, schemaVersions])
  // Not being able to open the database is for the app's error handling, not for an empty screen.
  if (failure != null) throw failure
  return db
}
