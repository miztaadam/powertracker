import type { SkipReason } from '../../db/schema'
import type { SkippedToday } from '../../state/useToday'
import { QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { SkipNextIcon, TuneIcon } from '../../ui/components/icons'
import { Menu, type MenuItem } from '../../ui/components/Menu'
import { COUNT_PLATES } from './PlateCount'
import { SHEET_TAP_WINDOW_MS, useEarlyTapGuard } from './tapGuard'
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

/** How long, in ms, the reason sheet ignores taps on its answers once it is open (see `SHEET_TAP_WINDOW_MS`). */
export const SKIP_SHEET_TAP_WINDOW_MS = SHEET_TAP_WINDOW_MS

export const ONE_SIDE_AT_A_TIME = 'One side at a time'
export const ONE_SIDE_ON = 'One side at a time · On'

/**
 * The row menu (⋮): Skip today with `onSkipToday`, Count plates on this machine with `onCountPlates` (1.17),
 * and One side at a time with `onToggleSingleSided`. Draws nothing when none are given.
 */
export function SkipMenu({
  name,
  onSkipToday,
  onCountPlates,
  singleSided,
  onToggleSingleSided,
}: {
  name: string
  onSkipToday?: () => void
  onCountPlates?: () => void
  singleSided?: boolean
  onToggleSingleSided?: () => void
}) {
  const items: MenuItem[] = [
    ...(onSkipToday ? [{ text: SKIP_TODAY, icon: <SkipNextIcon />, onSelect: onSkipToday }] : []),
    ...(onCountPlates ? [{ text: COUNT_PLATES, icon: <TuneIcon />, onSelect: onCountPlates, testId: 'exercise-menu-plates' }] : []),
    ...(onToggleSingleSided ? [{ text: singleSided ? ONE_SIDE_ON : ONE_SIDE_AT_A_TIME, icon: <TuneIcon />, onSelect: onToggleSingleSided, testId: 'exercise-menu-single-sided' }] : []),
  ]
  if (items.length === 0) return null
  return <Menu label={exerciseOptions(name)} items={items} />
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
  // Only the answers are held: a tap outside cancels at once, as on Android, since nothing is stored by it.
  const taps = useEarlyTapGuard('.today-skip-answers')
  const title = skipSheetTitle(name)
  return (
    <div ref={taps.ref} className="today-skip-taps" onClickCapture={taps.onClickCapture}>
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
