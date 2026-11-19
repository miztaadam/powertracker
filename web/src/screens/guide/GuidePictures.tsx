import { Component, useCallback, useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode, type RefObject } from 'react'
import type { TrainingMode } from '../../db/schema'
import { grit, type JsGuideFrame } from '../../domain'
import { TodayContent } from '../today/TodayScreen'
import { FLIP_EASING, planFlip, readBoxes, startFlip, todayTargets, type FlipBox, type FlipRun, type FlipTarget } from './guideFlip'
import { PHONE_HEIGHT, PHONE_WIDTH, pictureDescription } from './guideLabels'
import { NO_TODAY_ACTIONS, guideTodayState } from './guideSampleState'

/**
 * The Today screen at one step of logging a set (screen 4), `progress` through the move into it, with no-op actions:
 * the lift's editor shows the domain's in-between values (`guideSampleTodayEditor`).
 */
function TodayPicture({ mode, step, progress }: { mode: TrainingMode; step: string; progress: number }) {
  const editor = grit.guideSampleTodayEditor(mode, step, progress)
  const weightKg = editor.weightKg ?? null
  const { reps, rpe } = editor
  const state = useMemo(() => guideTodayState(mode, step, { weightKg, reps, rpe }), [mode, step, weightKg, reps, rpe])
  return <TodayContent state={state} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />
}

type FlipProps = {
  /** The step shown: a change of it is a step change, the only time layout is read. */
  stepIndex: number
  /** How far into the step's move the guide's clock is, in ms (past the move: it has ended). */
  moveMs: number
  /** Whether the picture moves at all (motion off: it is drawn still, at its last step). */
  on: boolean
  scale: number
  phone: RefObject<HTMLDivElement | null>
  screen: RefObject<HTMLDivElement | null>
  ghosts: RefObject<HTMLDivElement | null>
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

  private readonly timing = { moveMs: grit.guideMoveMs(), fadeMs: grit.guideFadeMs(), easing: FLIP_EASING }

  getSnapshotBeforeUpdate(prev: FlipProps): Before | null {
    if (prev.stepIndex === this.props.stepIndex) return null
    // A move still under way ends where it was going, so the boxes read are the rows' own.
    this.end()
    const { on, scale, phone, screen } = this.props
    if (!on || scale <= 0 || phone.current == null || screen.current == null) return null
    const targets = todayTargets(screen.current)
    return { boxes: readBoxes(targets, phone.current, scale), targets }
  }

  componentDidUpdate(_prev: FlipProps, _state: unknown, before: Before | null) {
    const { scale, phone, screen, ghosts, moveMs } = this.props
    if (before != null && phone.current != null && screen.current != null) {
      const targets = todayTargets(screen.current)
      const after = readBoxes(targets, phone.current, scale)
      const plan = planFlip(before.boxes, after, new Map(targets.map((t) => [t.key, t.parent])))
      // A row gone from the step: the node it was drawn with, and where (a part of a gone row goes with it).
      const gone = new Map(
        before.targets
          .filter((t) => plan.exits.includes(t.key) && !(t.parent != null && plan.exits.includes(t.parent)))
          .map((t) => [t.key, { el: t.el, box: before.boxes.get(t.key)! }]),
      )
      plan.exits = plan.exits.filter((key) => gone.has(key))
      this.run = startFlip(plan, targets, gone, ghosts.current, this.timing)
    }
    if (this.run == null) return
    if (moveMs >= this.timing.moveMs) this.end()
    else this.run.seek(moveMs)
  }

  componentWillUnmount() {
    this.end()
  }

  private end() {
    this.run?.finish()
    this.run = null
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

/** React 18 has no `inert` prop: the attribute is set on the element itself. */
const makeInert = (el: HTMLDivElement | null) => el?.setAttribute('inert', '')

/**
 * Screen `id`'s picture at `step`, in its frame: the real screen content with the guide's sample state, laid out on a
 * phone and shrunk to fit, with no-op actions. What is drawn is inert and hidden from the accessibility tree; the frame
 * says one description. Screens whose picture isn't built yet show the empty frame.
 */
export function GuidePicture({ id, mode, frame, moveMs = Number.POSITIVE_INFINITY, motionOn = false }: { id: string; mode: TrainingMode; frame: JsGuideFrame | null; moveMs?: number; motionOn?: boolean }) {
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
  const drawn = id === 'TODAY' && step != null
  const height = PHONE_HEIGHT
  const fit = useFit(slot, height)
  const scale = fit ?? 0
  let screen: ReactNode = null
  if (drawn) screen = <TodayPicture mode={mode} step={step} progress={frame!.progress} />
  return (
    <div className="guide-picture-slot" ref={slot}>
      <div
        className={drawn ? 'guide-picture' : 'guide-picture guide-picture-empty'}
        data-testid="guide-picture"
        data-step={step ?? undefined}
        data-progress={frame?.progress ?? undefined}
        role={drawn ? 'img' : undefined}
        aria-label={drawn ? pictureDescription(id) : undefined}
        style={drawn ? ({ width: `${PHONE_WIDTH * scale}px`, height: `${height * scale}px`, visibility: fit == null ? 'hidden' : undefined } as CSSProperties) : undefined}
      >
        {drawn && (
          <div className="guide-phone" ref={phone} style={{ width: `${PHONE_WIDTH}px`, height: `${height}px`, transform: `scale(${scale})` }}>
            <FlipLayer stepIndex={frame!.stepIndex} moveMs={moveMs} on={motionOn} scale={scale} phone={phone} screen={drawnScreen} ghosts={ghosts}>
              {/* Inert: no taps, no focus, no find-in-page; aria-hidden: one description for the whole picture. */}
              <div
                className="guide-picture-screen"
                aria-hidden="true"
                ref={setScreen}
              >
                {screen}
              </div>
            </FlipLayer>
            {/* Rows gone in a step, drawn where they were while they fade (the guide's own; React puts nothing in it). */}
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
