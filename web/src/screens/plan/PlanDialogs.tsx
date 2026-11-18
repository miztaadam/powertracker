import { useRef, useState, type MouseEvent, type ReactNode } from 'react'
import { grit } from '../../domain'
import type { CustomExercise, PickerEntry, StoredExercise } from '../../state/exercisePicker'
import { sideOf } from '../../db/schema'
import { MAX_PLAN_REPS, MAX_RANGE_REPS, MAX_RPE, MIN_RPE, type PlanItem } from '../../state/usePlan'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { formatRpe } from '../../ui/components/format'
import { Eyebrow } from '../../ui/components/NumberDisplay'
import { Stepper } from '../../ui/components/StepperField'
import { TextField } from '../../ui/components/TextField'
import { ExercisePicker } from '../library/ExercisePicker'
import { RpeInfoButton } from '../library/RpeSheet'
import './plan.css'

export type PlanItemDialogProps = {
  title: string
  /** A stored item (Edit), or the ready-made numbers of one not stored yet (Plan's Add), which has no id. */
  item: Pick<PlanItem, 'sets' | 'reps' | 'targetRpe' | 'repsHigh'> & Partial<Pick<PlanItem, 'mode'>>
  /** The name of the item's exercise as stored, or of the exercise just picked. */
  exerciseName: string
  /** The confirm button: "Save" (Edit), or "Add" on Plan's Add. */
  confirmText?: string
  /** The lifter's exercises and the recently used ones, for the picker behind "Change". */
  exercises: readonly StoredExercise[]
  recentIds: readonly number[]
  /** Creates a custom exercise; resolves to its picker entry, which becomes the item's exercise on Save. */
  onCreateCustom: (exercise: CustomExercise) => Promise<PickerEntry>
  /** Creating the custom exercise failed after its picker had closed. */
  onFailure: () => void
  /** `exercise` is null unless the exercise itself was changed. `repsHigh` is given for a range (a bodybuilding item). */
  onSave: (exercise: PickerEntry | null, sets: number, reps: number, rpe: number, repsHigh?: number) => void
  onDismiss: () => void
  /**
   * For this many ms after it opens, taps on the confirm button, on Cancel and outside the dialog do nothing: Plan's add
   * form opens under the finger that picked, where a double tap's second tap lands. Keyboard (Enter, Space, Escape) is
   * not held; neither are the steppers and Change. Default 0 (Edit): nothing is held.
   */
  ignoreTapsFor?: number
}

/** How long Plan's add form ignores taps on Add, Cancel and outside it once it opens (see `ignoreTapsFor`). */
export const ADD_FORM_TAP_WINDOW_MS = 300

/** The time now, on the clock events are stamped with (`performance.now()` can be faked in tests; this is not). */
const eventClock = () => new Event('now').timeStamp

/**
 * Sets × reps @ RPE of a plan item, and its exercise, which "Change" swaps through the picker. An item with a rep
 * range (the bodybuilding side) has two reps steppers, the bottom and the top; the top never goes below the bottom
 * and goes up to 30. Fixed reps (the powerlifting side) go up to 20.
 */
export function PlanItemDialog({ title, item, exerciseName, confirmText = 'Save', exercises, recentIds, onCreateCustom, onFailure, onSave, onDismiss, ignoreTapsFor = 0 }: PlanItemDialogProps) {
  // Null until the exercise is changed; saved with the item, not before.
  const [changed, setChanged] = useState<PickerEntry | null>(null)
  const [sets, setSets] = useState(item.sets)
  const [reps, setReps] = useState(item.reps)
  // A bodybuilding item always has a range; one with no top (restored or made by hand) opens as reps to reps.
  const [repsHigh, setRepsHigh] = useState(item.repsHigh ?? (sideOf(item) === 'BODYBUILDING' ? item.reps : null))
  const [rpe, setRpe] = useState(item.targetRpe)
  const [picking, setPicking] = useState(false)
  const [openedAt] = useState(eventClock)
  const actionsRef = useRef<HTMLSpanElement>(null)

  const buttons = (
    <>
      <QuietButton text="Cancel" onClick={onDismiss} />
      <PrimaryButton text={confirmText} onClick={() => onSave(changed, sets, reps, rpe, repsHigh ?? undefined)} compact />
    </>
  )
  const dialog = (
    <Dialog open title={title} onDismiss={onDismiss} actions={ignoreTapsFor > 0 ? <span ref={actionsRef} className="plan-form-actions">{buttons}</span> : buttons}>
      <div className="plan-form">
        <div className="plan-exercise" role="group" aria-label="Exercise">
          <div className="plan-grow">
            <Eyebrow text="Exercise" />
            <p className="title-medium">{changed?.name ?? exerciseName}</p>
          </div>
          <SecondaryButton text="Change" onClick={() => setPicking(true)} compact />
        </div>
        <LabeledStepper label="Sets" value={String(sets)} canDecrement={sets > 1} canIncrement={sets < 10} onStep={(by) => setSets(sets + by)} />
        {repsHigh == null ? (
          <LabeledStepper label="Reps" value={String(reps)} canDecrement={reps > 1} canIncrement={reps < MAX_PLAN_REPS} onStep={(by) => setReps(reps + by)} />
        ) : (
          <>
            <LabeledStepper label="Reps from" value={String(reps)} canDecrement={reps > 1} canIncrement={reps < repsHigh} onStep={(by) => setReps(reps + by)} />
            <LabeledStepper label="Reps to" value={String(repsHigh)} canDecrement={repsHigh > reps} canIncrement={repsHigh < MAX_RANGE_REPS} onStep={(by) => setRepsHigh(repsHigh + by)} />
          </>
        )}
        <LabeledStepper label="RPE" value={formatRpe(rpe)} canDecrement={rpe > MIN_RPE} canIncrement={rpe < MAX_RPE} onStep={(by) => setRpe(rpe + by * 0.5)} labelAction={<RpeInfoButton />} />
      </div>
    </Dialog>
  )
  // A click made by a tap (detail 1 or more) on Add, Cancel or the scrim, too soon after the form opened, stops here.
  // Enter and Space on a button click with detail 0, and Escape is a key, so neither is held. A screen reader that
  // activates with a plain click (detail 0) is not held either; one that simulates a tap is, like a tap.
  const holdEarlyTaps = (e: MouseEvent<HTMLDivElement>) => {
    if (e.detail === 0 || e.timeStamp - openedAt >= ignoreTapsFor) return
    const target = e.target as Node
    // The Dialog is drawn in place, so the scrim is this wrapper's only child.
    if (target === e.currentTarget.firstElementChild || actionsRef.current?.contains(target)) e.stopPropagation()
  }

  return (
    <>
      {ignoreTapsFor > 0 ? (
        <div className="plan-form-taps" onClickCapture={holdEarlyTaps}>
          {dialog}
        </div>
      ) : (
        dialog
      )}
      {picking && (
        <ExercisePicker
          title="Change exercise"
          exercises={exercises}
          recentIds={recentIds}
          onPick={(entry) => {
            setChanged(entry)
            setPicking(false)
          }}
          // The picker stays until the exercise is made; if making it fails, its create form stays with what was typed.
          onCreateCustom={(exercise) =>
            onCreateCustom(exercise).then((entry) => {
              setChanged(entry)
              setPicking(false)
            })
          }
          onDismiss={() => setPicking(false)}
          onCreateFailedWhileClosed={onFailure}
        />
      )}
    </>
  )
}

function LabeledStepper({
  label,
  value,
  canDecrement,
  canIncrement,
  onStep,
  labelAction,
}: {
  label: string
  value: string
  canDecrement: boolean
  canIncrement: boolean
  onStep: (by: -1 | 1) => void
  /** Goes right after the label (the RPE info button). */
  labelAction?: ReactNode
}) {
  return (
    <div className="plan-stepper-row">
      <span className="plan-stepper-label">
        <span className="title-medium" aria-hidden="true">
          {label}
        </span>
        {labelAction}
      </span>
      <Stepper label={label} valueText={value} canDecrement={canDecrement} canIncrement={canIncrement} onDecrement={() => onStep(-1)} onIncrement={() => onStep(1)} />
    </div>
  )
}

export type BaselineDialogProps = {
  liftName: string
  /** Receives the typed text, which `parseBaselineKg` accepts. */
  onSave: (text: string) => void
  onDismiss: () => void
}

/** Type a current best for a main lift. Only finite values up to 500 kg can be saved. */
export function BaselineDialog({ liftName, onSave, onDismiss }: BaselineDialogProps) {
  const [text, setText] = useState('')
  const valid = grit.parseBaselineKg(text) != null
  const save = () => valid && onSave(text)
  return (
    <Dialog
      open
      title={`Known best: ${liftName}`}
      onDismiss={onDismiss}
      actions={
        <>
          <QuietButton text="Cancel" onClick={onDismiss} />
          <PrimaryButton text="Save" onClick={save} enabled={valid} compact />
        </>
      }
    >
      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          save()
        }}
      >
        <TextField label="Estimated 1RM (kg)" value={text} onChange={setText} numeric autoFocus />
      </form>
    </Dialog>
  )
}
