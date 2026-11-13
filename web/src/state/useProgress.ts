import { liveQuery } from 'dexie'
import { useEffect, useMemo, useRef, useState } from 'react'
import { planFor } from '../db/catalogue'
import { getSettings, loadHistory, type GritDb } from '../db/db'
import { planItemToJs } from '../db/mapping'
import type { PlanItemRow, TrainingMode } from '../db/schema'
import { grit, type JsHistory, type JsPr } from '../domain'
import { historyOn } from './sides'
import { LIFTS, type Lift } from '../router'
import { addDays, weekStart } from './isoDate'
import type { RatioUi } from './useCoach'

/** How far back the charts look. */
export type ChartRange = '8w' | '6m' | '1y' | 'all'

export const CHART_RANGES: readonly ChartRange[] = ['8w', '6m', '1y', 'all']

export const DEFAULT_RANGE: ChartRange = '6m'

/** Weeks each range covers; null for everything. */
const RANGE_WEEKS: Record<ChartRange, number | null> = { '8w': 8, '6m': 26, '1y': 52, all: null }

type SbdLift = Exclude<Lift, 'STRICT_PRESS'>

const SBD: readonly SbdLift[] = ['SQUAT', 'BENCH', 'DEADLIFT']

/** The lifts Progress shows: squat, bench and deadlift, and strict press when its setting is on. */
const shownLifts = (showStrictPress: boolean): readonly Lift[] => (showStrictPress ? [...SBD, 'STRICT_PRESS'] : SBD)

/** A lift's current e1RM and how it moved over the last four weeks; null where there is nothing to compare. */
export type LiftSummary = { lift: Lift; e1rmKg: number | null; changeKg: number | null }

export type PrUi = { lift: Lift; pr: JsPr }

/** `weeks` is kg lifted (weight × reps) in each of the last eight weeks, oldest first; the last one is this week. */
export type VolumeUi = { lift: Lift; weeks: number[] }

export type ProgressState = {
  loading: boolean
  /** False until a main-lift set has been completed: the screen then shows only a hint. */
  hasData: boolean
  range: ChartRange
  /** The strict press setting: it is a fourth lift on the screen and in the total. */
  showStrictPress: boolean
  /** The shown lifts' current e1RMs added up; null until every one of them has one. */
  totalKg: number | null
  totalChangeKg: number | null
  lifts: LiftSummary[]
  /** The Mondays the e1RM chart spans, one per week with no gaps. */
  weeks: string[]
  /** Weekly best e1RM per shown lift to the half kilo, one entry per week in `weeks`; null for a week without that lift. */
  e1rm: Record<SbdLift, (number | null)[]> & { STRICT_PRESS?: (number | null)[] }
  /** The shown lifts' weekly bests added up, for the weeks in range that have every one of them. */
  totalTrend: { week: string; kg: number }[]
  bodyweight: { date: string; kg: number }[]
  /** Each lift's standing record, newest first. */
  prs: PrUi[]
  volumeFrom: string | null
  volume: VolumeUi[]
  ratios: RatioUi[]
}

const CHANGE_DAYS = 28
const E1RM_DAYS = 28
const VOLUME_WEEKS = 8

const empty = (loading: boolean, range: ChartRange, showStrictPress: boolean): ProgressState => ({
  loading,
  hasData: false,
  range,
  showStrictPress,
  totalKg: null,
  totalChangeKg: null,
  lifts: [],
  weeks: [],
  e1rm: { SQUAT: [], BENCH: [], DEADLIFT: [] },
  totalTrend: [],
  bodyweight: [],
  prs: [],
  volumeFrom: null,
  volume: [],
  ratios: [],
})

/** An estimate is not worth more than half a kilo of precision. */
export const toHalfKg = (kg: number) => Math.round(kg * 2) / 2

const percent = (factor: number) => Math.round(factor * 100)

const asLift = (name: string): Lift | null => (LIFTS.includes(name as Lift) ? (name as Lift) : null)

/**
 * Everything the Progress screen shows, from one snapshot of history. `today` is `YYYY-MM-DD`. Strict
 * press is a fourth lift throughout when `showStrictPress` is on; when it is off its sets change nothing here.
 */
export function buildProgress(history: JsHistory, range: ChartRange = DEFAULT_RANGE, today: string, showStrictPress = false): ProgressState {
  const shown = shownLifts(showStrictPress)
  /** One value per shown lift, keyed in the order they are shown. */
  const perLift = <T>(value: (lift: Lift) => T) => Object.fromEntries(shown.map((lift) => [lift, value(lift)])) as Record<SbdLift, T> & { STRICT_PRESS?: T }
  const isShown = (lift: string | null | undefined) => lift != null && (shown as readonly string[]).includes(lift)
  const mainIds = new Set(history.exercises.filter((e) => grit.isMainLift(e) && isShown(grit.mainLiftOf(e))).map((e) => e.id))
  const hasData = history.sessions.some((s) => s.sets.some((set) => !set.failed && mainIds.has(set.exerciseId)))
  if (!hasData) return empty(false, range, showStrictPress)

  const before = addDays(today, -CHANGE_DAYS)
  const current = perLift((lift) => grit.currentE1rm(history, lift, today, E1RM_DAYS) ?? null)
  const earlier = perLift((lift) => grit.currentE1rm(history, lift, before, E1RM_DAYS) ?? null)
  const lifts = shown.map((lift): LiftSummary => {
    const kgNow = current[lift]
    const kgThen = earlier[lift]
    const now = kgNow == null ? null : toHalfKg(kgNow)
    const then = kgThen == null ? null : toHalfKg(kgThen)
    return { lift, e1rmKg: now, changeKg: now != null && then != null ? now - then : null }
  })
  const totalNow = grit.total(history, today, showStrictPress) ?? null
  const totalBefore = grit.total(history, before, showStrictPress) ?? null
  const totalKg = totalNow == null ? null : toHalfKg(totalNow)

  // One continuous run of weeks, so time is to scale even when a week was missed.
  const trends = perLift((lift) => new Map(grit.weeklyTrend(history, lift, today).map((p) => [p.weekStart, p.value])))
  const thisWeek = weekStart(today)
  const firstWeek = shown.flatMap((lift) => [...(trends[lift]?.keys() ?? [])]).reduce((first, week) => (week < first ? week : first), thisWeek)
  const rangeWeeks = RANGE_WEEKS[range]
  const rangeStart = rangeWeeks == null ? null : addDays(thisWeek, -7 * (rangeWeeks - 1))
  const weeks: string[] = []
  for (let week = rangeStart != null && rangeStart > firstWeek ? rangeStart : firstWeek; week <= thisWeek; week = addDays(week, 7)) weeks.push(week)

  // Dates are unique per session on the web; were they not, the last session of a day would win.
  const bodyweight = new Map<string, number>()
  for (const s of [...history.sessions].sort((a, b) => a.date.localeCompare(b.date))) {
    if (s.bodyweightKg != null && (rangeStart == null || s.date >= rangeStart) && s.date <= today) bodyweight.set(s.date, s.bodyweightKg)
  }

  const volumeWeeks = Array.from({ length: VOLUME_WEEKS }, (_, i) => addDays(thisWeek, -7 * (VOLUME_WEEKS - 1 - i)))
  const volume = shown.map((lift): VolumeUi => {
    const byWeek = new Map(grit.weeklyVolume(history, lift).map((p) => [p.weekStart, p.value]))
    return { lift, weeks: volumeWeeks.map((week) => byWeek.get(week) ?? 0) }
  })

  const ratios = grit.ratioRanges().flatMap((pair): RatioUi[] => {
    const numerator = asLift(pair.numerator)
    const denominator = asLift(pair.denominator)
    const num = numerator && current[numerator]
    const den = denominator && current[denominator]
    if (!numerator || !denominator || num == null || den == null || den <= 0) return []
    return [{ numerator, denominator, valuePct: percent(num / den), lowPct: percent(pair.low), highPct: percent(pair.high) }]
  })

  return {
    loading: false,
    hasData: true,
    range,
    showStrictPress,
    totalKg,
    totalChangeKg: totalKg != null && totalBefore != null ? totalKg - toHalfKg(totalBefore) : null,
    lifts,
    weeks,
    e1rm: perLift((lift) =>
      weeks.map((week) => {
        const kg = trends[lift]?.get(week)
        return kg == null ? null : toHalfKg(kg)
      }),
    ),
    totalTrend: weeks.flatMap((week) => {
      const points = shown.map((lift) => trends[lift]?.get(week))
      return points.every((kg) => kg != null) ? [{ week, kg: points.reduce((sum, kg) => sum + kg, 0) }] : []
    }),
    bodyweight: [...bodyweight].map(([date, kg]) => ({ date, kg })),
    prs: shown.flatMap((lift): PrUi[] => {
      const pr = grit.prs(history, lift).at(-1)
      return pr ? [{ lift, pr }] : []
    }).sort((a, b) => b.pr.date.localeCompare(a.pr.date)),
    volumeFrom: volumeWeeks[0] ?? null,
    volume,
    ratios,
  }
}

/** One exercise of the bodybuilding plan on Progress: its best set in each of its last four sessions on that side. */
export type BodybuildingRow = {
  exerciseId: number
  name: string
  /** The exercise's main lift when it is one (its row opens that lift's history); null otherwise. */
  lift: Lift | null
  /** Oldest first. */
  bestSets: { date: string; weightKg: number; reps: number }[]
  /** Null with fewer than two sessions. */
  goingUp: boolean | null
}

/** Progress on the bodybuilding side (spec §7.6): the plan's exercises, in plan order. */
export type BodybuildingProgressState = { loading: boolean; rows: BodybuildingRow[] }

/** The bodybuilding side's rows: the domain's `bodybuildingProgress` over the whole log and that side's plan. */
export function buildBodybuildingProgress(history: JsHistory, plan: (PlanItemRow & { id: number })[], today: string): BodybuildingProgressState {
  const exercises = new Map(history.exercises.map((e) => [e.id, e]))
  return {
    loading: false,
    rows: grit.bodybuildingProgress(history, plan.map(planItemToJs), today).map((trend): BodybuildingRow => {
      const exercise = exercises.get(trend.exerciseId)
      return {
        exerciseId: trend.exerciseId,
        name: trend.name,
        lift: exercise && grit.isMainLift(exercise) ? asLift(grit.mainLiftOf(exercise) ?? '') : null,
        bestSets: trend.bestSets.map((b) => ({ date: b.date, weightKg: b.set.weightKg, reps: b.set.reps })),
        goingUp: trend.goingUp ?? null,
      }
    }),
  }
}

type Stored = { today: string; history: JsHistory; showStrictPress: boolean; side: TrainingMode; plan: (PlanItemRow & { id: number })[] }

/**
 * Where the lifter stands, read live from the database: it follows every logged set. `today` is the
 * real clock; the chart range is kept for as long as the screen is open. The charts, records and total belong to
 * the powerlifting side and read only its sessions; on the bodybuilding side `bodybuilding` holds that side's rows.
 */
export function useProgress(
  db: GritDb,
  today: () => string,
): { state: ProgressState; setRange: (range: ChartRange) => void; side: TrainingMode; bodybuilding: BodybuildingProgressState } {
  const clock = useRef(today)
  clock.current = today
  const [stored, setStored] = useState<Stored>()
  const [readError, setReadError] = useState<unknown>(null)
  const [range, setRange] = useState<ChartRange>(DEFAULT_RANGE)
  useEffect(() => {
    // The setting is read with the history, so the screen follows the switch while it is open.
    const subscription = liveQuery(async () => {
      const [history, settings, plan] = await Promise.all([loadHistory(db), getSettings(db), planFor(db, 'BODYBUILDING')])
      return { today: clock.current(), history, showStrictPress: settings.showStrictPress, side: settings.trainingMode, plan }
    }).subscribe({ next: setStored, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])
  // A failed read is for the app's error boundary, not for a screen that looks like nothing was logged.
  if (readError != null) throw readError

  const side = stored?.side ?? 'POWERLIFTING'
  const powerlifting = useMemo(() => (stored ? historyOn(stored.history, 'POWERLIFTING') : null), [stored])
  const state = useMemo(() => (stored && powerlifting ? buildProgress(powerlifting, range, stored.today, stored.showStrictPress) : empty(true, range, false)), [stored, powerlifting, range])
  const bodybuilding = useMemo(() => (stored?.side === 'BODYBUILDING' ? buildBodybuildingProgress(stored.history, stored.plan, stored.today) : { loading: stored == null, rows: [] }), [stored])
  return { state, setRange, side, bodybuilding }
}
