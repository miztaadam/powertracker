import type { ReactNode } from 'react'
import { cx } from './format'
import './ScreenHeader.css'

export type ScreenHeaderProps = {
  title: string
  eyebrow?: string
  /** Shown between the eyebrow and the title: the top-level screens pass `<ModeLabel />`. */
  label?: ReactNode
  /** A leading control, usually a back `IconButton`. */
  navigation?: ReactNode
  /** Controls on the right. */
  actions?: ReactNode
  className?: string
}

export type SectionHeaderProps = {
  title: string
  action?: ReactNode
  className?: string
}

/** The top of every screen: small upper-case eyebrow, a big condensed title, actions on the right. */
export function ScreenHeader({ title, eyebrow, label, navigation, actions, className }: ScreenHeaderProps) {
  return (
    <header className={cx('gr-screen-header', navigation != null && 'gr-screen-header-nav', className)}>
      {navigation}
      <div className="gr-screen-header-text">
        {eyebrow != null && <p className="eyebrow">{eyebrow}</p>}
        {label}
        <h1 className="gr-screen-title headline-large">{title}</h1>
      </div>
      {actions}
    </header>
  )
}

/** Names a group of content inside a screen, with an optional action on the right. */
export function SectionHeader({ title, action, className }: SectionHeaderProps) {
  return (
    <div className={cx('gr-section-header', className)}>
      <h2 className="gr-section-title eyebrow">{title}</h2>
      {action}
    </div>
  )
}
