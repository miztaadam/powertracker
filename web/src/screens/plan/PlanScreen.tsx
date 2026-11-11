import { useState } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { usePlan, type DaySection, type PlanActions, type PlanExercise, type PlanItem, type PlanRow, type PlanState } from '../../state/usePlan'
import { dayName } from '../../state/weekdayNames'
import { IconButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { formatRpe } from '../../ui/components/format'
import { AddIcon, DeleteIcon, EditIcon, KeyboardArrowDownIcon, KeyboardArrowUpIcon, SettingsIcon } from '../../ui/components/icons'
import { Menu } from '../../ui/components/Menu'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { Snackbar } from '../../ui/components/Snackbar'
import { BaselineDialog, PlanItemDialog } from './PlanDialogs'
import './plan.css'

export type PlanScreenProps = {
  db: GritDb
  onOpenSettings: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/** The weekly template: one card per training day, then the known bests. */
export function PlanScreen({ db, onOpenSettings, today = todayIso }: PlanScreenProps) {
  const plan = usePlan(db, today)
  return <PlanContent state={plan} actions={plan} onOpenSettings={onOpenSettings} />
}

/** The line a plan item is read and found by, e.g. "Squat  3 × 5 @ 8" (the same as on Android). */
export function planItemText(name: string, item: Pick<PlanItem, 'sets' | 'reps' | 'targetRpe'>): string {
  return `${name}  ${item.sets} × ${item.reps} @ ${formatRpe(item.targetRpe)}`
}

type OpenDialog = { kind: 'edit'; weekday: number; item: PlanItem | null } | { kind: 'baseline'; lift: PlanExercise } | null

export type PlanContentProps = { state: PlanState; actions: PlanActions; onOpenSettings: () => void }

const SAVE_FAILED = "Couldn't save — try again."

/** The Plan screen without its data source. A write that fails says so; it never fails silently. */
export function PlanContent({ state, actions, onOpenSettings }: PlanContentProps) {
  const [dialog, setDialog] = useState<OpenDialog>(null)
  // Counts failed writes: 0 shows nothing, and each new failure shows the message afresh.
  const [failures, setFailures] = useState(0)
  const reported = (work: Promise<unknown>) => void work.catch(() => setFailures((n) => n + 1))

  return (
    <section className="screen">
      <ScreenHeader title="Plan" eyebrow="Weekly template" actions={<IconButton label="Settings" icon={<SettingsIcon />} onClick={onOpenSettings} />} />
      <div className="screen-body">
        {state.sections.map((section) => (
          <DayCard
            key={section.weekday}
            section={section}
            onAdd={() => setDialog({ kind: 'edit', weekday: section.weekday, item: null })}
            onEdit={(item) => setDialog({ kind: 'edit', weekday: item.weekday, item })}
            onMove={(item, direction) => reported(actions.move(item, direction))}
            onDelete={(item) => reported(actions.remove(item))}
          />
        ))}
        {!state.loading && (
          <div className="plan-bests">
            <SectionHeader title="Known bests" />
            <Card contentPadding={16} spacing={0} className="plan-bests-card">
              <p className="plan-muted body-small">Tell the coach your current maxes so it can advise from day one.</p>
              {state.mainExercises.map((lift) => (
                <div key={lift.id} className="plan-best" role="group" aria-label={lift.name}>
                  <span className="plan-grow title-medium">{lift.name}</span>
                  <SecondaryButton text="Set best" onClick={() => setDialog({ kind: 'baseline', lift })} compact />
                </div>
              ))}
            </Card>
          </div>
        )}
      </div>

      {dialog?.kind === 'edit' && (
        <PlanItemDialog
          title={`${dialog.item == null ? 'Add to' : 'Edit'} ${dayName(dialog.weekday)}`}
          exercises={state.exercises}
          initial={dialog.item}
          defaultRpe={state.defaultRpe}
          onCreateCustom={actions.addCustomExercise}
          onFailure={() => setFailures((n) => n + 1)}
          onSave={(exerciseId, sets, reps, rpe) => {
            reported(actions.save(dialog.weekday, dialog.item, exerciseId, sets, reps, rpe))
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

      <Snackbar message={failures > 0 ? SAVE_FAILED : null} showId={failures} onDismiss={() => setFailures(0)} />
    </section>
  )
}

type DayCardProps = {
  section: DaySection
  onAdd: () => void
  onEdit: (item: PlanItem) => void
  onMove: (item: PlanItem, direction: -1 | 1) => void
  onDelete: (item: PlanItem) => void
}

function DayCard({ section, onAdd, onEdit, onMove, onDelete }: DayCardProps) {
  const { rows } = section
  const name = dayName(section.weekday)
  const sets = rows.reduce((sum, row) => sum + row.item.sets, 0)
  const summary = rows.length === 0 ? 'Rest day' : `${rows.length} ${rows.length === 1 ? 'exercise' : 'exercises'} · ${sets} sets`
  return (
    <section aria-label={name}>
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
            {rows.map((row, i) => (
              <PlanItemRow
                key={row.item.id}
                position={i + 1}
                row={row}
                canMoveUp={i > 0}
                canMoveDown={i < rows.length - 1}
                onMove={(direction) => onMove(row.item, direction)}
                onEdit={() => onEdit(row.item)}
                onDelete={() => onDelete(row.item)}
              />
            ))}
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
}

function PlanItemRow({ position, row, canMoveUp, canMoveDown, onMove, onEdit, onDelete }: PlanItemRowProps) {
  const { item, exerciseName } = row
  return (
    <li className="plan-row" aria-label={planItemText(exerciseName, item)}>
      <span className="plan-row-position title-small" aria-hidden="true">
        {position}
      </span>
      <div className="plan-grow plan-row-text" aria-hidden="true">
        <p className="plan-ellipsis title-medium">{exerciseName}</p>
        <p className="headline-small">
          {item.sets} × {item.reps}
          <span className="plan-muted"> @ </span>
          {formatRpe(item.targetRpe)}
        </p>
      </div>
      <IconButton label="Move up" icon={<KeyboardArrowUpIcon />} onClick={() => onMove(-1)} enabled={canMoveUp} />
      <IconButton label="Move down" icon={<KeyboardArrowDownIcon />} onClick={() => onMove(1)} enabled={canMoveDown} />
      <Menu
        label="More"
        items={[
          { text: 'Edit', icon: <EditIcon />, onSelect: onEdit },
          { text: 'Delete', icon: <DeleteIcon />, danger: true, onSelect: onDelete },
        ]}
      />
    </li>
  )
}
