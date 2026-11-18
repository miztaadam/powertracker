import { liveQuery } from 'dexie'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getSettings, loadHistory, type AppSettings, type GritDb } from '../db/db'
import { adviceForSide, historyOn } from './sides'
import { applyPlanChange, planFor, resolveCatalogue } from '../db/catalogue'
import { checkInToJs, loadCheckIns } from '../db/checkIns'
import { exerciseToJs, planItemToJs } from '../db/mapping'
import { sideOf, type AdviceRow, type ExerciseRow, type PlanItemRow, type TrainingMode } from '../db/schema'
import { grit, type JsExercise, type JsHistory } from '../domain'
import { haptic } from '../platform/haptics'
import { LIFTS, type Lift } from '../router'
import type { MissPoint } from './useToday'
import { dayName } from './weekdayNames'
import { withTransition } from '../ui/viewTransition'

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
  /**
   * A lagging body part (bodybuilding side): `group` is its `MuscleGroup` name. Either sets added to planned exercises
   * or one new exercise; its weekly sets before and after.
   */
  | { kind: 'moreWork'; group: string; addSets: SetAdditionUi[]; newExercise: NewExerciseUi | null; weeklyBefore: number; weeklyAfter: number }
  /** An exercise stuck for three weeks or more (bodybuilding side): what to swap it for, best first. */
  | { kind: 'swap'; exerciseId: number; name: string; group: string; since: string; weeks: number; alternatives: string[] }

/** One more set of a planned exercise on one day. */
export type SetAdditionUi = { weekday: number; name: string; fromSets: number; toSets: number }

/** A new exercise for a day, with its prescription. */
export type NewExerciseUi = { weekday: number; name: string; sets: number; repsLow: number; repsHigh: number; rpe: number }

export type CoachAdvice = { id: number; type: string; lift: Lift | null; createdAt: string; reason: string }

export type CoachCard = { advice: CoachAdvice; detail: CardDetail }

/** A one-line confirmation for a snackbar; `id` changes with every message, also when the text repeats. */
export type CoachMessage = { id: number; text: string }

export type CoachState = {
  loading: boolean
  cards: CoachCard[]
  message: CoachMessage | null
  /** The side whose feed this is: the side the app is on. Left out, the powerlifting side. */
  side?: TrainingMode
  /** A check-in is due (bodybuilding side only): the card at the top of the feed. */
  checkInDue?: boolean
}

export type CoachActions = {
  /**
   * Carries out the advice: accessories go into the plan; a deload or re-entry only needs to be accepted. More work
   * changes the plan as it says; a swap needs `choice`, one of its alternatives.
   */
  apply: (adviceId: number, choice?: string) => Promise<void>
  dismiss: (adviceId: number) => Promise<void>
  clearMessage: () => void
}

const FEED_ORDER = ['DELOAD', 'RE_ENTRY', 'STALL', 'WEAK_POINT', 'MORE_WORK', 'SWAP']
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
  | { kind: 'moreWork'; group: string; addSets: SetAdditionUi[]; newExercise: NewExerciseUi | null; weeklyBefore: number; weeklyAfter: number }
  | { kind: 'swap'; exerciseId: number; name: string; group: string; since: string; alternatives: string[] }

/** A payload weekday as 1 = Monday .. 7 = Sunday: stored payloads hold the name ("MONDAY"); a number passes through. */
const WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const weekdayOf = (day: number | string): number => (typeof day === 'number' ? day : WEEKDAYS.indexOf(day) + 1)

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
    case 'MoreWork': {
      if (p.group == null || p.weeklyBefore == null || p.weeklyAfter == null) return null
      const n = p.newExercise
      return {
        kind: 'moreWork',
        group: p.group,
        addSets: p.addSets.map((a) => ({ weekday: weekdayOf(a.weekday), name: a.name, fromSets: a.fromSets, toSets: a.toSets })),
        newExercise: n ? { weekday: weekdayOf(n.weekday), name: n.name, sets: n.sets, repsLow: n.repsLow, repsHigh: n.repsHigh, rpe: n.rpe } : null,
        weeklyBefore: p.weeklyBefore,
        weeklyAfter: p.weeklyAfter,
      }
    }
    case 'Swap':
      return p.exerciseId != null && p.name != null && p.group != null && p.since != null
        ? { kind: 'swap', exerciseId: p.exerciseId, name: p.name, group: p.group, since: p.since, alternatives: [...p.alternatives] }
        : null
    default:
      return null
  }
}

type StoredAdvice = AdviceRow & { id: number }
type LibraryExercise = JsExercise
type StoredPlanItem = PlanItemRow & { id: number }

type Stored = { today: string; side: TrainingMode; open: StoredAdvice[]; history: JsHistory; plan: StoredPlanItem[]; checkInDue: boolean }

/**
 * The feed of the side the app is on: its own advice, and every deload and easing back in whatever side it was
 * stored for (`adviceForSide`). The cards' figures (trends, ratios) read that side's sessions.
 */
async function readStored(db: GritDb, today: string): Promise<Stored> {
  const settings = await getSettings(db)
  const side = settings.trainingMode
  const [open, history, plan, checkIns] = await Promise.all([db.advice.where('status').equals('OPEN').toArray(), loadHistory(db), planFor(db, side), loadCheckIns(db)])
  // Check-ins belong to the bodybuilding side only.
  const checkInDue = side === 'BODYBUILDING' && grit.coachingCheckInDue(history, plan.map(planItemToJs), checkIns.map(checkInToJs), today)
  return { today, side, open: adviceForSide(open as StoredAdvice[], side), history: historyOn(history, side), plan: plan as StoredPlanItem[], checkInDue }
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

function detailFor(lift: Lift | null, payload: Payload, d: Stored, createdAt: string): CardDetail {
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
    case 'moreWork':
      return payload
    case 'swap':
      return { ...payload, weeks: weeksSince(payload.since, createdAt) }
  }
}

/** Whole weeks from `since` to `until` (ISO dates), as the coach counts them in a swap's reason. */
function weeksSince(since: string, until: string): number {
  const days = (Date.parse(`${until}T00:00:00Z`) - Date.parse(`${since}T00:00:00Z`)) / 86_400_000
  return Math.max(0, Math.floor(days / 7))
}

/** The feed: most urgent kind first, then newest. Targets belong to Today and are left out. */
function build(d: Stored, busy: ReadonlySet<number>): CoachCard[] {
  return d.open
    .filter((a) => FEED_ORDER.includes(a.type) && !busy.has(a.id) && !expired(a, d.today))
    .sort((a, b) => FEED_ORDER.indexOf(a.type) - FEED_ORDER.indexOf(b.type) || b.createdAt.localeCompare(a.createdAt) || b.id - a.id)
    .flatMap((a): CoachCard[] => {
      const payload = parsePayload(a.payloadJson)
      if (!payload) return []
      // A swap for an exercise no longer in the plan has nothing to swap.
      if (payload.kind === 'swap' && !d.plan.some((p) => p.exerciseId === payload.exerciseId)) return []
      const lift = asLift(a.lift)
      return [{ advice: { id: a.id, type: a.type, lift, createdAt: a.createdAt, reason: a.reason }, detail: detailFor(lift, payload, d, a.createdAt) }]
    })
}

/** The earliest weekday that already trains the lift; else the first training day of `mode`'s side; else Monday. */
function dayFor(lift: Lift | null, plan: PlanItemRow[], exercises: LibraryExercise[], settings: AppSettings, mode: TrainingMode): number {
  const mainIds = new Set(exercises.filter((e) => grit.isMainLift(e) && grit.mainLiftOf(e) === lift).map((e) => e.id))
  const days = plan.filter((p) => mainIds.has(p.exerciseId)).map((p) => p.weekday)
  if (days.length > 0) return Math.min(...days)
  const trainingDays = mode === 'BODYBUILDING' ? settings.bodybuildingTrainingDays : settings.trainingDays
  return trainingDays.length > 0 ? Math.min(...trainingDays) : 1
}

/**
 * Adds the prescriptions to the day that trains `lift`, skipping any already there and any name that is neither a row
 * nor in the catalogue. A catalogue exercise without a row gets one (`resolveCatalogue`), in the caller's transaction,
 * so it lands with the plan items or not at all. Returns what to tell the lifter.
 */
async function addToPlan(db: GritDb, lift: Lift | null, prescriptions: Prescription[], plan: PlanItemRow[], exercises: LibraryExercise[], mode: TrainingMode): Promise<string> {
  const settings = await getSettings(db)
  const day = dayFor(lift, plan, exercises, settings, mode)
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
/** What applying did: the words to show, and whether the advice is done (a swap that could not be made stays open). */
type Applied = { text: string; done: boolean }

function applyAdvice(db: GritDb, adviceId: number, choice: string | undefined): Promise<Applied | null> {
  return db.transaction('rw', [db.advice, db.planItems, db.exercises, db.skips, db.settings], async () => {
    const target = await db.advice.get(adviceId)
    const payload = target?.status === 'OPEN' && target.type !== 'TARGET' ? parsePayload(target.payloadJson) : null
    if (!target || !payload) return null
    if (payload.kind === 'swap' && (choice == null || !payload.alternatives.includes(choice))) return null
    let message: string
    if (payload.kind === 'deload') message = 'Deload week on — targets lightened for 7 days'
    else if (payload.kind === 'reEntry') message = 'Easing back in for 7 days'
    else if (payload.kind === 'moreWork' || payload.kind === 'swap') {
      // The domain works out what still applies to the bodybuilding plan as it is now; only that is changed, and the
      // advice is done either way (its message says when nothing could be).
      const [plan, rows] = await Promise.all([planFor(db, 'BODYBUILDING'), db.exercises.toArray()])
      const jsPlan = plan.map(planItemToJs)
      const exercises = rows.map((row) => exerciseToJs(row as ExerciseRow & { id: number }))
      const change =
        payload.kind === 'moreWork'
          ? grit.physiqueApplyMoreWork(target.payloadJson, jsPlan, exercises)
          : grit.physiqueApplySwap(target.payloadJson, choice!, jsPlan, exercises)
      // A swap that cannot be made changes nothing and its card stays, so another alternative can be tried. More work
      // is done either way, as the plan says.
      if (payload.kind === 'swap' && !change.changed) return { text: change.message, done: false }
      await applyPlanChange(db, change)
      message = change.message
    } else {
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
    return { text: message, done: true }
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
    async (adviceId: number, work: () => Promise<Applied | null>) => {
      if (busy.current.has(adviceId)) return
      busy.current.add(adviceId)
      // The card slides away and the cards below close up (stage 3).
      withTransition(() => setHidden(new Set(busy.current)))
      try {
        const done = await work()
        // Kept hidden on success: the stored status reaches the feed a moment after this returns.
        if (done != null) say(done.text)
        if (done != null && !done.done) {
          busy.current.delete(adviceId)
          setHidden(new Set(busy.current))
        }
      } catch {
        // A failed write must not lose the advice: the card comes back and the lifter can try again.
        busy.current.delete(adviceId)
        setHidden(new Set(busy.current))
        say(UPDATE_FAILED)
      }
    },
    [say],
  )

  // Apply and "Swap for …" tick once the advice is applied: a tap that applies nothing (a second tap, a swap the plan
  // no longer allows, a failed write) does not buzz. Dismiss does not buzz.
  const apply = useCallback(
    (adviceId: number, choice?: string) =>
      once(adviceId, async () => {
        const applied = await applyAdvice(db, adviceId, choice)
        if (applied?.done === true) haptic('tick')
        return applied
      }),
    [db, once],
  )
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
  return { loading: stored == null, cards, message, side: stored?.side ?? 'POWERLIFTING', checkInDue: stored?.checkInDue ?? false, apply, dismiss, clearMessage }
}
