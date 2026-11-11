import { useEffect, useState } from 'react'
import { ensureSeeded, openGritDb, type GritDb } from '../db/db'

/** Opens the app's database and adds the built-in exercises. Null until it is ready. */
export function useGritDb(): GritDb | null {
  const [db, setDb] = useState<GritDb | null>(null)
  const [failure, setFailure] = useState<unknown>(null)
  useEffect(() => {
    let closed = false
    let opened: GritDb | null = null
    openGritDb()
      .then(async (d) => {
        opened = d
        if (closed) return d.close()
        await ensureSeeded(d)
        if (!closed) setDb(d)
      })
      .catch((e: unknown) => {
        if (!closed) setFailure(e)
      })
    return () => {
      closed = true
      opened?.close()
    }
  }, [])
  // Not being able to open the database is for the app's error handling, not for an empty screen.
  if (failure != null) throw failure
  return db
}
