import { useEffect, useRef } from 'react'
import './Snackbar.css'

export type SnackbarProps = {
  /** What happened, in one line; null shows nothing. */
  message: string | null
  actionLabel?: string
  onAction?: () => void
  /** Called when the message has been up for `durationMs`. Clear `message` in it. */
  onDismiss?: () => void
  /** Default 4000; a snackbar with an action stays until it is used or replaced. */
  durationMs?: number
}

/**
 * A short confirmation above the tab bar ("Backup saved."), announced politely to screen readers.
 * The newest message replaces the one on screen. Keep it mounted and pass null when there is none.
 */
export function Snackbar({ message, actionLabel, onAction, onDismiss, durationMs }: SnackbarProps) {
  const dismiss = useRef(onDismiss)
  dismiss.current = onDismiss
  const duration = durationMs ?? (actionLabel != null ? null : 4000)

  useEffect(() => {
    if (message == null || duration == null) return
    const timer = setTimeout(() => dismiss.current?.(), duration)
    return () => clearTimeout(timer)
  }, [message, duration])

  return (
    <div className="gr-snackbar-host" role="status" aria-live="polite">
      {message != null && (
        <div key={message} className="gr-snackbar body-medium">
          <span className="gr-snackbar-message">{message}</span>
          {actionLabel != null && (
            <button type="button" className="gr-snackbar-action label-large" onClick={onAction}>
              {actionLabel}
            </button>
          )}
        </div>
      )}
    </div>
  )
}
