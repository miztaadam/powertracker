import { useId, useRef } from 'react'
import { cx } from './format'
import './NumberField.css'

export type NumberFieldProps = {
  /** The field's spoken name: "Weight, kilograms". */
  label: string
  /** What the field shows: the value, or the text being typed. */
  valueText: string
  /** A unit on the baseline after the value, e.g. "kg". Hidden while the field is empty. */
  unit?: string
  placeholder?: string
  /** The keypad: decimal for weight and RPE, numeric for whole numbers. */
  inputMode: 'decimal' | 'numeric'
  /** What is wrong with the entry; shown under the field and read with it. */
  error?: string
  /** Every keystroke. */
  onDraft: (text: string) => void
  /** The lifter left the field, or pressed Enter. */
  onCommit: (text: string) => void
  /** The cursor entered the field, or has left it (after the commit). */
  onFocusChange?: (focused: boolean) => void
  className?: string
}

/**
 * A big figure that is typed straight into: it looks like a NumberDisplay value, not a boxed form
 * field. Focus selects the value, so typing replaces it. The text is far above 16 px, so iOS does
 * not zoom the page.
 */
export function NumberField({ label, valueText, unit, placeholder, inputMode, error, onDraft, onCommit, onFocusChange, className }: NumberFieldProps) {
  const errorId = `${useId()}-error`
  // A tap focuses on the way down; the default on the way up would put the caret where the finger is and drop the selection.
  const justFocused = useRef(false)
  return (
    <span className={cx('gr-numfield', error != null && 'gr-numfield-invalid', className)}>
      <span className="gr-numfield-row">
        {/* The hidden copy of the text sizes the box, so the input is exactly as wide as its figures. */}
        <span className="gr-numfield-sizer display-medium" data-text={valueText !== '' ? valueText : (placeholder ?? '')}>
          <input
            className="gr-numfield-input"
            size={1}
            type="text"
            inputMode={inputMode}
            autoComplete="off"
            autoCorrect="off"
            enterKeyHint="done"
            aria-label={label}
            value={valueText}
            placeholder={placeholder}
            aria-invalid={error != null ? true : undefined}
            aria-describedby={error != null ? errorId : undefined}
            onChange={(e) => onDraft(e.target.value)}
            onFocus={(e) => {
              justFocused.current = true
              e.currentTarget.select()
              onFocusChange?.(true)
            }}
            onMouseUp={(e) => {
              if (justFocused.current) e.preventDefault()
              justFocused.current = false
            }}
            onBlur={(e) => {
              justFocused.current = false
              onCommit(e.currentTarget.value)
              onFocusChange?.(false)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') e.currentTarget.blur()
            }}
          />
        </span>
        {unit != null && valueText !== '' && <span className="gr-numfield-unit title-small">{unit}</span>}
      </span>
      {error != null && (
        <span id={errorId} className="gr-numfield-error body-small" role="alert">
          {error}
        </span>
      )}
    </span>
  )
}
