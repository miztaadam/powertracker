import { useEffect, useRef, useState } from 'react'
import type { GritDb } from '../../db/db'
import { grit, todayIso } from '../../domain'
import { useCheckIn, type CheckInActions, type CheckInState, type SavedLine } from '../../state/useCheckIn'
import { IconButton, PrimaryButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { afterDialogEntries, historyBack } from '../../ui/components/historyCloses'
import { CloseIcon } from '../../ui/components/icons'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { MuscleChecklist } from '../library/ExerciseBrowser'
import './checkin.css'

export const CHECK_IN_TITLE = 'Check-in'
export const LAGGING_QUESTION = 'Which body parts are lagging?'
export const LAGGING_LINE = 'Leave a body part unticked if it is on track.'
export const GROWING_QUESTION = 'Which are growing well?'
export const GROWING_LINE = "Body parts marked lagging can't also be growing well."
export const SAVED_TITLE = 'Check-in saved'
const SAVE_FAILED = 'The check-in could not be saved. Nothing was changed.'

/** The words for one lagging body part on the saved step. */
export function savedLineText(line: SavedLine): string {
  if (line.kind === 'advice') return 'More work suggested on Coach'
  const label = grit.muscleGroupLabel(line.group)
  switch (line.reason) {
    case 'AT_CEILING':
      return `${label} already gets ${line.weeklySets} sets a week, the most the coach adds.`
    case 'NO_EXERCISE':
      return `No other ${label.toLowerCase()} exercise uses the equipment in your plan.`
    case 'FULL':
      return 'Every session that could take one already has 10 exercises.'
    case 'LATER':
      return `The coach adds to three body parts at a time. It will look at ${label.toLowerCase()} again at your next check-in.`
    case 'WOULD_PASS_CEILING':
      return `More ${label.toLowerCase()} work would take a body part past 20 sets a week.`
  }
}

export type CheckInScreenProps = {
  db: GritDb
  /** Leaves without saving: nothing is changed. Also where the powerlifting side is sent. */
  onLeave: () => void
  /** After saving, "See Coach". */
  onSeeCoach: () => void
  /** After saving, "Done". */
  onDone: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/**
 * The check-in (bodybuilding mode stage 3), in the bodybuilding look: which body parts are lagging, which are growing
 * well, then what the coach made of it. Every body part starts on track. Nothing is stored until Save.
 */
export function CheckInScreen({ db, onLeave, onSeeCoach, onDone, today = todayIso }: CheckInScreenProps) {
  const checkIn = useCheckIn(db, today)
  const notHere = !checkIn.loading && checkIn.side !== 'BODYBUILDING'
  useEffect(() => {
    if (notHere) onLeave()
  }, [notHere, onLeave])
  if (checkIn.loading || notHere) return null
  return <CheckInContent state={checkIn} actions={checkIn} onLeave={onLeave} onSeeCoach={onSeeCoach} onDone={onDone} />
}

type ContentProps = { state: CheckInState; actions: CheckInActions; onLeave: () => void; onSeeCoach: () => void; onDone: () => void }

const groups = () => grit.muscleGroups().map((id) => ({ id, label: grit.muscleGroupLabel(id) }))

/** What the history entry of step 2 carries, beside what the page's own entry had (its `gritFrom`). */
type StepEntry = { checkInStep?: 'growing' }
const onStepEntry = () => (window.history.state as StepEntry | null)?.checkInStep === 'growing'

function CheckInContent({ state, actions, onLeave, onSeeCoach, onDone }: ContentProps) {
  const { step } = state
  // Step 2 has its own history entry (same address), so the browser's Back steps back to step 1, as Android's does;
  // Back from step 1 leaves. What to do on the next popstate when it is not that: stay (after saving) or leave.
  const nextPop = useRef<null | 'stay' | (() => void)>(null)
  const stepNow = useRef(step)
  stepNow.current = step
  const { back } = actions
  useEffect(() => {
    const onPop = () => {
      const then = nextPop.current
      nextPop.current = null
      if (then === 'stay') return
      if (then != null) then()
      else if (stepNow.current === 'growing') back()
    }
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [back])
  // Every history call here, and every read of `history.state` that decides one, goes through `afterDialogEntries`
  // (1.17.4): on an iPhone or iPad it runs once the entries kept for open dialogs are off the stack; elsewhere at once.
  const next = () => {
    afterDialogEntries(() => window.history.pushState({ ...((window.history.state as object | null) ?? {}), checkInStep: 'growing' } satisfies StepEntry, ''))
    actions.next()
  }
  /** Drops step 2's entry, then does `then` ('stay' keeps the screen as it is). */
  const dropStepEntry = (then: 'stay' | (() => void)) =>
    afterDialogEntries(() => {
      if (!onStepEntry()) {
        if (then !== 'stay') then()
        return
      }
      nextPop.current = then
      historyBack()
    })
  // A failed save is a plain line over the buttons, until the next try.
  const [failed, setFailed] = useState(false)
  const failure = useRef<HTMLParagraphElement>(null)
  useEffect(() => {
    if (failed) failure.current?.scrollIntoView?.({ block: 'nearest' })
  }, [failed])
  const save = async () => {
    setFailed(false)
    try {
      await actions.save()
      // Saved: Back no longer goes to step 2, and Done or See Coach leave from the page's own entry.
      dropStepEntry('stay')
    } catch {
      setFailed(true)
    }
  }
  const eyebrow = step === 'lagging' ? 'Step 1 of 2' : step === 'growing' ? 'Step 2 of 2' : undefined
  return (
    <section className="screen ci-screen">
      <ScreenHeader
        title={CHECK_IN_TITLE}
        eyebrow={eyebrow}
        label={<ModeLabel />}
        navigation={step === 'saved' ? undefined : <IconButton label="Close" icon={<CloseIcon />} onClick={() => dropStepEntry(onLeave)} />}
      />
      {step === 'lagging' && (
        <div className="screen-body ci-body">
          <h2 className="ci-question headline-small">{LAGGING_QUESTION}</h2>
          <p className="ci-hint body-medium">{LAGGING_LINE}</p>
          <Card contentPadding={0} className="ci-groups">
            <div role="group" aria-label={LAGGING_QUESTION}>
              <MuscleChecklist groups={groups()} ticked={state.lagging} onToggle={actions.toggleLagging} />
            </div>
          </Card>
        </div>
      )}
      {step === 'growing' && (
        <div className="screen-body ci-body">
          <h2 className="ci-question headline-small">{GROWING_QUESTION}</h2>
          <p className="ci-hint body-medium">{GROWING_LINE}</p>
          <Card contentPadding={0} className="ci-groups">
            <div role="group" aria-label={GROWING_QUESTION}>
              <MuscleChecklist groups={groups()} ticked={state.growingWell} onToggle={actions.toggleGrowing} disabled={state.lagging} />
            </div>
          </Card>
          {failed && (
            <p ref={failure} className="ci-failed body-medium" role="alert">
              {SAVE_FAILED}
            </p>
          )}
        </div>
      )}
      {step === 'saved' && (
        <div className="screen-body ci-body">
          <h2 className="ci-question headline-small">{SAVED_TITLE}</h2>
          {state.saved.length > 0 && (
            <Card contentPadding={0}>
              <ul className="ci-lines" aria-label="Lagging body parts">
                {state.saved.map((line) => (
                  <li key={line.group} className="ci-line">
                    <span className="ci-line-group title-medium">{grit.muscleGroupLabel(line.group)}</span>
                    <span className="ci-line-text body-medium">{savedLineText(line)}</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </div>
      )}
      <div className="ci-actions">
        {step === 'lagging' && <PrimaryButton text="Next" onClick={next} />}
        {step === 'growing' && (
          <>
            {/* As the browser's Back: through history when step 2 has its entry. */}
            <SecondaryButton text="Back" onClick={() => afterDialogEntries(() => (onStepEntry() ? historyBack() : actions.back()))} />
            <PrimaryButton text="Save" onClick={() => void save()} enabled={!state.saving} />
          </>
        )}
        {step === 'saved' && (
          <>
            {/* As on Android: with a card to see, See Coach is the main action. */}
            {state.hasAdvice ? (
              <>
                <SecondaryButton text="Done" onClick={onDone} />
                <PrimaryButton text="See Coach" onClick={onSeeCoach} />
              </>
            ) : (
              <PrimaryButton text="Done" onClick={onDone} />
            )}
          </>
        )}
      </div>
    </section>
  )
}
