import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { planFor, resolveCatalogue } from '../db/catalogue'
import { exerciseToJs } from '../db/mapping'
import { sideOf, type AdviceRow, type ExerciseRow, type PlanItemRow, type TrainingMode } from '../db/schema'
import { grit, type JsExercise, type JsHistory } from '../domain'
import { LIFTS, type Lift } from '../router'
import type { MissPoint } from './useToday'
import { dayName } from './weekdayNames'

/** An accessory and its dose, as the coach prescribes it. */
export type Prescription = { exerciseName: string; sets: number; repsLow: number; repsHigh: number }

/** One lift as a share of another, against the typical range for that pair. */
export type RatioUi = { numerator: Lift; denominator: Lift; valuePct: number; lowPct: number; highPct: number }

/** The figures a card shows under its reason; one shape per advice type. */
export type CardDetail =
  /** `trend` is the best weekly e1RM for up to the last six trained weeks, oldest first. */
  | { kind: 'stall'; trend: number[]; e1rmKg: number | null; prescriptions: Prescription[] }
  | { kind: 'deload'; volumePct: number; loadPct: number }
  | { kind: 'reEntry'; loadPct: number }
  /** `additions` is what Apply would add to the plan; `muscles` goes with `missPoint`. */
  | { kind: 'weakPoint'; ratio: RatioUi | null; missPoint: MissPoint | null; muscles: string[]; additions: Prescription[] }

export type CoachAdvice = { id: number; type: string; lift: Lift | null; createdAt: string; reason: string }

export type CoachCard = { advice: CoachAdvice; detail: CardDetail }

/** A one-line confirmation for a snackbar; `id` changes with every message, also when the text repeats. */
export type CoachMessage = { id: number; text: string }

export type CoachState = { loading: boolean; cards: CoachCard[]; message: CoachMessage | null }

export type CoachActions = {
  /** Carries out the advice: accessories go into the plan; a deload or re-entry only needs to be accepted. */
  apply: (adviceId: number) => Promise<void>
  dismiss: (adviceId: number) => Promise<void>
  clearMessage: () => void
}

const FEED_ORDER = ['DELOAD', 'RE_ENTRY', 'STALL', 'WEAK_POINT']
const TREND_WEEKS = 6
const WEAK_POINT_ADDITIONS = 2
const E1RM_DAYS = 28
const UPDATE_FAILED = "Couldn't update — try again"
const MISS_POINTS: readonly MissPoint[] = ['FLOOR', 'MID', 'LOCKOUT']

type Payload =
  | { kind: 'accessories'; items: Prescription[] }
  | { kind: 'deload'; volumeFactor: number; loadFactor: number }
  | { kind: 'reEntry'; loadFactor: number }
  | { kind: 'weakPoint'; lift: Lift; missPoint: MissPoint | null }

/** Any main lift, strict press included: its advice shows whatever the Progress setting says. */
const asLift = (value: unknown): Lift | null => (LIFTS.includes(value as Lift) ? (value as Lift) : null)

/** Reads the Android `AdvicePayload` JSON through the domain, which fills in Android's defaults. Null for a target or anything unreadable. */
function parsePayload(payloadJson: string): Payload | null {
  const p = grit.advicePayload(payloadJson)
  switch (p?.kind) {
    case 'Accessories':
      return { kind: 'accessories', items: p.items.map((i) => ({ exerciseName: i.exerciseName, sets: i.sets, repsLow: i.repsLow, repsHigh: i.repsHigh })) }
    case 'Deload':
      return p.volumeFactor != null && p.loadFactor != null ? { kind: 'deload', volumeFactor: p.volumeFactor, loadFactor: p.loadFactor } : null
    case 'ReEntry':
      return p.loadFactor != null ? { kind: 'reEntry', loadFactor: p.loadFactor } : null
    case 'WeakPoint': {
      const lift = asLift(p.lift)
      const missPoint = MISS_POINTS.includes(p.missPoint as MissPoint) ? (p.missPoint as MissPoint) : null
      return lift ? { kind: 'weakPoint', lift, missPoint } : null
    }
    default:
      return null
  }
}

type StoredAdvice = AdviceRow & { id: number }
type LibraryExercise = JsExercise
type StoredPlanItem = PlanItemRow & { id: number }

type Stored = { today: string; open: StoredAdvice[]; history: JsHistory; plan: StoredPlanItem[] }

/** The side whose plan the Coach screen reads. Only the powerlifting side's screens exist so far. */
const SIDE: TrainingMode = 'POWERLIFTING'

async function readStored(db: GritDb, today: string): Promise<Stored> {
  const [open, history, plan] = await Promise.all([db.advice.where('status').equals('OPEN').toArray(), loadHistory(db), planFor(db, SIDE)])
  return { today, open: open as StoredAdvice[], history, plan: plan as StoredPlanItem[] }
}

/** An OPEN deload or re-entry stops counting after two weeks; the domain decides when. */
const expired = (a: AdviceRow, today: string) => grit.openAdviceExpired(a.type, a.createdAt, today)

const percent = (factor: number) => Math.round(factor * 100)

const findExercise = (exercises: LibraryExercise[], name: string) => exercises.find((e) => !e.archived && e.name.toLowerCase() === name.toLowerCase())

/**
 * The ratio that explains why `lift` was flagged: the first of its pairs that is out of range on the
 * side that makes `lift` the weak one. Unless `flaggedOnly`, falls back to the lift's first pair with
 * figures, so a ratio flag always has its bar.
 */
function ratioFor(lift: Lift, history: JsHistory, today: string, flaggedOnly: boolean): RatioUi | null {
  const known = grit.ratioRanges().flatMap((pair): RatioUi[] => {
    const numerator = asLift(pair.numerator)
    const denominator = asLift(pair.denominator)
    if (!numerator || !denominator || (lift !== numerator && lift !== denominator)) return []
    const num = grit.currentE1rm(history, numerator, today, E1RM_DAYS)
    const den = grit.currentE1rm(history, denominator, today, E1RM_DAYS)
    if (num == null || den == null || den <= 0) return []
    return [{ numerator, denominator, valuePct: percent(num / den), lowPct: percent(pair.low), highPct: percent(pair.high) }]
  })
  const flagged = known.find((r) => (lift === r.numerator ? r.valuePct < r.lowPct : r.valuePct > r.highPct))
  return flagged ?? (flaggedOnly ? null : (known[0] ?? null))
}

/**
 * The first two options for the weak point that are not in the plan yet, dosed like a stall card's. An option is the
 * lifter's row of that name, else the catalogue entry, whose row Apply adds.
 */
function weakPointAdditions(lift: Lift, missPoint: MissPoint | null, plan: PlanItemRow[], exercises: LibraryExercise[]): Prescription[] {
  const planned = new Set(plan.map((p) => p.exerciseId))
  return grit
    .accessoryOptions(lift, missPoint)
    .flatMap((option) => {
      const exercise = findExercise(exercises, option.name)
      if (exercise) return planned.has(exercise.id) ? [] : [{ name: exercise.name, heavy: option.heavy }]
      const entry = grit.findInLibrary(option.name)
      return entry ? [{ name: entry.name, heavy: option.heavy }] : []
    })
    .slice(0, WEAK_POINT_ADDITIONS)
    .map(({ name, heavy }) => (heavy ? { exerciseName: name, sets: 3, repsLow: 3, repsHigh: 5 } : { exerciseName: name, sets: 3, repsLow: 8, repsHigh: 10 }))
}

function detailFor(lift: Lift | null, payload: Payload, d: Stored): CardDetail {
  switch (payload.kind) {
    case 'accessories': {
      const trend = lift ? grit.weeklyTrend(d.history, lift, d.today).slice(-TREND_WEEKS).map((p) => p.value) : []
      const current = (lift ? grit.currentE1rm(d.history, lift, d.today, E1RM_DAYS) : null) ?? trend[trend.length - 1] ?? null
      return { kind: 'stall', trend, e1rmKg: current, prescriptions: payload.items }
    }
    case 'deload':
      return { kind: 'deload', volumePct: percent(payload.volumeFactor), loadPct: percent(payload.loadFactor) }
    case 'reEntry':
      return { kind: 'reEntry', loadPct: percent(payload.loadFactor) }
    case 'weakPoint':
      return {
        kind: 'weakPoint',
        ratio: ratioFor(payload.lift, d.history, d.today, payload.missPoint != null),
        missPoint: payload.missPoint,
        muscles: [...grit.accessoryMuscles(payload.lift, payload.missPoint)],
        additions: weakPointAdditions(payload.lift, payload.missPoint, d.plan, d.history.exercises),
      }
  }
}

/** The feed: most urgent kind first, then newest. Targets belong to Today and are left out. */
function build(d: Stored, busy: ReadonlySet<number>): CoachCard[] {
  return d.open
    .filter((a) => FEED_ORDER.includes(a.type) && !busy.has(a.id) && !expired(a, d.today))
    .sort((a, b) => FEED_ORDER.indexOf(a.type) - FEED_ORDER.indexOf(b.type) || b.createdAt.localeCompare(a.createdAt) || b.id - a.id)
    .flatMap((a): CoachCard[] => {
      const payload = parsePayload(a.payloadJson)
      if (!payload) return []
      const lift = asLift(a.lift)
      return [{ advice: { id: a.id, type: a.type, lift, createdAt: a.createdAt, reason: a.reason }, detail: detailFor(lift, payload, d) }]
    })
}

/** The earliest weekday that already trains the lift; else the first training day; else Monday. */
function dayFor(lift: Lift | null, plan: PlanItemRow[], exercises: LibraryExercise[], settings: AppSettings): number {
  const mainIds = new Set(exercises.filter((e) => grit.isMainLift(e) && grit.mainLiftOf(e) === lift).map((e) => e.id))
  const days = plan.filter((p) => mainIds.has(p.exerciseId)).map((p) => p.weekday)
  if (days.length > 0) return Math.min(...days)
  return settings.trainingDays.length > 0 ? Math.min(...settings.trainingDays) : 1
}

/**
 * Adds the prescriptions to the day that trains `lift`, skipping any already there and any name that is neither a row
 * nor in the catalogue. A catalogue exercise without a row gets one (`resolveCatalogue`), in the caller's transaction,
 * so it lands with the plan items or not at all. Returns what to tell the lifter.
 */
async function addToPlan(db: GritDb, lift: Lift | null, prescriptions: Prescription[], plan: PlanItemRow[], exercises: LibraryExercise[], mode: TrainingMode): Promise<string> {
  const settings = await getSettings(db)
  const day = dayFor(lift, plan, exercises, settings)
  const onDay = plan.filter((p) => p.weekday === day)
  const taken = new Set(onDay.map((p) => p.exerciseId))
  let order = Math.max(-1, ...onDay.map((p) => p.order)) + 1
  let added = 0
  for (const p of prescriptions) {
    const exerciseId = findExercise(exercises, p.exerciseName)?.id ?? (grit.findInLibrary(p.exerciseName) ? await resolveCatalogue(db, p.exerciseName) : null)
    if (exerciseId == null || taken.has(exerciseId)) continue
    taken.add(exerciseId)
    await db.planItems.add({ weekday: day, exerciseId, sets: p.sets, reps: p.repsHigh, targetRpe: settings.defaultRpe, order: order++, mode })
    added++
  }
  const name = dayName(day)
  if (added === 0) return `Already in your plan on ${name}`
  return added === 1 ? `Added 1 exercise to ${name}` : `Added ${added} exercises to ${name}`
}

/**
 * Applies one piece of open advice and marks it APPLIED, in one transaction: if any write fails, the
 * plan is left as it was. Returns the confirmation, or null when there was nothing to apply.
 */
function applyAdvice(db: GritDb, adviceId: number): Promise<string | null> {
  return db.transaction('rw', [db.advice, db.planItems, db.exercises, db.settings], async () => {
    const target = await db.advice.get(adviceId)
    const payload = target?.status === 'OPEN' && target.type !== 'TARGET' ? parsePayload(target.payloadJson) : null
    if (!target || !payload) return null
    let message: string
    if (payload.kind === 'deload') message = 'Deload week on — targets lightened for 7 days'
    else if (payload.kind === 'reEntry') message = 'Easing back in for 7 days'
    else {
      // Added to the plan of the side the advice is for.
      const side = sideOf(target)
      const [plan, rows] = await Promise.all([planFor(db, side), db.exercises.toArray()])
      const exercises = rows.map((row) => exerciseToJs(row as ExerciseRow & { id: number }))
      message =
        payload.kind === 'accessories'
          ? await addToPlan(db, asLift(target.lift), payload.items, plan, exercises, side)
          : await addToPlan(db, payload.lift, weakPointAdditions(payload.lift, payload.missPoint, plan, exercises), plan, exercises, side)
    }
    await db.advice.update(adviceId, { status: 'APPLIED' })
    return message
  })
}

/**
 * The coach's open advice, most urgent first, read live from the database. `today` is the real clock.
 * Nothing is changed until `apply` or `dismiss` is called for a card.
 */
export function useCoach(db: GritDb, today: () => string): CoachState & CoachActions {
  const clock = useRef(today)
  clock.current = today
  const [stored, setStored] = useState<Stored>()
  const [readError, setReadError] = useState<unknown>(null)
  useEffect(() => {
    const subscription = liveQuery(() => readStored(db, clock.current())).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  // A failed read is for the app's error boundary, not for an empty feed that looks like no advice.
  if (readError != null) throw readError

  // Advice being applied or dismissed. Its card leaves the feed at once, so it cannot be tapped twice
  // and does not redraw while its accessories land in the plan; it comes back if the work fails.
  const busy = useRef<Set<number>>(new Set())
  const [hidden, setHidden] = useState<ReadonlySet<number>>(busy.current)
  const [message, setMessage] = useState<CoachMessage | null>(null)
  const messageCount = useRef(0)
  const say = useCallback((text: string) => setMessage({ id: ++messageCount.current, text }), [])

  const once = useCallback(
    async (adviceId: number, work: () => Promise<string | null>) => {
      if (busy.current.has(adviceId)) return
      busy.current.add(adviceId)
      setHidden(new Set(busy.current))
      try {
        const done = await work()
        // Kept hidden on success: the stored status reaches the feed a moment after this returns.
        if (done != null) say(done)
      } catch {
        // A failed write must not lose the advice: the card comes back and the lifter can try again.
        busy.current.delete(adviceId)
        setHidden(new Set(busy.current))
        say(UPDATE_FAILED)
      }
    },
    [say],
  )

  const apply = useCallback((adviceId: number) => once(adviceId, () => applyAdvice(db, adviceId)), [db, once])
  const dismiss = useCallback(
    (adviceId: number) =>
      once(adviceId, async () => {
        await db.advice.update(adviceId, { status: 'DISMISSED' })
        return null
      }),
    [db, once],
  )
  const clearMessage = useCallback(() => setMessage(null), [])

  const cards = useMemo(() => (stored ? build(stored, hidden) : []), [stored, hidden])
  return { loading: stored == null, cards, message, apply, dismiss, clearMessage }
}
