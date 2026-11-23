import { createContext, useContext } from 'react'
import type { TrainingMode } from '../../db/schema'
import './ModeLabel.css'

/** The side the app is on, set once at the app root (App.tsx) for every screen below it. */
export const TrainingModeContext = createContext<TrainingMode>('POWERLIFTING')

/**
 * The small BODYBUILDING pill under a top-level screen's date or overline (bodybuilding mode spec §7.2).
 * Drawn only on the bodybuilding side, so the powerlifting side is unchanged. Not a control: never a tab
 * stop, and a screen reader hears it once as "Bodybuilding mode". A screen that belongs to one side whatever the app is
 * on (the templates of the side in its address) passes that side as `mode`.
 */
export function ModeLabel({ mode }: { mode?: TrainingMode } = {}) {
  const appMode = useContext(TrainingModeContext)
  if ((mode ?? appMode) !== 'BODYBUILDING') return null
  return (
    <p className="gr-mode-label" data-testid="mode-label">
      <span className="gr-mode-label-pill" aria-hidden="true">
        BODYBUILDING
      </span>
      <span className="sr-only">Bodybuilding mode</span>
    </p>
  )
}
