import type { CSSProperties, ReactNode } from 'react'
import { cx } from './format'
import './Card.css'

export type CardProps = {
  children: ReactNode
  /** Padding inside the card in px. Default 16. */
  contentPadding?: number
  /** Gap between the card's children in px. Default 12. */
  spacing?: number
  className?: string
}

/** The standard container: one tonal step above the background with a hairline, no shadow. */
export function Card({ children, contentPadding = 16, spacing = 12, className }: CardProps) {
  const style = { '--card-padding': `${contentPadding}px`, '--card-gap': `${spacing}px` } as CSSProperties
  return (
    <div className={cx('gr-card', className)} style={style}>
      {children}
    </div>
  )
}
