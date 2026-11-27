import { useEffect, useRef, useState } from 'react'
import { restorePlan, type PlanSnapshot } from '../../db/catalogue'
import type { GritDb } from '../../db/db'
import { grit, todayIso } from '../../domain'
import { haptic } from '../../platform/haptics'
import type { TrainingMode } from '../../db/schema'
import type { PickerEntry } from '../../state/exercisePicker'
import { usePlan, type DaySection, type PlanActions, type PlanDraft, type PlanExercise, type PlanItem, type PlanRow, type PlanState } from '../../state/usePlan'
import { dayName } from '../../state/weekdayNames'
import { IconButton, PrimaryButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { formatKg, formatRpe } from '../../ui/components/format'
import { AddIcon, DeleteIcon, EditIcon, KeyboardArrowDownIcon, KeyboardArrowUpIcon, LinkIcon, LinkOffIcon, ReplayIcon, SettingsIcon, ViewAgendaIcon } from '../../ui/components/icons'
import { Menu } from '../../ui/components/Menu'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { Snackbar } from '../../ui/components/Snackbar'
import { slideName } from '../../ui/viewTransition'
import { ExercisePicker } from '../library/ExercisePicker'
import { UNDO } from '../today/SkipToday'
import { InfoButton } from '../library/InfoButton'
import { ADD_FORM_TAP_WINDOW_MS, BaselineDialog, PlanItemDialog } from './PlanDialogs'
import './plan.css'

export type PlanScreenProps = {
  db: GritDb
  onOpenSettings: () => void
  /** Opens the plan builder, which only the bodybuilding side has. Without it the side offers no builder. */
  onBuildPlan?: () => void
  /** Opens the templates for the side shown (1.14), on both sides. Without it Plan offers no templates. */
  onChooseTemplate?: (mode: TrainingMode) => void
  /**
   * A message for the snackbar once (after a template is saved, "Plan set: …"); `onMessageShown` clears it. With an
   * `undo`, the snackbar offers Undo of that plan replace for its usual time (1.14.1).
   */
  message?: string | PlanMessage | null
  onMessageShown?: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/** The weekly template: one card per training day, then the known bests. */
export function PlanScreen({ db, onOpenSettings, onBuildPlan, onChooseTemplate, message, onMessageShown, today = todayIso }: PlanScreenProps) {
  const plan = usePlan(db, today, { knownBests: true })
  // The snapshot's own side, whichever side Plan is showing.
  const undoReplace = (undo: PlanSnapshot) => restorePlan(db, undo.mode, undo)
  return (
    <PlanContent
      state={plan}
      actions={plan}
      onOpenSettings={onOpenSettings}
      onBuildPlan={onBuildPlan}
      onChooseTemplate={onChooseTemplate}
      message={message}
      onMessageShown={onMessageShown}
      onUndoReplace={undoReplace}
    />
  )
}

/** An item's reps: "5", or a range "8–12" on the bodybuilding side. */
export const repsOf = (item: Pick<PlanItem, 'reps' | 'repsHigh'>): string => grit.repsText(item.reps, item.repsHigh ?? null)

/** The line a plan item is read and found by, e.g. "Squat  3 × 5 @ 8" or "Curl  3 × 8–12 @ 8" (the same as on Android). */
export function planItemText(name: string, item: Pick<PlanItem, 'sets' | 'reps' | 'repsHigh' | 'targetRpe'>): string {
  return `${name}  ${item.sets} × ${repsOf(item)} @ ${formatRpe(item.targetRpe)}`
}

/** An empty bodybuilding plan says so, and offers the plan builder. */
export const EMPTY_BODYBUILDING_PLAN = 'Your bodybuilding plan is empty. Add exercises to each training day.'

/** The line an empty powerlifting plan shows, over its "Choose a template" button (1.14). */
export const EMPTY_POWERLIFTING_PLAN = 'Your plan is empty. Start from a template or add exercises to each day.'

export const CHOOSE_A_TEMPLATE = 'Choose a template'

/** A Known bests row's line (1.16.1), in Progress's own term: "Best e1RM 182.5 kg". `kg` is already to the half kilo. */
export const knownBestText = (kg: number): string => `Best e1RM ${formatKg(kg)} kg`

/** A message for Plan's snackbar from elsewhere, and the plan replace its Undo takes back, if any (1.14.1). */
export type PlanMessage = { text: string; undo?: PlanSnapshot }

/** The builder's snackbar on Plan after a save (1.14.1). */
export const PLAN_SAVED = 'Plan saved.'

/** An Undo of a plan replace that failed: nothing changed. */
export const UNDO_FAILED = "Couldn't undo. Your new plan is kept."

/** How long a snackbar stays, also one with Undo: the Snackbar's own default. */
const SNACKBAR_MS = 4000

type OpenDialog =
  | { kind: 'add'; weekday: number }
  /** The sets and reps form for what was just picked on Plan's Add: nothing is stored until its Add. */
  | { kind: 'addDetails'; weekday: number; entry: PickerEntry; draft: PlanDraft }
  | { kind: 'edit'; item: PlanItem; exerciseName: string }
  | { kind: 'baseline'; lift: PlanExercise }
  | null

export type PlanContentProps = {
  state: PlanState
  actions: PlanActions
  onOpenSettings: () => void
  onBuildPlan?: () => void
  onChooseTemplate?: (mode: TrainingMode) => void
  message?: string | PlanMessage | null
  onMessageShown?: () => void
  /** Undo of a plan replace: resolves to "stale" when a newer change came first, and nothing changed. */
  onUndoReplace?: (undo: PlanSnapshot) => Promise<'restored' | 'stale'>
}

const SAVE_FAILED = "Couldn't save — try again."

/** The Plan screen without its data source. A write that fails says so; it never fails silently. */
export function PlanContent({ state, actions, onOpenSettings, onBuildPlan, onChooseTemplate, message, onMessageShown, onUndoReplace }: PlanContentProps) {
  const [dialog, setDialog] = useState<OpenDialog>(null)
  // Counts failed writes: 0 shows nothing, and each new failure shows the message afresh.
  const [failures, setFailures] = useState(0)
  const reported = (work: Promise<unknown>) => void work.catch(() => setFailures((n) => n + 1))
  // The add form whose Add was tapped: a second tap on it, before it has closed, stores nothing more.
  const added = useRef<OpenDialog>(null)
  // The add form, with the ready-made numbers for `entry`, in the picker's place: one render, no moment without a dialog.
  const addForm = (weekday: number, entry: PickerEntry): OpenDialog => ({ kind: 'addDetails', weekday, entry, draft: actions.draft(entry) })

  // A message from elsewhere (a template saved) is taken once: shown here, and cleared where it came from.
  // Leaving Plan while it shows drops its Undo with it: the plan replace then stays, as after the snackbar's time.
  const [note, setNote] = useState<{ text: string; id: number; undo?: PlanSnapshot } | null>(null)
  const shownMessage = useRef(onMessageShown)
  shownMessage.current = onMessageShown
  useEffect(() => {
    if (message == null) return
    const { text, undo } = typeof message === 'string' ? { text: message, undo: undefined } : message
    setNote((n) => ({ text, id: (n?.id ?? 0) + 1, undo }))
    shownMessage.current?.()
  }, [message])
  const undoing = useRef(false)
  const undoReplace = (undo: PlanSnapshot) => {
    // One Undo at a time; a stale one (a newer replace or Undo came first) does nothing.
    if (undoing.current || onUndoReplace == null) return
    undoing.current = true
    onUndoReplace(undo)
      .catch(() => setNote((n) => ({ text: UNDO_FAILED, id: (n?.id ?? 0) + 1 })))
      .finally(() => {
        undoing.current = false
      })
  }

  // Nothing is called empty, and no menu is drawn, until the stored plan has been read (as Android's 9c89f4d).
  const loaded = !state.loading
  const bodybuilding = loaded && state.side === 'BODYBUILDING'
  const emptyPlan = loaded && state.sections.every((section) => section.rows.length === 0)
  // The bodybuilding side always says when it is empty; the powerlifting side does when it has templates to offer.
  const empty = emptyPlan && (bodybuilding || onChooseTemplate != null)
  const chooseTemplate = onChooseTemplate ? () => onChooseTemplate(state.side) : undefined
  const settings = <IconButton label="Settings" icon={<SettingsIcon />} onClick={onOpenSettings} />
  // Each side's menu once it has a plan: the builder on the bodybuilding side, then the templates on both.
  const menu = [
    ...(bodybuilding && onBuildPlan ? [{ text: 'Rebuild my plan', icon: <ReplayIcon />, onSelect: onBuildPlan }] : []),
    ...(chooseTemplate ? [{ text: CHOOSE_A_TEMPLATE, icon: <ViewAgendaIcon />, onSelect: chooseTemplate }] : []),
  ]
  const headerActions =
    loaded && !emptyPlan && menu.length > 0 ? (
      <span className="plan-header-actions">
        <Menu label="Plan options" items={menu} />
        {settings}
      </span>
    ) : (
      settings
    )

  return (
    <section className="screen">
      <ScreenHeader title="Plan" eyebrow="Weekly template" label={<ModeLabel />} actions={headerActions} />
      <div className="screen-body">
        {empty && (
          <div className="plan-empty">
            <p className="plan-empty-side body-medium">{bodybuilding ? EMPTY_BODYBUILDING_PLAN : EMPTY_POWERLIFTING_PLAN}</p>
            {/* A template is the first choice on both sides; the bodybuilding builder is under it. */}
            {chooseTemplate && <PrimaryButton text={CHOOSE_A_TEMPLATE} onClick={chooseTemplate} className="plan-empty-wide" />}
            {bodybuilding &&
              onBuildPlan &&
              (chooseTemplate ? <SecondaryButton text="Build my plan" onClick={onBuildPlan} className="plan-build plan-empty-wide" /> : <PrimaryButton text="Build my plan" onClick={onBuildPlan} className="plan-build" />)}
          </div>
        )}
        {state.sections.map((section) => (
          <DayCard
            key={section.weekday}
            section={section}
            onAdd={() => setDialog({ kind: 'add', weekday: section.weekday })}
            onEdit={(row) => setDialog({ kind: 'edit', item: row.item, exerciseName: row.exerciseName })}
            onMove={(item, direction) => reported(actions.move(item, direction))}
            onDelete={(item) => reported(actions.remove(item))}
            onSuperset={(item, linked) => reported(actions.setSuperset(item, linked))}
          />
        ))}
        {/* The squat, bench and deadlift baselines belong to the powerlifting side. */}
        {!state.loading && state.side !== 'BODYBUILDING' && (
          <div className="plan-bests">
            <SectionHeader title="Known bests" />
            <Card contentPadding={16} spacing={0} className="plan-bests-card">
              <p className="plan-muted body-small">Tell the coach your current maxes so it can advise from day one.</p>
              {state.mainExercises.map((lift) => (
                <div key={lift.id} className="plan-best" role="group" aria-label={lift.name}>
                  <KnownBestName name={lift.name} best={state.bests[lift.id]} />
                  <SecondaryButton text="Set best" onClick={() => setDialog({ kind: 'baseline', lift })} compact />
                </div>
              ))}
            </Card>
          </div>
        )}
      </div>

      {/*
        Picking opens the sets and reps form in the same render, filled in with the ready-made numbers; its Add stores the
        item and Cancel stores nothing. For its first moment the form ignores taps on Add, Cancel and outside it. Your own exercise is created first (and kept, whatever the form does); if that
        fails, no form opens.
      */}
      {(dialog?.kind === 'add' || dialog?.kind === 'addDetails') && (
        // While its add form is open, the picker stays mounted but hidden: closing it there would send the focus back to
        // the day's Add for a moment. Both close together, and the focus then goes back to Add.
        <div style={{ display: dialog.kind === 'addDetails' ? 'none' : 'contents' }}>
          <ExercisePicker
            title={`Add to ${dayName(dialog.weekday)}`}
            exercises={state.exercises}
            recentIds={state.recentIds}
            onPick={(entry) => setDialog(addForm(dialog.weekday, entry))}
            // The picker stays until the exercise is made, then the form opens over it. If making it fails, the picker's
            // create form stays with what was typed, and says so.
            onCreateCustom={(exercise) => {
              const picker = dialog
              return actions.addCustomExercise(exercise).then((entry) => setDialog((open) => (open === picker ? addForm(picker.weekday, entry) : open)))
            }}
            onDismiss={() => setDialog(null)}
            onCreateFailedWhileClosed={() => setFailures((n) => n + 1)}
          />
        </div>
      )}
      {dialog?.kind === 'addDetails' && (
        <PlanItemDialog
          title={`Add to ${dayName(dialog.weekday)}`}
          item={dialog.draft}
          exerciseName={dialog.entry.name}
          confirmText="Add"
          // It opens under the finger that picked: the second tap of a double tap on the row must not reach it.
          ignoreTapsFor={ADD_FORM_TAP_WINDOW_MS}
          exercises={state.exercises}
          recentIds={state.recentIds}
          onCreateCustom={actions.addCustomExercise}
          onFailure={() => setFailures((n) => n + 1)}
          // Each add ticks once, when it is stored; a failed add does not buzz.
          onSave={(exercise, sets, reps, targetRpe, repsHigh) => {
            if (added.current === dialog) return
            added.current = dialog
            reported(actions.add(dialog.weekday, exercise ?? dialog.entry, { sets, reps, targetRpe, ...(repsHigh != null ? { repsHigh } : {}) }).then(() => haptic('tick')))
            setDialog(null)
          }}
          onDismiss={() => setDialog(null)}
        />
      )}
      {dialog?.kind === 'edit' && (
        <PlanItemDialog
          title={`Edit ${dayName(dialog.item.weekday)}`}
          item={dialog.item}
          exerciseName={dialog.exerciseName}
          exercises={state.exercises}
          recentIds={state.recentIds}
          onCreateCustom={actions.addCustomExercise}
          onFailure={() => setFailures((n) => n + 1)}
          onSave={(exercise, sets, reps, rpe, repsHigh) => {
            reported(actions.save(dialog.item, exercise, sets, reps, rpe, repsHigh))
            setDialog(null)
          }}
          onDismiss={() => setDialog(null)}
        />
      )}
      {dialog?.kind === 'baseline' && (
        <BaselineDialog
          liftName={dialog.lift.name}
          onSave={(text) => {
            reported(actions.setBaseline(dialog.lift.id, text))
            setDialog(null)
          }}
          onDismiss={() => setDialog(null)}
        />
      )}

      <Snackbar
        message={failures > 0 ? SAVE_FAILED : (note?.text ?? null)}
        showId={failures > 0 ? failures : `note-${note?.id ?? 0}`}
        actionLabel={failures === 0 && note?.undo != null && onUndoReplace != null ? UNDO : undefined}
        onAction={() => {
          const undo = note?.undo
          setNote(null)
          if (undo != null) undoReplace(undo)
        }}
        // Undo is on offer for the usual snackbar time; after that the old plan is gone.
        durationMs={SNACKBAR_MS}
        onDismiss={() => (failures > 0 ? setFailures(0) : setNote(null))}
      />
    </section>
  )
}

/** A Known bests row's name, and under it the lift's best once it has one; with none, the row is as before 1.16.1. */
function KnownBestName({ name, best }: { name: string; best: number | undefined }) {
  if (best == null) return <span className="plan-grow title-medium">{name}</span>
  return (
    <span className="plan-grow plan-best-text">
      <span className="title-medium">{name}</span>
      <span className="plan-muted body-small">{knownBestText(best)}</span>
    </span>
  )
}

type DayCardProps = {
  section: DaySection
  onAdd: () => void
  onEdit: (row: PlanRow) => void
  onMove: (item: PlanItem, direction: -1 | 1) => void
  onDelete: (item: PlanItem) => void
  /** 1.24: links `item` to the one above it as a superset, or takes the link off. */
  onSuperset: (item: PlanItem, linked: boolean) => void
}

function DayCard({ section, onAdd, onEdit, onMove, onDelete, onSuperset }: DayCardProps) {
  const { rows } = section
  const name = dayName(section.weekday)
  const sets = rows.reduce((sum, row) => sum + row.item.sets, 0)
  const summary = rows.length === 0 ? 'Rest day' : `${rows.length} ${rows.length === 1 ? 'exercise' : 'exercises'} · ${sets} sets`
  return (
    <section aria-label={name} className="plan-day-section" {...slideName(`day-${section.weekday}`)}>
      <Card contentPadding={0} spacing={0} className="plan-day">
        <div className="plan-day-head">
          <div className="plan-grow">
            <h2 className="headline-medium">{name}</h2>
            <p className="plan-muted body-small">{summary}</p>
          </div>
          <SecondaryButton text="Add" onClick={onAdd} icon={<AddIcon />} compact />
        </div>
        {rows.length > 0 && (
          <ol className="plan-rows">
            {rows.map((row, i) => {
              // A pair's two rows are next to each other (1.24): the first names the second and the second the first, and
              // either one's menu takes the link off. The link itself is on the second.
              const partner = row.superset === 'FIRST' ? rows[i + 1] : row.superset === 'SECOND' ? rows[i - 1] : undefined
              const second = row.superset === 'FIRST' ? partner : row
              return (
                <PlanItemRow
                  key={row.item.id}
                  position={i + 1}
                  row={row}
                  partnerName={partner?.exerciseName ?? null}
                  canMoveUp={i > 0}
                  canMoveDown={i < rows.length - 1}
                  onMove={(direction) => onMove(row.item, direction)}
                  onEdit={() => onEdit(row)}
                  onDelete={() => onDelete(row.item)}
                  onSuperset={(linked) => onSuperset(linked ? row.item : (second ?? row).item, linked)}
                />
              )
            })}
          </ol>
        )}
      </Card>
    </section>
  )
}

type PlanItemRowProps = {
  position: number
  row: PlanRow
  canMoveUp: boolean
  canMoveDown: boolean
  onMove: (direction: -1 | 1) => void
  onEdit: () => void
  onDelete: () => void
  /** The other exercise of this row's superset (1.24); null when it is in none. */
  partnerName: string | null
  /** True: link this row to the one above it. False: take its superset's link off. */
  onSuperset: (linked: boolean) => void
}

/** A superset row's tag: "Superset A" on the first exercise, "Superset B" on the one done straight after it. */
export const supersetTag = (half: 'FIRST' | 'SECOND'): string => (half === 'FIRST' ? 'Superset A' : 'Superset B')

function PlanItemRow({ position, row, canMoveUp, canMoveDown, onMove, onEdit, onDelete, partnerName, onSuperset }: PlanItemRowProps) {
  const { item, exerciseName, superset } = row
  // Read with the row: "Squat  3 × 5 @ 8, superset with Row".
  const label = superset != null && partnerName != null ? `${planItemText(exerciseName, item)}, superset with ${partnerName}` : planItemText(exerciseName, item)
  return (
    <li className={superset === 'SECOND' ? 'plan-row plan-row-second' : 'plan-row'} aria-label={label} {...slideName(`plan-${item.id}`)}>
      <span className="plan-row-position title-small" aria-hidden="true">
        {position}
      </span>
      <div className="plan-grow plan-row-text" aria-hidden="true">
        {superset != null && <p className="plan-row-superset eyebrow">{supersetTag(superset)}</p>}
        <p className="plan-row-name title-medium">{exerciseName}</p>
        <p className="plan-row-dose headline-small">
          {item.sets} × {repsOf(item)}
          <span className="plan-muted"> @ </span>
          {formatRpe(item.targetRpe)}
        </p>
      </div>
      <InfoButton name={exerciseName} />
      <IconButton label="Move up" icon={<KeyboardArrowUpIcon />} onClick={() => onMove(-1)} enabled={canMoveUp} />
      <IconButton label="Move down" icon={<KeyboardArrowDownIcon />} onClick={() => onMove(1)} enabled={canMoveDown} />
      <Menu
        label="More"
        items={[
          { text: 'Edit', icon: <EditIcon />, onSelect: onEdit },
          // 1.24: offered where the rule allows it (one above it, another exercise, neither in a pair already).
          ...(row.canLink ? [{ text: 'Superset with the one above', icon: <LinkIcon />, onSelect: () => onSuperset(true) }] : []),
          ...(superset != null ? [{ text: 'Remove superset', icon: <LinkOffIcon />, onSelect: () => onSuperset(false) }] : []),
          { text: 'Delete', icon: <DeleteIcon />, danger: true, onSelect: onDelete },
        ]}
      />
    </li>
  )
}
