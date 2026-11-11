import { useState } from 'react'
import { grit } from '../../domain'
import type { MissPoint, PlannedItem, StoredSet, Suggestion, Target, TodayActions, TodayItem } from '../../state/useToday'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { CoachSuggestion } from '../../ui/components/CoachSuggestion'
import { Dialog } from '../../ui/components/Dialog'
import { formatKg, formatRpe } from '../../ui/components/format'
import { ArrowForwardIcon, BoltIcon, CheckIcon, CloseIcon } from '../../ui/components/icons'
import { Eyebrow } from '../../ui/components/NumberDisplay'
import { Pill } from '../../ui/components/Pill'
import { PlateStrip, PlateStripNote } from '../../ui/components/PlateStrip'
import { SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { SetRow } from '../../ui/components/SetRow'
import { StepperField } from '../../ui/components/StepperField'
import { NumberEntryDialog } from './TodayDialogs'
import './today.css'

/** Where the volt button leads once an exercise's sets are done: the next exercise, or Finish. */
export type NextStep = { label: string; onClick: () => void }

const BAR_KG = 20
const MAX_KG = 1000
const MAX_REPS = 99
const MIN_RPE = 6
const MAX_RPE = 10

/** RPE from which a set counts as a grind worth asking "where?" about. */
const GRIND_RPE = 9.5

const MISS_POINTS: readonly MissPoint[] = ['FLOOR', 'MID', 'LOCKOUT']
const MISS_LABELS = ['Floor', 'Mid', 'Lockout']

/** "155 × 5 @ 8". */
export function doseText(weightKg: number, reps: number, rpe: number | null): string {
  return `${formatKg(weightKg)} × ${reps}` + (rpe != null ? ` @ ${formatRpe(rpe)}` : '')
}

/** "5 × 5 @ 8": sets × reps @ RPE as planned. */
export function planText(item: PlannedItem): string {
  return `${item.sets} × ${item.reps} @ ${formatRpe(item.targetRpe)}`
}

/** The chip headline for a coach target: "155 × 5 @ 8", plus the set count when the coach changes it. */
export function targetHeadline(target: Target): string {
  return doseText(target.weightKg, target.reps, target.rpe) + (target.sets != null ? ` · ${target.sets} ${target.sets === 1 ? 'set' : 'sets'}` : '')
}

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8."); the chip already shows it as its headline.
const SUGGESTION_SENTENCE = /\s*Coach suggests .*? @ \d+(\.\d+)?\./

export function targetReason(suggestion: Suggestion): string {
  return suggestion.reason.replace(SUGGESTION_SENTENCE, '').trim()
}

/** The set a later look at this session would call the top set; null while every set has the same weight. */
function topSetIndex(sets: StoredSet[]): number | null {
  if (new Set(sets.map((s) => s.weightKg)).size < 2) return null
  let top: number | null = null
  sets.forEach((s, i) => {
    if (!s.failed && (top == null || s.weightKg >= (sets[top]?.weightKg ?? 0))) top = i
  })
  return top
}

function stepWeight(current: number | null, deltaKg: number): number {
  if (current == null) return BAR_KG
  return Math.round(Math.min(MAX_KG, Math.max(0, current + deltaKg)) * 100) / 100
}

export type ExerciseCardProps = {
  item: TodayItem
  /** "Exercise `position` of `count`". */
  position: number
  count: number
  weightStepKg: number
  changePlates: boolean
  next: NextStep
  actions: TodayActions
}

/** The exercise being logged: coach chip, sets so far, steppers, plates and the one big button. */
export function ExerciseCard({ item, position, count, weightStepKg, changePlates, next, actions }: ExerciseCardProps) {
  const id = item.exercise.id
  const [extraSet, setExtraSet] = useState(false)
  const [selected, setSelected] = useState<{ number: number; set: StoredSet } | null>(null)
  const top = topSetIndex(item.sets)
  const nextNumber = item.sets.length + 1

  return (
    <Card spacing={8}>
      <div className="today-card-head">
        <div className="today-grow">
          <Eyebrow text={`Exercise ${position} of ${count}`} />
          <h2 className="today-ellipsis headline-medium">{item.exercise.name}</h2>
        </div>
        {item.planned == null && item.sets.length === 0 ? (
          <QuietButton text="Remove" onClick={() => actions.removeExercise(id)} />
        ) : item.isComplete ? (
          <Pill text="Done" tone="success" icon={<CheckIcon />} />
        ) : (
          <Pill text={item.targetSets != null ? `Set ${nextNumber} of ${item.targetSets}` : `Set ${nextNumber}`} />
        )}
      </div>

      {item.suggestion && (
        <CoachSuggestion
          headline={targetHeadline(item.suggestion.target)}
          reason={targetReason(item.suggestion)}
          onApply={() => void actions.applyTarget(item.suggestion!.id)}
          onDismiss={() => void actions.dismissTarget(item.suggestion!.id)}
        />
      )}

      {item.sets.length > 0 && (
        <div>
          {item.sets.map((set, i) => (
            <div key={set.id}>
              <SetRow
                index={i + 1}
                weightKg={set.weightKg}
                reps={set.reps}
                rpe={set.rpe}
                state={set.failed ? 'failed' : 'done'}
                isTopSet={i === top}
                onClick={() => setSelected({ number: i + 1, set })}
              />
              {(set.failed || (set.rpe ?? 0) >= GRIND_RPE) && <MissRow number={i + 1} set={set} onSelect={(point) => void actions.setMissPoint(set.id, point)} />}
            </div>
          ))}
        </div>
      )}

      {!item.isComplete || extraSet ? (
        <>
          <hr className="today-hairline" />
          <Editor item={item} weightStepKg={weightStepKg} changePlates={changePlates} actions={actions} />
        </>
      ) : (
        <>
          <PrimaryButton className="today-next" text={next.label} onClick={next.onClick} icon={<ArrowForwardIcon />} />
          <div className="today-center">
            <QuietButton text="Add another set" onClick={() => setExtraSet(true)} />
          </div>
        </>
      )}

      {selected && (
        <Dialog
          open
          title={`Set ${selected.number}`}
          onDismiss={() => setSelected(null)}
          actions={
            <>
              <QuietButton text="Keep" onClick={() => setSelected(null)} />
              <QuietButton
                className="today-danger"
                text="Delete set"
                onClick={() => {
                  void actions.deleteSet(selected.set.id)
                  setSelected(null)
                }}
              />
            </>
          }
        >
          <p className="today-dose headline-small">{doseText(selected.set.weightKg, selected.set.reps, selected.set.rpe) + (selected.set.failed ? ' · failed' : '')}</p>
        </Dialog>
      )}
    </Card>
  )
}

function MissRow({ number, set, onSelect }: { number: number; set: StoredSet; onSelect: (point: MissPoint | null) => void }) {
  const question = set.failed ? 'Where did it stall?' : 'Where was it slow?'
  const chosen = MISS_POINTS.indexOf(set.missPoint as MissPoint)
  return (
    <div className="today-miss" data-testid={`miss-row-${number}`}>
      <span className="today-miss-question body-small">{question}</span>
      <SegmentedChoice
        label={`Set ${number}: ${question}`}
        options={MISS_LABELS}
        selectedIndex={chosen >= 0 ? chosen : null}
        // Tapping the chosen point again clears it.
        onSelect={(i) => onSelect(i === chosen ? null : (MISS_POINTS[i] ?? null))}
      />
    </div>
  )
}

function Editor({ item, weightStepKg, changePlates, actions }: { item: TodayItem; weightStepKg: number; changePlates: boolean; actions: TodayActions }) {
  const id = item.exercise.id
  const e = item.editor
  const weight = e.weightKg
  const [entry, setEntry] = useState<'weight' | 'reps' | null>(null)
  const isBarbellLift = item.exercise.category !== 'ACCESSORY'
  const log = (failed: boolean) => {
    if (weight != null) void actions.logSet(id, { weightKg: weight, reps: e.reps, rpe: e.rpe, failed })
  }

  return (
    <>
      <div className="today-steppers">
        <StepperField
          className="today-stepper-weight"
          label="Weight"
          valueText={weight != null ? formatKg(weight) : '–'}
          unit="kg"
          canDecrement={weight != null && weight > 0}
          canIncrement={weight == null || weight < MAX_KG}
          onDecrement={() => actions.edit(id, { ...e, weightKg: stepWeight(weight, -weightStepKg) })}
          onIncrement={() => actions.edit(id, { ...e, weightKg: stepWeight(weight, weightStepKg) })}
          onValueClick={() => setEntry('weight')}
        />
        <StepperField
          label="Reps"
          valueText={String(e.reps)}
          canDecrement={e.reps > 1}
          canIncrement={e.reps < MAX_REPS}
          onDecrement={() => actions.edit(id, { ...e, reps: e.reps - 1 })}
          onIncrement={() => actions.edit(id, { ...e, reps: e.reps + 1 })}
          onValueClick={() => setEntry('reps')}
        />
        <StepperField
          label="RPE"
          valueText={formatRpe(e.rpe)}
          canDecrement={e.rpe > MIN_RPE}
          canIncrement={e.rpe < MAX_RPE}
          onDecrement={() => actions.edit(id, { ...e, rpe: Math.max(MIN_RPE, e.rpe - 0.5) })}
          onIncrement={() => actions.edit(id, { ...e, rpe: Math.min(MAX_RPE, e.rpe + 0.5) })}
        />
      </div>

      {weight == null ? (
        <PlateStripNote title="No weight yet" caption="Tap the number to type it in." />
      ) : item.perSide != null ? (
        // Accessories may be dumbbells or machines: only draw a bar when there are plates to put on it.
        (isBarbellLift || item.perSide.length > 0) && <PlateStrip perSide={item.perSide} totalKg={weight} />
      ) : (
        isBarbellLift && <PlateStrip perSide={null} totalKg={weight} nearestKg={grit.roundLoad(weight, changePlates)} />
      )}

      <div className="today-log">
        <SecondaryButton text="Missed" onClick={() => log(true)} enabled={weight != null} icon={<CloseIcon />} />
        <PrimaryButton className="today-grow" text={`Log set ${item.sets.length + 1}`} onClick={() => log(false)} enabled={weight != null} icon={<CheckIcon />} />
      </div>

      {entry === 'weight' && (
        <NumberEntryDialog
          title="Weight"
          label="Kilograms"
          unit="kg"
          initial={weight}
          max={MAX_KG}
          whole={false}
          onDismiss={() => setEntry(null)}
          onSet={(kg) => {
            actions.edit(id, { ...e, weightKg: kg })
            setEntry(null)
          }}
        />
      )}
      {entry === 'reps' && (
        <NumberEntryDialog
          title="Reps"
          label="Reps in the set"
          initial={e.reps}
          min={1}
          max={MAX_REPS}
          whole
          onDismiss={() => setEntry(null)}
          onSet={(reps) => {
            actions.edit(id, { ...e, reps })
            setEntry(null)
          }}
        />
      )}
    </>
  )
}

/** Another exercise in today's session, folded to one line. Tap to log it. */
export function ExerciseRow({ item, onClick }: { item: TodayItem; onClick: () => void }) {
  return (
    <Card contentPadding={0}>
      <button type="button" className="today-row" onClick={onClick}>
        <span className="today-grow today-ellipsis title-medium">{item.exercise.name}</span>
        {item.suggestion && (
          <span className="today-row-bolt" role="img" aria-label="Coach suggestion">
            <BoltIcon size={18} />
          </span>
        )}
        {item.planned && <span className="today-row-plan headline-small">{planText(item.planned)}</span>}
        {item.isComplete ? (
          <Pill text="Done" tone="success" icon={<CheckIcon />} />
        ) : item.sets.length > 0 && item.targetSets != null ? (
          <Pill text={`${item.sets.length} of ${item.targetSets}`} />
        ) : (
          item.sets.length > 0 && <Pill text={`${item.sets.length} ${item.sets.length === 1 ? 'set' : 'sets'}`} />
        )}
      </button>
    </Card>
  )
}
