import { describe, expect, it } from 'vitest'
import { JsExercise, JsHistory, JsSession, JsSet } from '../domain'
import { buildLiftHistory } from './useLiftHistory'

const squat = new JsExercise(1, 'Squat', 'SQUAT', null, [], true, false)
const bench = new JsExercise(2, 'Bench', 'BENCH', null, [], true, false)
const pause = new JsExercise(3, 'Pause squat', 'VARIATION', 'SQUAT', [], true, false)
const curl = new JsExercise(4, 'Curl', 'ACCESSORY', null, [], true, false)

const today = '2026-11-11'
const set = (e: JsExercise, kg: number, reps: number, rpe: number | null, failed = false) => new JsSet(e.id, kg, reps, rpe, failed, null, false)
const history = (...sessions: [string, JsSet[]][]) =>
  new JsHistory(
    sessions.map(([date, sets], i) => new JsSession(i + 1, date, null, null, sets)),
    [squat, bench, pause, curl],
    [],
  )

describe('buildLiftHistory', () => {
  it('sessions_newestFirstWithTheLiftBeforeItsVariations', () => {
    const state = buildLiftHistory(
      history(
        ['2026-11-02', [set(squat, 140, 5, 8), set(bench, 100, 5, 8)]],
        ['2026-11-04', [set(bench, 100, 5, 8), set(curl, 20, 10, 8)]],
        ['2026-11-09', [set(pause, 120, 3, 8), set(squat, 145, 5, 8), set(pause, 120, 3, 8)]],
      ),
      'SQUAT',
      today,
    )

    expect(state.sessions.map((s) => s.date)).toEqual(['2026-11-09', '2026-11-02'])
    const [newest] = state.sessions
    expect(newest!.groups.map((g) => [g.exerciseName, g.isVariation, g.sets.map((s) => s.index)])).toEqual([
      ['Squat', false, [1]],
      ['Pause squat', true, [1, 2]],
    ])
  })

  it('prBadges_onPrSetsOnly', () => {
    const state = buildLiftHistory(
      history(
        ['2026-11-02', [set(squat, 140, 5, 8), set(squat, 140, 5, 8)]],
        // The failed 150 and the heavier variation never count; the repeat of 145 is not a new record.
        ['2026-11-09', [set(squat, 145, 5, 8), set(squat, 150, 3, 10, true), set(pause, 200, 3, 8), set(squat, 145, 5, 8), set(squat, 130, 5, 8)]],
      ),
      'SQUAT',
      today,
    )

    const flags = state.sessions.map((s) => s.groups.map((g) => g.sets.map((r) => r.isPr)))
    expect(flags).toEqual([[[true, false, false, false], [false]], [[true, false]]])
    expect(state.best).toMatchObject({ date: '2026-11-09', weightKg: 145, reps: 5, rpe: 8 })
    expect(state.currentE1rmKg).toBeCloseTo(state.best!.e1rmKg, 9)
  })

  it('empty_whenTheLiftWasNeverLogged', () => {
    const state = buildLiftHistory(history(['2026-11-02', [set(bench, 100, 5, 8)]]), 'SQUAT', today)

    expect(state).toMatchObject({ loading: false, sessions: [], best: null, currentE1rmKg: null })
  })

  it('bodybuildingSessions_areLeftOut_andDoNotMoveTheRecords', () => {
    // Fix round ruling A: the lift history belongs to the powerlifting side and lists its sessions only.
    const sessions = [
      new JsSession(1, '2026-11-02', null, null, [set(squat, 140, 5, 8)], 'POWERLIFTING'),
      new JsSession(2, '2026-11-05', null, null, [set(squat, 120, 10, 8), set(squat, 160, 3, 8)], 'BODYBUILDING'),
      new JsSession(3, '2026-11-09', null, null, [set(squat, 142.5, 5, 8)], 'POWERLIFTING'),
    ]
    const state = buildLiftHistory(new JsHistory(sessions, [squat, bench, pause, curl], []), 'SQUAT', today)

    expect(state.sessions.map((s) => s.date)).toEqual(['2026-11-09', '2026-11-02'])
    expect(state.best).toMatchObject({ date: '2026-11-09', weightKg: 142.5, reps: 5 })
    const onlyPowerlifting = buildLiftHistory(new JsHistory([sessions[0]!, sessions[2]!], [squat, bench, pause, curl], []), 'SQUAT', today)
    expect(state).toEqual(onlyPowerlifting)
  })
})
