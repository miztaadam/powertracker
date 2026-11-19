import type { TrainingMode } from '../../db/schema'

/*
 * The guide's own control words and labels, app-side (the screens' copy comes from the domain, `guideScreen`). Kept
 * apart from the components, with no styles, so the e2e reads the same strings the guide draws.
 */

export const SKIP = 'Skip'
export const BACK = 'Back'
export const NEXT = 'Next'
export const DONE = 'Done'
export const EXAMPLE = 'Example'

/** The guide's name, as a region of the page. */
export const GUIDE_LABEL = 'Guide'

/** What the row of dots says: the one screen shown of how many. */
export const dotsLabel = (index: number, count: number) => `Screen ${index + 1} of ${count}`

/** What a screen reader hears for a picture, the one thing of it in the accessibility tree. Only built pictures have one. */
const PICTURE_DESCRIPTIONS: Partial<Record<string, string>> = {
  TODAY: 'Example of the Today screen: a set’s weight, reps and effort are put in and the set is ticked, then an exercise is skipped.',
}

export function pictureDescription(id: string): string {
  const description = PICTURE_DESCRIPTIONS[id]
  if (description == null) throw new Error(`No picture for ${id} yet`)
  return description
}

/** The phone a picture's screen is laid out on before it is shrunk into the frame: the iPhone the e2e runs as. */
export const PHONE_WIDTH = 390

/**
 * The phone's height for `mode`'s Today picture: tall enough for that side's screen-4 day (`guideSampleTodayItems`), so
 * its last step shows the ticked set and the "Skipped today" line at the foot of the list. 844 px, the e2e iPhone, holds
 * powerlifting's; bodybuilding's lift card is taller (its stretch and squeeze cues), so its phone is taller, and drawn
 * smaller (as on Android). One height for both sides is the owner's call (see the 1.16 Task 2 fix report).
 */
export function phoneHeight(mode: TrainingMode): number {
  return mode === 'BODYBUILDING' ? 1180 : 844
}
