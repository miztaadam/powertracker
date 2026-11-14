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
  JsHistory,
  JsLibraryExercise,
  JsMuscleRow,
  JsPlanDraft,
  JsPlanChange,
  JsPlanItem,
  JsPr,
  JsRatioRange,
  JsSession,
  JsSet,
  JsWeeklyPoint,
} from './generated/powertracker-domain.mjs'

/** The shared Kotlin domain (coach, load maths, progress). Plain data in, plain data out. */
export const grit = GritDomain.getInstance()

/** The device's local calendar date as `YYYY-MM-DD` (never the UTC date). */
export function todayIso(): string {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}
