import { GritDomain } from './generated/powertracker-domain.mjs'

export {
  JsAccessoryOption,
  JsAdvice,
  JsBaseline,
  JsBuilderPick,
  JsCheckIn,
  JsCheckInOutcome,
  JsCoachSettings,
  JsDraftExercise,
  JsDraftSession,
  JsExercise,
  JsFormGuide,
  JsGuideScreen,
  JsHistory,
  JsLibraryExercise,
  JsMuscleFocus,
  JsMuscleRow,
  JsPlanDraft,
  JsPlanChange,
  JsPlanItem,
  JsPr,
  JsRatioRange,
  JsSession,
  JsSet,
  JsSkip,
  JsWeeklyPoint,
} from './generated/powertracker-domain.mjs'

/** The shared Kotlin domain (coach, load maths, progress). Plain data in, plain data out. */
export const grit = GritDomain.getInstance()

/** Where an exercise's target muscle is stretched and where it is squeezed (stage 4), or the one line saying there is none. */
export type MuscleFocus = { kind: 'lines'; stretch: string; squeeze: string } | { kind: 'none'; line: string }

/**
 * The stretch and squeeze lines for the exercise `name`, matched like its guide (a catalogue name or alias, ignoring
 * case). Null only for a name outside the catalogue (the lifter's own exercise): every catalogue exercise has lines.
 */
export function muscleFocus(name: string): MuscleFocus | null {
  const focus = grit.muscleFocus(name)
  if (focus == null) return null
  if (focus.none != null) return { kind: 'none', line: focus.none }
  if (focus.stretch == null || focus.squeeze == null) return null
  return { kind: 'lines', stretch: focus.stretch, squeeze: focus.squeeze }
}

/** The device's local calendar date as `YYYY-MM-DD` (never the UTC date). */
export function todayIso(): string {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}
