import { cleanup, render, screen } from '@testing-library/react'
import { createElement } from 'react'
import { describe, expect, it } from 'vitest'
import { JsExercise, JsHistory, JsSession, JsSet } from '../domain'
import { BodybuildingProgressContent, trendRowName } from '../screens/progress/BodybuildingProgress'
import { ExerciseHistoryContent } from '../screens/progress/ExerciseHistoryScreen'
import { addDays } from './isoDate'
import { buildExerciseHistory, buildLiftHistory } from './useLiftHistory'
import { buildBodybuildingProgress, buildProgress, type ChartRange } from './useProgress'

// Ported from Android's ProgressViewModelBuildTest.

const squat = new JsExercise(1, 'Squat', 'SQUAT', null, [], true, false)
const bench = new JsExercise(2, 'Bench', 'BENCH', null, [], true, false)
const deadlift = new JsExercise(3, 'Deadlift', 'DEADLIFT', null, [], true, false)

const today = '2026-11-11' // a Wednesday
const thisWeek = '2026-11-09'
const weeksAgo = (n: number) => addDays(thisWeek, -7 * n)

/** A single at RPE 10: its e1RM is its weight. */
const single = (exercise: JsExercise, kg: number, failed = false) => new JsSet(exercise.id, kg, 1, 10, failed, null, false)

const press = new JsExercise(4, 'Strict press', 'STRICT_PRESS', null, [], true, false)

const history = (...sessions: [string, JsSet[]][]) =>
  new JsHistory(
    sessions.map(([date, sets], i) => new JsSession(i + 1, date, null, null, sets)),
    [squat, bench, deadlift, press],
    [],
  )

const build = (h: JsHistory, range: ChartRange = '6m', showStrictPress = false) => buildProgress(h, range, today, showStrictPress)

/** Two weeks of all four lifts. */
const allFour = history(
  [weeksAgo(1), [single(squat, 180), single(bench, 120), single(deadlift, 210), single(press, 70)]],
  [thisWeek, [single(squat, 182.5), single(bench, 120), single(deadlift, 215), single(press, 72.5)]],
)

const without = (h: JsHistory, exercise: JsExercise) =>
  new JsHistory(
    h.sessions.map((s) => new JsSession(s.id, s.date, s.bodyweightKg, s.readiness, s.sets.filter((set) => set.exerciseId !== exercise.id))),
    h.exercises,
    h.baselines,
  )
const points = (values: readonly (number | null)[]) => values.filter((v) => v != null)

/** Squat nine weeks ago and this week. */
const nineWeeks = history([weeksAgo(9), [single(squat, 100)]], [thisWeek, [single(squat, 110)]])

describe('buildProgress', () => {
  it('range_eightWeeksDropsANineWeekOldPoint', () => {
    const state = build(nineWeeks, '8w')

    expect(state.weeks).toHaveLength(8)
    expect(state.weeks[0]).toBe(weeksAgo(7))
    expect(state.weeks[7]).toBe(thisWeek)
    expect(points(state.e1rm.SQUAT)).toEqual([110])
  })

  it('range_allKeepsEveryPoint', () => {
    const state = build(nineWeeks, 'all')

    expect(state.weeks).toHaveLength(10)
    expect(state.weeks[0]).toBe(weeksAgo(9))
    expect(points(state.e1rm.SQUAT)).toEqual([100, 110])
    // Weeks without the lift are gaps, not zeros.
    expect(state.e1rm.SQUAT[1]).toBeNull()
  })

  it('range_longerThanHistoryStartsAtTheFirstPoint', () => {
    expect(build(nineWeeks, '1y').weeks[0]).toBe(weeksAgo(9))
  })

  it('range_defaultsToSixMonths', () => {
    expect(buildProgress(nineWeeks, undefined, today).range).toBe('6m')
  })

  it('change_positive', () => {
    // 100 five weeks ago (inside the window that ended 28 days ago), 107.5 this week.
    const state = build(history([addDays(today, -35), [single(squat, 100)]], [addDays(today, -2), [single(squat, 107.5)]]))

    const summary = state.lifts.find((l) => l.lift === 'SQUAT')!
    expect(summary.e1rmKg).toBeCloseTo(107.5, 9)
    expect(summary.changeKg).toBeCloseTo(7.5, 9)
  })

  it('change_nullWithNoPriorData', () => {
    const state = build(history([addDays(today, -2), [single(squat, 107.5)]]))

    const summary = state.lifts.find((l) => l.lift === 'SQUAT')!
    expect(summary.e1rmKg).toBeCloseTo(107.5, 9)
    expect(summary.changeKg).toBeNull()
    expect(state.lifts.find((l) => l.lift === 'BENCH')!.e1rmKg).toBeNull()
  })

  it('totalChange_needsAllThreeLiftsInBothWindows', () => {
    const before = addDays(today, -35)
    const now = addDays(today, -2)
    const full = history(
      [before, [single(squat, 180), single(bench, 120), single(deadlift, 240)]],
      [now, [single(squat, 185), single(bench, 120), single(deadlift, 247.5)]],
    )
    const noPriorDeadlift = history([before, [single(squat, 180), single(bench, 120)]], [now, [single(squat, 185), single(bench, 120), single(deadlift, 247.5)]])

    expect(build(full).totalKg).toBeCloseTo(552.5, 9)
    expect(build(full).totalChangeKg).toBeCloseTo(12.5, 9)
    expect(build(noPriorDeadlift).totalKg).toBeCloseTo(552.5, 9)
    expect(build(noPriorDeadlift).totalChangeKg).toBeNull()
  })

  it('totalTrend_onlyWeeksWithAllThreeLifts', () => {
    const state = build(
      history(
        [weeksAgo(2), [single(squat, 180), single(bench, 120), single(deadlift, 240)]],
        [weeksAgo(1), [single(squat, 182.5), single(bench, 120)]],
        [thisWeek, [single(squat, 185), single(bench, 122.5), single(deadlift, 245)]],
      ),
    )

    expect(state.totalTrend).toEqual([
      { week: weeksAgo(2), kg: 540 },
      { week: thisWeek, kg: 552.5 },
    ])
  })

  it('hasData_needsACompletedMainLiftSet', () => {
    expect(build(history()).hasData).toBe(false)
    expect(build(history([today, [single(squat, 100, true)]])).hasData).toBe(false)
    expect(build(history([today, [single(squat, 100, true), single(squat, 90)]])).hasData).toBe(true)
  })

  it('hasData_ignoresAStrictPressOnlyHistory', () => {
    expect(build(history([today, [single(press, 60)]])).hasData).toBe(false)
    const both = build(history([today, [single(press, 60), single(squat, 100)]]))
    expect(both.hasData).toBe(true)
    expect(both.lifts.map((l) => l.lift)).toEqual(['SQUAT', 'BENCH', 'DEADLIFT'])
  })

  it('ratios_areEachPairAgainstItsTypicalRange', () => {
    const state = build(history([addDays(today, -2), [single(squat, 180), single(bench, 120), single(deadlift, 240)]]))

    expect(state.ratios).toEqual([
      { numerator: 'SQUAT', denominator: 'DEADLIFT', valuePct: 75, lowPct: 80, highPct: 90 },
      { numerator: 'BENCH', denominator: 'SQUAT', valuePct: 67, lowPct: 60, highPct: 70 },
    ])
  })

  it('ratios_leaveOutAPairWithoutBothLifts', () => {
    const state = build(history([addDays(today, -2), [single(squat, 180), single(bench, 120)]]))

    expect(state.ratios.map((r) => [r.numerator, r.denominator])).toEqual([['BENCH', 'SQUAT']])
  })

  it('volume_coversTheLastEightWeeksWithZerosForMissedOnes', () => {
    const state = build(nineWeeks)

    expect(state.volumeFrom).toBe(weeksAgo(7))
    expect(state.volume.find((v) => v.lift === 'SQUAT')!.weeks).toEqual([0, 0, 0, 0, 0, 0, 0, 110])
    expect(state.volume.find((v) => v.lift === 'BENCH')!.weeks).toEqual([0, 0, 0, 0, 0, 0, 0, 0])
  })

  it('prs_areEachLiftsStandingRecordNewestFirst', () => {
    const state = build(
      history([weeksAgo(2), [single(squat, 180), single(bench, 120)]], [weeksAgo(1), [single(squat, 170), single(bench, 122.5)]], [thisWeek, [single(deadlift, 240)]]),
    )

    expect(state.prs.map((p) => [p.lift, p.pr.date, p.pr.e1rmKg])).toEqual([
      ['DEADLIFT', thisWeek, 240],
      ['BENCH', weeksAgo(1), 122.5],
      ['SQUAT', weeksAgo(2), 180],
    ])
  })

  it('bodyweight_keepsTheSessionsInRangeOldestFirst', () => {
    const h = new JsHistory(
      [
        new JsSession(1, weeksAgo(9), 91, null, [single(squat, 100)]),
        new JsSession(2, weeksAgo(1), 92, null, []),
        new JsSession(3, thisWeek, null, null, [single(squat, 110)]),
        new JsSession(4, today, 92.5, null, []),
      ],
      [squat, bench, deadlift],
      [],
    )

    expect(build(h, '8w').bodyweight).toEqual([
      { date: weeksAgo(1), kg: 92 },
      { date: today, kg: 92.5 },
    ])
    expect(build(h, 'all').bodyweight).toHaveLength(3)
  })

  // ---- Strict press behind its setting (strict press spec §5.2) ----

  it('switchOff_progressUnchanged', () => {
    const off = build(allFour)

    // Strict press sets change nothing: the state is the one a lifter who never pressed gets.
    expect(off).toEqual(build(without(allFour, press)))
    expect(off.showStrictPress).toBe(false)
    expect(off.lifts.map((l) => l.lift)).toEqual(['SQUAT', 'BENCH', 'DEADLIFT'])
    expect(Object.keys(off.e1rm)).toEqual(['SQUAT', 'BENCH', 'DEADLIFT'])
    expect(off.volume.map((v) => v.lift)).toEqual(['SQUAT', 'BENCH', 'DEADLIFT'])
    expect(off.prs.some((p) => p.lift === 'STRICT_PRESS')).toBe(false)
    expect(off.totalKg).toBeCloseTo(517.5, 9)
    // Strict press alone is not something Progress has to show.
    expect(build(history([thisWeek, [single(press, 70)]])).hasData).toBe(false)
  })

  it('switchOn_showsStrictPressEverywhere', () => {
    const on = build(allFour, '6m', true)

    expect(on.showStrictPress).toBe(true)
    expect(on.lifts.map((l) => l.lift)).toEqual(['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS'])
    expect(on.lifts.at(-1)!.e1rmKg).toBeCloseTo(72.5, 9)
    expect(on.e1rm.STRICT_PRESS).toEqual([70, 72.5])
    expect(on.prs.find((p) => p.lift === 'STRICT_PRESS')!.pr.e1rmKg).toBeCloseTo(72.5, 9)
    expect(on.volume.find((v) => v.lift === 'STRICT_PRESS')!.weeks.at(-1)).toBeCloseTo(72.5, 9)
    // The total and its trend count all four.
    expect(on.totalKg).toBeCloseTo(590, 9)
    expect(on.totalTrend).toEqual([
      { week: weeksAgo(1), kg: 580 },
      { week: thisWeek, kg: 590 },
    ])
    // Strict press is in no ratio.
    expect(on.ratios).toHaveLength(2)
    expect(on.ratios.some((r) => r.numerator === 'STRICT_PRESS' || r.denominator === 'STRICT_PRESS')).toBe(false)
    // With the switch on, strict press alone is something to show.
    expect(build(history([thisWeek, [single(press, 70)]]), '6m', true).hasData).toBe(true)
  })

  it('switchOn_noPressData_totalExplains', () => {
    const noPress = without(allFour, press)
    const on = build(noPress, '6m', true)

    // Every included lift needs data: no total, no change, no trend. The other lifts are untouched.
    expect(on.totalKg).toBeNull()
    expect(on.totalChangeKg).toBeNull()
    expect(on.totalTrend).toEqual([])
    expect(on.lifts.at(-1)).toEqual({ lift: 'STRICT_PRESS', e1rmKg: null, changeKg: null })
    expect(on.e1rm.STRICT_PRESS).toEqual([null, null])
    expect(on.volume.find((v) => v.lift === 'STRICT_PRESS')!.weeks.at(-1)).toBe(0)
    expect(on.prs.some((p) => p.lift === 'STRICT_PRESS')).toBe(false)
    expect(on.lifts.slice(0, -1)).toEqual(build(noPress).lifts)
    expect(build(noPress).totalKg).toBeCloseTo(517.5, 9)
  })
})

describe('buildBodybuildingProgress', () => {
  const checkIn = (date: string) => ({ id: 1, date, lagging: [], growingWell: [] })

  it('checkedIn_onlyCountsACheckInOnOrBeforeToday', () => {
    const empty = history()
    expect(buildBodybuildingProgress(empty, [], today, []).checkedIn).toBe(false)
    expect(buildBodybuildingProgress(empty, [], today, [checkIn(today)]).checkedIn).toBe(true)
    // As on Android: a check-in dated after today does not count yet.
    expect(buildBodybuildingProgress(empty, [], today, [checkIn(addDays(today, 1))]).checkedIn).toBe(false)
  })
})

// 1.17: a plate-counted machine on Progress and in the exercise history (Android's PlatesProgressTest).
describe('plates on Progress and in history', () => {
  const pulldown = new JsExercise(5, 'Lat pulldown', 'ACCESSORY', null, ['LATS'], true, false, 'WHOLE')
  const curl = new JsExercise(6, 'Curl', 'ACCESSORY', null, ['BICEPS'], true, false)
  const bbPlan = [pulldown, curl].map((e, i) => ({ id: i + 1, weekday: 3, exerciseId: e.id, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: i, mode: 'BODYBUILDING' as const }))
  const plate = (e: JsExercise, n: number, reps: number) => new JsSet(e.id, n, reps, 8, false, null, false, 'PLATES')
  const kg = (e: JsExercise, n: number, reps: number) => new JsSet(e.id, n, reps, 8, false, null, false)
  const bbHistory = (...sessions: [string, JsSet[]][]) =>
    new JsHistory(
      sessions.map(([date, sets], i) => new JsSession(i + 1, date, null, null, sets, 'BODYBUILDING')),
      [pulldown, curl],
      [],
    )

  it('bodybuildingProgress_printsPlate9Times10', () => {
    const h = bbHistory([addDays(today, -7), [plate(pulldown, 8, 10), kg(curl, 20, 10)]], [addDays(today, -2), [plate(pulldown, 9, 10), kg(curl, 22, 10)]])
    const rows = buildBodybuildingProgress(h, bbPlan, today).rows
    const [pulldownRow, curlRow] = rows
    expect(pulldownRow!.bestSets).toStrictEqual([
      { date: addDays(today, -7), weightKg: 8, reps: 10, unit: 'PLATES' },
      { date: addDays(today, -2), weightKg: 9, reps: 10, unit: 'PLATES' },
    ])
    // A kg row is as it was: no unit key at all.
    expect(curlRow!.bestSets).toStrictEqual([
      { date: addDays(today, -7), weightKg: 20, reps: 10 },
      { date: addDays(today, -2), weightKg: 22, reps: 10 },
    ])
    expect(trendRowName(pulldownRow!)).toBe('Lat pulldown. plate 8 × 10, plate 9 × 10. Going up. Open history')
    expect(trendRowName(curlRow!)).toBe('Curl. 20 × 10, 22 × 10. Going up. Open history')
    // A half is spoken as the set rows speak it.
    const half = buildBodybuildingProgress(bbHistory([addDays(today, -2), [plate(pulldown, 7.5, 10)]]), bbPlan, today).rows[0]!
    expect(trendRowName(half)).toBe('Lat pulldown. plate 7 and a half × 10. Open history')
    render(createElement(BodybuildingProgressContent, { state: { loading: false, rows, bodyParts: [], checkedIn: false }, onOpenExercise: () => {} }))
    const lines = [...document.querySelectorAll('.bb-row-sets')].map((el) => el.textContent)
    expect(lines).toEqual(['plate 8 × 10 · plate 9 × 10', '20 × 10 · 22 × 10'])
    cleanup()
  })

  it('exerciseHistory_printsEachSetInItsOwnUnit', () => {
    // A kg session, then plates turned on and a plate session.
    const h = bbHistory([addDays(today, -14), [kg(pulldown, 60, 10)]], [addDays(today, -7), [plate(pulldown, 7, 10)]])
    const state = buildExerciseHistory(h, pulldown.id)
    render(createElement(ExerciseHistoryContent, { state, onBack: () => {} }))
    const [plates, kgs] = state.sessions
    expect(plates!.groups[0]!.sets.map((s) => [s.set.unit, s.isPr])).toEqual([['PLATES', false]])
    expect(kgs!.groups[0]!.sets.map((s) => [s.set.unit, s.isPr])).toEqual([['KG', false]])
    expect(screen.getByText('Set 1: plate 7 × 10 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('Set 1: 60 kg × 10 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('plate 7')).toBeInTheDocument()
    expect(screen.getByText('60')).toBeInTheDocument()
    expect(screen.queryByText('PR')).toBeNull()
    cleanup()
  })

  it('a plate set that matches a record is never marked as it', () => {
    // A squat set in plates (only a hand-edited backup could hold one) logged before the kg set that made the record.
    const squatRow = new JsExercise(1, 'Squat', 'SQUAT', null, [], true, false)
    const h = new JsHistory([new JsSession(1, addDays(today, -2), null, null, [new JsSet(1, 100, 5, 8, false, null, false, 'PLATES'), new JsSet(1, 100, 5, 8, false, null, false)])], [squatRow], [])
    const state = buildLiftHistory(h, 'SQUAT', today)
    expect(state.sessions[0]!.groups[0]!.sets.map((s) => [s.set.unit, s.isPr])).toEqual([
      ['PLATES', false],
      ['KG', true],
    ])
  })
})
