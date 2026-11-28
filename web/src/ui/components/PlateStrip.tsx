import { cx, formatKg } from './format'
import { FitnessCenterIcon, WarningAmberIcon } from './icons'
import { toneClass, type Tone } from './tone'
import './PlateStrip.css'
import './tone.css'

const BAR_KG = 20

export type PlateStripProps = {
  /** Plates on one side, heaviest first (`GritDomain.platesPerSide`); null when the plates cannot make the weight. */
  perSide: number[] | null
  /** The whole load in kg, bar included. Named in the note when `perSide` is null. */
  totalKg: number
  /** The nearest loadable weight, for the note when `perSide` is null. */
  nearestKg?: number
  caption?: string
  className?: string
}

export type PlateStripNoteProps = {
  title: string
  caption: string
  /** Neutral shows a barbell, any other tone a warning triangle. Default neutral. */
  tone?: Tone
  className?: string
}

/** "25 + 20 + 2.5" for the plates on one side, heaviest first; "Empty bar" for none. */
export function platesLabel(perSide: readonly number[]): string {
  return perSide.length === 0 ? 'Empty bar' : perSide.map(formatKg).join(' + ')
}

// Width (thickness) in px and height as a share of a full-size plate.
function plateShape(kg: number): [number, number] {
  if (kg >= 25) return [13, 1]
  if (kg >= 20) return [11, 1]
  if (kg >= 15) return [9, 1]
  if (kg >= 10) return [7, 1]
  if (kg >= 5) return [6, 0.62]
  if (kg >= 2.5) return [5, 0.5]
  if (kg >= 1.25) return [5, 0.4]
  if (kg >= 0.5) return [4, 0.32]
  return [3, 0.26]
}

function plateColor(kg: number): string {
  if (kg >= 25) return 'var(--plate-25)'
  if (kg >= 20) return 'var(--plate-20)'
  if (kg >= 15) return 'var(--plate-15)'
  if (kg >= 10) return 'var(--plate-10)'
  if (kg >= 5) return 'var(--plate-5)'
  if (kg >= 2.5) return 'var(--plate-2p5)'
  return 'var(--plate-small)'
}

const W = 112
const H = 48

function Bar({ perSide }: { perSide: readonly number[] }) {
  const plates: { x: number; w: number; h: number; kg: number }[] = []
  let x = 13
  for (const kg of perSide) {
    const [w, share] = plateShape(kg)
    if (x + w > W) break
    plates.push({ x, w, h: H * share, kg })
    x += w + 2
  }
  return (
    <svg className="gr-plates-bar" width={W} height={H} viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Plates per side: ${platesLabel(perSide)}`}>
      {/* Sleeve, subtle metallic sheen, then the thicker shoulder the plates sit against. */}
      <rect x={0} y={H / 2 - 3} width={W} height={6} rx={3} fill="var(--bar-steel)" />
      <rect x={0} y={H / 2 - 2} width={W} height={1.5} rx={0.75} fill="rgba(255,255,255,0.28)" />
      <rect x={6} y={H / 2 - 8} width={5} height={16} rx={1.5} fill="var(--bar-chrome)" />
      {plates.map((p, i) => {
        const y = (H - p.h) / 2
        const label = p.kg >= 10 ? String(p.kg) : null
        const darkText = p.kg === 15 || p.kg === 5
        return (
          <g key={`${i}-${p.kg}`} className="gr-plate-group" style={{ animationDelay: `${i * 28}ms` }}>
            {/* A faint edge keeps black plates visible on dark and white plates on light. */}
            <rect className="gr-plate" x={p.x} y={y} width={p.w} height={p.h} rx={2} fill={plateColor(p.kg)} />
            {/* Subtle top-edge highlight for 3D machined depth. */}
            <rect x={p.x + 1} y={y + 1} width={Math.max(1, p.w - 2)} height={2.5} rx={1} fill="rgba(255,255,255,0.22)" />
            {label && p.w >= 9 && (
              <text
                x={p.x + p.w / 2}
                y={H / 2 + 2.5}
                textAnchor="middle"
                fontSize="6.5"
                fontWeight="700"
                fill={darkText ? 'rgba(15,15,18,0.82)' : 'rgba(255,255,255,0.9)'}
                style={{ pointerEvents: 'none', userSelect: 'none' }}
              >
                {label}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}

/** Stands in for the drawn bar when there is nothing to draw: no weight yet, or one the plates cannot make. */
export function PlateStripNote({ title, caption, tone = 'neutral', className }: PlateStripNoteProps) {
  return (
    <div className={cx('gr-plates gr-plates-note', toneClass(tone), className)}>
      <span className="gr-plates-note-icon">{tone === 'neutral' ? <FitnessCenterIcon /> : <WarningAmberIcon />}</span>
      <div className="gr-plates-text">
        <span className="gr-plates-note-title title-small">{title}</span>
        <span className="gr-plates-note-caption body-small">{caption}</span>
      </div>
    </div>
  )
}

/**
 * One side of the bar, drawn as it is loaded (collar on the left, heaviest plate first) in IPF
 * colours, with the same list in figures beside it. When the plates cannot make `totalKg`
 * (`perSide` is null) it says so in words instead.
 */
export function PlateStrip({ perSide, totalKg, nearestKg, caption = `per side · ${BAR_KG} kg bar`, className }: PlateStripProps) {
  if (perSide == null) {
    return totalKg < BAR_KG ? (
      <PlateStripNote title="Lighter than the bar" caption={`The bar alone weighs ${BAR_KG} kg.`} tone="warning" className={className} />
    ) : (
      <PlateStripNote
        title={`Can't load ${formatKg(totalKg)} kg exactly`}
        caption={nearestKg != null ? `Nearest is ${formatKg(nearestKg)} kg.` : 'Pick a weight the plates can make.'}
        tone="warning"
        className={className}
      />
    )
  }
  return (
    <div className={cx('gr-plates', className)}>
      <Bar perSide={perSide} />
      <div className="gr-plates-text">
        <span className="gr-plates-label headline-small">{platesLabel(perSide)}</span>
        <span className="gr-plates-caption body-small">{caption}</span>
      </div>
    </div>
  )
}
