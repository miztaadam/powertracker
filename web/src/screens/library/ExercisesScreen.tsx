import { useRef, useState } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { exerciseCount, type ExerciseBrowser as BrowserState, type PickerEntry } from '../../state/exercisePicker'
import { planDayChoices, usePlan } from '../../state/usePlan'
import type { TodayActions, TodayState } from '../../state/useToday'
import { dayName } from '../../state/weekdayNames'
import { IconButton, PrimaryButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { CloseIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { Snackbar } from '../../ui/components/Snackbar'
import { ExerciseBrowser } from './ExerciseBrowser'
import { FormGuideSheet } from './FormGuideSheet'
import './library.css'

/** What the tab says when an add could not be stored. */
export const SAVE_FAILED = "Couldn't save — try again."

/** How long a message with a way on ("Open Plan") stays up. */
const ACTION_MESSAGE_MS = 10_000

export type ExercisesScreenProps = {
  db: GritDb
  /** Today's session, the one the Today tab logs to: whether it is open, and its own way of adding an exercise. */
  session: Pick<TodayState, 'loading' | 'sessionId' | 'summary'> & Pick<TodayActions, 'addToOpenSession'>
  onOpenPlan: () => void
  onOpenToday: () => void
  /** The search and filters, kept above the tab so they outlast a visit to another tab. */
  browser: BrowserState
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

type Message = { id: number; text: string; actionLabel?: string; onAction?: () => void }

/**
 * The Exercises tab: the catalogue and the lifter's own exercises to browse, each opening its form
 * guide with "Add to plan" and, during a session, "Add to today". Both go the way Plan and Today add
 * an exercise themselves. An add that fails says so; it never fails silently.
 */
export function ExercisesScreen({ db, session, onOpenPlan, onOpenToday, browser, today = todayIso }: ExercisesScreenProps) {
  const plan = usePlan(db, today)
  // The exercise whose sheet is open, and whether the sheet is on its guide or on the day to add it to.
  const [open, setOpen] = useState<PickerEntry | null>(null)
  const [choosingDay, setChoosingDay] = useState(false)
  const [message, setMessage] = useState<Message | null>(null)
  // A sheet answers once: a second tap landing before it closes (a double tap) adds nothing more and says nothing more.
  const answered = useRef(false)

  const show = (entry: PickerEntry) => {
    answered.current = false
    setChoosingDay(false)
    setOpen(entry)
  }
  const close = () => {
    setOpen(null)
    setChoosingDay(false)
  }
  /** One message at a time: the newest replaces the one on screen. */
  const say = (text: string, actionLabel?: string, onAction?: () => void) => setMessage((last) => ({ id: (last?.id ?? 0) + 1, text, actionLabel, onAction }))
  /** Runs the add once per sheet, closes the sheet when it has answered and says how it went; a failure is said too. */
  const once = <R,>(add: () => Promise<R>, report: (result: R) => void) => {
    if (answered.current) return
    answered.current = true
    add().then(
      (result) => {
        close()
        report(result)
      },
      () => {
        close()
        say(SAVE_FAILED)
      },
    )
  }

  const addToPlan = (entry: PickerEntry, weekday: number) =>
    once(
      () => plan.addOnce(weekday, entry),
      (result) => (result === 'ADDED' ? say(`Added to ${dayName(weekday)}`, 'Open Plan', onOpenPlan) : say(`Already in your plan on ${dayName(weekday)}.`)),
    )
  const addToToday = (entry: PickerEntry) =>
    once(
      // The session is looked at again now: it may have been finished since this button was drawn.
      () => session.addToOpenSession(entry.library?.name ?? null, entry.exerciseId),
      (result) => {
        if (result === 'ADDED') say('Added to today', 'Open Today', onOpenToday)
        else if (result === 'ALREADY_THERE') say("Already in today's session.")
        // Refused: there is no open session any more. Nothing was added.
        else if (result === 'REFUSED') say(SAVE_FAILED)
      },
    )
  const inSession = !session.loading && session.sessionId != null && session.summary == null

  return (
    <section className="screen lib-screen">
      <ScreenHeader title="Exercises" eyebrow={plan.loading ? undefined : `${exerciseCount(plan.exercises)} exercises`} />
      <ExerciseBrowser state={browser} muscleFilter="dropdown" exercises={plan.exercises} recentIds={plan.recentIds} onRow={show} onInfo={show} rowLabel={(name) => `Open ${name}`} thumbnails />

      {open != null && !choosingDay && (
        <FormGuideSheet
          name={open.name}
          onClose={close}
          actions={
            <>
              {inSession && <SecondaryButton text="Add to today" onClick={() => addToToday(open)} />}
              <PrimaryButton text="Add to plan" onClick={() => setChoosingDay(true)} />
            </>
          }
        />
      )}
      {open != null && choosingDay && <DayChooserSheet exerciseName={open.name} days={planDayChoices(plan.trainingDays)} onPick={(weekday) => addToPlan(open, weekday)} onClose={close} />}

      <Snackbar
        message={message?.text ?? null}
        showId={message?.id}
        actionLabel={message?.actionLabel}
        onAction={() => {
          const action = message?.onAction
          setMessage(null)
          action?.()
        }}
        onDismiss={() => setMessage(null)}
        durationMs={message?.actionLabel != null ? ACTION_MESSAGE_MS : undefined}
      />
    </section>
  )
}

/** The second step of "Add to plan", in the guide's place: which day the exercise goes on. Closing it closes the sheet. */
function DayChooserSheet({ exerciseName, days, onPick, onClose }: { exerciseName: string; days: readonly number[]; onPick: (weekday: number) => void; onClose: () => void }) {
  return (
    <Dialog
      open
      variant="sheet"
      portal
      title="Add to plan"
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">Add to plan</h2>
            <p className="lib-guide-where eyebrow">{exerciseName}</p>
          </div>
          <IconButton label="Close" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      <div className="lib-days">
        <p className="body-large">Which day?</p>
        {days.map((weekday) => (
          <SecondaryButton key={weekday} text={dayName(weekday)} onClick={() => onPick(weekday)} />
        ))}
      </div>
    </Dialog>
  )
}
