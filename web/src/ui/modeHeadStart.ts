import type { TrainingMode } from '../db/schema'

/**
 * The browser-storage key that remembers the side the app was last on, so a reload can put the bodybuilding
 * look on before the first paint (public/mode-head-start.js reads it, before the app has loaded). It is only a
 * head start: the stored settings remain the truth, and `useTrainingMode` corrects the look once they are read.
 * Kept in step with public/mode-head-start.js.
 */
export const MODE_KEY = 'grit:trainingMode'

/**
 * The browser-storage key a test run sets to `off` to run with motion off (playwright.config.ts sets it for every
 * e2e page). public/mode-head-start.js reads it, with the system's reduced-motion setting, and puts
 * `data-motion="off"` on <html>. Kept in step with that script.
 */
export const MOTION_KEY = 'grit:motion'

/** The side last remembered; the powerlifting side when nothing (or nothing readable) is remembered. */
export function rememberedMode(): TrainingMode {
  try {
    return localStorage.getItem(MODE_KEY) === 'BODYBUILDING' ? 'BODYBUILDING' : 'POWERLIFTING'
  } catch {
    // Storage blocked (a private window, a strict setting): no head start.
    return 'POWERLIFTING'
  }
}

/** Remembers the side read from the settings. The powerlifting side needs no head start and stores nothing. */
export function rememberMode(mode: TrainingMode): void {
  try {
    if (mode === 'BODYBUILDING') localStorage.setItem(MODE_KEY, mode)
    else localStorage.removeItem(MODE_KEY)
  } catch {
    // Storage blocked or full: the next reload shows the powerlifting palette for a moment, nothing worse.
  }
}
