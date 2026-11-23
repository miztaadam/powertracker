import type { ReactNode } from 'react'
import { cx } from './format'
import './Button.css'

export type ButtonProps = {
  text: string
  onClick: () => void
  /** Default true. */
  enabled?: boolean
  /** An icon element shown before the text, e.g. `<CheckIcon />`. Drawn at 18 px. */
  icon?: ReactNode
  /** 40 px tall to look at (still a 48 px touch target), for use inside cards and rows. */
  compact?: boolean
  className?: string
  /** The accessible name, where the visible text alone does not say what the button acts on ("Swap Leg press"). */
  label?: string
}

export type QuietButtonProps = Omit<ButtonProps, 'icon' | 'compact'>

export type IconButtonProps = {
  /** The accessible name: what the button does ("Dismiss", "Back"). */
  label: string
  icon: ReactNode
  onClick: () => void
  enabled?: boolean
  className?: string
}

function Base({ text, onClick, enabled = true, icon, compact = false, className, label, kind }: ButtonProps & { kind: string }) {
  return (
    <button type="button" className={cx('gr-btn label-large', kind, compact && 'gr-btn-compact', className)} aria-label={label} disabled={!enabled} onClick={onClick}>
      <span className="gr-btn-face">
        {icon != null && <span className="gr-btn-icon">{icon}</span>}
        <span className="gr-btn-text">{text}</span>
      </span>
    </button>
  )
}

/** The one accent-filled action on a screen or card. `compact` is for use inside cards and rows. */
export function PrimaryButton(props: ButtonProps) {
  return <Base {...props} kind="gr-btn-primary" />
}

/** A neutral tonal action that sits beside or below a primary one. */
export function SecondaryButton(props: ButtonProps) {
  return <Base {...props} kind="gr-btn-secondary" />
}

/** A text-only action: dismiss, cancel, skip. */
export function QuietButton(props: QuietButtonProps) {
  return <Base {...props} compact kind="gr-btn-quiet" />
}

/** A 48 px round button that shows only an icon; `label` names it. */
export function IconButton({ label, icon, onClick, enabled = true, className }: IconButtonProps) {
  return (
    <button type="button" className={cx('gr-icon-btn', className)} aria-label={label} disabled={!enabled} onClick={onClick}>
      {icon}
    </button>
  )
}
