import type { ReactNode } from 'react'
import { PrimaryButton, QuietButton } from './Button'
import { Card } from './Card'
import { cx } from './format'
import { toneClass, type Tone } from './tone'
import './AdviceCard.css'
import './tone.css'

export type AdviceCardProps = {
  /** STALL = warning, DELOAD = danger, WEAK_POINT / RE_ENTRY = info, TARGET = accent. */
  tone: Tone
  /** An icon element, drawn at 20 px in the tone's colour. */
  icon: ReactNode
  /** The kind of advice, e.g. "Stall · Bench". Shown upper-case. */
  tag: string
  /** What to do. */
  title: string
  /** Why. Left out when the detail below says it all. */
  body?: string
  primaryAction?: string
  onPrimary?: () => void
  secondaryAction?: string
  onSecondary?: () => void
  /** Optional detail between the body and the actions: accessories, a mini chart. */
  children?: ReactNode
  className?: string
}

const noop = () => {}

/** The shell of every coach card: a toned icon and tag, then what to do, why, optional detail, and actions. */
export function AdviceCard({ tone, icon, tag, title, body, primaryAction, onPrimary = noop, secondaryAction, onSecondary = noop, children, className }: AdviceCardProps) {
  return (
    <Card className={cx('gr-advice', toneClass(tone), className)}>
      <div className="gr-advice-head">
        <span className="gr-advice-icon">{icon}</span>
        <p className="gr-advice-tag eyebrow">{tag}</p>
      </div>
      <div className="gr-advice-copy">
        <h2 className="gr-advice-title headline-medium">{title}</h2>
        {body != null && <p className="gr-advice-body body-medium">{body}</p>}
      </div>
      {children}
      {(primaryAction != null || secondaryAction != null) && (
        <div className="gr-advice-actions">
          {primaryAction != null && <PrimaryButton text={primaryAction} onClick={onPrimary} compact />}
          {secondaryAction != null && <QuietButton text={secondaryAction} onClick={onSecondary} />}
        </div>
      )}
    </Card>
  )
}
