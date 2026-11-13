import { liveQuery } from 'dexie'
import { useEffect, useState } from 'react'
import { getSettings, type GritDb } from '../db/db'
import type { TrainingMode } from '../db/schema'
import { applyTrainingMode } from '../ui/theme'

/**
 * The side the app is on, read live from the settings, and its look put on the page root while the app
 * is open (`applyTrainingMode`). The powerlifting side until the settings have been read, or with no
 * database yet. Unmounting puts the powerlifting look back.
 */
export function useTrainingMode(db: GritDb | null): TrainingMode {
  const [mode, setMode] = useState<TrainingMode>('POWERLIFTING')
  useEffect(() => {
    if (db == null) return
    // A failed read leaves the look as it is; the screens that need the settings report it.
    const subscription = liveQuery(() => getSettings(db)).subscribe({ next: (s) => setMode(s.trainingMode), error: () => {} })
    return () => subscription.unsubscribe()
  }, [db])
  useEffect(() => applyTrainingMode(mode), [mode])
  useEffect(() => () => applyTrainingMode('POWERLIFTING'), [])
  return mode
}
