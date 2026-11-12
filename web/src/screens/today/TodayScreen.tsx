import { useEffect, useRef, useState, type ReactNode } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { useToday, type TodayActions, type TodayItem, type TodayState } from '../../state/useToday'
import { SecondaryButton } from '../../ui/components/Button'
import { EmptyState } from '../../ui/components/EmptyState'
import { AddIcon, FitnessCenterIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { Snackbar } from '../../ui/components/Snackbar'
import { BackupReminderCard } from './BackupReminderCard'
import { ExerciseCard, ExerciseRow, type NextStep } from './ExerciseCard'
import { StartSessionCard, SummaryCard } from './SessionCards'
import { ExercisePicker } from '../library/ExercisePicker'
import { TypoDialog } from './TodayDialogs'
import './today.css'

export type TodayScreenProps = {
  db: GritDb
  onOpenCoach: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
  /** Fires after each saved set (true for a main lift or variation), so a rest timer can start. */
  onSetLogged?: (isMainOrVariation: boolean) => void
  /** Opens Settings, for the backup reminder. Without it there is no reminder. */
  onOpenSettings?: () => void
  /** A card shown above the start card, before the backup reminder: the install card. */
  banner?: ReactNode
}

/** The logging screen with a session of its own: start card, the session's exercises, or the finish summary. */
export function TodayScreen({ onSetLogged, ...props }: TodayScreenProps) {
  const session = useToday(props.db, props.today ?? todayIso, onSetLogged)
  return <TodayView session={session} {...props} />
}

export type TodayViewProps = Omit<TodayScreenProps, 'onSetLogged'> & {
  /** The session to show, from `useToday`: the app's shell keeps one for the Today and Exercises tabs alike. */
  session: TodayState & TodayActions
}

/** The logging screen over a session kept elsewhere, so that it outlives the screen. */
export function TodayView({ session, db, onOpenCoach, today = todayIso, onOpenSettings, banner }: TodayViewProps) {
  const notice = (
    <>
      {banner}
      {onOpenSettings && <BackupReminderCard db={db} today={today()} onBackUp={onOpenSettings} />}
    </>
  )
  return <TodayContent state={session} actions={session} onOpenCoach={onOpenCoach} notice={notice} />
}

function dateParts(isoDate: string): { weekday: string; day: string; month: string } {
  const parts = new Intl.DateTimeFormat(undefined, { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' }).formatToParts(new Date(`${isoDate}T00:00:00Z`))
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? ''
  return { weekday: part('weekday'), day: part('day'), month: part('month') }
}

/** Planned sets still to do, or an added exercise with nothing logged yet. */
const hasWorkLeft = (item: TodayItem) => (item.targetSets != null ? !item.isComplete : item.sets.length === 0)

export type TodayContentProps = {
  state: TodayState
  actions: TodayActions
  onOpenCoach: () => void
  /** Shown above the start card or the summary, never while a session is being logged. */
  notice?: ReactNode
}

const SAVE_FAILED = "Couldn't save — try again."

/** The Today screen without its data source. A write that fails says so; it never fails silently. */
export function TodayContent({ state, actions: raw, onOpenCoach, notice }: TodayContentProps) {
  const { items } = state
  // Counts failed writes: 0 shows nothing, and each new failure shows the message afresh.
  const [failures, setFailures] = useState(0)
  const reported =
    <A extends unknown[]>(action: (...args: A) => Promise<void>) =>
    (...args: A) =>
      action(...args).catch(() => setFailures((n) => n + 1))
  const actions: TodayActions = {
    ...raw,
    startSession: reported(raw.startSession),
    logSet: reported(raw.logSet),
    confirmPending: reported(raw.confirmPending),
    setMissPoint: reported(raw.setMissPoint),
    deleteSet: reported(raw.deleteSet),
    applyTarget: reported(raw.applyTarget),
    dismissTarget: reported(raw.dismissTarget),
    finishSession: reported(raw.finishSession),
  }
  const { resolved } = state
  const inSession = state.sessionId != null && state.summary == null
  const [activeId, setActiveId] = useState<number | null>(null)
  const [picking, setPicking] = useState(false)
  const { weekday, day, month } = dateParts(state.date)

  // A catalogue exercise added without a row changes id when its first set saves the row: follow it.
  const chosenId = activeId != null ? (resolved[activeId] ?? activeId) : null
  useEffect(() => {
    if (chosenId !== activeId) setActiveId(chosenId)
  }, [chosenId, activeId])
  // Its card keeps the key it was added under, so it is the same card before and after: no jump, no lost state.
  const keyOf = (item: TodayItem) => Object.keys(resolved).find((standIn) => resolved[Number(standIn)] === item.exercise.id) ?? item.exercise.id

  // The exercise being logged: the one the lifter chose, else the first with sets left to do.
  const active = items.find((i) => i.exercise.id === chosenId) ?? items.find(hasWorkLeft) ?? items[items.length - 1]
  const activeExerciseId = active?.exercise.id ?? null
  // Pin it once chosen, so finishing its last set does not whisk the card (and its "where did it stall?") away.
  useEffect(() => {
    if (activeId == null && activeExerciseId != null) setActiveId(activeExerciseId)
  }, [activeId, activeExerciseId])

  const activeCard = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (inSession && activeExerciseId != null) activeCard.current?.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' })
  }, [inSession, activeExerciseId])

  function nextStep(index: number): NextStep {
    const upNext = [...items.slice(index + 1), ...items.slice(0, index)].find(hasWorkLeft)
    return upNext ? { label: `Next: ${upNext.exercise.name}`, onClick: () => setActiveId(upNext.exercise.id) } : { label: 'Finish session', onClick: () => void actions.finishSession() }
  }

  return (
    <section className="screen">
      <ScreenHeader
        title="Today"
        eyebrow={`${weekday} ${day} ${month}`}
        actions={inSession && <SecondaryButton className="today-finish" text="Finish" onClick={() => void actions.finishSession()} compact />}
      />
      <div className="screen-body">
        {!state.loading && !inSession && notice}
        {state.loading ? null : state.summary != null ? (
          <SummaryCard summary={state.summary} onOpenCoach={onOpenCoach} onKeepLogging={actions.dismissSummary} />
        ) : state.sessionId == null ? (
          <StartSessionCard items={items} dayName={weekday} onStart={(bodyweightKg, readiness) => void actions.startSession(bodyweightKg, readiness)} />
        ) : (
          <>
            {items.map((item, i) =>
              item === active ? (
                <div key={keyOf(item)} ref={activeCard}>
                  <ExerciseCard
                    item={item}
                    position={i + 1}
                    count={items.length}
                    weightStepKg={state.weightStepKg}
                    changePlates={state.changePlates}
                    next={nextStep(i)}
                    actions={actions}
                  />
                </div>
              ) : (
                <ExerciseRow key={keyOf(item)} item={item} onClick={() => setActiveId(item.exercise.id)} />
              ),
            )}
            {items.length === 0 && <EmptyState icon={<FitnessCenterIcon />} title="No exercises yet" body="Add what you are training today and log it set by set." />}
            <SecondaryButton text="Add exercise" onClick={() => setPicking(true)} icon={<AddIcon />} />
          </>
        )}
      </div>

      {state.pending && <TypoDialog pending={state.pending} onConfirm={() => void actions.confirmPending()} onEdit={actions.cancelPending} />}

      {picking && (
        <ExercisePicker
          title="Add exercise"
          exercises={state.exercises}
          recentIds={state.recentIds}
          // Picking one already in the session just opens its card.
          onPick={(entry) => {
            if (entry.library) setActiveId(actions.addFromCatalogue(entry.library.name))
            else if (entry.exerciseId != null) {
              actions.addExercise(entry.exerciseId)
              setActiveId(entry.exerciseId)
            }
            setPicking(false)
          }}
          onCreateCustom={(exercise) => {
            raw.createExercise(exercise).then(setActiveId, () => setFailures((n) => n + 1))
            setPicking(false)
          }}
          onDismiss={() => setPicking(false)}
        />
      )}

      <Snackbar message={failures > 0 ? SAVE_FAILED : null} showId={failures} onDismiss={() => setFailures(0)} />
    </section>
  )
}
