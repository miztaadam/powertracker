import { useId } from 'react'
import { cx } from './format'
import './SegmentedChoice.css'

export type SegmentedChoiceProps = {
  options: readonly string[]
  /** null when nothing is picked yet. */
  selectedIndex: number | null
  /** Called for every tap, also on the option that is already selected (a caller may clear it then). */
  onSelect: (index: number) => void
  /** Names the group for screen readers, e.g. "Where did it stall?". */
  label?: string
  className?: string
  /**
   * Each segment as wide as its own label, sharing out what is left equally (as Android's), instead of equal
   * segments. For a row whose labels differ a lot in length (the rest alert's "Sound + vibrate" beside "Off").
   */
  sizeToLabels?: boolean
}

export type ChoiceChipProps = {
  text: string
  selected: boolean
  onClick: () => void
  className?: string
}

/** Pick exactly one (or none yet) of a few short options: miss point, chart range. */
export function SegmentedChoice({ options, selectedIndex, onSelect, label, className, sizeToLabels = false }: SegmentedChoiceProps) {
  const name = useId()
  return (
    <div className={cx('gr-segmented', sizeToLabels && 'gr-segmented-fit', className)} role="radiogroup" aria-label={label}>
      {options.map((option, i) => (
        <label key={option} className="gr-segment">
          <input
            type="radio"
            className="sr-only"
            name={name}
            checked={i === selectedIndex}
            onChange={() => onSelect(i)}
            // A radio that is already checked fires no change; report the tap so a caller can clear the choice.
            onClick={() => i === selectedIndex && onSelect(i)}
          />
          <span className="gr-segment-face label-large">{option}</span>
        </label>
      ))}
    </div>
  )
}

/** A toggle chip. Selected is tonal (container-highest, an outline), so the accent stays reserved for the main action. */
export function ChoiceChip({ text, selected, onClick, className }: ChoiceChipProps) {
  return (
    <label className={cx('gr-chip', className)}>
      <input type="checkbox" className="sr-only" checked={selected} onChange={onClick} />
      <span className="gr-chip-face label-large">{text}</span>
    </label>
  )
}
