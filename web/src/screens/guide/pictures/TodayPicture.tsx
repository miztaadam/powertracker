import { useMemo } from 'react'
import type { TrainingMode } from '../../../db/schema'
import { grit } from '../../../domain'
import { TodayContent } from '../../today/TodayScreen'
import { TODAY_TAP_SELECTORS, todayTargets } from '../guideFlip'
import { PHONE_HEIGHT } from '../guideLabels'
import { NO_TODAY_ACTIONS, guideTodayState } from '../guideSampleState'
import type { PictureDef, PictureProps, PictureTap } from './picture'

/**
 * The Today screen at one step of logging a set (screen 4), `progress` through the move into it, with no-op actions:
 * the lift's editor shows the domain's in-between values (`guideSampleTodayEditor`).
 */
function TodayPicture({ mode, step, progress }: PictureProps) {
  const editor = grit.guideSampleTodayEditor(mode, step, progress)
  const weightKg = editor.weightKg ?? null
  const { reps, rpe } = editor
  const state = useMemo(() => guideTodayState(mode, step, { weightKg, reps, rpe }), [mode, step, weightKg, reps, rpe])
  return <TodayContent state={state} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />
}

const taps = (_mode: TrainingMode): Record<string, PictureTap> => Object.fromEntries(Object.entries(TODAY_TAP_SELECTORS).map(([tap, selector]) => [tap, { selector, ink: 'accent-ink' }]))

export const TODAY_PICTURE: PictureDef = { Draw: TodayPicture, outlined: true, height: PHONE_HEIGHT, targets: todayTargets, taps, rises: (key) => key === 'skipped' }
