import type { ReactNode } from 'react'
import { cx } from './format'
import { AddIcon, RemoveIcon } from './icons'
import { NumberDisplay } from './NumberDisplay'
import './StepperField.css'

export type StepperProps = {
  /** Names the value and its buttons: "Decrease <label>" / "Increase <label>". */
  label: string
  valueText: string
  canDecrement: boolean
  canIncrement: boolean
  onDecrement: () => void
  onIncrement: () => void
  className?: string
}

export type StepperFieldProps = StepperProps & {
  unit?: string
  /** Makes the value a button for typing the number. */
  onValueClick?: () => void
}

function StepButton({ label, icon, enabled, onClick, wide }: { label: string; icon: ReactNode; enabled: boolean; onClick: () => void; wide: boolean }) {
  return (
    <button type="button" className={cx('gr-step-btn', wide && 'gr-step-btn-wide')} aria-label={label} disabled={!enabled} onClick={onClick}>
      {icon}
    </button>
  )
}

/** A numeric value with minus/plus buttons on one line (settings, dialogs). */
export function Stepper({ label, valueText, canDecrement, canIncrement, onDecrement, onIncrement, className }: StepperProps) {
  return (
    <div className={cx('gr-stepper', className)} role="group" aria-label={label}>
      <StepButton label={`Decrease ${label}`} icon={<RemoveIcon />} enabled={canDecrement} onClick={onDecrement} wide={false} />
      <span className="gr-stepper-value headline-small" aria-live="polite">
        {valueText}
      </span>
      <StepButton label={`Increase ${label}`} icon={<AddIcon />} enabled={canIncrement} onClick={onIncrement} wide={false} />
    </div>
  )
}

/**
 * The in-session stepper: label, a big number, and two wide buttons underneath. Place three in a row.
 * The figure reads as one phrase ("Weight 155 kg"); with `onValueClick` it is a button for typing it.
 */
export function StepperField({ label, valueText, unit, canDecrement, canIncrement, onDecrement, onIncrement, onValueClick, className }: StepperFieldProps) {
  const spoken = [label, valueText, unit].filter(Boolean).join(' ')
  const figure = <NumberDisplay value={valueText} unit={unit} label={label} size="large" centered />
  return (
    <div className={cx('gr-stepper-field', className)}>
      {onValueClick ? (
        <button type="button" className="gr-stepper-figure" aria-label={`${spoken}, type ${label}`} onClick={onValueClick}>
          {figure}
        </button>
      ) : (
        <div className="gr-stepper-figure" role="group" aria-label={spoken}>
          {figure}
        </div>
      )}
      <div className="gr-stepper-buttons">
        <StepButton label={`Decrease ${label}`} icon={<RemoveIcon />} enabled={canDecrement} onClick={onDecrement} wide />
        <StepButton label={`Increase ${label}`} icon={<AddIcon />} enabled={canIncrement} onClick={onIncrement} wide />
      </div>
    </div>
  )
}
