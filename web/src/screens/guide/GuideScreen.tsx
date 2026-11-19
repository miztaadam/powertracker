import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { TrainingMode } from '../../db/schema'
import { grit, type JsGuideScreen } from '../../domain'
import { cx } from '../../ui/components/format'
import { ArrowBackIcon, ArrowForwardIcon, CheckIcon } from '../../ui/components/icons'
import { Pill } from '../../ui/components/Pill'
import { GuidePicture } from './GuidePictures'
import { BACK, DONE, EXAMPLE, GUIDE_LABEL, NEXT, SKIP, dotsLabel } from './guideLabels'
import './GuideScreen.css'

export { BACK, DONE, EXAMPLE, GUIDE_LABEL, NEXT, SKIP, dotsLabel, pictureDescription } from './guideLabels'

/** How the guide was left. */
export type GuideExit = 'skipped' | 'done' | 'choose-template' | 'build-plan' | 'see-plan'

/** A horizontal move at least this long, and longer than it is tall, turns the page. */
const SWIPE_PX = 50

/**
 * A picture's time: how many milliseconds into its steps it is (`guideFrame` turns that into a step and its move's
 * progress). Tests hold it (`heldGuideClock`); the guide runs on `tickingGuideClock`.
 */
export type GuideClock = {
  /**
   * A hook: milliseconds into a picture of `steps` (`guideSteps` names, the same array while they are the same), from 0
   * when it mounted; standing still while not `playing`.
   */
  useElapsed: (steps: readonly string[], playing: boolean) => number
}

/** The step whose motion (its tap ring, then its move) a picture is in or past at `t` ms; `starts` from `guideMotionStartMs`. */
const motionAt = (starts: readonly number[], t: number) => {
  let index = 0
  while (index < starts.length - 1 && starts[index + 1]! <= t) index += 1
  return index
}

/**
 * Real time, one value a frame while a step moves or a tap ring plays before it (requestAnimationFrame) and one timer
 * while it holds, so nothing is rendered; nothing at all once the last step has settled. It stands still while the page
 * is hidden and goes on from there when it is shown again.
 */
export const tickingGuideClock: GuideClock = {
  useElapsed(steps, playing) {
    const [shown, setShown] = useState(0)
    const at = useRef(0)
    const starts = useMemo(() => steps.map((_, i) => grit.guideMotionStartMs([...steps], i)), [steps])
    useEffect(() => {
      if (!playing) return
      const done = grit.guideDoneMs(starts.length)
      let frame: number | undefined
      let timer: ReturnType<typeof setTimeout> | undefined
      const set = (t: number) => {
        at.current = t
        setShown(t)
      }
      const stop = () => {
        if (frame !== undefined) cancelAnimationFrame(frame)
        clearTimeout(timer)
        frame = undefined
        timer = undefined
      }
      // The picture's time is the page's time less this: set when the clock (re)starts, so it never drifts.
      let base = 0
      const run = () => {
        const t = at.current
        if (t >= done) return
        const index = motionAt(starts, t)
        const settled = grit.guideSettledMs(index)
        if (t < settled) {
          // A move, or a tap ring and the move after it: one value a frame.
          const tick = () => {
            const now = Math.min(settled, performance.now() - base)
            set(now)
            if (now < settled) frame = requestAnimationFrame(tick)
            else run()
          }
          frame = requestAnimationFrame(tick)
        } else {
          // A hold: nothing changes until the next step's motion starts.
          const next = starts[index + 1]!
          timer = setTimeout(
            () => {
              set(next)
              run()
            },
            Math.max(0, next - (performance.now() - base)),
          )
        }
      }
      const start = () => {
        base = performance.now() - at.current
        run()
      }
      const onVisibility = () => {
        stop()
        if (!document.hidden) start()
      }
      document.addEventListener('visibilitychange', onVisibility)
      if (!document.hidden) start()
      return () => {
        stop()
        document.removeEventListener('visibilitychange', onVisibility)
      }
    }, [starts, playing])
    return shown
  },
}

/** A clock a test moves by hand: every picture is `ms` into its steps (held within them). */
export function heldGuideClock(ms: number): GuideClock {
  return { useElapsed: (steps) => Math.max(0, Math.min(ms, grit.guideDoneMs(steps.length))) }
}

/** A held clock with every picture at step `index`, settled. */
export const heldGuideClockAtStep = (index: number) => heldGuideClock(grit.guideSettledMs(index))

export type GuideScreenProps = {
  mode: TrainingMode
  firstLaunch: boolean
  hasPlan: boolean
  motionOn: boolean
  onPickSide: (mode: TrainingMode) => void
  onClose: (exit: GuideExit) => void
  clock?: GuideClock
  /** Opens the guide on a later screen (tests and screenshots); a `guideScreens()` id. */
  startAt?: string
}

/** A button as the app's (ui/components/Button), with the guide's test id. */
function GuideButton({ text, onClick, kind, icon, enabled = true, testId, className }: { text: string; onClick: () => void; kind: 'primary' | 'secondary' | 'quiet'; icon?: ReactNode; enabled?: boolean; testId: string; className?: string }) {
  // Never `disabled`: a focused button that became disabled would drop the keyboard focus to the page. Off, it says
  // so (aria-disabled) and does nothing.
  return (
    <button
      type="button"
      className={cx('gr-btn label-large', `gr-btn-${kind}`, kind === 'quiet' && 'gr-btn-compact', 'guide-btn', className)}
      aria-disabled={!enabled}
      onClick={() => {
        if (enabled) onClick()
      }}
      data-testid={testId}
    >
      <span className="gr-btn-face">
        {icon != null && <span className="gr-btn-icon">{icon}</span>}
        <span className="gr-btn-text">{text}</span>
      </span>
    </button>
  )
}

/**
 * One screen: its title and lines from the domain, then its picture, which shrinks to leave the text its room. Keyed by
 * screen, so its picture starts from the first step each time it is shown; its steps are worked out only once shown.
 */
function GuidePage({ id, mode, motionOn, clock, focusTitle }: { id: string; mode: TrainingMode; motionOn: boolean; clock: GuideClock; focusTitle: boolean }) {
  const screen: JsGuideScreen = useMemo(() => grit.guideScreen(id, mode), [id, mode])
  const titleRef = useRef<HTMLHeadingElement>(null)
  // The first screen's title takes the focus as the guide opens (what opened it has gone); later screens leave it on
  // the control that turned the page, and the live region says the new title.
  useEffect(() => {
    if (focusTitle) titleRef.current?.focus()
  }, [focusTitle])
  const steps = useMemo(() => grit.guideSteps(id, mode), [id, mode])
  const elapsed = clock.useElapsed(steps, motionOn)
  // With motion off: the last step, still.
  const t = motionOn ? Math.floor(elapsed) : grit.guideDoneMs(steps.length)
  const frame = useMemo(() => (steps.length === 0 ? null : grit.guideFrame(id, mode, t)), [id, mode, t, steps.length])
  // The control the next step stands for pressing, if any: its place is read as this step comes in, for its ring.
  const next = frame == null ? undefined : steps[frame.stepIndex + 1]
  const nextTap = next == null ? null : (grit.guideTap(next) ?? null)
  return (
    <div className="guide-page">
      {/* The text wraps and is never cut: past its share of the height it scrolls, and the picture takes the rest. */}
      <div className="guide-text">
        <h1 className="guide-title headline-medium" data-testid="guide-title" tabIndex={-1} ref={titleRef}>
          {screen.title}
        </h1>
        {screen.lines.map((line) => (
          <p key={line.text} className="guide-line body-large">
            {line.text}
          </p>
        ))}
      </div>
      <div className="guide-art">
        {screen.example && (
          <span className="guide-example" data-testid="guide-example">
            <Pill text={EXAMPLE} />
          </span>
        )}
        <GuidePicture id={id} mode={mode} frame={frame} moveMs={frame == null ? 0 : t - grit.guideStepStartMs(frame.stepIndex)} motionOn={motionOn} nextTap={nextTap} />
      </div>
    </div>
  )
}

/**
 * The guide (1.16): the domain's screens one at a time, a row of dots, Skip at the top right, Back and Next at the
 * bottom; a swipe or Escape (the browser's Back leaves the route) works too. `mode` is the side whose copy and colours
 * it shows. With `motionOn` false every picture shows its last step, still. Writes nothing: what an exit does is the
 * caller's (`onClose`). `firstLaunch`, `hasPlan` and `onPickSide` are for screens 2 and 8 (Task 3). Only the screen
 * shown is worked out: the sample's numbers for a later screen are computed when it is reached.
 */
export function GuideScreen({ mode, motionOn, onClose, clock = tickingGuideClock, startAt = 'WELCOME' }: GuideScreenProps) {
  const screens = useMemo(() => grit.guideScreens(), [])
  // Kept across a resize or rotation: the page is state, not layout.
  const [index, setIndex] = useState(() => Math.max(0, screens.indexOf(startAt)))
  const id = screens[index]!
  const last = index === screens.length - 1
  // Only the screen first shown has its title take the focus, once.
  const opening = useRef(true)
  useEffect(() => {
    opening.current = false
  }, [])
  const shownTitle = useMemo(() => grit.guideScreen(id, mode).title, [id, mode])

  const close = useRef(onClose)
  close.current = onClose
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close.current('skipped')
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  const go = (to: number) => setIndex(Math.max(0, Math.min(screens.length - 1, to)))
  const start = useRef<{ x: number; y: number } | null>(null)

  return (
    <section className="guide" data-testid="guide" aria-label={GUIDE_LABEL}>
      {/* Outside the keyed page, so a page change is a change of its text: read out politely. */}
      <p className="sr-only" aria-live="polite" data-testid="guide-announce">
        {shownTitle}
      </p>
      <div className="guide-top">
        <GuideButton text={SKIP} kind="quiet" onClick={() => onClose('skipped')} testId="guide-skip" />
      </div>
      <div
        className="guide-pages"
        data-testid="guide-pages"
        onPointerDown={(e) => {
          start.current = { x: e.clientX, y: e.clientY }
        }}
        onPointerUp={(e) => {
          const from = start.current
          start.current = null
          if (from == null) return
          const dx = e.clientX - from.x
          if (Math.abs(dx) >= SWIPE_PX && Math.abs(dx) > Math.abs(e.clientY - from.y)) go(index + (dx < 0 ? 1 : -1))
        }}
        onPointerCancel={() => {
          start.current = null
        }}
      >
        {/* Keyed by screen: a new page starts its picture from the first step, and slides in with motion on. */}
        <GuidePage key={id} id={id} mode={mode} motionOn={motionOn} clock={clock} focusTitle={opening.current} />
      </div>
      <div className="guide-dots" data-testid="guide-dots" role="img" aria-label={dotsLabel(index, screens.length)}>
        {screens.map((s, i) => (
          <span key={s} className={cx('guide-dot', i === index && 'guide-dot-current')} />
        ))}
      </div>
      <div className="guide-controls">
        <GuideButton text={BACK} kind="secondary" icon={<ArrowBackIcon />} onClick={() => go(index - 1)} enabled={index > 0} testId="guide-back" />
        {/* Screen 8's own buttons come with the first launch (Task 3); until then its Next is Done. */}
        <GuideButton
          text={last ? DONE : NEXT}
          kind="primary"
          icon={last ? <CheckIcon /> : <ArrowForwardIcon />}
          onClick={() => (last ? onClose('done') : go(index + 1))}
          testId="guide-next"
          className="guide-next"
        />
      </div>
    </section>
  )
}

export default GuideScreen
