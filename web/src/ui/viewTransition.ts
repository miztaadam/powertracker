import type { CSSProperties } from 'react'
import { flushSync } from 'react-dom'

/**
 * Stage 3, "things slide into place": a change to the screen that slides instead of jumping. The browser takes a
 * picture of the screen, `update` commits the React state (flushSync, so the new screen is in place when the
 * browser takes the second picture), and the elements with a `view-transition-name` (vt.css) move from the one to
 * the other.
 *
 * - With motion off (`data-motion="off"` on <html>: reduced motion, and every test) or in a browser without view
 *   transitions, `update` runs at once and nothing moves, as before.
 * - A transition still running when the next one starts is skipped first, so it ends where it was going and the
 *   new one starts from the screen as it is. Every update runs, in order.
 * - A finger going down during a transition skips it too, so the tap is never held up or lost.
 */
type Transition = { skipTransition: () => void; finished: Promise<void>; ready: Promise<void>; updateCallbackDone: Promise<void> }
type StartViewTransition = (update: () => void) => Transition

let running: Transition | null = null

/**
 * The class on <html> that puts the names on (vt.css) while a transition runs, and only then: a name makes its element
 * a stacking context, which would keep a row's menu under the next row.
 */
export const NAMING_CLASS = 'vt-naming'

export function withTransition(update: () => void): void {
  const start = (document as Document & { startViewTransition?: StartViewTransition }).startViewTransition
  if (typeof start !== 'function' || document.documentElement.getAttribute('data-motion') === 'off') {
    update()
    return
  }
  running?.skipTransition()
  const root = document.documentElement
  // On before the browser takes the first picture (the next frame), so the old screen has its names too.
  root.classList.add(NAMING_CLASS)
  let transition: Transition
  try {
    transition = start.call(document, () => flushSync(update))
  } catch {
    // The browser would not start one: the change simply happens, as without view transitions.
    running = null
    root.classList.remove(NAMING_CLASS)
    update()
    return
  }
  running = transition
  // The browser leaves the pictures out of hit testing, so a tap during the slide would land on nothing: the finger
  // going down ends the slide at once, so the tap reaches what is under it. Only listened for while one runs.
  const skipOnPress = () => transition.skipTransition()
  window.addEventListener('pointerdown', skipOnPress, { capture: true })
  // A skipped transition (a new one, a hidden page, a name used twice) rejects `ready`: the update has still run,
  // so there is nothing to report. A failed update rejects the others; its error has gone to React already.
  transition.ready.catch(() => {})
  transition.updateCallbackDone.catch(() => {})
  transition.finished
    .catch(() => {})
    .finally(() => {
      window.removeEventListener('pointerdown', skipOnPress, { capture: true })
      if (running === transition) {
        running = null
        root.classList.remove(NAMING_CLASS)
      }
    })
}

/**
 * Runs `work` once the transition running now has finished, or at once when none is. For what would move the page
 * under the moving pictures, such as scrolling the opened card into view: done during the transition, the screen would
 * jump when it ends.
 */
export function afterTransition(work: () => void): void {
  const now = running
  if (now == null) work()
  else void now.finished.catch(() => {}).then(work)
}

/** How long an action's request to slide waits for the change it causes, before it lapses. */
export const SLIDE_WAIT_MS = 1000

/**
 * For data read live from the database (a Dexie liveQuery), where the change an action causes arrives later, as a
 * new value: `arm()` before the write asks that the next value that changes `shape` (the ids on screen, their order)
 * slides into place. Every other value is committed at once, as before. A value is never committed out of order:
 * the one the transition commits is always the newest.
 */
export function slideNextChange<T>(commit: (value: T) => void, shape: (value: T) => string) {
  let until = 0
  let newest: { value: T } | null = null
  return {
    arm() {
      until = performance.now() + SLIDE_WAIT_MS
    },
    disarm() {
      until = 0
    },
    next(value: T) {
      const before = newest
      newest = { value }
      if (until > performance.now() && before != null && shape(before.value) !== shape(value)) {
        until = 0
        withTransition(() => commit(newest!.value))
      } else commit(value)
    },
  }
}

/**
 * The props that give an element its own name in a view transition (vt.css), with motion on only: spread them on the
 * element, e.g. `<li {...slideName(`plan-${id}`)}>`. A name is unique on the page: the kind of thing, then its id.
 */
export function slideName(name: string): { 'data-vt': string; style: CSSProperties } {
  return { 'data-vt': '', style: { '--vt-name': name } as CSSProperties }
}
