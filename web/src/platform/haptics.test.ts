import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { haptic, setTapHaptics, stepHaptic, STEP_HAPTIC_GAP_MS } from './haptics'

/** A fake `navigator.vibrate`; deleted again after each test, so `'vibrate' in navigator` is false as in jsdom. */
function fakeVibrate() {
  const vibrate = vi.fn(() => true)
  Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
  return vibrate
}

beforeEach(() => setTapHaptics(true))
afterEach(() => {
  setTapHaptics(false)
  Reflect.deleteProperty(navigator, 'vibrate')
})

describe('haptic', () => {
  it('does nothing where the browser cannot vibrate (iPhone)', () => {
    expect('vibrate' in navigator).toBe(false)
    expect(() => haptic('tick')).not.toThrow()
    expect(() => haptic('best')).not.toThrow()
  })

  it('does nothing while "Vibrate on taps" is off', () => {
    const vibrate = fakeVibrate()
    setTapHaptics(false)
    haptic('tick')
    haptic('best')
    expect(vibrate).not.toHaveBeenCalled()
  })

  it('ticks with one short pulse', () => {
    const vibrate = fakeVibrate()
    haptic('tick')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('gives a new best a stronger double buzz', () => {
    const vibrate = fakeVibrate()
    haptic('best')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith([30, 40, 60])
  })

  it('never breaks the screen when the vibrator throws', () => {
    Object.defineProperty(navigator, 'vibrate', {
      configurable: true,
      value: () => {
        throw new Error('blocked')
      },
    })
    expect(() => haptic('tick')).not.toThrow()
  })
})

describe('stepHaptic', () => {
  it('ticks once per step, and at most once every 100 ms when the steps come faster (a held key)', () => {
    const vibrate = fakeVibrate()
    const last = { current: Number.NEGATIVE_INFINITY }
    stepHaptic(last, 10_000)
    stepHaptic(last, 10_000 + 40)
    stepHaptic(last, 10_000 + STEP_HAPTIC_GAP_MS - 1)
    expect(vibrate).toHaveBeenCalledTimes(1)
    stepHaptic(last, 10_000 + STEP_HAPTIC_GAP_MS)
    expect(vibrate).toHaveBeenCalledTimes(2)
    stepHaptic(last, 10_000 + 3 * STEP_HAPTIC_GAP_MS)
    expect(vibrate).toHaveBeenCalledTimes(3)
    expect(vibrate).toHaveBeenLastCalledWith(10)
  })

  it('does nothing while the setting is off', () => {
    const vibrate = fakeVibrate()
    setTapHaptics(false)
    stepHaptic({ current: Number.NEGATIVE_INFINITY }, 20_000)
    expect(vibrate).not.toHaveBeenCalled()
  })
})
