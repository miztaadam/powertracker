import { useCallback, useEffect, useReducer, useRef, useSyncExternalStore } from 'react'

/**
 * The rest countdown: when it ends (null when no rest is running) and how long the rest is in all,
 * for the progress line. The same shape and rules as Android's `RestTimerState`.
 */
export type RestTimerState = { endsAtMs: number | null; totalSec: number }

export const IDLE: RestTimerState = { endsAtMs: null, totalSec: 0 }

/** Whole seconds left, rounded up so the clock reads 0:00 only when the rest is really over. Never negative. */
export function remainingSec(state: RestTimerState, nowMs: number): number {
  if (state.endsAtMs == null) return 0
  return Math.max(0, Math.ceil((state.endsAtMs - nowMs) / 1000))
}

export function isRunning(state: RestTimerState, nowMs: number): boolean {
  return state.endsAtMs != null && state.endsAtMs > nowMs
}

/** Moves the end by `deltaSec` seconds (negative shortens the rest), never to before `nowMs`. */
export function adjust(state: RestTimerState, deltaSec: number, nowMs: number): RestTimerState {
  if (state.endsAtMs == null) return state
  const moved = { endsAtMs: Math.max(state.endsAtMs + deltaSec * 1000, nowMs), totalSec: state.totalSec }
  return { ...moved, totalSec: Math.max(state.totalSec, remainingSec(moved, nowMs)) }
}

/** Where a running rest is kept, so a reload does not lose it. Saving `IDLE` forgets it. */
export type RestTimerStorage = {
  load: () => Promise<RestTimerState | null>
  save: (state: RestTimerState) => Promise<void>
}

/**
 * The one rest timer of the app. It holds only the end time: what is left is always worked out
 * from the clock, so a page that was suspended behind a locked screen shows the right time when it
 * comes back.
 */
export class RestTimerStore {
  private state: RestTimerState = IDLE
  private readonly listeners = new Set<() => void>()
  private storage: RestTimerStorage | null = null
  /** Counts changes, so a slow read of the saved rest never overwrites a newer one. */
  private changes = 0
  /** Saves run one after the other, so the stored rest is always the latest. */
  private saved: Promise<void> = Promise.resolve()

  get = (): RestTimerState => this.state

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  /** Starts a rest of `totalSec`, replacing any that is running. A rest of no length stops it. */
  start(totalSec: number, nowMs: number = Date.now()): void {
    this.set(totalSec > 0 ? { endsAtMs: nowMs + totalSec * 1000, totalSec } : IDLE)
  }

  /** Adds or takes off time. Taking off all that is left ends the rest, as Skip does. */
  adjust(deltaSec: number, nowMs: number = Date.now()): void {
    if (!isRunning(this.state, nowMs)) return
    const next = adjust(this.state, deltaSec, nowMs)
    this.set(isRunning(next, nowMs) ? next : IDLE)
  }

  /** Ends the rest now. */
  skip(): void {
    if (this.state.endsAtMs != null) this.set(IDLE)
  }

  /** Ends a rest that has reached zero. Returns how many ms ago zero was, or null while it runs or when there is none. */
  expire(nowMs: number = Date.now()): number | null {
    const endsAtMs = this.state.endsAtMs
    if (endsAtMs == null || endsAtMs > nowMs) return null
    this.set(IDLE)
    return nowMs - endsAtMs
  }

  /**
   * Keeps the rest in `storage` from now on, and picks up one that was running before a reload.
   * A saved rest that has ended meanwhile is forgotten. Returns the detach function.
   */
  attach(storage: RestTimerStorage): () => void {
    this.storage = storage
    const before = this.changes
    storage
      .load()
      .then((saved) => {
        if (this.storage !== storage || this.changes !== before || saved == null) return
        if (isRunning(saved, Date.now())) {
          this.state = saved
          this.changes++
          this.listeners.forEach((listener) => listener())
        } else {
          this.save(IDLE)
        }
      })
      // No stored rest to read: the timer works without one.
      .catch(() => {})
    return () => {
      if (this.storage === storage) this.storage = null
    }
  }

  private set(state: RestTimerState): void {
    this.state = state
    this.changes++
    this.save(state)
    this.listeners.forEach((listener) => listener())
  }

  private save(state: RestTimerState): void {
    const storage = this.storage
    if (storage == null) return
    // A failed write costs only the countdown after a reload.
    this.saved = this.saved.then(() => storage.save(state)).catch(() => {})
  }
}

/** How often the clock is looked at. Only to notice zero and to redraw: time is never counted in ticks. */
const TICK_MS = 200

/** Runs `check` every tick while a rest runs, and at once when the page comes back from a locked screen. */
function useTicks(endsAtMs: number | null, check: () => void): void {
  const latest = useRef(check)
  latest.current = check
  useEffect(() => {
    if (endsAtMs == null) return
    const run = () => latest.current()
    const timer = setInterval(run, TICK_MS)
    document.addEventListener('visibilitychange', run)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', run)
    }
  }, [endsAtMs])
}

export type RestTimer = {
  /** True from the start of a rest until it is skipped or its zero has been noticed. */
  running: boolean
  start: (totalSec: number) => void
  adjust: (deltaSec: number) => void
  skip: () => void
}

/**
 * The store's rest for the shell: whether one runs, and the three actions. It redraws only when the
 * rest itself changes (start, adjust, skip, zero), never as the seconds pass; `useRestCountdown`
 * does that for the bar alone. `onZero` fires once when the rest reaches zero (not on Skip), with how
 * many ms ago that was: about nothing while the page is running, much more when it was suspended
 * through the end of the rest.
 */
export function useRestTimer(store: RestTimerStore, onZero?: (lateMs: number) => void): RestTimer {
  const state = useSyncExternalStore(store.subscribe, store.get)
  useTicks(state.endsAtMs, () => {
    const lateMs = store.expire(Date.now())
    if (lateMs != null) onZero?.(lateMs)
  })
  return {
    running: state.endsAtMs != null,
    start: useCallback((totalSec: number) => store.start(totalSec), [store]),
    adjust: useCallback((deltaSec: number) => store.adjust(deltaSec), [store]),
    skip: useCallback(() => store.skip(), [store]),
  }
}

/** The time left, for the component that shows it: redrawn when the second on show changes. */
export function useRestCountdown(store: RestTimerStore): { remainingSec: number; totalSec: number } {
  const state = useSyncExternalStore(store.subscribe, store.get)
  // The second on show: setting the same value again does not redraw.
  const [, show] = useReducer((_: number, second: number) => second, 0)
  useTicks(state.endsAtMs, () => show(remainingSec(store.get(), Date.now())))
  // Read from the clock on every draw, so the first draw after a start or an adjust is right too.
  return { remainingSec: remainingSec(state, Date.now()), totalSec: state.totalSec }
}
