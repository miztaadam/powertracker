import { PHONE_HEIGHT } from '../guideLabels'
import { EmptyPlan } from './PlanPicture'
import type { PictureDef, PictureProps } from './picture'

/** Screen 8: the Plan screen as it opens on a new plan, ready for a template; the guide's own buttons are under it. */
function YourTurnPicture({ mode }: PictureProps) {
  return <EmptyPlan mode={mode} />
}

export const YOUR_TURN_PICTURE: PictureDef = { Draw: YourTurnPicture, outlined: true, height: PHONE_HEIGHT }
