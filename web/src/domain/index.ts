import { GritDomain } from './generated/powertracker-domain.mjs'

export {
  JsAccessoryOption,
  JsAdvice,
  JsBaseline,
  JsCoachSettings,
  JsExercise,
  JsHistory,
  JsPlanItem,
  JsPr,
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
