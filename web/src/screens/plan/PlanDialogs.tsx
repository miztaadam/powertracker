import { useState, type ReactNode } from 'react'
import { grit } from '../../domain'
import type { CustomExercise, PickerEntry, StoredExercise } from '../../state/exercisePicker'
import { MAX_RPE, MIN_RPE, type PlanItem } from '../../state/usePlan'
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
  item: PlanItem
  /** The name of the item's exercise as stored. */
  exerciseName: string
  /** The lifter's exercises and the recently used ones, for the picker behind "Change". */
  exercises: readonly StoredExercise[]
  recentIds: readonly number[]
  /** Creates a custom exercise; resolves to its picker entry, which becomes the item's exercise on Save. */
  onCreateCustom: (exercise: CustomExercise) => Promise<PickerEntry>
  /** Called when creating the custom exercise failed. */
  onFailure: () => void
  /** `exercise` is null unless the exercise itself was changed. */
  onSave: (exercise: PickerEntry | null, sets: number, reps: number, rpe: number) => void
  onDismiss: () => void
}

/** Sets × reps @ RPE of a plan item, and its exercise, which "Change" swaps through the picker. */
export function PlanItemDialog({ title, item, exerciseName, exercises, recentIds, onCreateCustom, onFailure, onSave, onDismiss }: PlanItemDialogProps) {
  // Null until the exercise is changed; saved with the item, not before.
  const [changed, setChanged] = useState<PickerEntry | null>(null)
  const [sets, setSets] = useState(item.sets)
  const [reps, setReps] = useState(item.reps)
  const [rpe, setRpe] = useState(item.targetRpe)
  const [picking, setPicking] = useState(false)

  return (
    <>
      <Dialog
        open
        title={title}
        onDismiss={onDismiss}
        actions={
          <>
            <QuietButton text="Cancel" onClick={onDismiss} />
            <PrimaryButton text="Save" onClick={() => onSave(changed, sets, reps, rpe)} compact />
          </>
        }
      >
        <div className="plan-form">
          <div className="plan-exercise" role="group" aria-label="Exercise">
            <div className="plan-grow">
              <Eyebrow text="Exercise" />
              <p className="title-medium">{changed?.name ?? exerciseName}</p>
            </div>
            <SecondaryButton text="Change" onClick={() => setPicking(true)} compact />
          </div>
          <LabeledStepper label="Sets" value={String(sets)} canDecrement={sets > 1} canIncrement={sets < 10} onStep={(by) => setSets(sets + by)} />
          <LabeledStepper label="Reps" value={String(reps)} canDecrement={reps > 1} canIncrement={reps < 20} onStep={(by) => setReps(reps + by)} />
          <LabeledStepper label="RPE" value={formatRpe(rpe)} canDecrement={rpe > MIN_RPE} canIncrement={rpe < MAX_RPE} onStep={(by) => setRpe(rpe + by * 0.5)} labelAction={<RpeInfoButton />} />
        </div>
      </Dialog>
      {picking && (
        <ExercisePicker
          title="Change exercise"
          exercises={exercises}
          recentIds={recentIds}
          onPick={(entry) => {
            setChanged(entry)
            setPicking(false)
          }}
          onCreateCustom={(exercise) => {
            setPicking(false)
            onCreateCustom(exercise).then(setChanged, onFailure)
          }}
          onDismiss={() => setPicking(false)}
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
