import { cx } from './format'
import './charts.css'

export type BarStripProps = {
  /** One quantity over equal periods (weekly volume), oldest first. */
  values: readonly number[]
  /** A CSS colour. */
  color: string
  /** Give it a size: width and height come from this class (or the parent's layout). */
  className?: string
}

/**
 * A row of bars with no axes: say the figure that matters in text next to it. Bars share the
 * strip's own scale; an empty period keeps a stub on the baseline so the rhythm of the weeks stays visible.
 */
export function BarStrip({ values, color, className }: BarStripProps) {
  const max = Math.max(0, ...values) || 1
  return (
    <div className={cx('gr-bars', className)} aria-hidden="true">
      {values.map((v, i) =>
        v > 0 ? <span key={i} className="gr-bar" style={{ height: `${(v / max) * 100}%`, background: color }} /> : <span key={i} className="gr-bar gr-bar-stub" />,
      )}
    </div>
  )
}
