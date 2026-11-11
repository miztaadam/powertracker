import { useState } from 'react'
import type { Lift } from '../../router'
import type { CustomExercise } from '../../state/exercisePicker'
import { LIFTS } from '../../state/useProgress'
import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { ChoiceChip } from '../../ui/components/SegmentedChoice'
import { TextField } from '../../ui/components/TextField'
import { LIFT_LABEL } from '../progress/lifts'
import './library.css'

export type CustomExerciseDialogProps = {
  /** What was searched for, as a start. */
  initialName: string
  onCreate: (exercise: CustomExercise) => void
  onDismiss: () => void
}

/** Name a new exercise the catalogue does not have: a variation of a main lift, or an accessory. */
export function CustomExerciseDialog({ initialName, onCreate, onDismiss }: CustomExerciseDialogProps) {
  const [name, setName] = useState(initialName)
  const [category, setCategory] = useState<CustomExercise['category']>('ACCESSORY')
  const [parent, setParent] = useState<Lift | null>(null)
  const [muscles, setMuscles] = useState('')
  const valid = name.trim() !== '' && (category !== 'VARIATION' || parent != null)
  const create = () =>
    onCreate({
      name: name.trim(),
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
      <div className="lib-form">
        <TextField label="Name" value={name} onChange={setName} autoFocus />
        <div className="lib-form-chips">
          <ChoiceChip text="Variation" selected={category === 'VARIATION'} onClick={() => setCategory('VARIATION')} />
          <ChoiceChip text="Accessory" selected={category === 'ACCESSORY'} onClick={() => setCategory('ACCESSORY')} />
        </div>
        {category === 'VARIATION' && (
          <div role="group" aria-label="Parent lift">
            <p className="lib-form-label title-small" aria-hidden="true">
              Parent lift
            </p>
            <div className="lib-form-chips">
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
