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
  LoadUnit,
  PlanItemRow,
  PlateCount,
  SessionRow,
  SetRow,
  TrainingMode,
} from './schema'
import { LOAD_UNITS, PLATE_COUNTS, plateCountOf, setUnit, sideOf, TRAINING_MODES } from './schema'

/** The domain's mode name as a row's. */
function mode(name: string): TrainingMode {
  if (!(TRAINING_MODES as readonly string[]).includes(name)) throw new Error(`Unknown training mode ${name}`)
  return name as TrainingMode
}

/** The domain's unit name as a row's. */
function loadUnit(name: string): LoadUnit {
  if (!(LOAD_UNITS as readonly string[]).includes(name)) throw new Error(`Unknown load unit ${name}`)
  return name as LoadUnit
}

/** The domain's plate setting name as a row's. */
function plateCount(name: string): PlateCount {
  if (!(PLATE_COUNTS as readonly string[]).includes(name)) throw new Error(`Unknown plate setting ${name}`)
  return name as PlateCount
}

export function exerciseToJs(r: ExerciseRow & { id: number }): JsExercise {
  return new JsExercise(r.id, r.name, r.category, r.parentLift, [...r.muscles], r.isBuiltIn, r.archived, plateCountOf(r))
}

/** An exercise that is off stores no `plateCount` key, as every row before schema version 6. */
export function exerciseFromJs(e: JsExercise): ExerciseRow & { id: number } {
  const count = plateCount(e.plateCount)
  return {
    id: e.id,
    name: e.name,
    category: e.category,
    parentLift: e.parentLift ?? null,
    muscles: [...e.muscles],
    isBuiltIn: e.isBuiltIn,
    archived: e.archived,
    ...(count !== 'OFF' ? { plateCount: count } : {}),
  }
}

export function setToJs(r: SetRow): JsSet {
  return new JsSet(r.exerciseId, r.weightKg, r.reps, r.rpe, r.failed, r.missPoint, r.isTopSet, setUnit(r))
}

/** A kg set stores no `unit` key, as every row before schema version 6. */
export function setFromJs(s: JsSet, sessionId: number): SetRow {
  const unit = loadUnit(s.unit)
  return {
    sessionId,
    exerciseId: s.exerciseId,
    weightKg: s.weightKg,
    reps: s.reps,
    rpe: s.rpe ?? null,
    failed: s.failed,
    missPoint: s.missPoint ?? null,
    isTopSet: s.isTopSet,
    ...(unit !== 'KG' ? { unit } : {}),
  }
}

export function sessionToJs(r: SessionRow & { id: number }, sets: SetRow[]): JsSession {
  return new JsSession(r.id, r.date, r.bodyweightKg, r.readiness, sets.map(setToJs), sideOf(r))
}

export function sessionFromJs(s: JsSession): SessionRow & { id: number } {
  return { id: s.id, date: s.date, bodyweightKg: s.bodyweightKg ?? null, readiness: s.readiness ?? null, mode: mode(s.mode) }
}

export function planItemToJs(r: PlanItemRow & { id: number }): JsPlanItem {
  return new JsPlanItem(r.id, r.weekday, r.exerciseId, r.sets, r.reps, r.targetRpe, r.order, sideOf(r), r.repsHigh ?? null)
}

/** Fixed reps store no `repsHigh` key, as every row before schema version 3. */
export function planItemFromJs(p: JsPlanItem): PlanItemRow & { id: number } {
  return {
    id: p.id,
    weekday: p.weekday,
    exerciseId: p.exerciseId,
    sets: p.sets,
    reps: p.reps,
    targetRpe: p.targetRpe,
    order: p.order,
    mode: mode(p.mode),
    ...(p.repsHigh != null ? { repsHigh: p.repsHigh } : {}),
  }
}

export function baselineToJs(r: BaselineRow): JsBaseline {
  return new JsBaseline(r.exerciseId, r.e1rmKg, r.date)
}

export function baselineFromJs(b: JsBaseline): BaselineRow {
  return { exerciseId: b.exerciseId, e1rmKg: b.e1rmKg, date: b.date }
}

export function adviceToJs(r: AdviceRow & { id: number }): JsAdvice {
  return new JsAdvice(r.id, r.type, r.lift, r.createdAt, r.reason, r.severity, r.status, r.payloadJson, sideOf(r))
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
    mode: mode(a.mode),
  }
}
