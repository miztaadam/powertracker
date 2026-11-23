import { sideOf, type AdviceRow, type TrainingMode } from '../db/schema'
import { grit, JsHistory } from '../domain'

/**
 * The advice a side shows (the domain's `Coaching.feedFor`): every deload, whatever side it was stored for, because
 * it belongs to the lifter, not to a side (bodybuilding mode spec §5.2); every easing back in from a break this side
 * was trained before (the domain decides, from `history`, the whole log on both sides; spec point 10); the rest where
 * the side matches. Never filter deloads by side.
 */
export function adviceForSide<T extends Pick<AdviceRow, 'type' | 'mode' | 'createdAt'>>(advice: readonly T[], mode: TrainingMode, history: JsHistory): T[] {
  return advice.filter(
    (a) => a.type === 'DELOAD' || (a.type === 'RE_ENTRY' ? grit.coachingReEntryShows(mode, a.createdAt, history) : sideOf(a) === mode),
  )
}

/** The history with only `mode`'s sessions: the same exercises and baselines (the domain's `TrainingHistory.on`). */
export function historyOn(history: JsHistory, mode: TrainingMode): JsHistory {
  return new JsHistory(
    history.sessions.filter((s) => s.mode === mode),
    history.exercises,
    history.baselines,
  )
}

/** The line Today shows when its session is on the other side from the one the app is on now (spec §5.1). */
export function otherSideLine(sessionSide: TrainingMode): string {
  return sessionSide === 'POWERLIFTING'
    ? "Today's session is on the powerlifting side. Bodybuilding starts with your next session."
    : "Today's session is on the bodybuilding side. Powerlifting starts with your next session."
}
