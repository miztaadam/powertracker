import type { ReactNode } from 'react'
import './lock.css'

export type GateScreenProps = {
  title: string
  body: string
  /** One line on what just happened ("Couldn't export."), announced politely. */
  note?: string | null
  /** The actions, at the bottom of the screen: the main one first. */
  children?: ReactNode
}

/**
 * A full screen that stands in for the app: the Grit icon, one line on why, and what can be done.
 * The layout of the Android lock screen, also used for the other states in which the app is not shown.
 */
export function GateScreen({ title, body, note, children }: GateScreenProps) {
  return (
    <div className="gate">
      <div className="gate-copy">
        <img className="gate-icon" src="/icons/icon-192.png" alt="Grit" width={96} height={96} />
        <h1 className="gate-title headline-medium">{title}</h1>
        <p className="gate-body body-medium">{body}</p>
        <p className="gate-note body-medium" role="status">
          {note}
        </p>
      </div>
      {children != null && <div className="gate-actions">{children}</div>}
    </div>
  )
}

/** What shows while it is not yet known what to show: the background and nothing else. */
export function BlankScreen() {
  return <div className="gate" />
}
