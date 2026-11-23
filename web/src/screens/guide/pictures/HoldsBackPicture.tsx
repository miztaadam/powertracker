import { useMemo } from 'react'
import { CoachContent } from '../../coach/CoachScreen'
import type { FlipTarget } from '../guideFlip'
import { PHONE_HEIGHT } from '../guideLabels'
import { NO_COACH_ACTIONS, guideHoldsBackState } from '../guideSampleState'
import { keyedBlocks, type PictureDef, type PictureProps } from './picture'

/** Screen 6: the Coach screen with one of its cards for when it holds back at each step, in the rule table's order. */
function HoldsBackPicture({ mode, stepIndex }: PictureProps) {
  const state = useMemo(() => guideHoldsBackState(mode, stepIndex), [mode, stepIndex])
  return <CoachContent state={state} actions={NO_COACH_ACTIONS} />
}

/** The feed's cards, each by its heading: the next card cross-fades in where the last one was, rising as it comes. */
const holdsTargets = (screen: HTMLElement): FlipTarget[] => keyedBlocks(screen, '.screen-body > *', 'card:')

export const HOLDS_BACK_PICTURE: PictureDef = { Draw: HoldsBackPicture, outlined: true, height: PHONE_HEIGHT, targets: holdsTargets, rises: (key) => key.startsWith('card:') }
