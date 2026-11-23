/** One line on a chart. A null value is a point in time with no reading: the line runs straight across it. */
export type ChartSeries = {
  name: string
  /** A CSS colour. Use the lift colours (`var(--chart-squat)` …) so a lift keeps its colour everywhere. */
  color: string
  values: readonly (number | null)[]
}

const STEPS = [2.5, 5, 10, 20, 25, 50, 100, 200]

/** The tick step that gives at most four intervals across `range`. */
export function niceStep(range: number): number {
  return STEPS.find((s) => range / s <= 4) ?? 500
}

/** Axis bounds on whole steps around the values, never zero-height. */
export function axis(values: readonly number[]): { lo: number; hi: number; ticks: number[] } {
  const min = Math.min(...values)
  const max = Math.max(...values)
  const step = niceStep(max - min)
  const lo = Math.floor(min / step) * step
  const top = Math.ceil(max / step) * step
  const hi = top === lo ? lo + step : top
  const ticks: number[] = []
  for (let t = lo; t <= hi + 0.01; t += step) ticks.push(t)
  return { lo, hi, ticks }
}

/** "x,y x,y" for an SVG polyline in a 0–100 box. */
export function polyline(points: readonly { x: number; y: number }[]): string {
  return points.map((p) => `${p.x},${p.y}`).join(' ')
}
