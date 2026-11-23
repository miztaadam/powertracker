import { useEffect, useRef, useState } from 'react'
import type { GritDb } from '../../../db/db'
import type { PlanSnapshot } from '../../../db/catalogue'
import { grit, todayIso, type JsDraftExercise, type JsLibraryExercise, type JsPlanDraft } from '../../../domain'
import { useExerciseBrowser } from '../../../state/exercisePicker'
import { BUILDER_STEPS, DAY_COUNTS, pickKey, usePlanBuilder, type PlanBuilderActions, type PlanBuilderState } from '../../../state/usePlanBuilder'
import { dayName } from '../../../state/weekdayNames'
import { IconButton, PrimaryButton, QuietButton, SecondaryButton } from '../../../ui/components/Button'
import { Card } from '../../../ui/components/Card'
import { Dialog } from '../../../ui/components/Dialog'
import { CloseIcon } from '../../../ui/components/icons'
import { ModeLabel } from '../../../ui/components/ModeLabel'
import { ScreenHeader } from '../../../ui/components/ScreenHeader'
import { SegmentedChoice } from '../../../ui/components/SegmentedChoice'
import { ExerciseBrowser, MuscleChecklist } from '../../library/ExerciseBrowser'
import { DraftLeftOut, DraftSessionCard, DraftVolume, SAVE_FAILED, SaveFailedLine, SwapSheet, WEEKDAYS_QUESTION, WeekdayChips, chooseDaysText, chosenDaysText } from './DraftParts'
import '../plan.css'
import './builder.css'

export { WEEKDAYS_QUESTION, WEEKLY_SETS_LINE } from './DraftParts'
export const BUILDER_TITLE = 'Build my plan'
export const DAYS_QUESTION = 'How many days a week?'
export const EXERCISES_QUESTION = 'Which exercises do you do now?'
export const EXERCISES_QUESTION_NONE = 'Which exercises would you like to do?'
export const BODY_PARTS_QUESTION = 'Which body parts do you most want to improve?'
export const BODY_PARTS_FULL = (max: number) => `That's ${max} already. Untick one to choose another.`
export const REPLACE_LINE = 'This replaces your bodybuilding plan. Your log is kept.'

export type PlanBuilderScreenProps = {
  db: GritDb
  /** Leaves the builder without saving: nothing is changed. Also where the powerlifting side is sent. */
  onLeave: () => void
  /** After "Use this plan" has saved: back to Plan, with what its snackbar can Undo when a plan was replaced (1.14.1). */
  onDone: (undo?: PlanSnapshot) => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/**
 * The bodybuilding plan builder (stage 2), in four steps: days, exercises, body parts, then the draft to adjust and
 * accept. Only the bodybuilding side has it. Nothing is stored until "Use this plan".
 */
export function PlanBuilderScreen({ db, onLeave, onDone, today = todayIso }: PlanBuilderScreenProps) {
  const builder = usePlanBuilder(db, today)
  const notHere = !builder.loading && builder.side !== 'BODYBUILDING'
  useEffect(() => {
    if (notHere) onLeave()
  }, [notHere, onLeave])
  if (builder.loading || notHere) return null
  return <PlanBuilderContent state={builder} actions={builder} onLeave={onLeave} onDone={onDone} />
}

type ContentProps = { state: PlanBuilderState; actions: PlanBuilderActions; onLeave: () => void; onDone: (undo?: PlanSnapshot) => void }

function PlanBuilderContent({ state, actions, onLeave, onDone }: ContentProps) {
  const { step } = state
  const number = BUILDER_STEPS.indexOf(step) + 1
  const [confirming, setConfirming] = useState(false)
  // A failed save is a plain line at the top of the draft, kept until the next try.
  const [failed, setFailed] = useState(false)
  const saving = useRef(false)

  const save = async () => {
    if (saving.current) return
    saving.current = true
    try {
      const undo = await actions.save()
      if (undo != null) onDone(undo)
      else onDone()
    } catch {
      setFailed(true)
    } finally {
      saving.current = false
    }
  }
  const use = async () => {
    setFailed(false)
    try {
      if (await actions.hasPlan()) setConfirming(true)
      else await save()
    } catch {
      setFailed(true)
    }
  }

  const nextText = step === 'exercises' && state.ticked.size === 0 ? 'Choose for me' : step === 'bodyParts' && state.priorities.length === 0 ? 'Skip' : 'Next'

  return (
    <section className={step === 'exercises' ? 'screen pb-screen pb-builder pb-screen-fill' : 'screen pb-screen pb-builder'}>
      <ScreenHeader title={BUILDER_TITLE} eyebrow={`Step ${number} of ${BUILDER_STEPS.length}`} label={<ModeLabel />} navigation={<IconButton label="Close" icon={<CloseIcon />} onClick={onLeave} />} />
      {step === 'days' && <DaysStep state={state} actions={actions} />}
      {step === 'exercises' && <ExercisesStep state={state} actions={actions} />}
      {step === 'bodyParts' && <BodyPartsStep state={state} actions={actions} />}
      {step === 'draft' && state.draft && <DraftStep draft={state.draft} actions={actions} failed={failed} />}
      <div className="pb-actions">
        {/* The first step has no step before it: Close leaves. */}
        {number > 1 && <SecondaryButton text="Back" onClick={actions.back} />}
        {step === 'draft' ? <PrimaryButton text="Use this plan" onClick={() => void use()} /> : <PrimaryButton text={nextText} onClick={actions.next} enabled={state.canGoOn} />}
      </div>
      {confirming && (
        <Dialog
          open
          className="pb-confirm-dialog"
          title="Use this plan?"
          onDismiss={() => setConfirming(false)}
          actions={
            <>
              <QuietButton text="Cancel" onClick={() => setConfirming(false)} />
              <PrimaryButton
                text="Replace"
                compact
                onClick={() => {
                  setConfirming(false)
                  void save()
                }}
              />
            </>
          }
        >
          <p className="body-medium">{REPLACE_LINE}</p>
        </Dialog>
      )}
    </section>
  )
}

type StepProps = { state: PlanBuilderState; actions: PlanBuilderActions }

function DaysStep({ state, actions }: StepProps) {
  const { dayCount, days } = state
  return (
    <div className="screen-body pb-body">
      <h2 className="pb-question headline-small">{DAYS_QUESTION}</h2>
      <SegmentedChoice
        label={DAYS_QUESTION}
        options={DAY_COUNTS.map((n) => `${n} days`)}
        selectedIndex={DAY_COUNTS.indexOf(dayCount)}
        onSelect={(i) => actions.chooseDayCount(DAY_COUNTS[i]!)}
      />
      <h2 className="pb-question headline-small">{WEEKDAYS_QUESTION}</h2>
      <WeekdayChips days={days} onDay={actions.toggleDay} />
      <p className="pb-hint body-medium">{days.length === dayCount ? chosenDaysText(days) : chooseDaysText(dayCount)}</p>
    </div>
  )
}

function ExercisesStep({ state, actions }: StepProps) {
  // The search and filters last while this step is on screen.
  const browser = useExerciseBrowser()
  const count = state.ticked.size
  return (
    <>
      <div className="pb-lead">
        <h2 className="pb-question headline-small">{count === 0 ? EXERCISES_QUESTION_NONE : EXERCISES_QUESTION}</h2>
        <p className="pb-hint body-medium" role="status">
          {`${count} chosen`}
        </p>
      </div>
      <ExerciseBrowser
        state={browser}
        muscleFilter="dropdown"
        exercises={state.exercises}
        recentIds={state.recentIds}
        onRow={actions.toggle}
        ticked={(entry) => state.ticked.has(pickKey(entry))}
        chosen={[...state.ticked.values()]}
      />
    </>
  )
}

function BodyPartsStep({ state, actions }: StepProps) {
  const max = grit.planBuilderMaxPriorities()
  const groups = grit.planBuilderTargetGroups().map((id) => ({ id, label: grit.muscleGroupLabel(id) }))
  return (
    <div className="screen-body pb-body">
      <h2 className="pb-question headline-small">{BODY_PARTS_QUESTION}</h2>
      <p className="pb-hint body-medium">{`Up to ${max}. They get more sets and go first in their sessions.`}</p>
      <Card contentPadding={0} className="pb-groups">
        <div role="group" aria-label={BODY_PARTS_QUESTION}>
          <MuscleChecklist groups={groups} ticked={state.priorities} onToggle={actions.togglePriority} />
        </div>
      </Card>
      <p className="pb-full body-medium" role="status">
        {state.prioritiesFull ? BODY_PARTS_FULL(max) : ''}
      </p>
    </div>
  )
}

type Swapping = { day: number; exercise: JsDraftExercise; options: JsLibraryExercise[] }

function DraftStep({ draft, actions, failed }: { draft: JsPlanDraft; actions: PlanBuilderActions; failed: boolean }) {
  const [swapping, setSwapping] = useState<Swapping | null>(null)
  // "Use this plan" is at the foot of the page: bring the line into view when a save fails.
  const failure = useRef<HTMLParagraphElement>(null)
  useEffect(() => {
    if (failed) failure.current?.scrollIntoView?.({ block: 'nearest' })
  }, [failed])
  return (
    <div className="screen-body pb-body">
      {failed && <SaveFailedLine text={SAVE_FAILED} lineRef={failure} />}
      <h2 className="pb-question headline-small">Your draft week</h2>
      <p className="pb-hint body-medium">Swap any exercise, or ask for another draft. Nothing is saved until you use this plan.</p>
      <SecondaryButton text="Another draft" onClick={actions.anotherDraft} className="pb-another" />
      {draft.sessions.map((session) => (
        <DraftSessionCard key={session.day} session={session} onSwap={(exercise) => setSwapping({ day: session.day, exercise, options: actions.alternatives(session.day, exercise.name) })} />
      ))}

      <DraftVolume volume={draft.volume} />
      <DraftLeftOut leftOut={draft.leftOut} />

      {swapping && (
        <SwapSheet
          name={swapping.exercise.name}
          where={dayName(swapping.day)}
          options={swapping.options}
          onPick={(to) => {
            actions.swap(swapping.day, swapping.exercise.name, to)
            setSwapping(null)
          }}
          onClose={() => setSwapping(null)}
        />
      )}
    </div>
  )
}
