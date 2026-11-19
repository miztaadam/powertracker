import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import type { TrainingMode } from '../../db/schema'
import { grit, type JsGuideScreen } from '../../domain'
import { cx } from '../../ui/components/format'
import { ArrowBackIcon, ArrowForwardIcon, CheckIcon } from '../../ui/components/icons'
import { Pill } from '../../ui/components/Pill'
import { GuidePicture } from './GuidePictures'
import './GuideScreen.css'

export { pictureDescription } from './GuidePictures'

/** How the guide was left. */
export type GuideExit = 'skipped' | 'done' | 'choose-template' | 'build-plan' | 'see-plan'

/** The guide's own control words and labels: the screens' copy comes from the domain (`guideScreen`). */
export const SKIP = 'Skip'
export const BACK = 'Back'
export const NEXT = 'Next'
export const DONE = 'Done'
export const EXAMPLE = 'Example'

/** What the row of dots says: the one screen shown of how many. */
export const dotsLabel = (index: number, count: number) => `Screen ${index + 1} of ${count}`

/** A horizontal move at least this long, and longer than it is tall, turns the page. */
const SWIPE_PX = 50

/** Which step a picture shows. Tests hold it (`heldGuideClock`); the guide runs on `tickingGuideClock`. */
export type GuideClock = {
  /** A hook: the step, from 0 to `count` - 1, of a picture that started when `playing` last became true; 0 while not. */
  useStep: (count: number, playing: boolean) => number
}

/** One step every `guideStepMs()`, from the first, holding on the last. */
export const tickingGuideClock: GuideClock = {
  useStep(count, playing) {
    const [shown, setShown] = useState({ count, playing, step: 0 })
    // Started afresh each time the picture comes on screen, and back at 0 at once when it leaves.
    const fresh = shown.count !== count || shown.playing !== playing
    if (fresh) setShown({ count, playing, step: 0 })
    useEffect(() => {
      if (!playing) return
      const stepMs = grit.guideStepMs()
      let step = 0
      let timer: ReturnType<typeof setTimeout> | undefined
      const tick = () => {
        step += 1
        setShown((s) => ({ ...s, step }))
        if (step < count - 1) timer = setTimeout(tick, stepMs)
      }
      if (count > 1) timer = setTimeout(tick, stepMs)
      return () => clearTimeout(timer)
    }, [count, playing])
    return fresh ? 0 : Math.min(shown.step, count - 1)
  },
}

/** A clock a test moves by hand: every picture shows step `held` (within its own steps). */
export function heldGuideClock(held: number): GuideClock {
  return { useStep: (count) => Math.max(0, Math.min(held, count - 1)) }
}

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
  return (
    <button type="button" className={cx('gr-btn label-large', `gr-btn-${kind}`, kind === 'quiet' && 'gr-btn-compact', className)} disabled={!enabled} onClick={onClick} data-testid={testId}>
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
function GuidePage({ id, mode, motionOn, clock }: { id: string; mode: TrainingMode; motionOn: boolean; clock: GuideClock }) {
  const screen: JsGuideScreen = useMemo(() => grit.guideScreen(id, mode), [id, mode])
  const steps = useMemo(() => grit.guideSteps(id, mode), [id, mode])
  const ticked = clock.useStep(steps.length, motionOn)
  const step = steps.length === 0 ? null : (motionOn ? steps[ticked] : steps[steps.length - 1]) ?? null
  return (
    <div className="guide-page">
      {/* The text wraps and is never cut: past its share of the height it scrolls, and the picture takes the rest. */}
      <div className="guide-text">
        <h1 className="guide-title headline-medium" data-testid="guide-title">
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
        <GuidePicture id={id} mode={mode} step={step} />
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
    <section className="guide" data-testid="guide" aria-roledescription="guide">
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
        <GuidePage key={id} id={id} mode={mode} motionOn={motionOn} clock={clock} />
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
