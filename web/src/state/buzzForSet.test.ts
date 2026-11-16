import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../db/db'
import { setTapHaptics } from '../platform/haptics'
import { buzzForSet } from './useToday'

// The buzz for a saved set is worked out after the save; when the history cannot be read it still ticks (1.7 review).
describe('buzzForSet', () => {
  let vibrate: ReturnType<typeof vi.fn>
  beforeEach(() => {
    vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
  })
  afterEach(() => {
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('aFailingHistoryRead_ticks', async () => {
    const gone = () => {
      throw new Error('disk gone')
    }
    const db = { sessions: { orderBy: gone }, sets: { orderBy: gone }, exercises: { toArray: gone }, baselines: { toArray: gone } } as unknown as GritDb
    await buzzForSet(db, 1, { exerciseId: 1, weightKg: 180, reps: 5, rpe: 8, failed: false }, '2026-11-11', 'POWERLIFTING')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })
})
