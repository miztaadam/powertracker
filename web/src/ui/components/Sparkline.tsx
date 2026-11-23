import type { CSSProperties } from 'react'
import { polyline } from './chartMath'
import { cx } from './format'
import './charts.css'

export type SparklineProps = {
  values: readonly number[]
  /** A CSS colour. */
  color: string
  /** A soft gradient under the line. Default true. */
  fill?: boolean
  /**
   * The smallest value range the height stands for. The line fills the height unless the values
   * vary by less than this: then it is drawn to this scale, so a flat trend looks flat.
   */
  minSpan?: number
  /** Draw the line in when it mounts (Progress only). CSS only, with motion on; the DOM is the finished line. */
  fillIn?: boolean
  /** Give it a size: width and height come from this class (or the parent's layout). */
  className?: string
}

/** A tiny trend line with no axes, for stat tiles and coach cards. Say the trend in words next to it. */
export function Sparkline({ values, color, fill = true, minSpan = 0, fillIn = false, className }: SparklineProps) {
  if (values.length < 2) return <div className={cx('gr-spark', fillIn && 'gr-fill-in', className)} aria-hidden="true" />
  const min = Math.min(...values)
  const range = Math.max(...values) - min
  const span = Math.max(range, minSpan) || 1
  // Centre the values when the scale is wider than they are.
  const lo = min - (span - range) / 2
  const points = values.map((v, i) => ({ x: (i / (values.length - 1)) * 100, y: 100 - ((v - lo) / span) * 100 }))
  const end = points[points.length - 1]!
  // The area under the line, as a clip path over the whole box (the line itself is inset by 6 px).
  const inset = (pct: number) => `calc(6px + (100% - 12px) * ${pct / 100})`
  const area = [`${inset(0)} 100%`, ...points.map((p) => `${inset(p.x)} ${inset(p.y)}`), `${inset(100)} 100%`].join(', ')
  return (
    <div className={cx('gr-spark', fillIn && 'gr-fill-in', className)} style={{ '--spark-color': color } as CSSProperties} aria-hidden="true">
      {fill && <div className="gr-spark-fill" style={{ clipPath: `polygon(${area})` }} />}
      <div className="gr-spark-line">
        <svg viewBox="0 0 100 100" preserveAspectRatio="none">
          <polyline points={polyline(points)} />
        </svg>
        <span className="gr-plot-marker" style={{ left: `${end.x}%`, top: `${end.y}%`, background: color }} />
      </div>
    </div>
  )
}
