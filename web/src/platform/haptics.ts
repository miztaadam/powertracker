import { canVibrate } from './device'

/** `tick`: one short pulse for a main tap. `best`: a stronger double buzz for a new best (PR). */
export type HapticKind = 'tick' | 'best'

const PATTERN: Record<HapticKind, number | number[]> = { tick: 10, best: [30, 40, 60] }

/**
 * "Vibrate on taps", as last read from the settings (state/useTapHaptics.ts keeps it current). Off until the first
 * read, so a lifter who turned it off never gets a buzz while the app is still opening.
 */
let tapsOn = false

/** Follows the "Vibrate on taps" setting. */
export function setTapHaptics(on: boolean): void {
  tapsOn = on
}

/** Whether a buzz could happen at all: the setting is on and the browser can vibrate. Work done only to choose a buzz is skipped otherwise. */
export function hapticsPossible(): boolean {
  return tapsOn && canVibrate()
}

/**
 * A buzz for a main action (stage 2, owner decision 2). Each goes with something on screen. Does nothing while the
 * setting is off or where the browser cannot vibrate (iPhones give web apps no vibration). `navigator.vibrate`
 * replaces any buzz still running, so fast taps never queue up.
 */
export function haptic(kind: HapticKind): void {
  if (!hapticsPossible()) return
  try {
    navigator.vibrate(PATTERN[kind])
  } catch {
    // A buzz that cannot run must not break the screen.
  }
}

/** Steps closer together than this (a held Enter or Space repeats the click) share one tick. */
export const STEP_HAPTIC_GAP_MS = 100

/**
 * The tick of a − or + step on the weight, reps or RPE stepper: one per tap, at most one every 100 ms. `last` holds
 * the time of the last tick (a ref of the card the steppers are on).
 */
export function stepHaptic(last: { current: number }, now: number = performance.now()): void {
  if (now - last.current < STEP_HAPTIC_GAP_MS) return
  last.current = now
  haptic('tick')
}
