import type { ReactNode } from 'react'
import { cx } from './format'
import { toneClass, type Tone } from './tone'
import './Pill.css'
import './tone.css'

export type PillProps = {
  text: string
  /** Default neutral. */
  tone?: Tone
  /** An icon element shown before the text, drawn at 14 px. */
  icon?: ReactNode
  className?: string
}

/** A small, non-interactive status label. The words carry the meaning; the tone supports them. */
export function Pill({ text, tone = 'neutral', icon, className }: PillProps) {
  return (
    <span className={cx('gr-pill label-medium', toneClass(tone), className)}>
      {icon != null && <span className="gr-pill-icon">{icon}</span>}
      {text}
    </span>
  )
}
