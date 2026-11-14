import { Fragment, useEffect, useId, useMemo, useRef, useState, type ReactNode } from 'react'
import { muscleGroups, normalise, pickerList, type ExerciseBrowser as BrowserState, type PickerEntry, type StoredExercise } from '../../state/exercisePicker'
import { IconButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { EmptyState } from '../../ui/components/EmptyState'
import { ArrowBackIcon, ArrowDropDownIcon, CheckIcon, CloseIcon, InfoIcon, SearchOffIcon } from '../../ui/components/icons'
import { SectionHeader } from '../../ui/components/ScreenHeader'
import { ChoiceChip } from '../../ui/components/SegmentedChoice'
import { TextField } from '../../ui/components/TextField'
import { ExerciseThumbnail } from './ExercisePicture'
import { InfoButton } from './InfoButton'
import { AREAS, EQUIPMENT, equipmentLabel } from './labels'
import './library.css'

/** What the picker says when nothing matches. */
export const NO_MATCH = 'No exercises match. Try fewer filters, or create your own.'
/** The same where exercises are only browsed, not made. */
export const NO_MATCH_BROWSING = 'No exercises match.'

export type ExerciseBrowserProps = {
  /** The search and filters, kept by the caller for as long as they should last. */
  state: BrowserState
  /** How muscles are chosen: the picker's tile `grid`, or the Exercises tab's multi-select `dropdown`. */
  muscleFilter: 'grid' | 'dropdown'
  /** The lifter's stored exercises. */
  exercises: readonly StoredExercise[]
  /** Stored exercise ids, most recently used first. */
  recentIds: readonly number[]
  /** A row was tapped. */
  onRow: (entry: PickerEntry) => void
  /** The row's info button was tapped. Without it the button opens the plain form guide. */
  onInfo?: (entry: PickerEntry) => void
  /** What a row tap does, as a screen reader says it ("Open Squat"). Without it the row is read as it is written. */
  rowLabel?: (name: string) => string
  /** "Create your own", below the list and in the empty state; none where exercises are only browsed. */
  create?: ReactNode
  /** Whether each row leads with its exercise's small picture (the Exercises tab). The picker's rows have none. */
  thumbnails?: boolean
  /**
   * Multi-select: whether the row's exercise is ticked. With it every row is a tick box that `onRow` toggles
   * (the plan builder's "Which exercises do you do now?"); without it a row tap is one choice.
   */
  ticked?: (entry: PickerEntry) => boolean
  /**
   * Multi-select: every ticked exercise, listed under "Chosen" above the list whatever the search and filters, so none
   * is out of sight. Each is a tick box like the list's own row.
   */
  chosen?: readonly PickerEntry[]
}

/**
 * Browsing the exercises, for the picker dialog and the Exercises tab alike: search, body-area chips
 * (one at a time), equipment chips (any of the chosen), then the lifter's exercises and the catalogue.
 *
 * With the `grid`, a grid of muscle groups leads the list while nothing is searched and no body area
 * is chosen; tapping one lists that muscle's exercises (equipment chips still narrow them) until
 * "All muscles", a search or a body area leaves it. With the `dropdown`, "Muscles" leads the
 * equipment chips and opens a sheet of muscle groups to tick: the list is every exercise that works
 * any ticked group, on top of the search and the other filters, and with none ticked it is the whole
 * catalogue under its body areas.
 */
export function ExerciseBrowser({ state, muscleFilter, exercises, recentIds, onRow, onInfo, rowLabel, create, thumbnails = false, ticked, chosen = [] }: ExerciseBrowserProps) {
  const { query, area, equipment, muscle, muscles } = state
  const dropdown = muscleFilter === 'dropdown'
  const [choosingMuscles, setChoosingMuscles] = useState(false)
  const list = useMemo(() => pickerList(query, area, equipment, exercises, recentIds, muscle, muscles), [query, area, equipment, exercises, recentIds, muscle, muscles])
  const searching = normalise(query) !== ''
  const showGrid = !dropdown && muscle == null && area == null && !searching
  // Under body areas only while the list is the alphabetical catalogue: a search is ranked, ticked muscles cut across areas.
  const byArea = dropdown && muscles.length === 0 && !searching
  const browsed = muscle != null ? muscleGroups().find((g) => g.id === muscle) : undefined
  const isEmpty = list.yours.length === 0 && list.all.length === 0

  // A new search or filter starts from its best match, not from wherever the last list was scrolled to.
  const scroller = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (scroller.current) scroller.current.scrollTop = 0
  }, [query, area, equipment, muscle, muscles])

  const rows = (entries: readonly PickerEntry[]) => <BrowserRows entries={entries} onRow={onRow} onInfo={onInfo} rowLabel={rowLabel} thumbnails={thumbnails} ticked={ticked} />

  return (
    <>
      <div className="lib-search">
        <TextField
          label="Search exercises"
          value={query}
          onChange={state.search}
          enterKeyHint="search"
          trailing={query !== '' && <IconButton label="Clear search" icon={<CloseIcon />} onClick={() => state.search('')} />}
        />
      </div>
      <div ref={scroller} className="lib-scroll">
        {/* The chips scroll away with the list, so the results get the room once a filter is set. */}
        <div className="lib-chips" role="group" aria-label="Body area">
          <ChoiceChip text="All" selected={area == null} onClick={() => state.chooseArea(null)} />
          {AREAS.map((a) => (
            <ChoiceChip key={a.id} text={a.label} selected={area === a.id} onClick={() => state.chooseArea(a.id)} />
          ))}
        </div>
        <div className="lib-chips">
          {dropdown && (
            <button type="button" className="lib-dropdown" aria-haspopup="dialog" aria-pressed={muscles.length > 0} onClick={() => setChoosingMuscles(true)}>
              <span className="lib-dropdown-face label-large">
                {muscles.length > 0 ? `Muscles · ${muscles.length}` : 'Muscles'}
                <ArrowDropDownIcon size={20} />
              </span>
            </button>
          )}
          <div className="lib-contents" role="group" aria-label="Equipment">
            {EQUIPMENT.map((e) => (
              <ChoiceChip key={e.id} text={e.label} selected={equipment.includes(e.id)} onClick={() => state.toggleEquipment(e.id)} />
            ))}
          </div>
        </div>
        {muscles.length > 0 && (
          // They wrap, so none is hidden.
          <div className="lib-ticked" role="group" aria-label="Chosen muscles">
            {muscleGroups()
              .filter((g) => muscles.includes(g.id))
              .map((g) => (
                <button key={g.id} type="button" className="lib-ticked-chip" aria-label={`Remove ${g.label}`} onClick={() => state.toggleMuscle(g.id)}>
                  <span className="lib-ticked-face label-large">
                    {g.label}
                    <CloseIcon size={16} />
                  </span>
                </button>
              ))}
            <QuietButton text="Clear" onClick={state.clearMuscles} />
          </div>
        )}
        {showGrid && (
          <>
            <SectionHeader className="lib-heading" title="Browse by muscle" />
            <div className="lib-muscles" role="group" aria-label="Browse by muscle">
              {muscleGroups().map((g) => (
                <button key={g.id} type="button" className="lib-muscle title-medium" onClick={() => state.browse(g.id)}>
                  {g.label}
                </button>
              ))}
            </div>
          </>
        )}
        {browsed && (
          <>
            <div className="lib-muscle-back">
              <SecondaryButton text="All muscles" onClick={() => state.browse(null)} icon={<ArrowBackIcon />} />
            </div>
            <SectionHeader className="lib-heading" title={`${browsed.label} exercises`} />
          </>
        )}
        {chosen.length > 0 && (
          <section aria-label="Chosen">
            <SectionHeader className="lib-heading" title="Chosen" />
            {rows(chosen)}
          </section>
        )}
        {isEmpty ? (
          create != null ? (
            <EmptyState icon={<SearchOffIcon />} title={NO_MATCH} body="Your own exercises work everywhere, without a form guide." action={create} />
          ) : (
            <EmptyState icon={<SearchOffIcon />} title={NO_MATCH_BROWSING} body="Try fewer filters." />
          )
        ) : (
          <>
            {list.yours.length > 0 && (
              <>
                <SectionHeader className="lib-heading" title="Your exercises" />
                {rows(list.yours)}
                {!byArea && <SectionHeader className="lib-heading" title="All exercises" />}
              </>
            )}
            {/* After "Chosen", the list needs a heading of its own; by area, the areas give it one. */}
            {list.yours.length === 0 && chosen.length > 0 && !byArea && <SectionHeader className="lib-heading" title="All exercises" />}
            {byArea ? <AreaSections entries={list.all} rows={rows} /> : rows(list.all)}
            {create != null && <div className="lib-create">{create}</div>}
          </>
        )}
      </div>
      {choosingMuscles && <MusclesSheet ticked={muscles} onToggle={state.toggleMuscle} onClose={() => setChoosingMuscles(false)} />}
    </>
  )
}

/** `entries` under their body areas, in the catalogue's area order; each area keeps the order it came in. */
function AreaSections({ entries, rows }: { entries: readonly PickerEntry[]; rows: (entries: readonly PickerEntry[]) => ReactNode }) {
  // The lifter's own have no area. While nothing is filtered they are under "Your exercises", so none is expected here.
  const noArea = entries.filter((e) => e.library == null)
  return (
    <>
      {noArea.length > 0 && rows(noArea)}
      {AREAS.map((area) => {
        const inArea = entries.filter((e) => e.library?.area === area.id)
        if (inArea.length === 0) return null
        return (
          <Fragment key={area.id}>
            <SectionHeader className="lib-heading" title={area.label} />
            {rows(inArea)}
          </Fragment>
        )
      })}
    </>
  )
}

/**
 * Muscle groups to tick, two to a row: the Muscles drop-down's list, the plan builder's body parts and the check-in's.
 * `disabled` groups are shown but cannot be ticked.
 */
export function MuscleChecklist({ groups, ticked, onToggle, disabled = [] }: { groups: readonly { id: string; label: string }[]; ticked: readonly string[]; onToggle: (group: string) => void; disabled?: readonly string[] }) {
  return (
    <div className="lib-options">
      {groups.map((g) => (
        <label key={g.id} className={disabled.includes(g.id) ? 'lib-option lib-option-disabled title-medium' : 'lib-option title-medium'}>
          <input type="checkbox" checked={ticked.includes(g.id)} disabled={disabled.includes(g.id)} onChange={() => onToggle(g.id)} />
          {g.label}
        </label>
      ))}
    </div>
  )
}

/** The Muscles drop-down: every muscle group, in the domain's order, to tick. It stays open while the lifter ticks. */
export function MusclesSheet({ ticked, onToggle, onClose }: { ticked: readonly string[]; onToggle: (group: string) => void; onClose: () => void }) {
  return (
    <Dialog
      open
      variant="sheet"
      portal
      title="Muscles"
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">Muscles</h2>
            <p className="lib-guide-where eyebrow">Exercises that work any of them</p>
          </div>
          <IconButton label="Close" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      <MuscleChecklist groups={muscleGroups()} ticked={ticked} onToggle={onToggle} />
    </Dialog>
  )
}

type BrowserRowsProps = Pick<ExerciseBrowserProps, 'onRow' | 'onInfo' | 'rowLabel' | 'thumbnails' | 'ticked'> & { entries: readonly PickerEntry[] }

function BrowserRows({ entries, onRow, onInfo, rowLabel, thumbnails, ticked }: BrowserRowsProps) {
  return (
    <ul className="lib-rows">
      {entries.map((entry) => (
        <BrowserRow key={entry.library?.name ?? `own-${entry.exerciseId}`} entry={entry} onRow={onRow} onInfo={onInfo} rowLabel={rowLabel} thumbnails={thumbnails} ticked={ticked} />
      ))}
    </ul>
  )
}

function BrowserRow({ entry, onRow, onInfo, rowLabel, thumbnails, ticked }: Omit<BrowserRowsProps, 'entries'> & { entry: PickerEntry }) {
  // With a label of its own, the row's button is named by it and the rest of the row describes it.
  const details = useId()
  const on = ticked?.(entry)
  return (
    <li className="lib-row" aria-label={entry.name}>
      <button
        type="button"
        className="lib-row-main"
        role={ticked ? 'checkbox' : undefined}
        aria-checked={ticked ? on : undefined}
        aria-label={rowLabel?.(entry.name)}
        aria-describedby={rowLabel ? details : undefined}
        onClick={() => onRow(entry)}
      >
        {ticked && (
          <span className={on ? 'lib-tick lib-tick-on' : 'lib-tick'} aria-hidden="true">
            {on && <CheckIcon size={18} />}
          </span>
        )}
        {/* By the catalogue's name: the lifter's own exercise has none, and gets the empty tile. */}
        {thumbnails && <ExerciseThumbnail name={entry.library?.name} />}
        <span className="lib-grow">
          <span className="lib-row-name title-medium">{entry.name}</span>
          {entry.muscles.length > 0 && <span className="lib-row-muscles body-medium">{entry.muscles.join(', ')}</span>}
        </span>
        <span className="lib-row-equipment label-medium">{entry.library?.equipment[0] != null ? equipmentLabel(entry.library.equipment[0]) : 'Your own'}</span>
      </button>
      {rowLabel && (
        <span id={details} hidden>
          {[entry.muscles.join(', '), entry.library?.equipment[0] != null ? equipmentLabel(entry.library.equipment[0]) : 'Your own'].filter(Boolean).join('. ')}
        </span>
      )}
      {/* Its own button, beside the row's: where a row tap picks the exercise, reading the guide must not. */}
      {onInfo ? <IconButton label={`Form guide for ${entry.name}`} icon={<InfoIcon />} onClick={() => onInfo(entry)} /> : <InfoButton name={entry.name} />}
    </li>
  )
}
