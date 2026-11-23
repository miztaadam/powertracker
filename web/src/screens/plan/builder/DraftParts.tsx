import type { Ref } from 'react'
import { grit, type JsDraftExercise, type JsDraftSession, type JsLibraryExercise, type JsPlanDraft } from '../../../domain'
import { dayName } from '../../../state/weekdayNames'
import { IconButton, SecondaryButton } from '../../../ui/components/Button'
import { Card } from '../../../ui/components/Card'
import { Dialog } from '../../../ui/components/Dialog'
import { CloseIcon } from '../../../ui/components/icons'
import { SectionHeader } from '../../../ui/components/ScreenHeader'
import { ChoiceChip } from '../../../ui/components/SegmentedChoice'
import { formatRpe } from '../../../ui/components/format'
import { planItemText, repsOf } from '../PlanScreen'
import '../plan.css'
import './builder.css'

// The draft's parts, shared by the plan builder and the plan templates (1.14). Their defaults are the builder's own.

export const WEEKDAYS_QUESTION = 'Which days?'
export const WEEKLY_SETS_LINE = '10 or more a week is the guideline for growth. Make a body part a priority to give it more.'
export const SAVE_FAILED = 'The plan could not be saved. Nothing was changed.'

export const WEEK = [1, 2, 3, 4, 5, 6, 7]
/** "Mon", as on the weekday chips. */
export const shortDay = (day: number) => dayName(day, 'short').slice(0, 3)
/** "Mon, Wed, Fri": the chosen days under the chips. */
export const chosenDaysText = (days: readonly number[]) => [...days].sort((a, b) => a - b).map(shortDay).join(', ')
/** "Choose 3 days.": under the chips while the count is off. */
export const chooseDaysText = (count: number) => `Choose ${count} days.`

/** The seven weekday chips, Monday first, in one card. */
export function WeekdayChips({ days, onDay, label = WEEKDAYS_QUESTION }: { days: readonly number[]; onDay: (day: number) => void; label?: string }) {
  return (
    <Card contentPadding={0} className="pb-days">
      <div className="pb-days-row" role="group" aria-label={label}>
        {WEEK.map((day) => (
          <ChoiceChip key={day} text={shortDay(day)} selected={days.includes(day)} onClick={() => onDay(day)} />
        ))}
      </div>
    </Card>
  )
}

/** An exercise's numbers as a plan item has them. With fixed reps (the powerlifting side) there is no top of a range. */
export const prescription = (e: JsDraftExercise, fixedReps = false) => ({ sets: e.sets, reps: e.repsLow, repsHigh: fixedReps ? undefined : e.repsHigh, targetRpe: e.rpe })

export type DraftSessionCardProps = {
  session: JsDraftSession
  onSwap: (exercise: JsDraftExercise) => void
  /** The day's name in the heading and the region's label: the weekday by default. */
  where?: string
  /** Reps are fixed ("3 × 5 @ 8"), not a range. */
  fixedReps?: boolean
  /** Whether an exercise has a Swap button: all do by default. */
  canSwap?: (exercise: JsDraftExercise) => boolean
}

/** One day of a draft week: "<Day> · <title>", its count, and each exercise with its numbers and Swap. */
export function DraftSessionCard({ session, onSwap, where = dayName(session.day), fixedReps = false, canSwap = () => true }: DraftSessionCardProps) {
  const sets = session.exercises.reduce((sum, e) => sum + e.sets, 0)
  return (
    <section aria-label={where}>
      <Card contentPadding={0} spacing={0} className="plan-day">
        <div className="plan-day-head">
          <div className="plan-grow">
            <h2 className="headline-medium">{`${where} · ${session.title}`}</h2>
            <p className="plan-muted body-small">{`${session.exercises.length} exercises · ${sets} sets`}</p>
          </div>
        </div>
        <ol className="plan-rows">
          {session.exercises.map((e, i) => (
            <li key={e.name} className="plan-row" aria-label={planItemText(e.name, prescription(e, fixedReps))}>
              <span className="plan-row-position title-small" aria-hidden="true">
                {i + 1}
              </span>
              <div className="plan-grow plan-row-text" aria-hidden="true">
                <p className="plan-row-name title-medium">{e.name}</p>
                <p className="pb-dose title-medium">
                  {e.sets} × {repsOf(prescription(e, fixedReps))}
                  <span className="plan-muted"> @ </span>
                  {formatRpe(e.rpe)}
                </p>
              </div>
              {canSwap(e) && <SecondaryButton text="Swap" label={`Swap ${e.name}`} onClick={() => onSwap(e)} compact />}
            </li>
          ))}
        </ol>
      </Card>
    </section>
  )
}

/** Each body part's weekly sets, with the short ones marked, and the guideline under them. */
export function DraftVolume({ volume }: { volume: JsPlanDraft['volume'] }) {
  return (
    <section aria-label="Weekly sets">
      <SectionHeader title="Weekly sets" />
      <Card contentPadding={16} spacing={0}>
        <ul className="pb-volume">
          {volume.map((v) => {
            const label = grit.muscleGroupLabel(v.group)
            return (
              <li key={v.group} className="pb-volume-row" aria-label={`${label}, ${v.weeklySets} sets${v.short ? ', under 10' : ''}`}>
                <span className="pb-grow title-medium" aria-hidden="true">
                  {label}
                </span>
                {v.short && (
                  <span className="pb-short label-medium" aria-hidden="true">
                    Under 10
                  </span>
                )}
                <span className="pb-sets title-medium" aria-hidden="true">
                  {v.weeklySets}
                </span>
              </li>
            )
          })}
        </ul>
        <p className="pb-note body-small">{WEEKLY_SETS_LINE}</p>
      </Card>
    </section>
  )
}

/** What could not go in the draft, each with its reason. Nothing when the list is empty. */
export function DraftLeftOut({ leftOut }: { leftOut: JsPlanDraft['leftOut'] }) {
  if (leftOut.length === 0) return null
  return (
    <section aria-label="Left out">
      <SectionHeader title="Left out" />
      <Card contentPadding={16} spacing={0}>
        <ul className="pb-left-out">
          {leftOut.map((l) => (
            <li key={l.name} className="pb-left-out-row">
              <p className="title-medium">{l.name}</p>
              <p className="pb-note body-small">{l.reason}</p>
            </li>
          ))}
        </ul>
      </Card>
    </section>
  )
}

export type SwapSheetProps = {
  name: string
  /** The day, as the exercise's card names it. */
  where: string
  options: readonly JsLibraryExercise[]
  onPick: (name: string) => void
  onClose: () => void
}

/** The sheet of what `name` can be swapped for. */
export function SwapSheet({ name, where, options, onPick, onClose }: SwapSheetProps) {
  const title = `Swap ${name}`
  return (
    <Dialog
      open
      variant="sheet"
      portal
      title={title}
      onDismiss={onClose}
      header={
        <div className="lib-guide-head">
          <div className="lib-grow">
            <h2 className="headline-medium">{title}</h2>
            <p className="lib-guide-where eyebrow">{where}</p>
          </div>
          <IconButton label="Close" icon={<CloseIcon />} onClick={onClose} />
        </div>
      }
    >
      {options.length === 0 ? (
        <p className="pb-none body-medium">Nothing else in the library fits this place.</p>
      ) : (
        <ul className="lib-rows">
          {options.map((option) => (
            <li key={option.name} className="lib-row" aria-label={option.name}>
              <button type="button" className="lib-row-main" onClick={() => onPick(option.name)}>
                <span className="lib-grow">
                  <span className="lib-row-name title-medium">{option.name}</span>
                  {option.muscles.length > 0 && <span className="lib-row-muscles body-medium">{option.muscles.join(', ')}</span>}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Dialog>
  )
}

/** A failed save's line, at the top of a draft. */
export function SaveFailedLine({ text, lineRef }: { text: string; lineRef?: Ref<HTMLParagraphElement> }) {
  return (
    <p ref={lineRef} className="pb-failed body-medium" role="alert">
      {text}
    </p>
  )
}
