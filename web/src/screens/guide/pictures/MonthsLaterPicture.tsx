import { useMemo } from 'react'
import { grit } from '../../../domain'
import { BodybuildingProgressContent } from '../../progress/BodybuildingProgress'
import { ProgressContent } from '../../progress/ProgressScreen'
import type { FlipTarget } from '../guideFlip'
import { PHONE_HEIGHT } from '../guideLabels'
import { guideMonthsBodybuildingState, guideMonthsState } from '../guideSampleState'
import { keyedBlocks, type PictureDef, type PictureProps } from './picture'

const none = () => {}

/**
 * Screen 7: the Progress screen as the run goes by, drawn on the day the domain gives (`guideSampleMonthsDate`): whole
 * weeks, one at a time through each move, so the chart gains its points and the bests and the total count up through
 * what the screen would really have shown. On bodybuilding the check-in arrives at the end, marking the lagging muscle.
 */
function MonthsLaterPicture({ mode, step, progress }: PictureProps) {
  const date = grit.guideSampleMonthsDate(mode, step, progress)
  const arrived = grit.guideSampleMonthsCheckIn(mode, step, progress) != null
  // Worked out again only when the day (or the check-in's arrival) changes, not on every frame of a move.
  const content = useMemo(() => {
    if (mode !== 'BODYBUILDING') return <ProgressContent state={guideMonthsState(mode, date)} onRange={none} onOpenLift={none} />
    const checkIn = arrived ? (grit.guideSampleMonthsCheckIn(mode, step, 1) ?? null) : null
    return <BodybuildingProgressContent state={guideMonthsBodybuildingState(mode, date, checkIn)} onOpenExercise={none} onCheckIn={none} />
  }, [mode, date, arrived, step])
  return content
}

/** The screen's blocks, and on bodybuilding each exercise's and body part's row in them. */
function monthsTargets(screen: HTMLElement): FlipTarget[] {
  const body = screen.querySelector<HTMLElement>('.screen-body')
  if (body == null) return []
  return keyedBlocks(body, ':scope > *', 'block:').flatMap((block) => [block, ...keyedBlocks(block.el, '.bb-rows > li', `${block.key}>`, block.key)])
}

export const MONTHS_LATER_PICTURE: PictureDef = { Draw: MonthsLaterPicture, outlined: true, height: PHONE_HEIGHT, targets: monthsTargets }
