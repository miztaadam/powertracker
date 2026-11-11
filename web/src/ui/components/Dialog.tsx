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
  /** Default true: the rest of the page is inert while the dialog is open. Off only where a dialog is drawn as a sample. */
  isolate?: boolean
}

const FOCUSABLE = 'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'

/** Makes everything outside `el` inert (out of reach of touch, keyboard and screen readers). Returns the undo. */
function isolateFromPage(el: HTMLElement): () => void {
  const made: Element[] = []
  for (let node: Element = el; node.parentElement && node !== document.body; node = node.parentElement) {
    for (const sibling of node.parentElement.children) {
      if (sibling !== node && !sibling.hasAttribute('inert')) {
        sibling.setAttribute('inert', '')
        made.push(sibling)
      }
    }
  }
  return () => made.forEach((sibling) => sibling.removeAttribute('inert'))
}

/**
 * A modal question or small form over a scrim. Focus moves into it, stays in it, and goes back on
 * close; the page behind it is inert meanwhile, so VoiceOver cannot wander out of it either.
 */
export function Dialog({ open, title, children, actions, onDismiss, isolate = true }: DialogProps) {
  const titleId = useId()
  const scrim = useRef<HTMLDivElement>(null)
  const panel = useRef<HTMLDivElement>(null)
  // Only a press that both starts and ends on the scrim is a tap outside: one that starts in the dialog
  // (selecting text in a field) or ends in it still sends its click to the scrim.
  const pressedOnScrim = useRef(false)
  const releasedOnScrim = useRef(false)
  const dismiss = useRef(onDismiss)
  dismiss.current = onDismiss

  // What had the focus before the dialog appeared; read while rendering, before a field inside can take it.
  const opener = useRef<Element | null>(null)
  const wasOpen = useRef(false)
  if (open && !wasOpen.current) opener.current = document.activeElement
  wasOpen.current = open

  useEffect(() => {
    if (!open) return
    const before = opener.current
    // A field inside that asked for the focus keeps it.
    if (!panel.current?.contains(document.activeElement)) panel.current?.focus()
    const release = isolate && scrim.current ? isolateFromPage(scrim.current) : () => {}
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape') dismiss.current()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      release()
      if (before instanceof HTMLElement) before.focus()
    }
  }, [open, isolate])

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
    <div
      ref={scrim}
      className="gr-dialog-scrim"
      onPointerDown={(e) => {
        pressedOnScrim.current = e.target === e.currentTarget
      }}
      onPointerUp={(e) => {
        releasedOnScrim.current = e.target === e.currentTarget
      }}
      onClick={(e) => {
        if (pressedOnScrim.current && releasedOnScrim.current && e.target === e.currentTarget) onDismiss()
        pressedOnScrim.current = false
        releasedOnScrim.current = false
      }}
    >
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
