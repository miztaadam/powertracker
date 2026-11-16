import type { CSSProperties } from 'react'
import { cx } from './format'
import './charts.css'

export type BarStripProps = {
  /** One quantity over equal periods (weekly volume), oldest first. */
  values: readonly number[]
  /** A CSS colour. */
  color: string
  /**
   * Grow the bars from the baseline when the strip mounts, one after another (Progress only). CSS only, with motion on;
   * the DOM holds the full heights. `--i` is each bar's place in the stagger.
   */
  fillIn?: boolean
  /** Give it a size: width and height come from this class (or the parent's layout). */
  className?: string
}

/**
 * A row of bars with no axes: say the figure that matters in text next to it. Bars share the
 * strip's own scale; an empty period keeps a stub on the baseline so the rhythm of the weeks stays visible.
 */
export function BarStrip({ values, color, fillIn = false, className }: BarStripProps) {
  const max = Math.max(0, ...values) || 1
  return (
    <div className={cx('gr-bars', fillIn && 'gr-fill-in', className)} aria-hidden="true">
      {values.map((v, i) => {
        const order = fillIn ? ({ '--i': i } as CSSProperties) : undefined
        // An empty week is the same element as a full one, so a week that fills while the strip is shown does not grow again.
        return v > 0 ? (
          <span key={i} className="gr-bar" style={{ height: `${(v / max) * 100}%`, background: color, ...order }} />
        ) : (
          <span key={i} className="gr-bar gr-bar-stub" style={order} />
        )
      })}
    </div>
  )
}
