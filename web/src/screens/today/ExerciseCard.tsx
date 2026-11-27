import { useRef, useState } from 'react'
import { grit, todayIso } from '../../domain'
import { setUnit, type LoadUnit, type TrainingMode } from '../../db/schema'
import type { MissPoint, PlannedItem, SetEditor, StoredSet, Suggestion, Target, TodayActions, TodayItem } from '../../state/useToday'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { CoachSuggestion } from '../../ui/components/CoachSuggestion'
import { Dialog } from '../../ui/components/Dialog'
import { cx, formatKg, formatLoad, formatRpe } from '../../ui/components/format'
import { ArrowForwardIcon, BoltIcon, CheckIcon, CloseIcon } from '../../ui/components/icons'
import { Eyebrow } from '../../ui/components/NumberDisplay'
import { NumberField } from '../../ui/components/NumberField'
import { Pill } from '../../ui/components/Pill'
import { PlateStrip, PlateStripNote } from '../../ui/components/PlateStrip'
import { SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { SetRow } from '../../ui/components/SetRow'
import { StepperField } from '../../ui/components/StepperField'
import { slideName, withTransition } from '../../ui/viewTransition'
import { InfoButton } from '../library/InfoButton'
import { RpeInfoButton } from '../library/RpeSheet'
import { MAX_KG, MAX_REPS, MAX_RPE, MIN_RPE, entryMessage, parseEntry, weightKind, type EntryKind } from './entry'
import { LastTime } from './LastTime'
import { WarmUp } from './WarmUp'
import { warmUpSets } from '../../state/warmUp'
import { SkipMenu } from './SkipToday'
import './today.css'

/** Where the accent button leads once an exercise's sets are done: the next exercise, or Finish. */
export type NextStep = { label: string; onClick: () => void }

const BAR_KG = 20

/** RPE from which a set counts as a grind worth asking "where?" about. */
const GRIND_RPE = 9.5

const MISS_POINTS: readonly MissPoint[] = ['FLOOR', 'MID', 'LOCKOUT']

/** "Floor / Mid / Lockout", or "Off the shoulders / …" for a strict press. An accessory has no lift: it gets the plain names. */
const missLabels = (lift: string | null) => MISS_POINTS.map((point) => grit.missPointLabel(lift ?? 'SQUAT', point))

/** "155 × 5 @ 8"; "plate 7½ × 12 @ 8" for a plate number (1.17). */
export function doseText(weightKg: number, reps: number, rpe: number | null, unit: LoadUnit = 'KG'): string {
  return `${unit === 'PLATES' ? grit.plateText(weightKg) : formatKg(weightKg)} × ${reps}` + (rpe != null ? ` @ ${formatRpe(rpe)}` : '')
}

/** "5 × 5 @ 8", or "3 × 8–12 @ 8" for a rep range: sets × reps @ RPE as planned. */
export function planText(item: PlannedItem): string {
  return `${item.sets} × ${grit.repsText(item.reps, item.repsHigh ?? null)} @ ${formatRpe(item.targetRpe)}`
}

/**
 * The chip headline for a coach target: "155 × 5 @ 8", or for a rep range "26 kg × 8–12 @ 8" (the mockup's
 * bodybuilding card), plus the set count when the coach changes it. A plate target reads "plate 7 × 8–12 @ 8" (1.17).
 */
export function targetHeadline(target: Target): string {
  const unit = setUnit(target)
  const dose =
    target.repsHigh != null
      ? `${formatLoad(target.weightKg, unit)} × ${grit.repsText(target.reps, target.repsHigh)} @ ${formatRpe(target.rpe)}`
      : doseText(target.weightKg, target.reps, target.rpe, unit)
  return dose + (target.sets != null ? ` · ${target.sets} ${target.sets === 1 ? 'set' : 'sets'}` : '')
}

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8.", or "… Coach suggests 26 × 8–12 @ 8." on
// the bodybuilding side); the chip already shows it as its headline.
const SUGGESTION_SENTENCE = /\s*Coach suggests .*? @ \d+(\.\d+)?\./

export function targetReason(suggestion: Suggestion): string {
  return suggestion.reason.replace(SUGGESTION_SENTENCE, '').trim()
}

/**
 * The set a later look at this session would call the top set, among the sets in `unit` only (a plate number is never
 * weighed against kg), as an index into all of `sets`; null while every such set has the same weight.
 */
function topSetIndex(sets: StoredSet[], unit: LoadUnit): number | null {
  const mine = sets.flatMap((s, i) => (setUnit(s) === unit ? [{ s, i }] : []))
  if (new Set(mine.map(({ s }) => s.weightKg)).size < 2) return null
  let top: { s: StoredSet; i: number } | null = null
  for (const m of mine) if (!m.s.failed && (top == null || m.s.weightKg >= top.s.weightKg)) top = m
  return top?.i ?? null
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
  /** The day shown, `YYYY-MM-DD`: "Last time" writes the year of a session from another one. Left out, it is today. */
  today?: string
  /** Opens the reason sheet (1.13); left out, Skip today is not in the row menu (an added exercise, or one with sets). */
  onSkipToday?: () => void
  /** Opens the plate sheet (1.17); left out, Count plates is not in the row menu. With neither, there is no menu. */
  onCountPlates?: () => void
}

/** The exercise being logged: coach chip, sets so far, steppers, plates and the one big button. */
export function ExerciseCard({ item, position, count, weightStepKg, changePlates, next, actions, side = 'POWERLIFTING', today = todayIso(), onSkipToday, onCountPlates }: ExerciseCardProps) {
  const id = item.exercise.id
  const [extraSet, setExtraSet] = useState(false)
  const [selected, setSelected] = useState<{ number: number; set: StoredSet } | null>(null)
  const powerlifting = side === 'POWERLIFTING'
  const top = powerlifting ? topSetIndex(item.sets, setUnit(item)) : null
  const nextNumber = item.sets.length + 1
  // The card takes sets: the entry shows, not the way on.
  const entering = !item.isComplete || extraSet
  // Last time's set with the number of the one about to be logged, when it is in the unit the exercise counts in now.
  const matching = entering ? item.lastTime?.sets[item.sets.length] : undefined
  // ... and, for a plate number, on the grid the machine counts on now (plate 7½ is not offered once whole plates are
  // on), as the coach's targets are held to.
  const usable =
    matching != null && setUnit(matching) === setUnit(item) && (setUnit(matching) !== 'PLATES' || grit.plateCheck(matching.weightKg, item.plateCount ?? 'OFF') === 'OK')

  return (
    <Card spacing={8}>
      <div className="today-card-head">
        <div className="today-card-title">
          <Eyebrow text={`Exercise ${position} of ${count}`} />
          <h2 className="today-name headline-medium">{item.exercise.name}</h2>
        </div>
        {item.planned == null && item.sets.length === 0 ? (
          <QuietButton text="Remove" onClick={() => actions.removeExercise(id)} />
        ) : item.isComplete ? (
          <Pill text="Done" tone="success" icon={<CheckIcon />} />
        ) : (
          <Pill text={item.targetSets != null ? `Set ${nextNumber} of ${item.targetSets}` : `Set ${nextNumber}`} />
        )}
        {/* Together, at the right: when the head wraps they go to the next line as one, never the ⋮ alone at the left (1.17.1). */}
        <span className="today-card-actions">
          <InfoButton name={item.exercise.name} />
          <SkipMenu name={item.exercise.name} onSkipToday={onSkipToday} onCountPlates={onCountPlates} />
        </span>
      </div>

      {item.suggestion && (
        <CoachSuggestion
          headline={targetHeadline(item.suggestion.target)}
          reason={targetReason(item.suggestion)}
          // Ticks once the target is applied (useToday), once however fast the taps.
          onApply={() => void actions.applyTarget(item.suggestion!.id)}
          onDismiss={() => void actions.dismissTarget(item.suggestion!.id)}
        />
      )}

      {item.sets.length > 0 && (
        <div>
          {item.sets.map((set, i) => (
            <div key={set.id} {...slideName(`set-${set.id}`)}>
              <SetRow
                index={i + 1}
                weightKg={set.weightKg}
                reps={set.reps}
                rpe={set.rpe}
                state={set.failed ? 'failed' : 'done'}
                isTopSet={i === top}
                onClick={() => setSelected({ number: i + 1, set })}
                // Each set in its own unit, whatever the exercise's setting is now.
                unit={set.unit}
              />
              {powerlifting && (set.failed || (set.rpe ?? 0) >= GRIND_RPE) && <MissRow number={i + 1} set={set} lift={grit.mainLiftOf(item.exercise) ?? null} onSelect={(point) => void actions.setMissPoint(set.id, point)} />}
            </div>
          ))}
        </div>
      )}

      <LastTime
        last={item.lastTime}
        today={today}
        // The set about to be logged, while the card takes sets: its row of last time is marked.
        next={entering ? item.sets.length : undefined}
        // Weight and reps only, and only from a set in the unit the field is in now: a plate number never goes in a kg field.
        onUse={usable ? (set) => actions.edit(id, { ...item.editor, weightKg: set.weightKg, reps: set.reps }) : undefined}
      />

      {entering ? (
        <>
          <hr className="today-hairline" />
          {/* A change of weight field (kg, whole plates, half steps) starts the editor afresh: nothing typed in one is read as the other. */}
          <Editor key={weightKind(setUnit(item), item.plateCount ?? 'OFF')} item={item} weightStepKg={weightStepKg} changePlates={changePlates} actions={actions} />
        </>
      ) : (
        <>
          <PrimaryButton className="today-next" text={next.label} onClick={next.onClick} icon={<ArrowForwardIcon />} />
          <div className="today-center">
            <QuietButton text="Add another set" onClick={() => withTransition(() => setExtraSet(true))} />
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
          <p className="today-dose headline-small">{doseText(selected.set.weightKg, selected.set.reps, selected.set.rpe, setUnit(selected.set)) + (selected.set.failed ? ' · failed' : '')}</p>
        </Dialog>
      )}
    </Card>
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

/** The editor value each field types into: both plate kinds type the weight, as a plate number (1.17). */
const FIELD_OF = { kg: 'weightKg', reps: 'reps', rpe: 'rpe', plate: 'weightKg', plateHalf: 'weightKg' } as const satisfies Record<EntryKind, keyof SetEditor>

/**
 * Text in a field that is not the editor's value (yet). `base` is the editor value it was typed over:
 * when that has changed since (the coach's Apply), the text is stale and is never saved over the
 * newer value. `error` is set once the text has been refused.
 */
type Draft = { text: string; base: number | null; error?: string }
type Drafts = Partial<Record<EntryKind, Draft>>

/**
 * Puts every typed number that is good into the editor. What was refused stays, with its message; the rest is dropped.
 * Only the card's own `kinds` are read: a draft of another weight kind (a kg weight typed before plates were turned on)
 * is dropped unread, never put in the PLATE field.
 */
function settle(editor: SetEditor, drafts: Drafts, kinds: readonly EntryKind[]): { editor: SetEditor; drafts: Drafts } {
  const next = { ...editor }
  const kept: Drafts = {}
  for (const kind of kinds) {
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
  // The weight is typed as kg or as a plate number (1.17); each card settles only its own three kinds.
  const unit = setUnit(item)
  const plateCount = item.plateCount ?? 'OFF'
  const weightField = weightKind(unit, plateCount)
  const kinds: readonly EntryKind[] = [weightField, 'reps', 'rpe']

  // What a field shows instead of the editor's value. Text typed over an older value is stale: it
  // stays only where the cursor is, so the typing is not cut short. Anywhere else the newer value
  // shows, which is also what Log set saves. A refused entry always gives way to a newer value.
  const shown = (kind: EntryKind): Draft | undefined => {
    const draft = drafts[kind]
    return draft != null && (draft.base === e[FIELD_OF[kind]] || (draft.error == null && kind === focused)) ? draft : undefined
  }
  // What is not shown is dropped, so it cannot come back if the value returns to what it was.
  if ((Object.keys(drafts) as EntryKind[]).some((kind) => drafts[kind] != null && (!kinds.includes(kind) || shown(kind) == null))) {
    setDrafts(Object.fromEntries(kinds.map((kind) => [kind, shown(kind)])))
  }
  // The editor as it will be once the typing is committed: the plates and the buttons follow the typing.
  const typed = settle(e, drafts, kinds).editor
  const weight = typed.weightKg
  const perSide = weight == null || unit === 'PLATES' ? null : weight === e.weightKg ? item.perSide : (grit.platesPerSide(weight, changePlates) ?? null)
  // The warm-up follows the typing too (1.23); before the first set only, as the item's own. An exercise with no plan
  // item is judged a high-rep one by the reps in the field.
  const warmUp =
    item.sets.length > 0
      ? []
      : weight === e.weightKg && typed.reps === e.reps
        ? item.warmUp
        : warmUpSets(item.exercise, weight, item.planned?.reps ?? typed.reps, changePlates)

  /** Leaving a field commits what was typed into it. */
  const commit = (kind: EntryKind) => {
    const settled = settle(e, { [kind]: drafts[kind] }, kinds)
    setDrafts({ ...drafts, [kind]: settled.drafts[kind] })
    if (!sameEditor(settled.editor, e)) actions.edit(id, settled.editor)
  }
  /** − or +: steps from what is typed, or from the last good value when that was refused, and clears the message. */
  const step = (kind: EntryKind, next: (editor: SetEditor) => number) => {
    const settled = settle(e, drafts, kinds)
    setDrafts({ ...settled.drafts, [kind]: undefined })
    actions.edit(id, { ...settled.editor, [FIELD_OF[kind]]: next(settled.editor) })
  }
  const fields = useRef<HTMLDivElement>(null)
  /** Log set or Missed. A refused entry on the card stops it: the lifter meant some other number, so the cursor goes to it instead. */
  const log = (failed: boolean) => {
    const settled = settle(e, drafts, kinds)
    const refused = kinds.findIndex((kind) => settled.drafts[kind] != null)
    // The cursor moves first: the field it leaves commits its own entry, and the whole card's is stored after it.
    if (refused >= 0) fields.current?.querySelectorAll('input')[refused]?.focus()
    setDrafts(settled.drafts)
    if (!sameEditor(settled.editor, e)) actions.edit(id, settled.editor)
    if (refused >= 0) return
    const now = settled.editor
    // The set carries the unit of the field it was typed in; kg writes no unit key.
    if (now.weightKg != null) void actions.logSet(id, { weightKg: now.weightKg, reps: now.reps, rpe: now.rpe, failed, ...(unit === 'PLATES' ? { unit } : {}) })
  }

  const field = (kind: EntryKind, label: string, valueText: string, inputMode: 'decimal' | 'numeric', unitText?: string, placeholder = unitText) => (
    <NumberField
      label={label}
      valueText={shown(kind)?.text ?? valueText}
      unit={unitText}
      placeholder={placeholder}
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
      <WarmUp sets={warmUp} done={item.warmUpDone} onTick={(key, done) => actions.tickWarmUp(id, key, done)} />
      <div className="today-steppers" ref={fields}>
        {weightField === 'kg' ? (
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
        ) : (
          // A plate number: one step of the machine's grid; + on an empty field gives plate 1, − stops at plate 1. The
          // field shows 7.5 (what it reads back), with nothing after it.
          <StepperField
            className="today-stepper-weight"
            label="Plate"
            valueText={weightText}
            canDecrement={weight != null && weight > 1}
            canIncrement={weight == null || weight < grit.maxPlate()}
            onDecrement={() => step(weightField, (now) => grit.plateDown(now.weightKg, plateCount))}
            onIncrement={() => step(weightField, (now) => grit.plateUp(now.weightKg, plateCount))}
            tick
            field={field(weightField, 'Weight, plates', weightText, weightField === 'plateHalf' ? 'decimal' : 'numeric', undefined, 'plate')}
          />
        )}
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

      {unit === 'PLATES' ? null : weight == null ? (
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

/**
 * Another exercise in today's session, folded to one line. Tap to log it. `onSkipToday` (1.13) and `onCountPlates` (1.17)
 * put their items in the row menu; with neither there is none.
 */
export function ExerciseRow({ item, onClick, onSkipToday, onCountPlates }: { item: TodayItem; onClick: () => void; onSkipToday?: () => void; onCountPlates?: () => void }) {
  return (
    <Card contentPadding={0} className="today-row-card">
      <button type="button" className="today-row" onClick={onClick}>
        {/* The plan and the count go under the name when both do not fit: the name is never cut short. */}
        <span className="today-grow today-pair">
          <span className="today-pair-first today-pair-row">
            <span className="today-name title-medium">
              {item.exercise.name}
            </span>
            {item.suggestion && (
              <span className="today-row-bolt" role="img" aria-label="Coach suggestion">
                <BoltIcon size={18} />
              </span>
            )}
          </span>
          <span className="today-pair-second today-pair-row">
            {item.planned && <span className="today-row-plan headline-small">{planText(item.planned)}</span>}
            {item.isComplete ? (
              <Pill text="Done" tone="success" icon={<CheckIcon />} />
            ) : item.sets.length > 0 && item.targetSets != null ? (
              <Pill text={`${item.sets.length} of ${item.targetSets}`} />
            ) : (
              item.sets.length > 0 && <Pill text={`${item.sets.length} ${item.sets.length === 1 ? 'set' : 'sets'}`} />
            )}
          </span>
        </span>
      </button>
      {/* Beside the row's button, not inside it: reading the guide must not open the card. On a narrow row the two stack,
          so that a long word of the name still has its room (1.13). */}
      <span className="today-row-actions">
        <InfoButton name={item.exercise.name} />
        <SkipMenu name={item.exercise.name} onSkipToday={onSkipToday} onCountPlates={onCountPlates} />
      </span>
    </Card>
  )
}
