import type { ReactNode } from 'react'
import { cx } from './format'
import { AddIcon, RemoveIcon } from './icons'
import { Eyebrow, NumberDisplay } from './NumberDisplay'
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
  /** Shown in place of the figure: a NumberField for typing the number. The caption above it stays. */
  field?: ReactNode
  /** A small action right of the caption (the RPE info button). It adds no height and is not part of the field's label. */
  labelAction?: ReactNode
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
 * The figure reads as one phrase ("Weight 155 kg"); with `field` the number is typed into instead,
 * and a tap anywhere on the caption or the figure puts the cursor in it, except on `labelAction`.
 */
export function StepperField({ label, valueText, unit, canDecrement, canIncrement, onDecrement, onIncrement, field, labelAction, className }: StepperFieldProps) {
  const spoken = [label, valueText, unit].filter(Boolean).join(' ')
  return (
    <div className={cx('gr-stepper-field', labelAction != null && 'gr-stepper-field-action', className)}>
      {field != null ? (
        <label className="gr-stepper-figure">
          <Eyebrow text={label} />
          {field}
        </label>
      ) : (
        <div className="gr-stepper-figure" role="group" aria-label={spoken}>
          <NumberDisplay value={valueText} unit={unit} label={label} size="large" centered />
        </div>
      )}
      {labelAction != null && (
        // Laid over the caption, not inside the label: a label may hold no control but its field.
        // The unseen copy of the caption puts the action right after the real one.
        <div className="gr-stepper-action">
          <span className="gr-stepper-action-spacer eyebrow" data-text={label} aria-hidden="true" />
          {labelAction}
        </div>
      )}
      <div className="gr-stepper-buttons">
        <StepButton label={`Decrease ${label}`} icon={<RemoveIcon />} enabled={canDecrement} onClick={onDecrement} wide />
        <StepButton label={`Increase ${label}`} icon={<AddIcon />} enabled={canIncrement} onClick={onIncrement} wide />
      </div>
    </div>
  )
}
