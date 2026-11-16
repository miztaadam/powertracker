import { useRef, useState } from 'react'
import { grit, muscleFocus } from '../../domain'
import type { TrainingMode } from '../../db/schema'
import { haptic } from '../../platform/haptics'
import type { MissPoint, PlannedItem, SetEditor, StoredSet, Suggestion, Target, TodayActions, TodayItem } from '../../state/useToday'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { CoachSuggestion } from '../../ui/components/CoachSuggestion'
import { Dialog } from '../../ui/components/Dialog'
import { cx, formatKg, formatRpe } from '../../ui/components/format'
import { ArrowForwardIcon, BoltIcon, CheckIcon, CloseIcon } from '../../ui/components/icons'
import { Eyebrow } from '../../ui/components/NumberDisplay'
import { NumberField } from '../../ui/components/NumberField'
import { Pill } from '../../ui/components/Pill'
import { PlateStrip, PlateStripNote } from '../../ui/components/PlateStrip'
import { SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { SetRow } from '../../ui/components/SetRow'
import { StepperField } from '../../ui/components/StepperField'
import { InfoButton } from '../library/InfoButton'
import { RpeInfoButton } from '../library/RpeSheet'
import { MAX_KG, MAX_REPS, MAX_RPE, MIN_RPE, entryMessage, parseEntry, type EntryKind } from './entry'
import './today.css'

/** Where the accent button leads once an exercise's sets are done: the next exercise, or Finish. */
export type NextStep = { label: string; onClick: () => void }

const BAR_KG = 20

/** RPE from which a set counts as a grind worth asking "where?" about. */
const GRIND_RPE = 9.5

const MISS_POINTS: readonly MissPoint[] = ['FLOOR', 'MID', 'LOCKOUT']

/** "Floor / Mid / Lockout", or "Off the shoulders / …" for a strict press. An accessory has no lift: it gets the plain names. */
const missLabels = (lift: string | null) => MISS_POINTS.map((point) => grit.missPointLabel(lift ?? 'SQUAT', point))

/** "155 × 5 @ 8". */
export function doseText(weightKg: number, reps: number, rpe: number | null): string {
  return `${formatKg(weightKg)} × ${reps}` + (rpe != null ? ` @ ${formatRpe(rpe)}` : '')
}

/** "5 × 5 @ 8", or "3 × 8–12 @ 8" for a rep range: sets × reps @ RPE as planned. */
export function planText(item: PlannedItem): string {
  return `${item.sets} × ${grit.repsText(item.reps, item.repsHigh ?? null)} @ ${formatRpe(item.targetRpe)}`
}

/**
 * The chip headline for a coach target: "155 × 5 @ 8", or for a rep range "26 kg × 8–12 @ 8" (the mockup's
 * bodybuilding card), plus the set count when the coach changes it.
 */
export function targetHeadline(target: Target): string {
  const dose = target.repsHigh != null ? `${formatKg(target.weightKg)} kg × ${grit.repsText(target.reps, target.repsHigh)} @ ${formatRpe(target.rpe)}` : doseText(target.weightKg, target.reps, target.rpe)
  return dose + (target.sets != null ? ` · ${target.sets} ${target.sets === 1 ? 'set' : 'sets'}` : '')
}

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8.", or "… Coach suggests 26 × 8–12 @ 8." on
// the bodybuilding side); the chip already shows it as its headline.
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
  /**
   * The side of the session. On the bodybuilding side there is no "Top set" chip and no "where did it stall?"
   * question (spec §7.4); a set can still be marked Missed.
   */
  side?: TrainingMode
}

/** The exercise being logged: coach chip, sets so far, steppers, plates and the one big button. */
export function ExerciseCard({ item, position, count, weightStepKg, changePlates, next, actions, side = 'POWERLIFTING' }: ExerciseCardProps) {
  const id = item.exercise.id
  const [extraSet, setExtraSet] = useState(false)
  const [selected, setSelected] = useState<{ number: number; set: StoredSet } | null>(null)
  const powerlifting = side === 'POWERLIFTING'
  const top = powerlifting ? topSetIndex(item.sets) : null
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
        <InfoButton name={item.exercise.name} />
      </div>

      {item.suggestion && (
        <CoachSuggestion
          headline={targetHeadline(item.suggestion.target)}
          reason={targetReason(item.suggestion)}
          onApply={() => {
            // Apply on the coach's suggestion ticks, as Apply on a coach card does.
            haptic('tick')
            void actions.applyTarget(item.suggestion!.id)
          }}
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
              {powerlifting && (set.failed || (set.rpe ?? 0) >= GRIND_RPE) && <MissRow number={i + 1} set={set} lift={grit.mainLiftOf(item.exercise) ?? null} onSelect={(point) => void actions.setMissPoint(set.id, point)} />}
            </div>
          ))}
        </div>
      )}

      {!powerlifting && <Cues name={item.exercise.name} />}

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

/**
 * Where the muscle stretches and where to squeeze it (stage 4), between the logged sets and the weight entry as the
 * mockup has it: bodybuilding side only. Nothing at all, taking no room, for an exercise with no lines: one with
 * nothing to aim for (a plank), or the lifter's own (outside the catalogue).
 */
function Cues({ name }: { name: string }) {
  const focus = muscleFocus(name)
  if (focus?.kind !== 'lines') return null
  const rows: [label: string, line: string][] = [
    ['Stretch', focus.stretch],
    ['Squeeze', focus.squeeze],
  ]
  return (
    // A list of two, each row read once as "Stretch: …", as on Android; what shows is the label beside its line.
    <ul role="list" className="today-cues" aria-label="Stretch and squeeze">
      {rows.map(([label, line]) => (
        <li key={label} className="today-cue">
          <span className="today-cue-label eyebrow" aria-hidden="true">
            {label.toUpperCase()}
          </span>
          <span className="today-cue-line body-large" aria-hidden="true">
            {line}
          </span>
          <span className="sr-only">{`${label}: ${line}`}</span>
        </li>
      ))}
    </ul>
  )
}

function MissRow({ number, set, lift, onSelect }: { number: number; set: StoredSet; lift: string | null; onSelect: (point: MissPoint | null) => void }) {
  const question = set.failed ? 'Where did it stall?' : 'Where was it slow?'
  const chosen = MISS_POINTS.indexOf(set.missPoint as MissPoint)
  return (
    // "Off the shoulders" needs the whole width: the question goes above the points, not beside them.
    <div className={cx('today-miss', lift === 'STRICT_PRESS' && 'today-miss-stacked')} data-testid={`miss-row-${number}`}>
      <span className="today-miss-question body-small">{question}</span>
      <SegmentedChoice
        label={`Set ${number}: ${question}`}
        options={missLabels(lift)}
        selectedIndex={chosen >= 0 ? chosen : null}
        // Tapping the chosen point again clears it.
        onSelect={(i) => onSelect(i === chosen ? null : (MISS_POINTS[i] ?? null))}
      />
    </div>
  )
}

/** The editor value each field types into. */
const FIELD_OF = { kg: 'weightKg', reps: 'reps', rpe: 'rpe' } as const satisfies Record<EntryKind, keyof SetEditor>
const KINDS: readonly EntryKind[] = ['kg', 'reps', 'rpe']

/**
 * Text in a field that is not the editor's value (yet). `base` is the editor value it was typed over:
 * when that has changed since (the coach's Apply), the text is stale and is never saved over the
 * newer value. `error` is set once the text has been refused.
 */
type Draft = { text: string; base: number | null; error?: string }
type Drafts = Partial<Record<EntryKind, Draft>>

/** Puts every typed number that is good into the editor. What was refused stays, with its message; the rest is dropped. */
function settle(editor: SetEditor, drafts: Drafts): { editor: SetEditor; drafts: Drafts } {
  const next = { ...editor }
  const kept: Drafts = {}
  for (const kind of KINDS) {
    const draft = drafts[kind]
    if (draft == null || draft.base !== editor[FIELD_OF[kind]]) continue
    if (draft.error != null) {
      kept[kind] = draft
      continue
    }
    const entry = parseEntry(draft.text, kind)
    if (entry.ok) next[FIELD_OF[kind]] = entry.value
    else if (entry.reason !== 'empty') kept[kind] = { ...draft, error: entryMessage(kind, entry.reason) }
  }
  return { editor: next, drafts: kept }
}

const sameEditor = (a: SetEditor, b: SetEditor) => a.weightKg === b.weightKg && a.reps === b.reps && a.rpe === b.rpe

function Editor({ item, weightStepKg, changePlates, actions }: { item: TodayItem; weightStepKg: number; changePlates: boolean; actions: TodayActions }) {
  const id = item.exercise.id
  const e = item.editor
  const [drafts, setDrafts] = useState<Drafts>({})
  const [focused, setFocused] = useState<EntryKind | null>(null)
  const isBarbellLift = item.exercise.category !== 'ACCESSORY'

  // What a field shows instead of the editor's value. Text typed over an older value is stale: it
  // stays only where the cursor is, so the typing is not cut short. Anywhere else the newer value
  // shows, which is also what Log set saves. A refused entry always gives way to a newer value.
  const shown = (kind: EntryKind): Draft | undefined => {
    const draft = drafts[kind]
    return draft != null && (draft.base === e[FIELD_OF[kind]] || (draft.error == null && kind === focused)) ? draft : undefined
  }
  // What is not shown is dropped, so it cannot come back if the value returns to what it was.
  if (KINDS.some((kind) => drafts[kind] != null && shown(kind) == null)) {
    setDrafts(Object.fromEntries(KINDS.map((kind) => [kind, shown(kind)])))
  }
  // The editor as it will be once the typing is committed: the plates and the buttons follow the typing.
  const typed = settle(e, drafts).editor
  const weight = typed.weightKg
  const perSide = weight == null ? null : weight === e.weightKg ? item.perSide : (grit.platesPerSide(weight, changePlates) ?? null)

  /** Leaving a field commits what was typed into it. */
  const commit = (kind: EntryKind) => {
    const settled = settle(e, { [kind]: drafts[kind] })
    setDrafts({ ...drafts, [kind]: settled.drafts[kind] })
    if (!sameEditor(settled.editor, e)) actions.edit(id, settled.editor)
  }
  /** − or +: steps from what is typed, or from the last good value when that was refused, and clears the message. */
  const step = (kind: EntryKind, next: (editor: SetEditor) => number) => {
    const settled = settle(e, drafts)
    setDrafts({ ...settled.drafts, [kind]: undefined })
    actions.edit(id, { ...settled.editor, [FIELD_OF[kind]]: next(settled.editor) })
  }
  const fields = useRef<HTMLDivElement>(null)
  /** Log set or Missed. A refused entry on the card stops it: the lifter meant some other number, so the cursor goes to it instead. */
  const log = (failed: boolean) => {
    const settled = settle(e, drafts)
    const refused = KINDS.findIndex((kind) => settled.drafts[kind] != null)
    // The cursor moves first: the field it leaves commits its own entry, and the whole card's is stored after it.
    if (refused >= 0) fields.current?.querySelectorAll('input')[refused]?.focus()
    setDrafts(settled.drafts)
    if (!sameEditor(settled.editor, e)) actions.edit(id, settled.editor)
    if (refused >= 0) return
    const now = settled.editor
    if (now.weightKg != null) void actions.logSet(id, { weightKg: now.weightKg, reps: now.reps, rpe: now.rpe, failed })
  }

  const field = (kind: EntryKind, label: string, valueText: string, inputMode: 'decimal' | 'numeric', unit?: string) => (
    <NumberField
      label={label}
      valueText={shown(kind)?.text ?? valueText}
      unit={unit}
      placeholder={unit}
      inputMode={inputMode}
      error={shown(kind)?.error}
      onDraft={(text) => setDrafts({ ...drafts, [kind]: { text, base: e[FIELD_OF[kind]] } })}
      onCommit={() => commit(kind)}
      onFocusChange={(has) => setFocused((now) => (has ? kind : now === kind ? null : now))}
    />
  )
  const weightText = e.weightKg != null ? formatKg(e.weightKg) : ''

  return (
    <>
      <div className="today-steppers" ref={fields}>
        <StepperField
          className="today-stepper-weight"
          label="Weight"
          valueText={weightText}
          unit="kg"
          canDecrement={weight != null && weight > 0}
          canIncrement={weight == null || weight < MAX_KG}
          onDecrement={() => step('kg', (now) => stepWeight(now.weightKg, -weightStepKg))}
          onIncrement={() => step('kg', (now) => stepWeight(now.weightKg, weightStepKg))}
          tick
          field={field('kg', 'Weight, kilograms', weightText, 'decimal', 'kg')}
        />
        <StepperField
          label="Reps"
          valueText={String(e.reps)}
          canDecrement={typed.reps > 1}
          canIncrement={typed.reps < MAX_REPS}
          onDecrement={() => step('reps', (now) => now.reps - 1)}
          onIncrement={() => step('reps', (now) => now.reps + 1)}
          tick
          field={field('reps', 'Reps', String(e.reps), 'numeric')}
        />
        <StepperField
          label="RPE"
          valueText={formatRpe(e.rpe)}
          canDecrement={typed.rpe > MIN_RPE}
          canIncrement={typed.rpe < MAX_RPE}
          onDecrement={() => step('rpe', (now) => Math.max(MIN_RPE, now.rpe - 0.5))}
          onIncrement={() => step('rpe', (now) => Math.min(MAX_RPE, now.rpe + 0.5))}
          tick
          field={field('rpe', 'RPE', formatRpe(e.rpe), 'decimal')}
          labelAction={<RpeInfoButton inline />}
        />
      </div>

      {weight == null ? (
        <PlateStripNote title="No weight yet" caption="Type the weight to see the plates." />
      ) : perSide != null ? (
        // Accessories may be dumbbells or machines: only draw a bar when there are plates to put on it.
        (isBarbellLift || perSide.length > 0) && <PlateStrip perSide={perSide} totalKg={weight} />
      ) : (
        isBarbellLift && <PlateStrip perSide={null} totalKg={weight} nearestKg={grit.roundLoad(weight, changePlates)} />
      )}

      <div className="today-log">
        <SecondaryButton text="Missed" onClick={() => log(true)} enabled={weight != null} icon={<CloseIcon />} />
        <PrimaryButton className="today-grow" text={`Log set ${item.sets.length + 1}`} onClick={() => log(false)} enabled={weight != null} icon={<CheckIcon />} />
      </div>
    </>
  )
}

/** Another exercise in today's session, folded to one line. Tap to log it. */
export function ExerciseRow({ item, onClick }: { item: TodayItem; onClick: () => void }) {
  return (
    <Card contentPadding={0} className="today-row-card">
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
      {/* Beside the row's button, not inside it: reading the guide must not open the card. */}
      <InfoButton name={item.exercise.name} />
    </Card>
  )
}
