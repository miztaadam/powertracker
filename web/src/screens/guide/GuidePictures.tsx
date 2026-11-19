import { useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import type { TrainingMode } from '../../db/schema'
import { TodayContent } from '../today/TodayScreen'
import { NO_TODAY_ACTIONS, guideTodayState } from './guideSampleState'

/** The phone a picture's screen is laid out on before it is shrunk into the frame: the iPhone the e2e runs as. */
export const PHONE_WIDTH = 390

/**
 * The phone's height for `mode`'s Today picture: tall enough for that side's sample day, so its last step shows the
 * ticked set and the "Skipped today" line at the foot of the list. 844 px holds powerlifting's four exercises;
 * bodybuilding's Monday has seven, so its phone is taller, and drawn smaller (as on Android).
 */
export function phoneHeight(mode: TrainingMode): number {
  return mode === 'BODYBUILDING' ? 1180 : 844
}

/** What a screen reader hears for a picture, the one thing of it in the accessibility tree. Only built pictures have one. */
const PICTURE_DESCRIPTIONS: Partial<Record<string, string>> = {
  TODAY: 'Example of the Today screen: a set’s weight, reps and effort are put in and the set is ticked, then an exercise is skipped.',
}

export function pictureDescription(id: string): string {
  const description = PICTURE_DESCRIPTIONS[id]
  if (description == null) throw new Error(`No picture for ${id} yet`)
  return description
}

/** The Today screen at one step of logging a set (screen 4), with no-op actions. */
function TodayPicture({ mode, step }: { mode: TrainingMode; step: string }) {
  const state = useMemo(() => guideTodayState(mode, step), [mode, step])
  return <TodayContent state={state} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />
}

/**
 * The scale that fits a phone `height` tall into the slot: read when the slot's size changes (a resize, a rotation),
 * never per step. Without ResizeObserver (jsdom) it is drawn full size.
 */
function useFit(slot: React.RefObject<HTMLDivElement | null>, height: number): number {
  const [size, setSize] = useState<{ width: number; height: number } | null>(null)
  useEffect(() => {
    const el = slot.current
    if (el == null || typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setSize({ width: entry.contentRect.width, height: entry.contentRect.height })
    })
    observer.observe(el)
    return () => observer.disconnect()
  }, [slot])
  if (size == null) return 1
  return Math.max(0, Math.min(size.width / PHONE_WIDTH, size.height / height))
}

/** React 18 has no `inert` prop: the attribute is set on the element itself. */
const makeInert = (el: HTMLDivElement | null) => el?.setAttribute('inert', '')

/**
 * Screen `id`'s picture at `step`, in its frame: the real screen content with the guide's sample state, laid out on a
 * phone and shrunk to fit, with no-op actions. What is drawn is inert and hidden from the accessibility tree; the frame
 * says one description. Screens whose picture isn't built yet show the empty frame.
 */
export function GuidePicture({ id, mode, step }: { id: string; mode: TrainingMode; step: string | null }) {
  const slot = useRef<HTMLDivElement>(null)
  const drawn = id === 'TODAY' && step != null
  const height = phoneHeight(mode)
  const scale = useFit(slot, height)
  let screen: ReactNode = null
  if (drawn) screen = <TodayPicture mode={mode} step={step} />
  return (
    <div className="guide-picture-slot" ref={slot}>
      <div
        className={drawn ? 'guide-picture' : 'guide-picture guide-picture-empty'}
        data-testid="guide-picture"
        data-step={step ?? undefined}
        role={drawn ? 'img' : undefined}
        aria-label={drawn ? pictureDescription(id) : undefined}
        style={drawn ? ({ width: `${PHONE_WIDTH * scale}px`, height: `${height * scale}px` } as CSSProperties) : undefined}
      >
        {drawn && (
          <div className="guide-phone" style={{ width: `${PHONE_WIDTH}px`, height: `${height}px`, transform: `scale(${scale})` }}>
            {/* Inert: no taps, no focus, no find-in-page; aria-hidden: one description for the whole picture. */}
            <div className="guide-picture-screen" aria-hidden="true" ref={makeInert}>
              {screen}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
