import type { CSSProperties } from 'react'
import { axis, polyline, type ChartSeries } from './chartMath'
import { cx, formatChartValue, formatKg } from './format'
import './charts.css'

export type { ChartSeries } from './chartMath'

export type LineChartProps = {
  /** Up to four series on one shared axis. */
  series: readonly ChartSeries[]
  /** One entry per point; leave entries blank to skip them. */
  xLabels: readonly string[]
  /** Default "kg". */
  unit?: string
  className?: string
}

function latest(s: ChartSeries): number {
  for (let i = s.values.length - 1; i >= 0; i--) {
    const v = s.values[i]
    if (v != null) return v
  }
  return 0
}

/**
 * Change over time on one shared axis. A legend with each series' latest value is always shown;
 * values are written in text colour, the dot beside them carries identity.
 */
export function LineChart({ series, xLabels, unit = 'kg', className }: LineChartProps) {
  const all = series.flatMap((s) => s.values.filter((v): v is number => v != null))
  const summary = series.map((s) => `${s.name} ${formatChartValue(latest(s))} ${unit}`).join(', ')
  const count = Math.max(0, ...series.map((s) => s.values.length))
  const { lo, hi, ticks } = all.length > 0 ? axis(all) : { lo: 0, hi: 1, ticks: [] }
  const x = (i: number) => (count <= 1 ? 0 : (i / (count - 1)) * 100)
  const y = (v: number) => 100 - ((v - lo) / (hi - lo)) * 100

  return (
    <div className={cx('gr-linechart', className)}>
      <div className="gr-legend">
        {series.map((s) => (
          <span key={s.name} className="gr-legend-item">
            <span className="gr-legend-dot" style={{ background: s.color }} />
            <span className="gr-legend-name body-small">{s.name}</span>
            <span className="gr-legend-value title-small">{formatChartValue(latest(s))}</span>
          </span>
        ))}
      </div>
      <div className="gr-plot body-small" role="img" aria-label={`Line chart. Latest: ${summary}`}>
        {all.length > 0 && (
          <>
            <div className="gr-plot-y" aria-hidden="true">
              <span className="gr-plot-y-sizer">{formatKg(hi)}</span>
              {ticks.map((t) => (
                <span key={t} className="gr-plot-y-label" style={{ top: `${y(t)}%` }}>
                  {formatKg(t)}
                </span>
              ))}
            </div>
            <div className="gr-plot-area" aria-hidden="true">
              {ticks.map((t) => (
                <span key={t} className="gr-plot-grid" style={{ top: `${y(t)}%` }} />
              ))}
              <div className="gr-plot-lines">
                <svg viewBox="0 0 100 100" preserveAspectRatio="none">
                  {series.map((s) => (
                    <polyline
                      key={s.name}
                      points={polyline(s.values.flatMap((v, i) => (v == null ? [] : [{ x: x(i), y: y(v) }])))}
                      stroke={s.color}
                    />
                  ))}
                </svg>
                {series.map((s) => {
                  let last = -1
                  s.values.forEach((v, i) => {
                    if (v != null) last = i
                  })
                  const v = last >= 0 ? s.values[last] : null
                  if (v == null) return null
                  // End marker with a surface ring so it stays readable where lines cross.
                  return <span key={s.name} className="gr-plot-marker" style={{ left: `${x(last)}%`, top: `${y(v)}%`, background: s.color }} />
                })}
              </div>
              <div className="gr-plot-x">
                {xLabels.map((text, i) => {
                  if (text.trim() === '') return null
                  const pos = x(i)
                  const edge = pos <= 0 ? 'gr-plot-x-first' : pos >= 100 ? 'gr-plot-x-last' : undefined
                  const style = edge ? undefined : ({ '--x': pos / 100 } as CSSProperties)
                  return (
                    <span key={i} className={cx('gr-plot-x-label', edge)} style={style}>
                      {text}
                    </span>
                  )
                })}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
