import { useCallback, useEffect, useId, useMemo, useRef, useState, type Dispatch, type ReactNode, type RefObject, type SetStateAction } from 'react'
import type { TrainingMode } from '../../db/schema'
import { grit, type JsGuideScreen } from '../../domain'
import { cx } from '../../ui/components/format'
import { ArrowBackIcon, ArrowForwardIcon, CheckIcon } from '../../ui/components/icons'
import { Pill } from '../../ui/components/Pill'
import { GuidePicture, isOutlined } from './GuidePictures'
import { BACK, EXAMPLE, GUIDE_LABEL, NEXT, SKIP, dotsLabel, lastButtons, sideName } from './guideLabels'
import { outlineArt, type OutlineArt } from './outlineArt'
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
  /**
   * A held time for a picture with a loop of its own (screen 2, whose one step settles at once): it draws that moment
   * and runs nothing. Absent on the real clock: the picture runs its own loop.
   */
  heldLoopMs?: number
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
  return { useElapsed: (steps) => Math.max(0, Math.min(ms, grit.guideDoneMs(steps.length))), heldLoopMs: ms }
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
function GuideButton({ text, onClick, kind, icon, enabled = true, testId, className }: { text: string; onClick: () => void; kind: 'primary' | 'secondary' | 'quiet'; icon?: ReactNode; enabled?: boolean; testId?: string; className?: string }) {
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

/** Screen 2 on a first launch: the two sides, the one on drawn as chosen. A tap sets the real side (the caller's). */
function SideButtons({ mode, onPick }: { mode: TrainingMode; onPick: (mode: TrainingMode) => void }) {
  return (
    <div className="guide-sides" role="group" aria-label="Your side">
      {(['POWERLIFTING', 'BODYBUILDING'] as const).map((side) => (
        <button
          key={side}
          type="button"
          className={cx('gr-btn label-large', side === mode ? 'gr-btn-primary' : 'gr-btn-secondary', 'guide-btn guide-side')}
          aria-pressed={side === mode}
          onClick={() => onPick(side)}
          data-testid={`guide-side-${side.toLowerCase()}`}
        >
          <span className="gr-btn-face">
            {side === mode && (
              <span className="gr-btn-icon">
                <CheckIcon />
              </span>
            )}
            <span className="gr-btn-text">{sideName(side)}</span>
          </span>
        </button>
      ))}
    </div>
  )
}

/**
 * Whether the text box scrolls (its words taller than its box): read when the box or its words change size (a resize,
 * a font loading late), never per step. False where nothing can observe a size (jsdom).
 */
function useScrolls(): [(el: HTMLDivElement | null) => void, boolean] {
  const [scrolls, setScrolls] = useState(false)
  const observer = useRef<ResizeObserver | null>(null)
  const ref = useCallback((el: HTMLDivElement | null) => {
    observer.current?.disconnect()
    observer.current = null
    if (el == null || typeof ResizeObserver === 'undefined') return
    const check = () => setScrolls(el.scrollHeight > el.clientHeight + 1)
    const watch = new ResizeObserver(check)
    watch.observe(el)
    for (const child of el.children) watch.observe(child)
    observer.current = watch
  }, [])
  return [ref, scrolls]
}

/**
 * One screen: its title and lines from the domain, then its picture, which shrinks to leave the text its room. Keyed by
 * screen, so its picture starts from the first step each time it is shown; its steps are worked out only once shown.
 */
function GuidePage({ id, mode, motionOn, clock, focusTitle, below, artHeight }: { id: string; mode: TrainingMode; motionOn: boolean; clock: GuideClock; focusTitle: boolean; below?: ReactNode; artHeight: number | null }) {
  const outlined = isOutlined(id)
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
  const titleId = useId()
  const [textRef, scrolls] = useScrolls()
  return (
    <div className={cx('guide-page', outlined && artHeight != null && 'guide-page-outlined')}>
      {/*
       * The text wraps and is never cut: past its share of the height it scrolls, and the picture takes the rest. While
       * it scrolls it is a named region in the tab order, so a keyboard can scroll it and a screen reader finds it.
       */}
      <div
        className="guide-text"
        data-testid="guide-text"
        ref={textRef}
        tabIndex={scrolls ? 0 : undefined}
        role={scrolls ? 'region' : undefined}
        aria-labelledby={scrolls ? titleId : undefined}
      >
        <h1 className="guide-title headline-medium" data-testid="guide-title" tabIndex={-1} ref={titleRef} id={titleId}>
          {screen.title}
        </h1>
        {screen.lines.map((line) => (
          <p key={line.text} className="guide-line body-large">
            {line.text}
          </p>
        ))}
      </div>
      {/* On the outlined screens the picture's place is one height (spec point 11): the text above takes the rest, and scrolls. */}
      <div className="guide-art" style={outlined && artHeight != null ? { height: `${artHeight}px` } : undefined}>
        {screen.example && (
          <span className="guide-example" data-testid="guide-example">
            <Pill text={EXAMPLE} />
          </span>
        )}
        {/* An outlined screen with no label keeps its place, unseen, so its phone is the size of the others'. */}
        {!screen.example && outlined && (
          <span className="guide-example guide-example-unseen" aria-hidden="true">
            <Pill text={EXAMPLE} />
          </span>
        )}
        <GuidePicture id={id} mode={mode} frame={frame} moveMs={frame == null ? 0 : t - grit.guideStepStartMs(frame.stepIndex)} motionOn={motionOn} nextTap={nextTap} heldLoopMs={clock.heldLoopMs} />
        {below}
      </div>
    </div>
  )
}

/** The screens whose picture is a drawn phone in its outline, worked out once. */
const OUTLINED = (screens: readonly string[]) => screens.filter(isOutlined)

/** React 18 has no `inert` prop: the attribute is set on the element itself. */
const makeInert = (el: HTMLDivElement | null) => el?.setAttribute('inert', '')

/**
 * What the outlined screens leave their picture (spec point 11, `outlineArt`), worked out from what is laid out, never
 * from which page is current: the pages and the live controls together (the same on every page), less the tallest text
 * of the outlined screens on either side, less the controls: one row (Back, Next) on the first seven screens, and screen
 * 8's own (its first button's words, which may wrap, and its second button, if any) on the last, so screen 8 gives up
 * only what its controls really take. Unseen copies of those texts, of both sets of controls and of the picture's place
 * are laid out here as the pages lay them out; read when a size changes (a resize, a rotation, the fonts loading), never
 * per step. Null until first measured, and where nothing can observe a size (jsdom): the pictures then simply take what
 * their text leaves.
 */
function OutlineMeasure({ screens, controls, lastPrimary, lastSecondary, onArt }: { screens: readonly string[]; controls: RefObject<HTMLDivElement | null>; lastPrimary: string; lastSecondary: string | null; onArt: Dispatch<SetStateAction<OutlineArt | null>> }) {
  const host = useRef<HTMLDivElement | null>(null)
  const setHost = useCallback((el: HTMLDivElement | null) => {
    host.current = el
    makeInert(el)
  }, [])
  const texts = useMemo(() => (['POWERLIFTING', 'BODYBUILDING'] as const).flatMap((side) => OUTLINED(screens).map((id) => grit.guideScreen(id, side))), [screens])
  useEffect(() => {
    const el = host.current
    const live = controls.current
    if (el == null || live == null || typeof ResizeObserver === 'undefined') return
    const height = (selector: string) => el.querySelector<HTMLElement>(selector)?.offsetHeight ?? 0
    const measure = () => {
      const tallest = Math.max(0, ...[...el.querySelectorAll<HTMLElement>('.guide-measure-text')].map((t) => t.offsetHeight))
      const art = outlineArt({
        budget: el.clientHeight + live.offsetHeight,
        tallest,
        standardControls: height('.guide-measure-controls-standard'),
        lastControls: height('.guide-measure-controls-last'),
        chrome: height('.guide-measure-art'),
      })
      // The same values keep the same state: no new render while nothing has changed.
      onArt((before) => (before != null && before.shared === art.shared && before.last === art.last ? before : art))
    }
    const observer = new ResizeObserver(measure)
    observer.observe(el)
    observer.observe(live)
    // The copies too: a font that loads late changes their size, not the host's.
    for (const copy of el.querySelectorAll('.guide-measure-text, .guide-measure-controls, .guide-measure-art')) observer.observe(copy)
    let gone = false
    const fonts = typeof document !== 'undefined' ? document.fonts : undefined
    fonts?.ready.then(() => {
      if (!gone) measure()
    })
    fonts?.addEventListener?.('loadingdone', measure)
    return () => {
      gone = true
      observer.disconnect()
      fonts?.removeEventListener?.('loadingdone', measure)
    }
  }, [onArt, controls, lastPrimary, lastSecondary])
  return (
    <div className="guide-measure" ref={setHost} aria-hidden="true">
      {texts.map((screen, i) => (
        <div key={i} className="guide-measure-page">
          {/* Their own class names, so nothing that looks for the page's text finds these copies. */}
          <div className="guide-measure-text">
            <h1 className="guide-measure-title headline-medium">{screen.title}</h1>
            {screen.lines.map((line) => (
              <p key={line.text} className="guide-measure-line body-large">
                {line.text}
              </p>
            ))}
          </div>
        </div>
      ))}
      {/* Both sets of controls, as the guide lays them out: the first seven screens', and screen 8's. */}
      <div className="guide-measure-controls guide-measure-controls-standard">
        <GuideButton text={BACK} kind="secondary" icon={<ArrowBackIcon />} onClick={() => {}} />
        <GuideButton text={NEXT} kind="primary" icon={<ArrowForwardIcon />} onClick={() => {}} className="guide-next" />
      </div>
      <div className="guide-measure-controls guide-measure-controls-last">
        <GuideButton text={BACK} kind="secondary" icon={<ArrowBackIcon />} onClick={() => {}} />
        <GuideButton text={lastPrimary} kind="primary" icon={<CheckIcon />} onClick={() => {}} className="guide-next" />
        {lastSecondary != null && <GuideButton text={lastSecondary} kind="secondary" onClick={() => {}} className="guide-wide" />}
      </div>
      {/* The picture's place with no phone in it: its padding, and the Example label with its gap. */}
      <div className="guide-measure-art">
        <span className="guide-example">
          <Pill text={EXAMPLE} />
        </span>
        <div />
      </div>
    </div>
  )
}

/**
 * The guide (1.16): the domain's screens one at a time, a row of dots, Skip at the top right, Back and Next at the
 * bottom; a swipe or Escape (the browser's Back leaves the route) works too. `mode` is the side whose copy and colours
 * it shows. With `motionOn` false every picture shows its last step, still. Writes nothing: what an exit and a side pick
 * do is the caller's (`onClose`, `onPickSide`). Only on a `firstLaunch` does screen 2 offer the two sides; picking the
 * side already on calls nothing. Screen 8 offers what `lastButtons` gives for `hasPlan`. Only the screen shown is worked
 * out: the sample's numbers for a later screen are computed when it is reached.
 */
export function GuideScreen({ mode, firstLaunch, hasPlan, motionOn, onPickSide, onClose, clock = tickingGuideClock, startAt = 'WELCOME' }: GuideScreenProps) {
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

  const buttons = lastButtons(mode, hasPlan)
  const [art, setArt] = useState<OutlineArt | null>(null)
  const controls = useRef<HTMLDivElement>(null)
  // Screen 8 alone gives up what its taller controls take; every other outlined screen keeps the shared height.
  const artHeight = art == null ? null : last ? art.last : art.shared
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
        <OutlineMeasure screens={screens} controls={controls} lastPrimary={buttons.primary[0]} lastSecondary={buttons.secondary?.[0] ?? null} onArt={setArt} />
        {/* Keyed by screen: a new page starts its picture from the first step, and slides in with motion on. */}
        <GuidePage
          key={id}
          id={id}
          mode={mode}
          motionOn={motionOn}
          clock={clock}
          focusTitle={opening.current}
          artHeight={artHeight}
          below={firstLaunch && id === 'SIDE' ? <SideButtons mode={mode} onPick={(side) => side !== mode && onPickSide(side)} /> : undefined}
        />
      </div>
      <div className="guide-dots" data-testid="guide-dots" role="img" aria-label={dotsLabel(index, screens.length)}>
        {screens.map((s, i) => (
          <span key={s} className={cx('guide-dot', i === index && 'guide-dot-current')} />
        ))}
      </div>
      <div className="guide-controls" ref={controls}>
        <GuideButton text={BACK} kind="secondary" icon={<ArrowBackIcon />} onClick={() => go(index - 1)} enabled={index > 0} testId="guide-back" />
        {/* On screen 8, Next becomes its first button: Choose a template, or Done with a plan. */}
        <GuideButton
          text={last ? buttons.primary[0] : NEXT}
          kind="primary"
          icon={last ? <CheckIcon /> : <ArrowForwardIcon />}
          onClick={() => (last ? onClose(buttons.primary[1]) : go(index + 1))}
          testId="guide-next"
          className="guide-next"
        />
        {/* Below them, the full width: Build my plan (bodybuilding, no plan), or See my plan. */}
        {last && buttons.secondary != null && (
          <GuideButton text={buttons.secondary[0]} kind="secondary" onClick={() => onClose(buttons.secondary![1])} testId="guide-last-secondary" className="guide-wide" />
        )}
      </div>
    </section>
  )
}

export default GuideScreen
