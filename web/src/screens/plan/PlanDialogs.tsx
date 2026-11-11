import { useState } from 'react'
import { grit } from '../../domain'
import type { Lift } from '../../router'
import type { CustomExercise, PlanExercise, PlanItem } from '../../state/usePlan'
import { LIFTS } from '../../state/useProgress'
import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { cx, formatRpe } from '../../ui/components/format'
import { CheckIcon } from '../../ui/components/icons'
import { ChoiceChip } from '../../ui/components/SegmentedChoice'
import { Stepper } from '../../ui/components/StepperField'
import { TextField } from '../../ui/components/TextField'
import { LIFT_LABEL } from '../progress/lifts'
import './plan.css'

const noop = () => {}
const clamp = (value: number, low: number, high: number) => Math.min(high, Math.max(low, value))

export type PlanItemDialogProps = {
  title: string
  /** The exercises to choose from. */
  exercises: PlanExercise[]
  /** The item being edited, or null for a new one (3 × 5 at the default RPE). */
  initial: PlanItem | null
  defaultRpe: number
  /** Creates a custom exercise; resolves to its id, which becomes the selection. */
  onCreateCustom: (exercise: CustomExercise) => Promise<number>
  /** Called when creating the custom exercise failed. */
  onFailure: () => void
  onSave: (exerciseId: number, sets: number, reps: number, rpe: number) => void
  onDismiss: () => void
}

/** Pick an exercise (or create one) and its sets × reps @ RPE. */
export function PlanItemDialog({ title, exercises, initial, defaultRpe, onCreateCustom, onFailure, onSave, onDismiss }: PlanItemDialogProps) {
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(initial?.exerciseId ?? null)
  const [sets, setSets] = useState(initial?.sets ?? 3)
  const [reps, setReps] = useState(initial?.reps ?? 5)
  const [rpe, setRpe] = useState(initial?.targetRpe ?? clamp(defaultRpe, 6, 10))
  const [showCustom, setShowCustom] = useState(false)

  const q = query.trim().toLowerCase()
  const starts = (e: PlanExercise) => (e.name.toLowerCase().startsWith(q) ? 0 : 1)
  const matches = exercises
    .filter((e) => q === '' || e.name.toLowerCase().includes(q))
    .sort((a, b) => starts(a) - starts(b) || a.name.toLowerCase().localeCompare(b.name.toLowerCase()))
  const selected = exercises.find((e) => e.id === selectedId)

  return (
    <>
      <Dialog
        open
        title={title}
        // While the custom-exercise dialog is on top, Escape and taps belong to it.
        onDismiss={showCustom ? noop : onDismiss}
        actions={
          <>
            <QuietButton text="Cancel" onClick={onDismiss} />
            <PrimaryButton text="Save" onClick={() => selected && onSave(selected.id, sets, reps, rpe)} enabled={selected != null} compact />
          </>
        }
      >
        <div className="plan-form">
          <TextField label="Search exercises" value={query} onChange={setQuery} />
          <div className="plan-picker-list">
            {matches.map((exercise) => {
              const isSelected = exercise.id === selectedId
              return (
                <button key={exercise.id} type="button" className={cx('plan-picker-item body-large', isSelected && 'plan-picker-item-selected')} aria-pressed={isSelected} onClick={() => setSelectedId(exercise.id)}>
                  <span className="plan-grow plan-ellipsis">{exercise.name}</span>
                  {isSelected && <CheckIcon className="plan-picker-check" />}
                </button>
              )
            })}
          </div>
          <QuietButton text="Add custom exercise" onClick={() => setShowCustom(true)} />
          <p className="plan-selected">Selected: {selected?.name ?? 'none'}</p>
          <LabeledStepper label="Sets" value={String(sets)} canDecrement={sets > 1} canIncrement={sets < 10} onStep={(by) => setSets(sets + by)} />
          <LabeledStepper label="Reps" value={String(reps)} canDecrement={reps > 1} canIncrement={reps < 20} onStep={(by) => setReps(reps + by)} />
          <LabeledStepper label="RPE" value={formatRpe(rpe)} canDecrement={rpe > 6} canIncrement={rpe < 10} onStep={(by) => setRpe(rpe + by * 0.5)} />
        </div>
      </Dialog>
      {showCustom && (
        <CustomExerciseDialog
          onCreate={(exercise) => {
            setShowCustom(false)
            onCreateCustom(exercise).then(setSelectedId, onFailure)
          }}
          onDismiss={() => setShowCustom(false)}
        />
      )}
    </>
  )
}

function LabeledStepper({ label, value, canDecrement, canIncrement, onStep }: { label: string; value: string; canDecrement: boolean; canIncrement: boolean; onStep: (by: -1 | 1) => void }) {
  return (
    <div className="plan-stepper-row">
      <span className="title-medium" aria-hidden="true">
        {label}
      </span>
      <Stepper label={label} valueText={value} canDecrement={canDecrement} canIncrement={canIncrement} onDecrement={() => onStep(-1)} onIncrement={() => onStep(1)} />
    </div>
  )
}

type CustomExerciseDialogProps = { onCreate: (exercise: CustomExercise) => void; onDismiss: () => void }

/** Name a new exercise: a variation of a main lift, or an accessory. */
function CustomExerciseDialog({ onCreate, onDismiss }: CustomExerciseDialogProps) {
  const [name, setName] = useState('')
  const [category, setCategory] = useState<CustomExercise['category']>('ACCESSORY')
  const [parent, setParent] = useState<Lift | null>(null)
  const [muscles, setMuscles] = useState('')
  const valid = name.trim() !== '' && (category !== 'VARIATION' || parent != null)
  const create = () =>
    onCreate({
      name,
      category,
      parentLift: category === 'VARIATION' ? parent : null,
      muscles: muscles
        .split(',')
        .map((m) => m.trim())
        .filter((m) => m !== ''),
    })

  return (
    <Dialog
      open
      title="Add custom exercise"
      onDismiss={onDismiss}
      actions={
        <>
          <QuietButton text="Cancel" onClick={onDismiss} />
          <PrimaryButton text="Create" onClick={create} enabled={valid} compact />
        </>
      }
    >
      <div className="plan-form">
        <TextField label="Name" value={name} onChange={setName} autoFocus />
        <div className="plan-chips">
          <ChoiceChip text="Variation" selected={category === 'VARIATION'} onClick={() => setCategory('VARIATION')} />
          <ChoiceChip text="Accessory" selected={category === 'ACCESSORY'} onClick={() => setCategory('ACCESSORY')} />
        </div>
        {category === 'VARIATION' && (
          <div role="group" aria-label="Parent lift">
            <p className="plan-form-label title-small" aria-hidden="true">
              Parent lift
            </p>
            <div className="plan-chips">
              {LIFTS.map((lift) => (
                <ChoiceChip key={lift} text={LIFT_LABEL[lift]} selected={parent === lift} onClick={() => setParent(lift)} />
              ))}
            </div>
          </div>
        )}
        <TextField label="Muscles (comma-separated)" value={muscles} onChange={setMuscles} />
      </div>
    </Dialog>
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
