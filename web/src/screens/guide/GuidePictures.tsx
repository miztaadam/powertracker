import { Component, useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode, type RefObject } from 'react'
import type { TrainingMode } from '../../db/schema'
import { grit, type JsGuideFrame } from '../../domain'
import { FLIP_EASING, planFlip, readBoxes, startFlip, startRing, tapTarget, tickDots, type FlipBox, type FlipRun, type FlipTarget, type RingRun } from './guideFlip'
import { PHONE_WIDTH, pictureDescription } from './guideLabels'
import type { PictureDef, PictureTap } from './pictures/picture'
import { HOLDS_BACK_PICTURE } from './pictures/HoldsBackPicture'
import { MONTHS_LATER_PICTURE } from './pictures/MonthsLaterPicture'
import { PLAN_PICTURE } from './pictures/PlanPicture'
import { SIDE_PICTURE } from './pictures/SidePicture'
import { TARGETS_PICTURE } from './pictures/TargetsPicture'
import { TODAY_PICTURE } from './pictures/TodayPicture'
import { WELCOME_PICTURE } from './pictures/WelcomePicture'
import { YOUR_TURN_PICTURE } from './pictures/YourTurnPicture'

/** Each screen's picture, by `guideScreens()` id. A screen without one shows the empty frame. */
const PICTURES: Partial<Record<string, PictureDef>> = {
  WELCOME: WELCOME_PICTURE,
  SIDE: SIDE_PICTURE,
  PLAN: PLAN_PICTURE,
  TODAY: TODAY_PICTURE,
  TARGETS: TARGETS_PICTURE,
  HOLDS_BACK: HOLDS_BACK_PICTURE,
  MONTHS_LATER: MONTHS_LATER_PICTURE,
  YOUR_TURN: YOUR_TURN_PICTURE,
}

/** Whether screen `id`'s picture is a drawn phone in its outline (spec point 11). */
export const isOutlined = (id: string) => PICTURES[id]?.outlined === true

type FlipProps = {
  /** The step shown: a change of it is a step change, the only time layout is read. */
  stepIndex: number
  /** How far into the step's move the guide's clock is, in ms (past the move: it has ended). */
  moveMs: number
  /** Whether the picture moves at all (motion off: it is drawn still, at its last step). */
  on: boolean
  /** The picture's rows the layout motion moves (none: it moves nothing), its taps' controls, and the rows that rise. */
  targets: ((screen: HTMLElement) => FlipTarget[]) | undefined
  taps: Record<string, PictureTap>
  rises: ((key: string) => boolean) | undefined
  scale: number
  phone: RefObject<HTMLDivElement | null>
  screen: RefObject<HTMLDivElement | null>
  ghosts: RefObject<HTMLDivElement | null>
  /** The control the next step stands for pressing (a `GuideTap` name), read as this step comes in; null if none. */
  nextTap: string | null
  /** The ring on it, in the last of this step's hold: the control, and how far into the ring the clock is, in ms. */
  tap: string | null
  tapMs: number
  children: ReactNode
}

type Before = { boxes: Map<string, FlipBox>; targets: FlipTarget[] }

/**
 * The guide's layout motion around a picture (guideFlip.ts): it reads the rows' boxes just before a step change is
 * committed (`getSnapshotBeforeUpdate`, while the old rows are still drawn) and just after it, and starts the moves
 * paused; then each render moves them to the guide's clock. Renders between step changes read no layout.
 */
class FlipLayer extends Component<FlipProps> {
  private run: FlipRun | null = null

  /** The next step's tap ring: where its control is (read as this step came in), and the ring once it plays. */
  private tapBox: FlipBox | null = null
  private ring: RingRun | null = null
  private tapInk: PictureTap['ink'] = 'accent-ink'

  private readonly timing = { moveMs: grit.guideMoveMs(), fadeMs: grit.guideFadeMs(), easing: FLIP_EASING }

  componentDidMount() {
    this.readTap()
    this.moveRing()
  }

  getSnapshotBeforeUpdate(prev: FlipProps): Before | null {
    if (prev.stepIndex === this.props.stepIndex) return null
    // A move still under way ends where it was going, so the boxes read are the rows' own; a ring under way ends.
    this.end()
    this.endRing()
    const { on, scale, phone, screen, targets: targetsOf } = this.props
    if (!on || targetsOf == null || scale <= 0 || phone.current == null || screen.current == null) return null
    const targets = targetsOf(screen.current)
    return { boxes: readBoxes(targets, phone.current, scale), targets }
  }

  componentDidUpdate(_prev: FlipProps, _state: unknown, before: Before | null) {
    const { scale, phone, screen, ghosts, moveMs, targets: targetsOf, rises } = this.props
    if (before != null && targetsOf != null && phone.current != null && screen.current != null) {
      const targets = targetsOf(screen.current)
      const after = readBoxes(targets, phone.current, scale)
      const plan = planFlip(before.boxes, after, new Map(targets.map((t) => [t.key, t.parent])))
      // A row gone from the step: the node it was drawn with, and where (a part of a gone row goes with it).
      const gone = new Map(
        before.targets
          .filter((t) => plan.exits.includes(t.key) && !(t.parent != null && plan.exits.includes(t.parent)))
          .map((t) => [t.key, { el: t.el, box: before.boxes.get(t.key)! }]),
      )
      plan.exits = plan.exits.filter((key) => gone.has(key))
      // In the same read pass: the dots of sets logged in this step, and the control the next step will press.
      const ticks = tickDots(plan.enters, targets, phone.current, scale)
      // A new row inside a new row comes in with it: only the outer one fades (its parts would fade twice over).
      const parentOf = new Map(targets.map((t) => [t.key, t.parent]))
      const entering = new Set(plan.enters)
      const outer = (key: string) => !entering.has(parentOf.get(key) ?? '')
      plan.enters = plan.enters.filter(outer)
      plan.crossFades = plan.crossFades.filter(outer)
      this.readTap()
      this.run = startFlip(plan, targets, gone, ghosts.current, this.timing, ticks, rises)
    } else if (_prev.stepIndex !== this.props.stepIndex || (_prev.scale !== this.props.scale && this.ring == null)) {
      // A step change with no rows to move, or the phone's first size (a ring before the first step change, screen 3's
      // Choose a template, is read once the phone has a size): where the next step's control is, one read.
      this.readTap()
    }
    this.moveRing()
    if (this.run == null) return
    if (moveMs >= this.timing.moveMs) this.end()
    else this.run.seek(moveMs)
  }

  componentWillUnmount() {
    this.end()
    this.endRing()
  }

  private end() {
    this.run?.finish()
    this.run = null
  }

  /** Where the next step's control is: one read, as a step comes in (never while the ring plays). */
  private readTap() {
    const { on, scale, phone, screen, nextTap, taps } = this.props
    const tap = nextTap == null ? undefined : taps[nextTap]
    this.tapBox = on && nextTap != null && tap != null && scale > 0 && phone.current != null && screen.current != null ? tapTarget(nextTap, screen.current, phone.current, scale, tap.selector) : null
    this.tapInk = tap?.ink ?? 'accent-ink'
  }

  /** The ring follows the guide's clock while the frame says it plays; otherwise there is none. */
  private moveRing() {
    const { on, tap, tapMs, ghosts } = this.props
    if (!on || tap == null || this.tapBox == null || ghosts.current == null) {
      this.endRing()
      return
    }
    this.ring ??= startRing(ghosts.current, this.tapBox, grit.guideTapMs(), this.tapInk)
    this.ring?.seek(tapMs)
  }

  private endRing() {
    this.ring?.finish()
    this.ring = null
  }

  render() {
    return this.props.children
  }
}

/**
 * The scale that fits a phone `height` tall into the slot: read when the slot's size changes (a resize, a rotation),
 * never per step. Null until the first measure, so the phone is never painted at full size over the controls; without
 * ResizeObserver (jsdom) it is drawn full size.
 */
function useFit(slot: React.RefObject<HTMLDivElement | null>, height: number): number | null {
  const observes = typeof ResizeObserver !== 'undefined'
  const [size, setSize] = useState<{ width: number; height: number } | null>(null)
  useEffect(() => {
    const el = slot.current
    if (el == null || !observes) return
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setSize({ width: entry.contentRect.width, height: entry.contentRect.height })
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [slot, observes])
  if (!observes) return 1
  if (size == null) return null
  return Math.max(0, Math.min(size.width / PHONE_WIDTH, size.height / height))
}

const NO_TAPS: Record<string, PictureTap> = {}

/** React 18 has no `inert` prop: the attribute is set on the element itself. */
const makeInert = (el: HTMLDivElement | null) => el?.setAttribute('inert', '')

/**
 * Screen `id`'s picture at `step`, in its frame: the real screen content with the guide's sample state, laid out on a
 * phone and shrunk to fit, with no-op actions. What is drawn is inert and hidden from the accessibility tree; the frame
 * says one description. Screens whose picture isn't built yet show the empty frame.
 */
export function GuidePicture({
  id,
  mode,
  frame,
  moveMs = Number.POSITIVE_INFINITY,
  motionOn = false,
  nextTap = null,
  heldLoopMs,
}: {
  id: string
  mode: TrainingMode
  frame: JsGuideFrame | null
  moveMs?: number
  motionOn?: boolean
  nextTap?: string | null
  /** A held clock's time, for a picture with a loop of its own (screen 2). */
  heldLoopMs?: number
}) {
  const slot = useRef<HTMLDivElement>(null)
  const phone = useRef<HTMLDivElement>(null)
  const drawnScreen = useRef<HTMLDivElement | null>(null)
  const ghosts = useRef<HTMLDivElement | null>(null)
  // Stable, so React never detaches them for a render: the layout layer reads both while a step change commits.
  const setScreen = useCallback((el: HTMLDivElement | null) => {
    drawnScreen.current = el
    makeInert(el)
  }, [])
  const setGhosts = useCallback((el: HTMLDivElement | null) => {
    ghosts.current = el
    makeInert(el)
  }, [])
  const step = frame?.step ?? null
  const def = PICTURES[id]
  const drawn = def != null && step != null
  const height = def?.height ?? 0
  const fit = useFit(slot, height)
  const scale = fit ?? 0
  // Worked out once per picture and side: the taps' controls never change between steps.
  const taps = useMemo(() => def?.taps?.(mode) ?? NO_TAPS, [def, mode])
  let screen: ReactNode = null
  if (drawn) {
    const Draw = def!.Draw
    screen = <Draw mode={mode} step={step} stepIndex={frame!.stepIndex} progress={frame!.progress} motionOn={motionOn} heldLoopMs={heldLoopMs} />
  }
  if (drawn && !def!.outlined) {
    // Art that is no screen of the app (WELCOME, SIDE): it fills the picture's place, with no phone and no outline.
    return (
      <div className="guide-picture-slot" ref={slot}>
        <div
          className="guide-picture guide-picture-art"
          data-testid="guide-picture"
          data-outlined="false"
          data-step={step ?? undefined}
          data-stepindex={frame?.stepIndex ?? undefined}
          data-progress={frame?.progress ?? undefined}
          role="img"
          aria-label={pictureDescription(id)}
        >
          <div className="guide-picture-screen" aria-hidden="true" ref={setScreen}>
            {screen}
          </div>
        </div>
      </div>
    )
  }
  return (
    <div className="guide-picture-slot" ref={slot}>
      <div
        className={drawn ? 'guide-picture' : 'guide-picture guide-picture-empty'}
        data-outlined={drawn ? 'true' : undefined}
        data-testid="guide-picture"
        data-step={step ?? undefined}
        data-stepindex={frame?.stepIndex ?? undefined}
        data-progress={frame?.progress ?? undefined}
        role={drawn ? 'img' : undefined}
        aria-label={drawn ? pictureDescription(id) : undefined}
        style={drawn ? ({ width: `${PHONE_WIDTH * scale}px`, height: `${height * scale}px`, visibility: fit == null ? 'hidden' : undefined } as CSSProperties) : undefined}
      >
        {drawn && (
          <div className="guide-phone" ref={phone} style={{ width: `${PHONE_WIDTH}px`, height: `${height}px`, transform: `scale(${scale})` }}>
            <FlipLayer
              stepIndex={frame!.stepIndex}
              moveMs={moveMs}
              on={motionOn}
              targets={def!.targets}
              taps={taps}
              rises={def!.rises}
              scale={scale}
              phone={phone}
              screen={drawnScreen}
              ghosts={ghosts}
              nextTap={nextTap}
              tap={frame!.tap ?? null}
              tapMs={frame!.tapProgress * grit.guideTapMs()}
            >
              {/* Inert: no taps, no focus, no find-in-page; aria-hidden: one description for the whole picture. */}
              <div
                className="guide-picture-screen"
                aria-hidden="true"
                ref={setScreen}
              >
                {screen}
              </div>
            </FlipLayer>
            {/* Rows gone in a step, drawn where they were while they fade, and the guide's tick and tap ring (its own; React puts nothing in it). */}
            <div
              className="guide-flip-ghosts"
              aria-hidden="true"
              ref={setGhosts}
            />
          </div>
        )}
      </div>
    </div>
  )
}
