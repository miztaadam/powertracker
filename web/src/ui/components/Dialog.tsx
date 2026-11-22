import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { closeRequestsSupported, holdCloseRequests } from './closeRequests'
import { cx } from './format'
import './Dialog.css'

export type DialogProps = {
  open: boolean
  title: string
  /** The message, or a small form. */
  children?: ReactNode
  /** Buttons, right-aligned: a `QuietButton` to cancel, then the compact `PrimaryButton`. */
  actions?: ReactNode
  /** Called for Escape, a tap outside the dialog and (1.17.4, where the browser has CloseWatcher) Back on a phone. */
  onDismiss: () => void
  /** Default true: the rest of the page is inert while the dialog is open. Off only where a dialog is drawn as a sample. */
  isolate?: boolean
  /**
   * How it sits on the page. `dialog` (default) is the small centred panel. `screen` fills the phone,
   * for a task with a list of its own. `sheet` rises from the bottom edge, for something to read
   * with the page still in view behind it.
   */
  variant?: 'dialog' | 'screen' | 'sheet'
  /** Drawn in place of the title line (a `screen` or `sheet` brings its own heading and close button). `title` still names the dialog. */
  header?: ReactNode
  /** Draws the dialog at the end of the page instead of where it is used: for one opened from inside a row, a heading or another dialog. */
  portal?: boolean
  /**
   * The dialog's own name for its styles: on the panel, and with "-scrim" on the scrim (1.17.3: the dialogs laid out
   * for a very narrow screen, narrowDeeper.css). Without it the dialog's classes are exactly the shared ones.
   */
  className?: string
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

/** The dialogs that are open, oldest first. Escape belongs to the last one only. */
const openDialogs: object[] = []

/**
 * A modal question or small form over a scrim. Focus moves into it, stays in it, and goes back on
 * close; the page behind it is inert meanwhile, so VoiceOver cannot wander out of it either.
 */
export function Dialog({ open, title, children, actions, onDismiss, isolate = true, variant = 'dialog', header, portal = false, className }: DialogProps) {
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
    const me = {}
    openDialogs.push(me)
    if (closeRequestsSupported()) {
      // 1.17.4: Back on a phone, and Escape (itself a close request here, so no keydown listener as well), dismiss the
      // dialog on top, through the app's one close watcher (closeRequests.ts).
      const letGo = holdCloseRequests(() => dismiss.current())
      return () => {
        letGo()
        openDialogs.splice(openDialogs.indexOf(me), 1)
        release()
        if (before instanceof HTMLElement) before.focus()
      }
    }
    const onKey = (e: globalThis.KeyboardEvent) => {
      // With one dialog over another, Escape closes the one on top.
      if (e.key === 'Escape' && openDialogs.at(-1) === me) dismiss.current()
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      openDialogs.splice(openDialogs.indexOf(me), 1)
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

  const dialog = (
    <div
      ref={scrim}
      className={cx('gr-dialog-scrim', variant !== 'dialog' && `gr-dialog-scrim-${variant}`, className != null && `${className}-scrim`)}
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
      <div
        ref={panel}
        className={cx('gr-dialog', variant !== 'dialog' && `gr-dialog-${variant}`, className)}
        role="dialog"
        aria-modal="true"
        aria-labelledby={header == null ? titleId : undefined}
        aria-label={header != null ? title : undefined}
        tabIndex={-1}
        onKeyDown={trapTab}
      >
        {header ?? (
          <h2 id={titleId} className="gr-dialog-title headline-small">
            {title}
          </h2>
        )}
        {/* A screen or a sheet lays out and styles its own content. */}
        {children != null && (variant === 'dialog' ? <div className="gr-dialog-body body-medium">{children}</div> : children)}
        {actions != null && <div className="gr-dialog-actions">{actions}</div>}
      </div>
    </div>
  )
  return portal ? createPortal(dialog, document.body) : dialog
}
