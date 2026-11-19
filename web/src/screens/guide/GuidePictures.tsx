import { useEffect, useMemo, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import type { TrainingMode } from '../../db/schema'
import { TodayContent } from '../today/TodayScreen'
import { PHONE_WIDTH, phoneHeight, pictureDescription } from './guideLabels'
import { NO_TODAY_ACTIONS, guideTodayState } from './guideSampleState'

/** The Today screen at one step of logging a set (screen 4), with no-op actions. */
function TodayPicture({ mode, step }: { mode: TrainingMode; step: string }) {
  const state = useMemo(() => guideTodayState(mode, step), [mode, step])
  return <TodayContent state={state} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />
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
export function GuidePicture({ id, mode, step }: { id: string; mode: TrainingMode; step: string | null }) {
  const slot = useRef<HTMLDivElement>(null)
  const drawn = id === 'TODAY' && step != null
  const height = phoneHeight(mode)
  const fit = useFit(slot, height)
  const scale = fit ?? 0
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
        style={drawn ? ({ width: `${PHONE_WIDTH * scale}px`, height: `${height * scale}px`, visibility: fit == null ? 'hidden' : undefined } as CSSProperties) : undefined}
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
