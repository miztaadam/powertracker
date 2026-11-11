import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import './Dialog.css'

export type DialogProps = {
  open: boolean
  title: string
  /** The message, or a small form. */
  children?: ReactNode
  /** Buttons, right-aligned: a `QuietButton` to cancel, then the compact `PrimaryButton`. */
  actions?: ReactNode
  /** Called for Escape and a tap outside the dialog. */
  onDismiss: () => void
}

const FOCUSABLE = 'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'

/** A modal question or small form over a scrim. Focus moves into it, stays in it, and goes back on close. */
export function Dialog({ open, title, children, actions, onDismiss }: DialogProps) {
  const titleId = useId()
  const panel = useRef<HTMLDivElement>(null)
  const dismiss = useRef(onDismiss)
  dismiss.current = onDismiss

  useEffect(() => {
    if (!open) return
    const before = document.activeElement
    panel.current?.focus()
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') dismiss.current()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      if (before instanceof HTMLElement) before.focus()
    }
  }, [open])

  if (!open) return null

  const trapTab = (e: KeyboardEvent) => {
    if (e.key !== 'Tab' || !panel.current) return
    const items = Array.from(panel.current.querySelectorAll<HTMLElement>(FOCUSABLE))
    const first = items[0]
    const last = items[items.length - 1]
    if (!first || !last) {
      e.preventDefault()
      return
    }
    const active = document.activeElement
    if (e.shiftKey && (active === first || active === panel.current)) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && active === last) {
      e.preventDefault()
      first.focus()
    }
  }

  return (
    <div className="gr-dialog-scrim" onClick={(e) => e.target === e.currentTarget && onDismiss()}>
      <div ref={panel} className="gr-dialog" role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} onKeyDown={trapTab}>
        <h2 id={titleId} className="gr-dialog-title headline-small">
          {title}
        </h2>
        {children != null && <div className="gr-dialog-body body-medium">{children}</div>}
        {actions != null && <div className="gr-dialog-actions">{actions}</div>}
      </div>
    </div>
  )
}
