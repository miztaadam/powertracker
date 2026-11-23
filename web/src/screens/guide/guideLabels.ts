/*
 * The guide's own control words and labels, app-side (the screens' copy comes from the domain, `guideScreen`). Kept
 * apart from the components, with no styles, so the e2e reads the same strings the guide draws.
 */

export const SKIP = 'Skip'
export const BACK = 'Back'
export const NEXT = 'Next'
export const DONE = 'Done'
export const EXAMPLE = 'Example'
export const SEE_MY_PLAN = 'See my plan'

/** Settings' row for the guide (1.16), and the section it sits in. */
export const GUIDE_SECTION = 'Help'
export const GUIDE_ROW = 'Guide'
export const GUIDE_ROW_SUPPORTING = 'See how Grit works'
/** The empty plan's own words (PlanScreen's CHOOSE_A_TEMPLATE and its Build my plan button); a unit test keeps them equal. */
export const CHOOSE_A_TEMPLATE = 'Choose a template'
export const BUILD_MY_PLAN = 'Build my plan'

type Side = 'POWERLIFTING' | 'BODYBUILDING'
type Exit = 'skipped' | 'done' | 'choose-template' | 'build-plan' | 'see-plan'

/** Screen 2's buttons on a first launch: each side by its name. */
export const sideName = (mode: Side) => (mode === 'BODYBUILDING' ? 'Bodybuilding' : 'Powerlifting')

/**
 * Screen 8's buttons (Task 3): with no plan on the side on, Choose a template, and on bodybuilding Build my plan below
 * it; with a plan, Done and See my plan.
 */
export function lastButtons(mode: Side, hasPlan: boolean): { primary: [string, Exit]; secondary: [string, Exit] | null } {
  if (hasPlan) return { primary: [DONE, 'done'], secondary: [SEE_MY_PLAN, 'see-plan'] }
  if (mode === 'BODYBUILDING') return { primary: [CHOOSE_A_TEMPLATE, 'choose-template'], secondary: [BUILD_MY_PLAN, 'build-plan'] }
  return { primary: [CHOOSE_A_TEMPLATE, 'choose-template'], secondary: null }
}

/** The guide's name, as a region of the page. */
export const GUIDE_LABEL = 'Guide'

/** What the row of dots says: the one screen shown of how many. */
export const dotsLabel = (index: number, count: number) => `Screen ${index + 1} of ${count}`

/** What a screen reader hears for a picture, the one thing of it in the accessibility tree. Only built pictures have one. */
const PICTURE_DESCRIPTIONS: Partial<Record<string, string>> = {
  // The same words as Android's (GuidePictures.kt).
  WELCOME: 'The Grit mark, then the colours of its two sides, powerlifting and bodybuilding.',
  SIDE: 'The hammer of the powerlifting side and the pump of the bodybuilding side, side by side.',
  PLAN: 'Example of the Plan screen: a 3-day template is chosen, its weekdays are picked, one exercise is swapped, then the week is saved.',
  TARGETS: "Example of the coach's target for one lift, session after session.",
  HOLDS_BACK: 'Example of the Coach screen: its cards for when it holds back, one after another.',
  MONTHS_LATER: 'Example of the Progress screen months later: the weeks fill in and the bests climb.',
  YOUR_TURN: 'The Plan screen as it opens on a new plan, ready for a template.',
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
 * The phone's height, the same for both sides: tall enough for screen 4's day (`guideSampleTodayItems`) on either side,
 * so its last step shows the ticked set and the "Skipped today" line at the foot of the list, with a line to spare.
 */
export const PHONE_HEIGHT = 920
