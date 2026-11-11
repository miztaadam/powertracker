import { useEffect, useMemo, useRef, useState } from 'react'
import { muscleGroups, normalise, pickerList, type CustomExercise, type PickerEntry, type StoredExercise } from '../../state/exercisePicker'
import { IconButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { EmptyState } from '../../ui/components/EmptyState'
import { AddIcon, ArrowBackIcon, CloseIcon, SearchOffIcon } from '../../ui/components/icons'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { ChoiceChip } from '../../ui/components/SegmentedChoice'
import { TextField } from '../../ui/components/TextField'
import { CustomExerciseDialog } from './CustomExerciseDialog'
import { InfoButton } from './InfoButton'
import { AREAS, EQUIPMENT, equipmentLabel } from './labels'
import './library.css'

/** What the picker says when nothing matches. */
export const NO_MATCH = 'No exercises match. Try fewer filters, or create your own.'

export type ExercisePickerProps = {
  title: string
  /** The lifter's stored exercises. */
  exercises: readonly StoredExercise[]
  /** Stored exercise ids, most recently used first. */
  recentIds: readonly number[]
  /** The row that was tapped. Called once: a second tap on the way out picks nothing more. */
  onPick: (entry: PickerEntry) => void
  /** An exercise of the lifter's own to make, and use as if it had been picked. Called once, like `onPick`. */
  onCreateCustom: (exercise: CustomExercise) => void
  onDismiss: () => void
}

/**
 * The full-screen exercise picker of Plan and Today: search, body-area chips (one at a time),
 * equipment chips (any of the chosen), then the lifter's exercises and the catalogue. Tapping a
 * row picks it; tapping its info button only opens the guide.
 *
 * While nothing is searched and no body area is chosen, a grid of muscle groups leads the list.
 * Tapping one lists that muscle's exercises (equipment chips still narrow them) until "All muscles",
 * Escape, a search or a body area leaves it.
 */
export function ExercisePicker({ title, exercises, recentIds, onPick, onCreateCustom, onDismiss }: ExercisePickerProps) {
  const [query, setQuery] = useState('')
  const [area, setArea] = useState<string | null>(null)
  const [equipment, setEquipment] = useState<readonly string[]>([])
  const [muscle, setMuscle] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const list = useMemo(() => pickerList(query, area, equipment, exercises, recentIds, muscle), [query, area, equipment, exercises, recentIds, muscle])
  const showGrid = muscle == null && area == null && normalise(query) === ''
  const browsed = muscle != null ? muscleGroups().find((g) => g.id === muscle) : undefined
  const isEmpty = list.yours.length === 0 && list.all.length === 0

  // A new search or filter starts from its best match, not from wherever the last list was scrolled to.
  const scroller = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (scroller.current) scroller.current.scrollTop = 0
  }, [query, area, equipment, muscle])

  // One answer per opening: a double tap must not add the exercise twice.
  const answered = useRef(false)
  const once = <A,>(answer: (value: A) => void) => (value: A) => {
    if (answered.current) return
    answered.current = true
    answer(value)
  }
  const pick = once(onPick)
  const create = once(onCreateCustom)

  // Searching is over everything, and a body area is its own way in: both leave the muscle.
  const type = (text: string) => {
    setQuery(text)
    if (text !== '') setMuscle(null)
  }
  const chooseArea = (id: string | null) => {
    setArea(id)
    setMuscle(null)
  }
  const toggleEquipment = (id: string) => setEquipment((chosen) => (chosen.includes(id) ? chosen.filter((e) => e !== id) : [...chosen, id]))
  // Escape steps back out of a muscle to the grid first, like Android's back button; then it closes the picker.
  const stepBack = () => (muscle != null ? setMuscle(null) : onDismiss())
  const createButton = <SecondaryButton text="Create your own" onClick={() => setCreating(true)} icon={<AddIcon />} />

  return (
    <>
      <Dialog open variant="screen" title={title} onDismiss={stepBack} header={<ScreenHeader title={title} navigation={<IconButton label="Close" icon={<CloseIcon />} onClick={onDismiss} />} />}>
        <div className="lib-search">
          <TextField
            label="Search exercises"
            value={query}
            onChange={type}
            enterKeyHint="search"
            trailing={query !== '' && <IconButton label="Clear search" icon={<CloseIcon />} onClick={() => setQuery('')} />}
          />
        </div>
        <div ref={scroller} className="lib-scroll">
          {/* The chips scroll away with the list, so the results get the room once a filter is set. */}
          <div className="lib-chips" role="group" aria-label="Body area">
            <ChoiceChip text="All" selected={area == null} onClick={() => chooseArea(null)} />
            {AREAS.map((a) => (
              <ChoiceChip key={a.id} text={a.label} selected={area === a.id} onClick={() => chooseArea(area === a.id ? null : a.id)} />
            ))}
          </div>
          <div className="lib-chips" role="group" aria-label="Equipment">
            {EQUIPMENT.map((e) => (
              <ChoiceChip key={e.id} text={e.label} selected={equipment.includes(e.id)} onClick={() => toggleEquipment(e.id)} />
            ))}
          </div>
          {showGrid && (
            <>
              <SectionHeader className="lib-heading" title="Browse by muscle" />
              <div className="lib-muscles" role="group" aria-label="Browse by muscle">
                {muscleGroups().map((g) => (
                  <button key={g.id} type="button" className="lib-muscle title-medium" onClick={() => setMuscle(g.id)}>
                    {g.label}
                  </button>
                ))}
              </div>
            </>
          )}
          {browsed && (
            <>
              <div className="lib-muscle-back">
                <SecondaryButton text="All muscles" onClick={() => setMuscle(null)} icon={<ArrowBackIcon />} />
              </div>
              <SectionHeader className="lib-heading" title={`${browsed.label} exercises`} />
            </>
          )}
          {isEmpty ? (
            <EmptyState icon={<SearchOffIcon />} title={NO_MATCH} body="Your own exercises work everywhere, without a form guide." action={createButton} />
          ) : (
            <>
              {list.yours.length > 0 && (
                <>
                  <SectionHeader className="lib-heading" title="Your exercises" />
                  <PickerRows entries={list.yours} onPick={pick} />
                  <SectionHeader className="lib-heading" title="All exercises" />
                </>
              )}
              <PickerRows entries={list.all} onPick={pick} />
              <div className="lib-create">{createButton}</div>
            </>
          )}
        </div>
      </Dialog>
      {creating && (
        <CustomExerciseDialog
          initialName={query.trim()}
          onCreate={(exercise) => {
            setCreating(false)
            create(exercise)
          }}
          onDismiss={() => setCreating(false)}
        />
      )}
    </>
  )
}

function PickerRows({ entries, onPick }: { entries: readonly PickerEntry[]; onPick: (entry: PickerEntry) => void }) {
  return (
    <ul className="lib-rows">
      {entries.map((entry) => (
        <li key={entry.library?.name ?? `own-${entry.exerciseId}`} className="lib-row" aria-label={entry.name}>
          <button type="button" className="lib-row-main" onClick={() => onPick(entry)}>
            <span className="lib-grow">
              <span className="lib-row-name title-medium">{entry.name}</span>
              {entry.muscles.length > 0 && <span className="lib-row-muscles body-medium">{entry.muscles.join(', ')}</span>}
            </span>
            <span className="lib-row-equipment label-medium">{entry.library?.equipment[0] != null ? equipmentLabel(entry.library.equipment[0]) : 'Your own'}</span>
          </button>
          {/* Its own button, beside the row's: reading the guide must not pick the exercise. */}
          <InfoButton name={entry.name} />
        </li>
      ))}
    </ul>
  )
}
