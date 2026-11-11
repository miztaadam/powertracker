import { cx } from './format'
import './Switch.css'

export type SwitchProps = {
  /** The accessible name: the setting it turns on ("Change plates"). */
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
  enabled?: boolean
  className?: string
}

/** An on/off setting. On is the accent with an ink thumb; off is an outlined track. */
export function Switch({ label, checked, onChange, enabled = true, className }: SwitchProps) {
  return (
    <button type="button" role="switch" className={cx('gr-switch', className)} aria-checked={checked} aria-label={label} disabled={!enabled} onClick={() => onChange(!checked)}>
      <span className="gr-switch-track">
        <span className="gr-switch-thumb" />
      </span>
    </button>
  )
}
