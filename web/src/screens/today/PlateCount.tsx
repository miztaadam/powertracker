import { useEffect, useId, useRef, useState } from 'react'
import type { LoadUnit, PlateCount } from '../../db/schema'
import { PrimaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { useEarlyTapGuard } from './tapGuard'
import './today.css'

// Counting plates on a stack machine (1.17, plate-count design "The setting"). The Android app's Today uses the same words
// (ui/today/PlateCount.kt).
export const COUNT_PLATES = 'Count plates on this machine'

/** The sheet's accessible name; its heading alone would share a name with the form guide's sheet. */
export const plateSheetLabel = (name: string) => `Count plates on ${name}`

export const PLATE_QUESTION = 'How does this machine show its weight?'

/** The sheet's three choices, in its order: the label and the grid's hint. */
export const PLATE_CHOICES: readonly [value: PlateCount, label: string, hint: string | null][] = [
  ['OFF', 'In kg', null],
  ['WHOLE', 'Whole plates', '1, 2, 3 …'],
  ['HALF', 'Half steps too', '1, 1½, 2 …'],
]

/**
 * The line under the choices: only when the choice starts a new unit (`chosen` is not `opened`, the unit when the sheet
 * opened) and the exercise has sets in the unit it leaves. Whole plates ↔ half steps is never a change of unit.
 */
export function plateNote(opened: LoadUnit, chosen: LoadUnit, hasSetsInOpenedUnit: boolean): string | null {
  if (chosen === opened || !hasSetsInOpenedUnit) return null
  return chosen === 'PLATES' ? 'The coach starts this exercise fresh in plates. Your kg sets stay as they are.' : 'The coach goes back to kg. Your plate sets stay as they are.'
}

export type PlateCountSheetProps = {
  name: string
  /** The stored setting. */
  current: PlateCount
  /** `plateNote`'s line, or null. */
  note: string | null
  /** A choice was tapped: it applies at once. It stays marked until `current` shows it, or its promise rejects. */
  onChoose: (value: PlateCount) => Promise<void> | void
  /** Done, Escape or a tap outside: only closes. */
  onDismiss: () => void
}

/**
 * Asks how `name`'s machine shows its weight. A choice applies when tapped; tapping the one already marked does nothing,
 * and Done only closes. The mark moves at once and stays until the stored setting shows it or the save fails. For a
 * moment after the sheet opens its choices ignore taps, as the skip sheet's answers do; Done is not held. The content
 * scrolls, so Done is reachable at any text size.
 */
export function PlateCountSheet({ name, current, note, onChoose, onDismiss }: PlateCountSheetProps) {
  // The tapped choice, as Android's `saving`: marked at once, and kept marked until the stored setting shows it or its
  // save fails. A save settles after its write and the coach's pass but can settle before the screen's live data shows
  // the new setting; clearing it then would flick the mark back to the old choice for a moment. A failed save shows
  // what is stored at once. An earlier save no longer counts once a later tap is made.
  const [saving, setSaving] = useState<PlateCount | null>(null)
  useEffect(() => {
    if (saving != null && current === saving) setSaving(null)
  }, [saving, current])
  const marked = saving ?? current
  const taps = useRef(0)
  const choose = (value: PlateCount) => {
    // Compared with what is marked: tapping it does nothing, any other tap stores, so the last tap wins.
    if (value === marked) return
    setSaving(value)
    const tap = ++taps.current
    Promise.resolve()
      .then(() => onChoose(value))
      .catch(() => {
        if (tap === taps.current) setSaving(null)
      })
  }
  const guard = useEarlyTapGuard('.today-plate-options')
  const questionId = useId()
  const group = useId()
  return (
    <div ref={guard.ref} className="today-skip-taps" onClickCapture={guard.onClickCapture}>
      <Dialog
        open
        variant="sheet"
        title={plateSheetLabel(name)}
        onDismiss={onDismiss}
        header={
          <div className="today-skip-head">
            <h2 className="headline-small">{name}</h2>
          </div>
        }
      >
        <div className="today-plate-sheet" data-testid="plate-sheet">
          <p id={questionId} className="today-plate-question body-large">
            {PLATE_QUESTION}
          </p>
          <div className="today-plate-options" role="radiogroup" aria-labelledby={questionId}>
            {PLATE_CHOICES.map(([value, label, hint]) => {
              const hintId = `${group}-${value}`
              return (
                <label key={value} className="today-plate-option" data-testid={`plate-option-${value}`}>
                  <input
                    type="radio"
                    className="today-plate-radio"
                    name={group}
                    value={value}
                    checked={marked === value}
                    aria-describedby={hint != null ? hintId : undefined}
                    onChange={() => choose(value)}
                  />
                  {/* The label and its hint wrap under each other where they do not fit side by side. */}
                  <span className="today-plate-text">
                    <span className="today-plate-label title-medium">{label}</span>
                    {hint != null && (
                      <span id={hintId} className="today-plate-hint body-medium" aria-hidden="true">
                        {hint}
                      </span>
                    )}
                  </span>
                </label>
              )
            })}
          </div>
          {note != null && (
            <p className="today-plate-note body-medium" data-testid="plate-note">
              {note}
            </p>
          )}
          <div data-testid="plate-done" className="today-plate-done">
            <PrimaryButton text="Done" onClick={onDismiss} />
          </div>
        </div>
      </Dialog>
    </div>
  )
}
