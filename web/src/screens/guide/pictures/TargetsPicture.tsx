import { useMemo } from 'react'
import { grit } from '../../../domain'
import { TodayContent } from '../../today/TodayScreen'
import { todayTargets } from '../guideFlip'
import { PHONE_HEIGHT } from '../guideLabels'
import { NO_TODAY_ACTIONS, guideTargetsState } from '../guideSampleState'
import type { PictureDef, PictureProps } from './picture'

/**
 * Screen 5: Today on each session of the sample lift's run, its card holding the coach's target for that day and the
 * fields as following it puts them in, rolled from the session before through values the app allows
 * (`guideSampleTargetEditor`). On bodybuilding the reps climb, then the weight.
 */
function TargetsPicture({ mode, stepIndex, progress }: PictureProps) {
  const editor = grit.guideSampleTargetEditor(mode, stepIndex, progress)
  const weightKg = editor.weightKg ?? null
  const { reps, rpe } = editor
  const state = useMemo(() => guideTargetsState(mode, stepIndex, { weightKg, reps, rpe }), [mode, stepIndex, weightKg, reps, rpe])
  return <TodayContent state={state} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />
}

export const TARGETS_PICTURE: PictureDef = { Draw: TargetsPicture, outlined: true, height: PHONE_HEIGHT, targets: todayTargets }
