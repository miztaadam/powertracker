import type { LoadUnit } from '../../db/schema'
import { grit } from '../../domain'
import { cx, formatKg, formatRpe } from './format'
import { CheckIcon, CloseIcon, EmojiEventsIcon } from './icons'
import { Pill } from './Pill'
import './SetRow.css'

export type SetState = 'planned' | 'done' | 'failed'

export type SetRowProps = {
  /** The set number, from 1. */
  index: number
  weightKg: number
  reps: number
  rpe: number | null
  state: SetState
  isTopSet?: boolean
  isPr?: boolean
  onClick?: () => void
  className?: string
  /** What `weightKg` is (1.17): kg when left out, or a plate number on a plate-counted machine, "plate 7½". */
  unit?: LoadUnit
  /** Compact Right/Left breakdown for a single-sided set, e.g. "R 12 · L 10". */
  splitText?: string
}

/** "Set 1: 150 kg × 5 at RPE 8, top set, personal record, done"; "Set 1: plate 7 and a half × 12 at RPE 8, done". */
export function setRowLabel({ index, weightKg, reps, rpe, state, isTopSet = false, isPr = false, unit, splitText }: SetRowProps): string {
  let s = `Set ${index}: ${unit === 'PLATES' ? grit.plateSpoken(weightKg) : `${formatKg(weightKg)} kg`} × ${reps}`
  if (rpe != null) s += ` at RPE ${formatRpe(rpe)}`
  if (splitText) s += `, ${splitText}`
  if (isTopSet) s += ', top set'
  if (isPr) s += ', personal record'
  return `${s}, ${state}`
}

/**
 * One set: number, weight × reps @ RPE, and its state. Planned sets are dimmed, done sets get a
 * green tick, failed sets a red cross and a "Failed" pill; a personal record gets a "PR" pill.
 * Reads to a screen reader as one sentence.
 */
export function SetRow(props: SetRowProps) {
  const { index, weightKg, reps, rpe, state, isTopSet = false, isPr = false, onClick, className, unit, splitText } = props
  const content = (
    <>
      <span className="sr-only">{setRowLabel(props)}</span>
      <span className="gr-set-visual" aria-hidden="true">
        <span className="gr-set-index title-small">{index}</span>
        {/* The load (weight × reps) and the effort (@ RPE) each in a box of their own, so a narrow screen can put them on two lines. */}
        <span className="gr-set-line headline-small">
          <span className="gr-set-load">
            <span>{unit === 'PLATES' ? grit.plateText(weightKg) : formatKg(weightKg)}</span>
            <span className="gr-set-reps">
              <span className="gr-set-soft gr-set-times">×</span>
              <span>{reps}</span>
            </span>
          </span>
          {rpe != null && (
            <span className="gr-set-effort">
              <span className="gr-set-soft gr-set-at">@</span>
              <span>{formatRpe(rpe)}</span>
            </span>
          )}
        </span>
        {(isTopSet || isPr || splitText) && (
          // Both pills stay on one line where they fit; on a narrow phone with a long weight the second goes under the first.
          <span className="gr-set-pills">
            {splitText && <Pill text={splitText} />}
            {isTopSet && <Pill text="Top set" />}
            {isPr && <Pill text="PR" tone="success" icon={<EmojiEventsIcon />} />}
          </span>
        )}
        <span className="gr-set-spacer" />
        {state === 'planned' && <span className="gr-set-dot gr-set-dot-planned" />}
        {state === 'done' && (
          <span className="gr-set-dot gr-set-dot-done">
            <CheckIcon size={16} />
          </span>
        )}
        {state === 'failed' && (
          <>
            <Pill text="Failed" tone="danger" />
            <span className="gr-set-dot gr-set-dot-failed">
              <CloseIcon size={16} />
            </span>
          </>
        )}
      </span>
    </>
  )
  const classes = cx('gr-set', `gr-set-${state}`, className)
  return onClick ? (
    <button type="button" className={classes} onClick={onClick}>
      {content}
    </button>
  ) : (
    <div className={classes}>{content}</div>
  )
}
