import { cx } from './format'
import { ChevronRightIcon } from './icons'
import { NumberDisplay } from './NumberDisplay'
import { toneClass, type Tone } from './tone'
import './StatTile.css'
import './tone.css'

export type StatTileProps = {
  label: string
  value: string
  unit?: string
  /** A change note under the figure, e.g. "+5 kg in 4 wk". */
  note?: string
  /** Default neutral. */
  noteTone?: Tone
  /** Makes the whole tile a button that opens more about the figure; a chevron says so. */
  onClick?: () => void
  /** What the tap does, e.g. "Open squat history". */
  onClickLabel?: string
  className?: string
}

/** A labelled figure with an optional change note underneath. */
export function StatTile({ label, value, unit, note, noteTone = 'neutral', onClick, onClickLabel, className }: StatTileProps) {
  const content = (
    <>
      <NumberDisplay value={value} unit={unit} label={label} size="medium" />
      {note != null && <span className={cx('gr-stat-note label-medium', toneClass(noteTone))}>{note}</span>}
    </>
  )
  if (!onClick) return <div className={cx('gr-stat', className)}>{content}</div>
  const spoken = [label, [value, unit].filter(Boolean).join(' '), note, onClickLabel].filter(Boolean).join(', ')
  return (
    <button type="button" className={cx('gr-stat gr-stat-button', className)} aria-label={spoken} onClick={onClick}>
      {content}
      <ChevronRightIcon size={18} className="gr-stat-chevron" />
    </button>
  )
}
