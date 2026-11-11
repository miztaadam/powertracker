import type { CSSProperties } from 'react'
import { cx } from './format'
import './NumberDisplay.css'

export type NumberSize = 'hero' | 'large' | 'medium'

export type EyebrowProps = {
  text: string
  /** A CSS colour; default is the muted text colour. */
  color?: string
  className?: string
}

export type NumberDisplayProps = {
  value: string
  unit?: string
  label?: string
  /** hero = 64 px (total), large = 44 px (stepper values, default), medium = 30 px (stat tiles). */
  size?: NumberSize
  /** A CSS colour for the value; default is the text colour. */
  color?: string
  centered?: boolean
  className?: string
}

const SIZE_CLASS: Record<NumberSize, string> = { hero: 'display-large', large: 'display-medium', medium: 'display-small' }

/** Upper-case caption above a value or a group. */
export function Eyebrow({ text, color, className }: EyebrowProps) {
  return (
    <p className={cx('eyebrow', className)} style={color ? { color } : undefined}>
      {text}
    </p>
  )
}

/** A glanceable figure: optional eyebrow label, a big tabular number, and a small unit on the baseline. */
export function NumberDisplay({ value, unit, label, size = 'large', color, centered = false, className }: NumberDisplayProps) {
  const style = color ? ({ '--number-color': color } as CSSProperties) : undefined
  return (
    <div className={cx('gr-number', centered && 'gr-number-centered', className)} style={style}>
      {label != null && <Eyebrow text={label} />}
      <div className="gr-number-row">
        <span className={cx('gr-number-value', SIZE_CLASS[size])}>{value}</span>
        {unit != null && <span className="gr-number-unit title-small">{unit}</span>}
      </div>
    </div>
  )
}
