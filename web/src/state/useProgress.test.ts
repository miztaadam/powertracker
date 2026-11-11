import { describe, expect, it } from 'vitest'
import { JsExercise, JsHistory, JsSession, JsSet } from '../domain'
import { addDays } from './isoDate'
import { buildProgress, type ChartRange } from './useProgress'

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

const build = (h: JsHistory, range: ChartRange = '6m') => buildProgress(h, range, today)
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
})
