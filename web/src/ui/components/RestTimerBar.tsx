import { QuietButton, SecondaryButton } from './Button'
import { cx } from './format'
import './RestTimerBar.css'

export type RestTimerBarProps = {
  remainingSec: number
  totalSec: number
  onSubtractThirty: () => void
  onAddThirty: () => void
  onSkip: () => void
  className?: string
}

/** 108 → "1:48". */
export function formatClock(seconds: number): string {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

/**
 * The rest countdown docked above the tab bar: time left in big figures, a line along the top
 * edge that empties as the rest runs out, and −30 s / +30 s / Skip.
 */
export function RestTimerBar({ remainingSec, totalSec, onSubtractThirty, onAddThirty, onSkip, className }: RestTimerBarProps) {
  const fraction = totalSec > 0 ? Math.min(1, Math.max(0, remainingSec / totalSec)) : 0
  const clock = formatClock(remainingSec)
  return (
    <div className={cx('gr-rest', className)}>
      <div className="gr-rest-track" aria-hidden="true">
        <div className="gr-rest-fill" style={{ width: `${fraction * 100}%` }} />
      </div>
      <div className="gr-rest-row">
        <div className="gr-rest-time" role="timer" aria-label={`Rest, ${clock} left`}>
          <p className="eyebrow">Rest</p>
          <span className="gr-rest-clock display-small">{clock}</span>
        </div>
        <SecondaryButton text="−30 s" onClick={onSubtractThirty} compact />
        <SecondaryButton text="+30 s" onClick={onAddThirty} compact />
        <QuietButton text="Skip" onClick={onSkip} />
      </div>
    </div>
  )
}
