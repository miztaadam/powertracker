import type { TrainingMode } from '../db/schema'

/** The page background of each side, per scheme (tokens.css `--bg`), for the browser chrome. */
const BACKGROUND = {
  POWERLIFTING: { dark: '#0B0C0E', light: '#F4F4F0' },
  BODYBUILDING: { dark: '#070B14', light: '#EDF2FA' },
} as const

export type Scheme = keyof (typeof BACKGROUND)['POWERLIFTING']

const query = (): MediaQueryList | null =>
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: light)')
    : null

/** The system colour scheme; dark unless the system asks for light. */
export function currentScheme(): Scheme {
  return query()?.matches ? 'light' : 'dark'
}

/** The side whose look is on: the one `applyTrainingMode` last set. */
const currentMode = (): TrainingMode => (document.documentElement.dataset.mode === 'bodybuilding' ? 'BODYBUILDING' : 'POWERLIFTING')

function applyThemeColor() {
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', BACKGROUND[currentMode()][currentScheme()])
}

/** Keeps the browser chrome (`theme-color`) in step with the system scheme. Returns a stop function. */
export function syncThemeColor(): () => void {
  applyThemeColor()
  const mq = query()
  mq?.addEventListener('change', applyThemeColor)
  return () => mq?.removeEventListener('change', applyThemeColor)
}

/**
 * Puts on the look of `mode`: `data-mode="bodybuilding"` on <html> swaps the palette in tokens.css; the
 * powerlifting side has no attribute. The browser chrome follows.
 */
export function applyTrainingMode(mode: TrainingMode): void {
  const root = document.documentElement
  if (mode === 'BODYBUILDING') root.dataset.mode = 'bodybuilding'
  else delete root.dataset.mode
  applyThemeColor()
}
