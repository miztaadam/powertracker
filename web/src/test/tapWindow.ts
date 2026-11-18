import { vi } from 'vitest'
import { ADD_FORM_TAP_WINDOW_MS } from '../screens/plan/PlanDialogs'

/**
 * Moves the clock past the moment in which Plan's add form ignores taps (the second tap of a double tap on a picker
 * row). Only `Date` is faked, and it keeps running; undo with `vi.useRealTimers()` after the test.
 */
export function pastTheTapWindow() {
  if (!vi.isFakeTimers()) vi.useFakeTimers({ toFake: ['Date'], shouldAdvanceTime: true })
  vi.setSystemTime(Date.now() + ADD_FORM_TAP_WINDOW_MS)
}
