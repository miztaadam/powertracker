import { createContext, useContext, useEffect, useSyncExternalStore } from 'react'
import { flushSync } from 'react-dom'
import type { TrainingMode } from '../../db/schema'
import { grit } from '../../domain'
import { applyTrainingMode } from '../theme'
import { buzz, dropBuzzWithoutMotion, followSound, playSound, prefetchSounds, pumpBuzzWithoutMotion, stopSound, unlockSound, type ModeSwitchKind } from './modeSwitchFeedback'

export type { ModeSwitchKind } from './modeSwitchFeedback'

/**
 * The mode switch animations' controller (1.15): one clock, the side the app shows, the vibration and the sound.
 * Settings' "Bodybuilding mode" switch starts it, and nothing else does; ModeSwitchOverlay draws it.
 *
 * States: idle → running (`drop` or `pump`) → idle.
 * - **flip(kind, switch)**, from the switch tap. The setting is stored by the caller straight after, as before.
 *   - Motion off (`data-motion="off"`: reduced motion, and every e2e page): nothing is drawn and the side changes
 *     when the stored side does, as before, but the sound plays from its start and the vibration still lands where
 *     the animation's would.
 *   - Otherwise the side shown stays the old one (`shown`) and the clock starts at the tap: each frame draws the time
 *     since the tap, the first one included, as the sound and the vibration run from it.
 * - **The moment**: the drop's impact (`T_IMPACT`) or the pump's third beat (`P_BEAT[2]`). The side shown turns to
 *   the new one there, through the overlay's reveal. The drop vibrates there; the pump gives one pulse per beat.
 * - **finish(reason)** ends it at its end state, from anywhere: `end` (the clock ran out), `skip` (a tap anywhere but
 *   the switch: it does nothing else; the vibration is cancelled and one short tick given), `flip` (the switch again:
 *   the new one starts at once), `leave` (Settings
 *   closed), `hidden` (the page went to the background). The new side is put on if the moment had not come.
 */
export type ModeSwitchState = {
  kind: ModeSwitchKind | null
  shown: TrainingMode | null
}

/** What the controller asks of the overlay. All of it is optional: with no overlay, only the side changes. */
export type ModeSwitchPainter = {
  /** Builds the animation's shapes; `source` is the switch (the pump starts from it). */
  begin(kind: ModeSwitchKind, source: Element | null): void
  paint(tMs: number): void
  /**
   * At the moment: shows the new side, running `swap` (which puts it on) inside the reveal. `swap` says false when it
   * did nothing (its run has ended since), and then nothing else is to be done.
   */
  reveal(swap: () => boolean): void
  /** Clears everything drawn and every transform. */
  end(): void
}

export type ModeSwitchFeedback = {
  buzz: (pattern: number | number[]) => void
  unlock: () => void
  /** `tied`: the sound follows the animation clock (`clock()`), or plays from its start with no clock. */
  play: (kind: ModeSwitchKind, tied: (() => number | null) | null) => void
  follow: (tMs: number) => void
  stop: (fade?: number) => void
}

export type ModeSwitchDeps = {
  now: () => number
  raf: (cb: FrameRequestCallback) => number
  caf: (id: number) => void
  /** Motion off: reduced motion, or the e2e flag (both put `data-motion="off"` on <html>). */
  motionOff: () => boolean
  feedback: ModeSwitchFeedback
}

export type FinishReason = 'end' | 'skip' | 'flip' | 'leave' | 'hidden'

export type ModeSwitchController = {
  flip(kind: ModeSwitchKind, source: Element | null): void
  finish(reason: FinishReason): void
  /** The side the stored settings hold, as last read. */
  noteStored(mode: TrainingMode): void
  /** Lets the stored side show again (a write that failed): at once, or at the end of the animation running now. */
  release(): void
  state(): ModeSwitchState
  subscribe(listener: () => void): () => void
  setPainter(painter: ModeSwitchPainter | null): void
}

/** The tick a skipping tap gets once the running vibration is cancelled. */
export const SKIP_TICK_MS = 40

/** How long a skipping tap's click is held back, at most. */
const SWALLOW_MS = 1000

const DEFAULT_DEPS: ModeSwitchDeps = {
  now: () => performance.now(),
  raf: (cb) => requestAnimationFrame(cb),
  caf: (id) => cancelAnimationFrame(id),
  motionOff: () => document.documentElement.getAttribute('data-motion') === 'off',
  feedback: {
    buzz,
    unlock: unlockSound,
    play: playSound,
    follow: followSound,
    stop: stopSound,
  },
}

type Run = {
  kind: ModeSwitchKind
  to: TrainingMode
  t: number
  /** When the tap came (`deps.now()`): the clock is the time since, as the sound and the vibration run from it. */
  tap: number
  raf: number
  swapped: boolean
  moment: boolean
  beats: boolean[]
  /** The switch's row: a press inside it is a flip, not a skip. */
  row: Element | null
  source: Element | null
  /** The write failed: the stored side shows again at the end. */
  released: boolean
  /** The painter has built the shapes (in the first frame). */
  begun: boolean
}

const target = (kind: ModeSwitchKind): TrainingMode => (kind === 'drop' ? 'POWERLIFTING' : 'BODYBUILDING')
const other = (mode: TrainingMode): TrainingMode => (mode === 'POWERLIFTING' ? 'BODYBUILDING' : 'POWERLIFTING')

export function createModeSwitch(overrides: Partial<ModeSwitchDeps> = {}): ModeSwitchController {
  const deps = { ...DEFAULT_DEPS, ...overrides }
  const fb = deps.feedback
  const listeners = new Set<() => void>()
  let snapshot: ModeSwitchState = { kind: null, shown: null }
  let run: Run | null = null
  let painter: ModeSwitchPainter | null = null
  let stored: TrainingMode | null = null
  let swallowUntil = 0
  /** A press on the switch's row during the reveal, which the browser gives to <html>: its click goes to the switch. */
  let forwardTo: HTMLElement | null = null
  let listening = false
  /** A sound and buzz started with motion off, which leaving or hiding the page still stops. */
  let playingFree = false

  function set(next: ModeSwitchState) {
    if (next.kind === snapshot.kind && next.shown === snapshot.shown) return
    snapshot = next
    listeners.forEach((l) => l())
  }

  /**
   * Puts `mode` on now: the page's look at once, React's content in the same frame (`sync`). From inside React's own
   * effects (the stored side arriving, Settings closing) it is not flushed: React is already committing there, would
   * warn, and draws it straight after anyway.
   */
  function show(mode: TrainingMode | null, kind: ModeSwitchKind | null, sync = true) {
    applyTrainingMode(mode ?? stored ?? 'POWERLIFTING')
    if (!sync) return set({ kind, shown: mode })
    try {
      flushSync(() => set({ kind, shown: mode }))
    } catch {
      set({ kind, shown: mode })
    }
  }

  function swapFor(r: Run) {
    return () => {
      // A reveal that ran late, for a run that has already ended, does nothing.
      if (run !== r || r.swapped) return false
      r.swapped = true
      show(r.to, r.kind)
      return true
    }
  }

  function moment(r: Run) {
    r.moment = true
    const swap = swapFor(r)
    if (painter != null) painter.reveal(swap)
    else swap()
  }

  function step(r: Run) {
    if (r.kind === 'drop') {
      const drop = grit.modeSwitchDropTimeline()
      if (!r.moment && r.t >= drop.impact) {
        moment(r)
        fb.buzz(grit.modeSwitchVibration().dropPattern)
      }
      if (r.t >= drop.end) return finish('end')
    } else {
      const pump = grit.modeSwitchPumpTimeline()
      const pulses = grit.modeSwitchVibration().pumpPulses
      for (let i = 0; i < 3; i++) {
        if (!r.beats[i] && r.t >= pump.beats[i]!) {
          r.beats[i] = true
          fb.buzz(pulses[i]!)
          if (i === 2) moment(r)
        }
      }
      if (r.t >= pump.end) return finish('end')
    }
    if (run !== r) return
    painter?.paint(r.t)
    fb.follow(r.t)
  }

  function frame() {
    const r = run
    if (r == null) return
    // The time since the tap, on the clock the tap was read on (not the frame's timestamp: the two can start from
    // different points), with no cap: a late frame jumps ahead, as the sound and the vibration have gone on.
    r.t = Math.max(r.t, deps.now() - r.tap)
    if (!r.begun) {
      r.begun = true
      painter?.begin(r.kind, r.source)
    }
    step(r)
    if (run === r) r.raf = deps.raf(frame)
  }

  function finish(reason: FinishReason) {
    const r = run
    if (r == null) {
      // With motion off there is no run, but the sound and the buzz may still be going.
      if (playingFree && (reason === 'leave' || reason === 'hidden')) {
        playingFree = false
        fb.stop(0.04)
        fb.buzz(0)
      }
      return
    }
    run = null
    deps.caf(r.raf)
    if (reason !== 'end') fb.stop(0.04)
    if (reason === 'skip') {
      // As the reference: the running vibration is cancelled, then one short tick answers the tap.
      fb.buzz(0)
      fb.buzz(SKIP_TICK_MS)
    } else if (reason !== 'end') fb.buzz(0)
    painter?.end()
    // The new side is on from here; the stored side takes over once it says the same (a write may still be on its way).
    const keep = stored === r.to || r.released ? null : r.to
    // Settings closing ends it from its effect's clean-up: React is committing then.
    show(keep, null, reason !== 'leave')
  }

  function onPointerDown(event: PointerEvent) {
    const r = run
    // A new press: whatever click the last skip held back has come or will not come now.
    swallowUntil = 0
    forwardTo = null
    if (r == null) return
    if (r.row != null) {
      const box = r.row.getBoundingClientRect()
      if (event.clientX >= box.left && event.clientX <= box.right && event.clientY >= box.top && event.clientY <= box.bottom) {
        forwardTo = event.target === document.documentElement && r.source instanceof HTMLElement ? r.source : null
        return
      }
    }
    finish('skip')
    swallowUntil = deps.now() + SWALLOW_MS
  }

  function onClick(event: MouseEvent) {
    const forward = forwardTo
    forwardTo = null
    if (forward != null && event.target === document.documentElement) {
      event.stopImmediatePropagation()
      event.preventDefault()
      forward.click()
      return
    }
    if (swallowUntil === 0) return
    const swallow = deps.now() < swallowUntil
    swallowUntil = 0
    if (!swallow) return
    event.stopImmediatePropagation()
    event.preventDefault()
  }

  function onVisibility() {
    if (document.visibilityState === 'hidden') finish('hidden')
  }

  /** Listened for from the first switch on, never at app start; idle, each returns at once. */
  function listen() {
    if (listening || typeof document === 'undefined') return
    listening = true
    document.addEventListener('pointerdown', onPointerDown, { capture: true })
    document.addEventListener('click', onClick, { capture: true })
    document.addEventListener('visibilitychange', onVisibility)
  }

  function flip(kind: ModeSwitchKind, source: Element | null) {
    finish('flip')
    fb.unlock()
    const to = target(kind)
    if (deps.motionOff()) {
      listen()
      playingFree = true
      // The look follows the stored side as it always has, once the write lands (a moment later).
      if (snapshot.shown != null) show(null, null)
      fb.play(kind, null)
      fb.buzz(kind === 'drop' ? dropBuzzWithoutMotion() : pumpBuzzWithoutMotion())
      return
    }
    playingFree = false
    listen()
    const r: Run = {
      kind,
      to,
      t: 0,
      tap: deps.now(),
      raf: 0,
      swapped: false,
      moment: false,
      beats: [false, false, false],
      row: source?.closest('label, .settings-row') ?? source,
      source,
      released: false,
      begun: false,
    }
    run = r
    set({ kind, shown: other(to) })
    applyTrainingMode(other(to))
    fb.play(kind, () => (run === r ? r.t : null))
    // The shapes are built and drawn in the very next frame, not in the tap: the tap stays short, and what it
    // changed shows in that same frame. (Nothing of either animation shows at t = 0 anyway.)
    r.raf = deps.raf(frame)
  }

  return {
    flip,
    finish,
    noteStored(mode) {
      stored = mode
      // From the roots' effect (useShownMode): React is committing then.
      if (run == null && snapshot.shown != null && snapshot.shown === mode) show(null, null, false)
    },
    release() {
      if (run != null) run.released = true
      else if (snapshot.shown != null) show(null, null)
    },
    state: () => snapshot,
    subscribe(listener) {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    setPainter(next) {
      painter = next
    },
  }
}

/** The app's one controller. */
export const modeSwitch = createModeSwitch()

/** The controller below (tests give their own). */
export const ModeSwitchContext = createContext<ModeSwitchController>(modeSwitch)

export function useModeSwitch(): ModeSwitchController {
  return useContext(ModeSwitchContext)
}

/** The controller's state, live. */
export function useModeSwitchState(controller: ModeSwitchController = modeSwitch): ModeSwitchState {
  return useSyncExternalStore(controller.subscribe, controller.state, controller.state)
}

/**
 * The side an animation holds on screen, or null when none does. Only a change of it redraws: the app's root reads
 * it, and a tap must not redraw the whole app for nothing.
 */
export function useHeldMode(controller: ModeSwitchController = modeSwitch): TrainingMode | null {
  const held = () => controller.state().shown
  return useSyncExternalStore(controller.subscribe, held, held)
}

/**
 * The side the app shows: the stored side, except while a mode switch animation holds the old one (until its
 * moment) or the new one (until the stored side says the same).
 */
export function useShownMode(stored: TrainingMode, controller: ModeSwitchController = modeSwitch): TrainingMode {
  const shown = useHeldMode(controller)
  useEffect(() => controller.noteStored(stored), [controller, stored])
  return shown ?? stored
}

/** The screen the animations are drawn on, in CSS px: the layout viewport, as the overlay (fixed, inset 0) covers it. */
export function screenSize(): { w: number; h: number } {
  const el = document.documentElement
  return { w: el.clientWidth || window.innerWidth, h: el.clientHeight || window.innerHeight }
}

/** The centre of the switch's track on the screen, wherever Settings is scrolled; null if it cannot be found. */
export function switchCentre(source: Element | null): { x: number; y: number } | null {
  const track = source?.querySelector('.gr-switch-track') ?? source
  const box = track?.getBoundingClientRect()
  return box != null && box.width > 0 ? { x: box.left + box.width / 2, y: box.top + box.height / 2 } : null
}

/**
 * Works the shapes out ahead (the drop's cracks, dust and sparks; the pump's veins from the switch as it sits now),
 * so that the tap only builds them. The shared model keeps the last set; the first set takes a couple of frames.
 */
export function warmModeSwitchShapes(source: Element | null): void {
  // Marked, so the speed check can tell a cold first frame from a warm one.
  performance.mark?.('ms-warm')
  const { w, h } = screenSize()
  const hit = grit.modeSwitchHitPoint(w, h)
  grit.modeSwitchCracks(w, h, hit.x, hit.y)
  const src = switchCentre(source) ?? hit
  grit.modeSwitchVeins(w, h, src.x, src.y)
  grit.modeSwitchVeinNodes(w, h, src.x, src.y)
}

/** How long Settings must have been still after a scroll before the shapes are worked out again. */
const SCROLL_IDLE_MS = 150

type IdleWindow = Window & {
  requestIdleCallback?: (cb: () => void, o?: { timeout: number }) => number
  cancelIdleCallback?: (id: number) => void
}

/**
 * For Settings, which holds the switch: the shapes are worked out ahead when the browser is idle (never at app start),
 * so a tap draws its first frame at once. The shared model keeps one set for the switch where it sits, so they are
 * worked out again whenever that moves: once Settings is open, after each change of the side shown (its rows, and the
 * label above the title, move the switch) and once a scroll has come to rest. Never while an animation runs.
 * `side` is the side shown; `running` is whether an animation is running.
 */
export function useWarmModeSwitch(side: TrainingMode | null = null, running = false): void {
  useEffect(() => {
    if (running) return
    const w = window as IdleWindow
    let idleId: number | null = null
    let timerId: number | null = null
    const warm = () => {
      idleId = null
      try {
        warmModeSwitchShapes(document.querySelector('.settings-mode-switch'))
      } catch {
        // Only a head start: the tap works them out itself.
      }
    }
    const whenIdle = () => {
      if (idleId != null) return
      if (typeof w.requestIdleCallback === 'function') idleId = w.requestIdleCallback(warm, { timeout: 1000 })
      else idleId = window.setTimeout(warm, 300)
    }
    whenIdle()
    // A scroll moves the switch: once it has come to rest, again.
    const scroller = document.querySelector('.shell-main')
    const onScroll = () => {
      if (timerId != null) window.clearTimeout(timerId)
      timerId = window.setTimeout(() => {
        timerId = null
        whenIdle()
      }, SCROLL_IDLE_MS)
    }
    scroller?.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      scroller?.removeEventListener('scroll', onScroll)
      if (timerId != null) window.clearTimeout(timerId)
      if (idleId != null) {
        if (typeof w.cancelIdleCallback === 'function') w.cancelIdleCallback(idleId)
        window.clearTimeout(idleId)
      }
    }
  }, [side, running])
}

/**
 * For Settings, which holds the switch: once the stored side is known, the sounds are fetched ahead of the tap, the one
 * the next switch plays first (never at app start). `prefetch` is for tests.
 */
export function usePrefetchModeSwitchSounds(stored: TrainingMode | null, prefetch: (first: ModeSwitchKind) => void = prefetchSounds): void {
  const known = stored != null
  useEffect(() => {
    if (stored == null) return
    prefetch(stored === 'BODYBUILDING' ? 'drop' : 'pump')
    // Once per opening: what the next switch plays changes with each switch, and both are fetched by then.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [known])
}

/** For the screen that holds the switch: closing it ends a running animation at its end state. */
export function useEndModeSwitchOnLeave(controller: ModeSwitchController = modeSwitch): void {
  useEffect(() => () => controller.finish('leave'), [controller])
}
