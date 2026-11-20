import { useEffect, useRef, type CSSProperties } from 'react'
import { grit } from '../../../domain'
import { currentScheme } from '../../../ui/theme'
import { NS, buildDrop, buildPump, node, paintDropArt, paintPumpArt } from '../../../ui/motion/modeSwitchPaint'
import { PHONE_HEIGHT, sideName } from '../guideLabels'
import type { PictureDef, PictureProps } from './picture'

/**
 * Screen 2: the mode switch's hammer (the powerlifting side) and pump (the bodybuilding side), cut down and side by
 * side, each in a box of its side's colours, drawn with the switch's own art (`modeSwitchPaint.ts`) at the moment of
 * the domain's loop (`guideSideFrame`). Silent: no sound and no vibration, and nothing of the switch's controller.
 *
 * Its one step settles at once, so the guide's clock gives it no time: while it is shown with motion on, it runs its
 * own frame loop, standing still while the page is hidden. Motion off shows `guideSideStill`; a held clock
 * (`heldLoopMs`) draws that moment and runs nothing.
 */
function SidePicture({ motionOn, heldLoopMs }: PictureProps) {
  const dropHost = useRef<HTMLDivElement>(null)
  const pumpHost = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const box = grit.guideSideBox()
    const light = currentScheme() === 'light'
    const flashScale = grit.modeSwitchLightColors().flashOpacityScale
    const svg = (host: HTMLDivElement) => node('svg', { viewBox: `0 0 ${box.w} ${box.h}`, width: '100%', height: '100%', focusable: 'false', preserveAspectRatio: 'xMidYMid slice' }, host).el
    const dropEl = dropHost.current!
    const pumpEl = pumpHost.current!
    const drop = buildDrop(svg(dropEl), box.w, box.h, light)
    const pump = buildPump(svg(pumpEl), box.w, box.h, light, { x: box.srcX, y: box.srcY })
    const paint = (f: { dropMs: number; pumpMs: number }) => {
      paintDropArt(drop, grit.modeSwitchDropFrame(f.dropMs, box.w, box.h), light, flashScale)
      paintPumpArt(pump, grit.modeSwitchPumpFrame(f.pumpMs, box.w, box.h, box.srcX, box.srcY))
    }
    const clear = () => {
      dropEl.replaceChildren()
      pumpEl.replaceChildren()
    }
    if (!motionOn) {
      paint(grit.guideSideStill())
      return clear
    }
    if (heldLoopMs != null) {
      paint(grit.guideSideFrame(heldLoopMs))
      return clear
    }
    // The loop's time is the page's time less `base`, set when it (re)starts, so a hidden page goes on from where it was.
    let at = 0
    let base = 0
    let frame: number | undefined
    const tick = () => {
      at = performance.now() - base
      paint(grit.guideSideFrame(at))
      frame = requestAnimationFrame(tick)
    }
    const start = () => {
      base = performance.now() - at
      paint(grit.guideSideFrame(at))
      frame = requestAnimationFrame(tick)
    }
    const stop = () => {
      if (frame !== undefined) cancelAnimationFrame(frame)
      frame = undefined
    }
    const onVisibility = () => {
      stop()
      if (!document.hidden) start()
    }
    document.addEventListener('visibilitychange', onVisibility)
    if (!document.hidden) start()
    else paint(grit.guideSideFrame(0))
    return () => {
      stop()
      document.removeEventListener('visibilitychange', onVisibility)
      clear()
    }
  }, [motionOn, heldLoopMs])
  const box = grit.guideSideBox()
  return (
    <div className="guide-side-loop" style={{ '--side-ratio': box.w / box.h } as CSSProperties}>
      {(
        [
          ['POWERLIFTING', dropHost],
          ['BODYBUILDING', pumpHost],
        ] as const
      ).map(([side, host]) => (
        <div key={side} className={`guide-side-motion guide-side-${side.toLowerCase()}`}>
          <div className="guide-side-box" ref={host} data-testid={`guide-side-box-${side.toLowerCase()}`} />
          <span className="guide-side-panel-name title-medium">{sideName(side)}</span>
        </div>
      ))}
    </div>
  )
}

export const SIDE_PICTURE: PictureDef = { Draw: SidePicture, outlined: false, height: PHONE_HEIGHT }

/** For tests: the SVG namespace the art is drawn in. */
export const SIDE_NS = NS
