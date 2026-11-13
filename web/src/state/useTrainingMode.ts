import { liveQuery } from 'dexie'
import { useEffect, useState } from 'react'
import { getSettings, type GritDb } from '../db/db'
import type { TrainingMode } from '../db/schema'
import { rememberMode, rememberedMode } from '../ui/modeHeadStart'
import { applyTrainingMode } from '../ui/theme'

/**
 * The side the app is on, read live from the settings, and its look put on the page root while the app
 * is open (`applyTrainingMode`). Until the settings have been read (or with no database yet) it is the side
 * remembered at the last visit, the head start public/mode-head-start.js already put on, so a reload does
 * not flash the powerlifting palette. Each read remembers the side for the next visit. Unmounting puts the
 * powerlifting look back.
 */
export function useTrainingMode(db: GritDb | null): TrainingMode {
  const [mode, setMode] = useState<TrainingMode>(rememberedMode)
  useEffect(() => {
    if (db == null) return
    // A failed read leaves the look as it is; the screens that need the settings report it.
    const subscription = liveQuery(() => getSettings(db)).subscribe({
      next: (s) => {
        rememberMode(s.trainingMode)
        setMode(s.trainingMode)
      },
      error: () => {},
    })
    return () => subscription.unsubscribe()
  }, [db])
  useEffect(() => applyTrainingMode(mode), [mode])
  useEffect(() => () => applyTrainingMode('POWERLIFTING'), [])
  return mode
}
