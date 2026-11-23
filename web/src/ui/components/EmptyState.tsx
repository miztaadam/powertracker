import type { ReactNode } from 'react'
import { cx } from './format'
import './EmptyState.css'

export type EmptyStateProps = {
  /** An icon element, drawn at 28 px. */
  icon: ReactNode
  title: string
  body: string
  /** The next step, usually one button. */
  action?: ReactNode
  className?: string
}

/** What a screen shows when it has nothing yet: say what will appear and offer the next step. */
export function EmptyState({ icon, title, body, action, className }: EmptyStateProps) {
  return (
    <div className={cx('gr-empty', className)}>
      <span className="gr-empty-icon">{icon}</span>
      <h2 className="gr-empty-title headline-small">{title}</h2>
      <p className="gr-empty-body body-medium">{body}</p>
      {action != null && <div className="gr-empty-action">{action}</div>}
    </div>
  )
}
