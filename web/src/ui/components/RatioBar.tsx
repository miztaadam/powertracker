import type { CSSProperties } from 'react'
import { cx } from './format'
import { Pill } from './Pill'
import { toneClass, type Tone } from './tone'
import './RatioBar.css'
import './tone.css'

export type RatioBarProps = {
  /** e.g. "Squat ÷ Deadlift". */
  label: string
  valuePct: number
  lowPct: number
  highPct: number
  /**
   * Slide the marker in from the start of the bar when it mounts (Progress only). CSS only, with motion on; the marker
   * is at its value in the DOM. `--xq` is that place as a plain number, for the slide's container units.
   */
  fillIn?: boolean
  className?: string
}

/**
 * A lift ratio against its typical range: the band marks the range (with its bounds in figures),
 * the marker is the lifter. In range reads green, outside reads amber, and a pill says which.
 */
export function RatioBar({ label, valuePct, lowPct, highPct, fillIn = false, className }: RatioBarProps) {
  const inRange = valuePct >= lowPct && valuePct <= highPct
  const tone: Tone = inRange ? 'success' : 'warning'
  const status = valuePct < lowPct ? 'Below range' : valuePct > highPct ? 'Above range' : 'In range'
  // Show the range in the middle third of the bar, whatever its bounds.
  const span = Math.max(highPct - lowPct, 1)
  const domainLo = lowPct - span
  const domain = highPct + span - domainLo
  const x = (pct: number) => (Math.min(Math.max(pct - domainLo, 0), domain) / domain) * 100
  return (
    <div className={cx('gr-ratio', fillIn && 'gr-fill-in', className)}>
      <div className="gr-ratio-head">
        <span className="gr-ratio-label title-small">{label}</span>
        <Pill text={status} tone={tone} />
        <span className="gr-ratio-value headline-small">{valuePct}%</span>
      </div>
      <div className={cx('gr-ratio-bar body-small', toneClass(tone))} aria-hidden="true">
        <span className="gr-ratio-track" />
        <span className="gr-ratio-band" style={{ left: `${x(lowPct)}%`, width: `${x(highPct) - x(lowPct)}%` }} />
        <span className="gr-ratio-marker" style={{ '--x': `${x(valuePct)}%`, ...(fillIn && { '--xq': x(valuePct) }) } as CSSProperties} />
        <span className="gr-ratio-bound" style={{ left: `${x(lowPct)}%` }}>
          {lowPct}%
        </span>
        <span className="gr-ratio-bound" style={{ left: `${x(highPct)}%` }}>
          {highPct}%
        </span>
      </div>
    </div>
  )
}
