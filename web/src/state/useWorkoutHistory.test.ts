import { describe, expect, it } from 'vitest'
import { JsExercise, JsHistory, JsSession, JsSet } from '../domain'
import { buildWorkoutHistory } from './useWorkoutHistory'

const squat = new JsExercise(1, 'Squat', 'SQUAT', null, [], true, false)
const curl = new JsExercise(2, 'Curl', 'ACCESSORY', null, [], true, false)
// Archived: its sets stay in the log under its name.
const pulldown = new JsExercise(3, 'Lat pulldown', 'ACCESSORY', null, [], true, true, 'HALF')

const set = (e: JsExercise, kg: number, reps: number, rpe: number | null = 8, failed = false, unit = 'KG') => new JsSet(e.id, kg, reps, rpe, failed, null, false, unit)
const session = (id: number, date: string, sets: JsSet[], mode = 'POWERLIFTING', bodyweightKg: number | null = null) => new JsSession(id, date, bodyweightKg, null, sets, mode)
const history = (...sessions: JsSession[]) => new JsHistory(sessions, [squat, curl, pulldown], [])

describe('buildWorkoutHistory', () => {
  it('workouts_newestFirst_fromBothSides_withTheirSideCountsAndBodyweight', () => {
    const state = buildWorkoutHistory(
      history(
        session(1, '2026-11-02', [set(squat, 140, 5)]),
        session(3, '2026-11-09', [set(squat, 145, 5), set(squat, 145, 4)], 'POWERLIFTING', 82.5),
        session(2, '2026-11-04', [set(curl, 20, 12), set(squat, 100, 10)], 'BODYBUILDING'),
      ),
    )
    expect(state.loading).toBe(false)
    expect(state.workouts.map((w) => [w.sessionId, w.date, w.side, w.setCount, w.bodyweightKg])).toEqual([
      [3, '2026-11-09', 'POWERLIFTING', 2, 82.5],
      [2, '2026-11-04', 'BODYBUILDING', 2, null],
      [1, '2026-11-02', 'POWERLIFTING', 1, null],
    ])
    expect(state.workouts[1]!.exercises.map((e) => e.name)).toEqual(['Curl', 'Squat'])
  })

  it('aSessionWithNoSets_isNoWorkout', () => {
    const state = buildWorkoutHistory(history(session(1, '2026-11-02', []), session(2, '2026-11-04', [set(squat, 140, 5)]), session(3, '2026-11-09', [])))
    expect(state.workouts.map((w) => w.sessionId)).toEqual([2])
    expect(buildWorkoutHistory(history()).workouts).toEqual([])
  })

  it('anExerciseComeBackTo_isOneGroup_inTheOrderFirstLogged_withItsSetsInOrder', () => {
    const state = buildWorkoutHistory(history(session(1, '2026-11-02', [set(squat, 100, 5), set(curl, 20, 12), set(squat, 120, 5), set(curl, 20, 11), set(squat, 140, 3)])))
    const [workout] = state.workouts
    expect(workout!.exercises.map((e) => [e.exerciseId, e.name, e.sets.map((s) => `${s.weightKg}x${s.reps}`)])).toEqual([
      [1, 'Squat', ['100x5', '120x5', '140x3']],
      [2, 'Curl', ['20x12', '20x11']],
    ])
    expect(workout!.setCount).toBe(5)
  })

  it('everyKindOfSet_isCarriedAsLogged', () => {
    const state = buildWorkoutHistory(
      history(session(1, '2026-11-02', [set(pulldown, 60, 12), set(pulldown, 7.5, 10, 9, false, 'PLATES'), set(pulldown, 8, 6, null), set(pulldown, 8, 4, 10, true, 'PLATES')], 'BODYBUILDING')),
    )
    // One group for the archived exercise, under its name; each set in the unit it was logged in.
    expect(state.workouts[0]!.exercises).toEqual([
      {
        exerciseId: 3,
        name: 'Lat pulldown',
        sets: [
          { weightKg: 60, reps: 12, rpe: 8, failed: false },
          { weightKg: 7.5, reps: 10, rpe: 9, failed: false, unit: 'PLATES' },
          { weightKg: 8, reps: 6, rpe: null, failed: false },
          { weightKg: 8, reps: 4, rpe: 10, failed: true, unit: 'PLATES' },
        ],
      },
    ])
  })

  it('aSetOfAnExerciseWithNoRow_stillShows', () => {
    const state = buildWorkoutHistory(new JsHistory([session(1, '2026-11-02', [new JsSet(99, 50, 5, 8, false, null, false)])], [squat], []))
    expect(state.workouts[0]!.exercises.map((e) => [e.exerciseId, e.name, e.sets.length])).toEqual([[99, 'Exercise', 1]])
  })
})
