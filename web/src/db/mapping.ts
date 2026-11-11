import {
  JsAdvice,
  JsBaseline,
  JsExercise,
  JsPlanItem,
  JsSession,
  JsSet,
} from '../domain'
import type {
  AdviceRow,
  BaselineRow,
  ExerciseRow,
  PlanItemRow,
  SessionRow,
  SetRow,
} from './schema'

export function exerciseToJs(r: ExerciseRow & { id: number }): JsExercise {
  return new JsExercise(r.id, r.name, r.category, r.parentLift, [...r.muscles], r.isBuiltIn, r.archived)
}

export function exerciseFromJs(e: JsExercise): ExerciseRow & { id: number } {
  return {
    id: e.id,
    name: e.name,
    category: e.category,
    parentLift: e.parentLift ?? null,
    muscles: [...e.muscles],
    isBuiltIn: e.isBuiltIn,
    archived: e.archived,
  }
}

export function setToJs(r: SetRow): JsSet {
  return new JsSet(r.exerciseId, r.weightKg, r.reps, r.rpe, r.failed, r.missPoint, r.isTopSet)
}

export function setFromJs(s: JsSet, sessionId: number): SetRow {
  return {
    sessionId,
    exerciseId: s.exerciseId,
    weightKg: s.weightKg,
    reps: s.reps,
    rpe: s.rpe ?? null,
    failed: s.failed,
    missPoint: s.missPoint ?? null,
    isTopSet: s.isTopSet,
  }
}

export function sessionToJs(r: SessionRow & { id: number }, sets: SetRow[]): JsSession {
  return new JsSession(r.id, r.date, r.bodyweightKg, r.readiness, sets.map(setToJs))
}

export function sessionFromJs(s: JsSession): SessionRow & { id: number } {
  return { id: s.id, date: s.date, bodyweightKg: s.bodyweightKg ?? null, readiness: s.readiness ?? null }
}

export function planItemToJs(r: PlanItemRow & { id: number }): JsPlanItem {
  return new JsPlanItem(r.id, r.weekday, r.exerciseId, r.sets, r.reps, r.targetRpe, r.order)
}

export function planItemFromJs(p: JsPlanItem): PlanItemRow & { id: number } {
  return {
    id: p.id,
    weekday: p.weekday,
    exerciseId: p.exerciseId,
    sets: p.sets,
    reps: p.reps,
    targetRpe: p.targetRpe,
    order: p.order,
  }
}

export function baselineToJs(r: BaselineRow): JsBaseline {
  return new JsBaseline(r.exerciseId, r.e1rmKg, r.date)
}

export function baselineFromJs(b: JsBaseline): BaselineRow {
  return { exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: b.date }
}

export function adviceToJs(r: AdviceRow & { id: number }): JsAdvice {
  return new JsAdvice(r.id, r.type, r.lift, r.createdAt, r.reason, r.severity, r.status, r.payloadJson)
}

export function adviceFromJs(a: JsAdvice): AdviceRow & { id: number } {
  return {
    id: a.id,
    type: a.type,
    lift: a.lift ?? null,
    createdAt: a.createdAt,
    reason: a.reason,
    severity: a.severity,
    status: a.status,
    payloadJson: a.payloadJson,
  }
}
