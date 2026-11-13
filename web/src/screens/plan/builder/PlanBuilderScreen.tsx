import { useEffect, useRef, useState } from 'react'
import type { GritDb } from '../../../db/db'
import { grit, todayIso, type JsDraftExercise, type JsDraftSession, type JsLibraryExercise, type JsPlanDraft } from '../../../domain'
import { useExerciseBrowser } from '../../../state/exercisePicker'
import { BUILDER_STEPS, DAY_COUNTS, pickKey, usePlanBuilder, type PlanBuilderActions, type PlanBuilderState } from '../../../state/usePlanBuilder'
import { dayName } from '../../../state/weekdayNames'
import { IconButton, PrimaryButton, QuietButton, SecondaryButton } from '../../../ui/components/Button'
import { Card } from '../../../ui/components/Card'
import { Dialog } from '../../../ui/components/Dialog'
import { CloseIcon } from '../../../ui/components/icons'
import { ModeLabel } from '../../../ui/components/ModeLabel'
import { ScreenHeader, SectionHeader } from '../../../ui/components/ScreenHeader'
import { ChoiceChip, SegmentedChoice } from '../../../ui/components/SegmentedChoice'
import { ExerciseBrowser, MuscleChecklist } from '../../library/ExerciseBrowser'
import { planItemText, repsOf } from '../PlanScreen'
import { formatRpe } from '../../../ui/components/format'
import '../plan.css'
import './builder.css'

export const BUILDER_TITLE = 'Build my plan'
export const DAYS_QUESTION = 'How many days a week?'
export const WEEKDAYS_QUESTION = 'Which days?'
export const EXERCISES_QUESTION = 'Which exercises do you do now?'
export const EXERCISES_QUESTION_NONE = 'Which exercises would you like to do?'
export const BODY_PARTS_QUESTION = 'Which body parts do you most want to improve?'
export const BODY_PARTS_FULL = (max: number) => `That's ${max} already. Untick one to choose another.`
export const WEEKLY_SETS_LINE = '10 or more a week is the guideline for growth. Make a body part a priority to give it more.'
export const REPLACE_LINE = 'This replaces your bodybuilding plan. Your log is kept.'
const SAVE_FAILED = 'The plan could not be saved. Nothing was changed.'

export type PlanBuilderScreenProps = {
  db: GritDb
  /** Leaves the builder without saving: nothing is changed. Also where the powerlifting side is sent. */
  onLeave: () => void
  /** After "Use this plan" has saved: back to Plan. */
  onDone: () => void
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

type ContentProps = { state: PlanBuilderState; actions: PlanBuilderActions; onLeave: () => void; onDone: () => void }

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
      await actions.save()
      onDone()
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
    <section className={step === 'exercises' ? 'screen pb-screen pb-screen-fill' : 'screen pb-screen'}>
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

const WEEK = [1, 2, 3, 4, 5, 6, 7]
/** "Mon", as on the weekday chips. */
const shortDay = (day: number) => dayName(day, 'short').slice(0, 3)

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
      <Card contentPadding={0} className="pb-days">
        <div className="pb-days-row" role="group" aria-label={WEEKDAYS_QUESTION}>
          {WEEK.map((day) => (
            <ChoiceChip key={day} text={shortDay(day)} selected={days.includes(day)} onClick={() => actions.toggleDay(day)} />
          ))}
        </div>
      </Card>
      <p className="pb-hint body-medium">{days.length === dayCount ? days.map(shortDay).join(', ') : `Choose ${dayCount} days.`}</p>
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
      {failed && (
        <p ref={failure} className="pb-failed body-medium" role="alert">
          {SAVE_FAILED}
        </p>
      )}
      <h2 className="pb-question headline-small">Your draft week</h2>
      <p className="pb-hint body-medium">Swap any exercise, or ask for another draft. Nothing is saved until you use this plan.</p>
      <SecondaryButton text="Another draft" onClick={actions.anotherDraft} className="pb-another" />
      {draft.sessions.map((session) => (
        <DraftSessionCard key={session.day} session={session} onSwap={(exercise) => setSwapping({ day: session.day, exercise, options: actions.alternatives(session.day, exercise.name) })} />
      ))}

      <section aria-label="Weekly sets">
        <SectionHeader title="Weekly sets" />
        <Card contentPadding={16} spacing={0}>
          <ul className="pb-volume">
            {draft.volume.map((v) => {
              const label = grit.muscleGroupLabel(v.group)
              return (
                <li key={v.group} className="pb-volume-row" aria-label={`${label}, ${v.weeklySets} sets${v.short ? ', under 10' : ''}`}>
                  <span className="pb-grow title-medium" aria-hidden="true">
                    {label}
                  </span>
                  {v.short && (
                    <span className="pb-short label-medium" aria-hidden="true">
                      Under 10
                    </span>
                  )}
                  <span className="pb-sets title-medium" aria-hidden="true">
                    {v.weeklySets}
                  </span>
                </li>
              )
            })}
          </ul>
          <p className="pb-note body-small">{WEEKLY_SETS_LINE}</p>
        </Card>
      </section>

      {draft.leftOut.length > 0 && (
        <section aria-label="Left out">
          <SectionHeader title="Left out" />
          <Card contentPadding={16} spacing={0}>
            <ul className="pb-left-out">
              {draft.leftOut.map((l) => (
                <li key={l.name} className="pb-left-out-row">
                  <p className="title-medium">{l.name}</p>
                  <p className="pb-note body-small">{l.reason}</p>
                </li>
              ))}
            </ul>
          </Card>
        </section>
      )}

      {swapping && (
        <SwapSheet
          swapping={swapping}
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

const prescription = (e: JsDraftExercise) => ({ sets: e.sets, reps: e.repsLow, repsHigh: e.repsHigh, targetRpe: e.rpe })

function DraftSessionCard({ session, onSwap }: { session: JsDraftSession; onSwap: (exercise: JsDraftExercise) => void }) {
  const name = dayName(session.day)
  const sets = session.exercises.reduce((sum, e) => sum + e.sets, 0)
  return (
    <section aria-label={name}>
      <Card contentPadding={0} spacing={0} className="plan-day">
        <div className="plan-day-head">
          <div className="plan-grow">
            <h2 className="headline-medium">{`${name} · ${session.title}`}</h2>
            <p className="plan-muted body-small">{`${session.exercises.length} exercises · ${sets} sets`}</p>
          </div>
        </div>
        <ol className="plan-rows">
          {session.exercises.map((e, i) => (
            <li key={e.name} className="plan-row" aria-label={planItemText(e.name, prescription(e))}>
              <span className="plan-row-position title-small" aria-hidden="true">
                {i + 1}
              </span>
              <div className="plan-grow plan-row-text" aria-hidden="true">
                <p className="plan-row-name title-medium">{e.name}</p>
                <p className="pb-dose title-medium">
                  {e.sets} × {repsOf(prescription(e))}
                  <span className="plan-muted"> @ </span>
                  {formatRpe(e.rpe)}
                </p>
              </div>
              <SecondaryButton text="Swap" label={`Swap ${e.name}`} onClick={() => onSwap(e)} compact />
            </li>
          ))}
        </ol>
      </Card>
    </section>
  )
}

function SwapSheet({ swapping, onPick, onClose }: { swapping: Swapping; onPick: (name: string) => void; onClose: () => void }) {
  const title = `Swap ${swapping.exercise.name}`
  return (
    <Dialog
      open
      variant="sheet"
      portal
      title={title}
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">{title}</h2>
            <p className="lib-guide-where eyebrow">{dayName(swapping.day)}</p>
          </div>
          <IconButton label="Close" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      {swapping.options.length === 0 ? (
        <p className="pb-none body-medium">Nothing else in the library fits this place.</p>
      ) : (
        <ul className="lib-rows">
          {swapping.options.map((option) => (
            <li key={option.name} className="lib-row" aria-label={option.name}>
              <button type="button" className="lib-row-main" onClick={() => onPick(option.name)}>
                <span className="lib-grow">
                  <span className="lib-row-name title-medium">{option.name}</span>
                  {option.muscles.length > 0 && <span className="lib-row-muscles body-medium">{option.muscles.join(', ')}</span>}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}
