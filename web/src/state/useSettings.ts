import { liveQuery } from 'dexie'
import { useCallback, useEffect, useState } from 'react'
import { DEFAULT_SETTINGS, getSettings, readSafetyCopy, updateSettings, type AppSettings, type GritDb } from '../db/db'
import { storageProtected } from '../platform/storage'

export type SettingsState = {
  loading: boolean
  settings: AppSettings
  /** Whether the browser keeps Grit's storage; null until it has answered. */
  storageProtected: boolean | null
  /** The copy saved before the last schema upgrade: a restorable backup, a raw dump, or none. */
  safetyCopy: 'backup' | 'raw' | null
}

export type SettingsActions = {
  /** Applies `change` to the stored settings. */
  update: (change: (current: AppSettings) => AppSettings) => Promise<void>
}

type Stored = { settings: AppSettings; safetyCopy: SettingsState['safetyCopy'] }

async function readStored(db: GritDb): Promise<Stored> {
  const [settings, copy] = await Promise.all([getSettings(db), readSafetyCopy(db)])
  return { settings, safetyCopy: copy?.kind ?? null }
}

/** The settings, read live from the database, with what the Settings screen shows beside them. */
export function useSettings(db: GritDb): SettingsState & SettingsActions {
  const [stored, setStored] = useState<Stored | null>(null)
  const [readError, setReadError] = useState<unknown>(null)
  const [isProtected, setProtected] = useState<boolean | null>(null)
  useEffect(() => {
    const subscription = liveQuery(() => readStored(db)).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  useEffect(() => {
    let stale = false
    storageProtected()
      .then((value) => {
        if (!stale) setProtected(value)
      })
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [])
  // A failed read is for the app's error boundary, not for a screen of default settings.
  if (readError != null) throw readError

  const update = useCallback<SettingsActions['update']>(
    async (change) => {
      await updateSettings(db, change)
    },
    [db],
  )

  return { loading: stored == null, settings: stored?.settings ?? DEFAULT_SETTINGS, storageProtected: isProtected, safetyCopy: stored?.safetyCopy ?? null, update }
}
