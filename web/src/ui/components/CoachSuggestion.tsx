import { IconButton, PrimaryButton } from './Button'
import { cx } from './format'
import { BoltIcon, CloseIcon } from './icons'
import './CoachSuggestion.css'

export type CoachSuggestionProps = {
  /** The target, e.g. "155 × 5 @ 8". */
  headline: string
  /** Why, e.g. "Last time: 150 × 5 @ 7." */
  reason: string
  onApply: () => void
  onDismiss: () => void
  className?: string
}

/**
 * The coach's next-session target, shown inline on an exercise: "Coach suggests" + the headline
 * + the reason, with Apply and Dismiss. Nothing changes until Apply.
 */
export function CoachSuggestion({ headline, reason, onApply, onDismiss, className }: CoachSuggestionProps) {
  return (
    <div className={cx('gr-suggestion', className)}>
      <div className="gr-suggestion-text">
        <div className="gr-suggestion-tag">
          <BoltIcon size={14} />
          <p className="eyebrow">Coach suggests</p>
        </div>
        <p className="gr-suggestion-headline headline-medium">{headline}</p>
        <p className="gr-suggestion-reason body-small">{reason}</p>
      </div>
      <PrimaryButton text="Apply" onClick={onApply} compact className="gr-suggestion-apply" />
      <IconButton label="Dismiss" icon={<CloseIcon size={20} />} onClick={onDismiss} />
    </div>
  )
}
