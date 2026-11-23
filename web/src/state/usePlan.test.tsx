import { cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { updateSettings, type GritDb } from '../db/db'
import { grit } from '../domain'
import { TODAY, addDays, baseline, exerciseNamed, logSet, set, startSession, todayFixture } from '../test/todayFixture'
import { usePlan } from './usePlan'

// Known bests' figures (1.16.1) are worked out only for a caller that shows them (Plan), and only on the
// powerlifting side: the Exercises tab's "Add to plan" reads no history for them.

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  vi.restoreAllMocks()
  cleanup()
  await fx.dispose()
})

const today = () => TODAY

describe('usePlan: Known bests', () => {
  it('withoutTheOption_readsNoBaselines_andWorksOutNoBest', async () => {
    const squat = (await exerciseNamed(db, 'Squat')).id
    await baseline(db, squat, 180, TODAY)
    const baselines = vi.spyOn(db.baselines, 'toArray')
    const knownBest = vi.spyOn(grit, 'knownBest')

    const hook = renderHook(() => usePlan(db, today))
    await waitFor(() => expect(hook.result.current.loading).toBe(false))

    expect(hook.result.current.bests).toEqual({})
    expect(baselines).not.toHaveBeenCalled()
    expect(knownBest).not.toHaveBeenCalled()
  })

  it('onTheBodybuildingSide_worksOutNoBest', async () => {
    await baseline(db, (await exerciseNamed(db, 'Squat')).id, 180, TODAY)
    await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING' }))
    const knownBest = vi.spyOn(grit, 'knownBest')

    const hook = renderHook(() => usePlan(db, today, { knownBests: true }))
    await waitFor(() => expect(hook.result.current.side).toBe('BODYBUILDING'))

    expect(hook.result.current.bests).toEqual({})
    expect(knownBest).not.toHaveBeenCalled()
  })

  it('onPowerlifting_followsALoggedSet_andATypedBest', async () => {
    const squat = (await exerciseNamed(db, 'Squat')).id
    const hook = renderHook(() => usePlan(db, today, { knownBests: true }))
    await waitFor(() => expect(hook.result.current.loading).toBe(false))
    expect(hook.result.current.bests).toEqual({})

    const session = await startSession(db, addDays(TODAY, -1))
    await logSet(db, session, set(squat, 170, { reps: 1, rpe: 10 }))
    await waitFor(() => expect(hook.result.current.bests).toEqual({ [squat]: 170 }))

    await hook.result.current.setBaseline(squat, '185')
    await waitFor(() => expect(hook.result.current.bests).toEqual({ [squat]: 185 }))
  })
})
