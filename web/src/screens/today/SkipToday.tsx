import { useEffect, useRef, useState, type MouseEvent } from 'react'
import type { SkipReason } from '../../db/schema'
import type { SkippedToday } from '../../state/useToday'
import { QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { SkipNextIcon } from '../../ui/components/icons'
import { Menu } from '../../ui/components/Menu'
import './today.css'

// Skipping a planned exercise for today (1.13, skip-a-planned-exercise spec, "What you see on Today"). The Android app's
// Today uses the same words (ui/today/SkipToday.kt).
export const SKIP_TODAY = 'Skip today'
export const UNDO = 'Undo'
export const SKIPPED_TODAY = 'Skipped today'
export const BRING_BACK = 'Bring back'

/** The reason sheet's title. */
export const skipSheetTitle = (name: string) => `Why skip ${name}?`

/** The snackbar after a skip, with Undo. */
export const skippedMessage = (name: string) => `${name} skipped.`

/** The row menu's name, for a screen reader. */
export const exerciseOptions = (name: string) => `Options for ${name}`

/** The four answers, in the sheet's order; "Just skip" stores no reason. */
export const SKIP_ANSWERS: readonly [text: string, reason: SkipReason | null][] = [
  ['Busy or no equipment', 'BUSY'],
  ['Pain', 'PAIN'],
  ['Out of time', 'TIME'],
  ['Just skip', null],
]

/**
 * How long, in ms, the reason sheet ignores taps on its answers once it is open: it opens under the finger
 * that chose Skip today, where a double tap's second tap would land (as Plan's add form, ADD_FORM_TAP_WINDOW_MS). Counted
 * from the end of its slide up, as on Android, so the slide does not use the moment up; taps meanwhile are ignored too.
 */
export const SKIP_SHEET_TAP_WINDOW_MS = 300

/** The time now, on the clock events are stamped with (as Plan's add form reads it). */
const eventClock = () => new Event('now').timeStamp

/** The row menu (⋮), with its one item, Skip today. */
export function SkipMenu({ name, onSkipToday }: { name: string; onSkipToday: () => void }) {
  return <Menu label={exerciseOptions(name)} items={[{ text: SKIP_TODAY, icon: <SkipNextIcon />, onSelect: onSkipToday }]} />
}

export type SkipReasonSheetProps = {
  name: string
  /** An answer was chosen; null is "Just skip". */
  onReason: (reason: SkipReason | null) => void
  /** Escape or a tap outside: nothing is stored. */
  onDismiss: () => void
}

/**
 * Asks why `name` is skipped. Choosing an answer calls `onReason`; Escape or a tap outside calls `onDismiss` and nothing
 * is stored. While it slides up and for SKIP_SHEET_TAP_WINDOW_MS after, taps on the answers do nothing (a tap outside still cancels, as on Android); the keyboard
 * (Enter, Space, Escape) is not held, as on Plan's add form.
 */
export function SkipReasonSheet({ name, onReason, onDismiss }: SkipReasonSheetProps) {
  // When the sheet finished sliding up, on the events' clock; null while it is still moving.
  const [openedAt, setOpenedAt] = useState<number | null>(null)
  const taps = useRef<HTMLDivElement>(null)
  useEffect(() => {
    let gone = false
    const panel = taps.current?.querySelector('.gr-dialog')
    const moving = panel?.getAnimations?.() ?? []
    Promise.all(moving.map((a) => a.finished.catch(() => {}))).then(() => {
      if (!gone) setOpenedAt(eventClock())
    })
    return () => {
      gone = true
    }
  }, [])
  const title = skipSheetTitle(name)
  // A click made by a tap (detail 1 or more) on an answer, while the sheet slides up or too soon after, stops here.
  const holdEarlyTaps = (e: MouseEvent<HTMLDivElement>) => {
    if (e.detail === 0 || (openedAt != null && e.timeStamp - openedAt >= SKIP_SHEET_TAP_WINDOW_MS)) return
    const target = e.target as Element
    // Only the answers: a tap outside cancels at once, as on Android, since nothing is stored by it.
    if (target.closest('.today-skip-answers') != null) e.stopPropagation()
  }
  return (
    <div ref={taps} className="today-skip-taps" onClickCapture={holdEarlyTaps}>
      <Dialog
        open
        variant="sheet"
        title={title}
        onDismiss={onDismiss}
        header={
          <div className="today-skip-head">
            <h2 className="headline-small">{title}</h2>
          </div>
        }
      >
        <div className="today-skip-answers" role="group" aria-label="Reason">
          {SKIP_ANSWERS.map(([text, reason]) => (
            <SecondaryButton key={text} text={text} onClick={() => onReason(reason)} />
          ))}
        </div>
      </Dialog>
    </div>
  )
}

/** The quiet line at the end of Today's list: what was skipped today, each with Bring back. */
export function SkippedTodayLine({ skipped, onBringBack }: { skipped: readonly SkippedToday[]; onBringBack: (exerciseId: number) => void }) {
  return (
    <section className="today-skipped" aria-label={SKIPPED_TODAY}>
      <h2 className="today-skipped-title label-large">{SKIPPED_TODAY}</h2>
      <ul role="list" className="today-skipped-list">
        {skipped.map((s) => (
          <li key={s.exerciseId} className="today-skipped-row">
            <span className="today-grow today-name body-medium">
              {s.name}
            </span>
            <QuietButton text={BRING_BACK} label={`${BRING_BACK} ${s.name}`} onClick={() => onBringBack(s.exerciseId)} />
          </li>
        ))}
      </ul>
    </section>
  )
}
