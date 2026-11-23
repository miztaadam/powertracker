import { useId, type ReactNode } from 'react'
import { cx } from './format'
import './TextField.css'

export type TextFieldProps = {
  label: string
  value: string
  onChange: (value: string) => void
  /** A number: shows the decimal keypad. The value stays a string; parse it where it is used. */
  numeric?: boolean
  /** A unit after the value, e.g. "kg". */
  unit?: string
  placeholder?: string
  /** What is wrong with the value; shown under the field and read with it. */
  error?: string
  autoFocus?: boolean
  /** A control at the end of the box, e.g. an `IconButton` that clears a search. */
  trailing?: ReactNode
  /** What the keyboard's return key says: "done" (default) or "search". */
  enterKeyHint?: 'done' | 'search'
  className?: string
}

/** A labelled one-line input. Text is 16 px so iOS does not zoom the page on focus. */
export function TextField({ label, value, onChange, numeric = false, unit, placeholder, error, autoFocus, trailing, enterKeyHint = 'done', className }: TextFieldProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className={cx('gr-field', error != null && 'gr-field-invalid', className)}>
      <label htmlFor={id} className="gr-field-label eyebrow">
        {label}
      </label>
      <div className={cx('gr-field-box', trailing != null && 'gr-field-box-trailing')}>
        <input
          id={id}
          className="gr-field-input"
          type="text"
          inputMode={numeric ? 'decimal' : undefined}
          autoComplete="off"
          autoCorrect="off"
          autoCapitalize={enterKeyHint === 'search' ? 'none' : undefined}
          enterKeyHint={enterKeyHint}
          value={value}
          placeholder={placeholder}
          autoFocus={autoFocus}
          aria-invalid={error != null ? true : undefined}
          aria-describedby={error != null ? errorId : undefined}
          onChange={(e) => onChange(e.target.value)}
        />
        {unit != null && <span className="gr-field-unit title-small">{unit}</span>}
        {trailing}
      </div>
      {error != null && (
        <p id={errorId} className="gr-field-error body-small">
          {error}
        </p>
      )}
    </div>
  )
}
