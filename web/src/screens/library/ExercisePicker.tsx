import { useRef, useState } from 'react'
import { useExerciseBrowser, type CustomExercise, type PickerEntry, type StoredExercise } from '../../state/exercisePicker'
import { IconButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { AddIcon, CloseIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { CustomExerciseDialog } from './CustomExerciseDialog'
import { ExerciseBrowser } from './ExerciseBrowser'
import './library.css'

export type ExercisePickerProps = {
  title: string
  /** The lifter's stored exercises. */
  exercises: readonly StoredExercise[]
  /** Stored exercise ids, most recently used first. */
  recentIds: readonly number[]
  /** The row that was tapped. Called once: a second tap on the way out picks nothing more. */
  onPick: (entry: PickerEntry) => void
  /** An exercise of the lifter's own to make, and use as if it had been picked. Called once, like `onPick`. */
  /**
   * Creates the lifter's own exercise. When it gives back a promise, the create form stays open until that settles: it
   * closes once the exercise is made, and if making it fails it stays with everything typed and says so.
   */
  onCreateCustom: (exercise: CustomExercise) => unknown
  onDismiss: () => void
}

/**
 * The full-screen exercise picker of Plan and Today: the exercise browser with its muscle grid.
 * Tapping a row picks it; tapping its info button only opens the guide. Escape steps back out of a
 * muscle to the grid first, then closes the picker.
 */
export function ExercisePicker({ title, exercises, recentIds, onPick, onCreateCustom, onDismiss }: ExercisePickerProps) {
  const browser = useExerciseBrowser()
  const [creating, setCreating] = useState(false)

  // One answer per opening: a double tap must not add the exercise twice.
  const answered = useRef(false)
  const once = <A,>(answer: (value: A) => void) => (value: A) => {
    if (answered.current) return
    answered.current = true
    answer(value)
  }
  const pick = once(onPick)
  // Creates that failed while this picker was open: the create form stays, with what was typed, and says so.
  const [createFailures, setCreateFailures] = useState(0)
  const create = (exercise: CustomExercise) => {
    if (answered.current) return
    answered.current = true
    const made = onCreateCustom(exercise)
    if (typeof (made as PromiseLike<unknown> | null)?.then !== 'function') {
      setCreating(false)
      return
    }
    // The form stays open until the exercise is made. If that fails, nothing was answered: Create works again.
    ;(made as PromiseLike<unknown>).then(
      () => setCreating(false),
      () => {
        answered.current = false
        setCreateFailures((n) => n + 1)
      },
    )
  }

  // Escape steps back out of a muscle to the grid first, like Android's back button; then it closes the picker.
  const stepBack = () => (browser.muscle != null ? browser.browse(null) : onDismiss())

  return (
    <>
      <Dialog open variant="screen" title={title} onDismiss={stepBack} header={<ScreenHeader title={title} navigation={<IconButton label="Close" icon={<CloseIcon />} onClick={onDismiss} />} />}>
        <ExerciseBrowser
          state={browser}
          muscleFilter="grid"
          exercises={exercises}
          recentIds={recentIds}
          onRow={pick}
          create={<SecondaryButton text="Create your own" onClick={() => setCreating(true)} icon={<AddIcon />} />}
        />
      </Dialog>
      {creating && (
        <CustomExerciseDialog
          initialName={browser.query.trim()}
          onCreate={create}
          onDismiss={() => setCreating(false)}
          failures={createFailures}
        />
      )}
    </>
  )
}
