import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { exerciseToJs } from '../db/mapping'
import type { AdviceRow, ExerciseRow, PlanItemRow } from '../db/schema'
import { grit, type JsExercise, type JsHistory } from '../domain'
import type { Lift } from '../router'
import type { MissPoint } from './useToday'

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
/** An OPEN deload or re-entry stops counting after this many days (the domain's `Coach.OPEN_ACTIVE_DAYS`). */
const OPEN_ACTIVE_DAYS = 14
const UPDATE_FAILED = "Couldn't update — try again"
const LIFTS: readonly Lift[] = ['SQUAT', 'BENCH', 'DEADLIFT']
const MISS_POINTS: readonly MissPoint[] = ['FLOOR', 'MID', 'LOCKOUT']

type Payload =
  | { kind: 'accessories'; items: Prescription[] }
  | { kind: 'deload'; volumeFactor: number; loadFactor: number }
  | { kind: 'reEntry'; loadFactor: number }
  | { kind: 'weakPoint'; lift: Lift; missPoint: MissPoint | null }

const asLift = (value: unknown): Lift | null => (LIFTS.includes(value as Lift) ? (value as Lift) : null)
const numberOr = (value: unknown, fallback: number) => (typeof value === 'number' ? value : fallback)

function isPrescription(value: unknown): value is Prescription {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return typeof v.exerciseName === 'string' && typeof v.sets === 'number' && typeof v.repsLow === 'number' && typeof v.repsHigh === 'number'
}

/** Reads the Android `AdvicePayload` JSON; fields Android leaves out carry its defaults. Null for a target or anything unreadable. */
function parsePayload(payloadJson: string): Payload | null {
  try {
    const p = JSON.parse(payloadJson) as Record<string, unknown>
    switch (typeof p.type === 'string' ? p.type.slice(p.type.lastIndexOf('.') + 1) : '') {
      case 'Accessories':
        return { kind: 'accessories', items: Array.isArray(p.items) ? p.items.filter(isPrescription) : [] }
      case 'Deload':
        return { kind: 'deload', volumeFactor: numberOr(p.volumeFactor, 0.6), loadFactor: numberOr(p.loadFactor, 0.9) }
      case 'ReEntry':
        return { kind: 'reEntry', loadFactor: numberOr(p.loadFactor, 0.85) }
      case 'WeakPoint': {
        const lift = asLift(p.lift)
        const missPoint = MISS_POINTS.includes(p.missPoint as MissPoint) ? (p.missPoint as MissPoint) : null
        return lift ? { kind: 'weakPoint', lift, missPoint } : null
      }
      default:
        return null
    }
  } catch {
    return null
  }
}

type StoredAdvice = AdviceRow & { id: number }
type LibraryExercise = JsExercise
type StoredPlanItem = PlanItemRow & { id: number }

type Stored = { today: string; open: StoredAdvice[]; history: JsHistory; plan: StoredPlanItem[] }

async function readStored(db: GritDb, today: string): Promise<Stored> {
  const [open, history, plan] = await Promise.all([db.advice.where('status').equals('OPEN').toArray(), loadHistory(db), db.planItems.toArray()])
  return { today, open: open as StoredAdvice[], history, plan: plan as StoredPlanItem[] }
}

/** The weekday's full name in the device's language, as Android shows it. 1 = Monday. */
function dayName(weekday: number): string {
  // 5 January 2026 is a Monday.
  return new Intl.DateTimeFormat(undefined, { weekday: 'long', timeZone: 'UTC' }).format(new Date(Date.UTC(2026, 0, 4 + weekday)))
}

function minusDays(iso: string, days: number): string {
  const d = new Date(`${iso}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() - days)
  return d.toISOString().slice(0, 10)
}

const expired = (a: AdviceRow, today: string) => (a.type === 'DELOAD' || a.type === 'RE_ENTRY') && a.createdAt < minusDays(today, OPEN_ACTIVE_DAYS - 1)

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

/** The first two options for the weak point that are in the library and not in the plan yet, dosed like a stall card's. */
function weakPointAdditions(lift: Lift, missPoint: MissPoint | null, plan: PlanItemRow[], exercises: LibraryExercise[]): Prescription[] {
  const planned = new Set(plan.map((p) => p.exerciseId))
  return grit
    .accessoryOptions(lift, missPoint)
    .flatMap((option) => {
      const exercise = findExercise(exercises, option.name)
      return exercise && !planned.has(exercise.id) ? [{ name: exercise.name, heavy: option.heavy }] : []
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

/** Adds the prescriptions to the day that trains `lift`, skipping any already there. Returns what to tell the lifter. */
async function addToPlan(db: GritDb, lift: Lift | null, prescriptions: Prescription[], plan: PlanItemRow[], exercises: LibraryExercise[]): Promise<string> {
  const settings = await getSettings(db)
  const day = dayFor(lift, plan, exercises, settings)
  const onDay = plan.filter((p) => p.weekday === day)
  const taken = new Set(onDay.map((p) => p.exerciseId))
  let order = Math.max(-1, ...onDay.map((p) => p.order)) + 1
  let added = 0
  for (const p of prescriptions) {
    const exercise = findExercise(exercises, p.exerciseName)
    if (!exercise || taken.has(exercise.id)) continue
    taken.add(exercise.id)
    await db.planItems.add({ weekday: day, exerciseId: exercise.id, sets: p.sets, reps: p.repsHigh, targetRpe: settings.defaultRpe, order: order++ })
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
      const [plan, rows] = await Promise.all([db.planItems.toArray(), db.exercises.toArray()])
      const exercises = rows.map((row) => exerciseToJs(row as ExerciseRow & { id: number }))
      message =
        payload.kind === 'accessories'
          ? await addToPlan(db, asLift(target.lift), payload.items, plan, exercises)
          : await addToPlan(db, payload.lift, weakPointAdditions(payload.lift, payload.missPoint, plan, exercises), plan, exercises)
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
