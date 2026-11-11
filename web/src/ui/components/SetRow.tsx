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
}

/** "Set 1: 150 kg × 5 at RPE 8, top set, personal record, done". */
export function setRowLabel({ index, weightKg, reps, rpe, state, isTopSet = false, isPr = false }: SetRowProps): string {
  let s = `Set ${index}: ${formatKg(weightKg)} kg × ${reps}`
  if (rpe != null) s += ` at RPE ${formatRpe(rpe)}`
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
  const { index, weightKg, reps, rpe, state, isTopSet = false, isPr = false, onClick, className } = props
  const content = (
    <>
      <span className="sr-only">{setRowLabel(props)}</span>
      <span className="gr-set-visual" aria-hidden="true">
        <span className="gr-set-index title-small">{index}</span>
        <span className="gr-set-line headline-small">
          <span>{formatKg(weightKg)}</span>
          <span className="gr-set-soft gr-set-times">×</span>
          <span>{reps}</span>
          {rpe != null && (
            <>
              <span className="gr-set-soft gr-set-at">@</span>
              <span>{formatRpe(rpe)}</span>
            </>
          )}
        </span>
        {(isTopSet || isPr) && (
          // Both pills stay on one line where they fit; on a narrow phone with a long weight the second goes under the first.
          <span className="gr-set-pills">
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
