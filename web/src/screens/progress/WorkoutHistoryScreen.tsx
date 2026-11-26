import { useEffect, useRef, useState } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { useWorkoutHistory, type Workout, type WorkoutHistoryState } from '../../state/useWorkoutHistory'
import { IconButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { EmptyState } from '../../ui/components/EmptyState'
import { ArrowBackIcon, FitnessCenterIcon } from '../../ui/components/icons'
import { PastSets } from '../../ui/components/PastSets'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { formatSessionDate } from './dates'
import { WorkoutCalendar } from './WorkoutCalendar'
import { workoutSummary } from './workoutSummary'
import './progress.css'

export { workoutSummary }

/** How many workouts are drawn at first, and how many more each "Show older workouts" adds: a long log is never drawn whole. */
export const WORKOUTS_PER_PAGE = 20

export type WorkoutHistoryScreenProps = {
  db: GritDb
  onBack: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/** Every past workout, newest first, from both sides (1.19): opened from Progress's History button. Read-only. */
export function WorkoutHistoryScreen({ db, onBack, today = todayIso }: WorkoutHistoryScreenProps) {
  const state = useWorkoutHistory(db)
  return <WorkoutHistoryContent state={state} onBack={onBack} today={today()} />
}

const count = (n: number, one: string) => `${n} ${n === 1 ? one : `${one}s`}`

/** The id of a workout's date heading: what a tap on its day in the calendar brings up and puts the focus on. */
const headingId = (workout: Workout) => `workout-${workout.sessionId}`

/**
 * The History screen without its data source. `today` (1.22) is the day it is, for the calendar: required, so no test
 * or picture reads the real clock by leaving it out.
 */
export function WorkoutHistoryContent({ state, onBack, today }: { state: WorkoutHistoryState; onBack: () => void; today: string }) {
  const [shown, setShown] = useState(WORKOUTS_PER_PAGE)
  const total = state.workouts.length
  // A workout picked in the calendar: drawn first (it may be past the pages shown), then brought to the top.
  const [picked, setPicked] = useState<{ workout: Workout; at: number } | null>(null)
  const body = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (picked == null) return
    const heading = body.current?.querySelector<HTMLElement>(`#${headingId(picked.workout)}`)
    if (heading == null) return
    // The whole card to the top of the screen, at once (no animation to wait for), and the focus on its date, so a
    // screen reader and a keyboard go on from there. No history entry: Back still leaves for Progress.
    ;(heading.closest('.workout') ?? heading).scrollIntoView({ block: 'start' })
    heading.focus({ preventScroll: true })
  }, [picked])

  function open(workout: Workout) {
    const index = state.workouts.indexOf(workout)
    if (index < 0) return
    // Whole pages down to the one that holds it, as "Show older workouts" would have drawn them.
    setShown((n) => Math.max(n, Math.ceil((index + 1) / WORKOUTS_PER_PAGE) * WORKOUTS_PER_PAGE))
    setPicked((p) => ({ workout, at: (p?.at ?? 0) + 1 }))
  }

  return (
    <section className="screen">
      <ScreenHeader title="History" eyebrow={state.loading ? undefined : count(total, 'workout')} navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />} />
      <div className="screen-body" ref={body}>
        {!state.loading && total === 0 && <EmptyState icon={<FitnessCenterIcon />} title="No workouts yet" body="Every workout you log on Today will be listed here, with each exercise and its sets." />}
        {/* With nothing logged (or nothing read yet) there is nothing to mark: no calendar. */}
        {total > 0 && <WorkoutCalendar workouts={state.workouts} today={today} onOpen={open} />}
        {state.workouts.slice(0, shown).map((workout) => (
          <WorkoutCard key={workout.sessionId} workout={workout} />
        ))}
        {total > shown && (
          <div className="workout-older">
            <SecondaryButton text="Show older workouts" onClick={() => setShown((n) => n + WORKOUTS_PER_PAGE)} />
          </div>
        )}
      </div>
    </section>
  )
}

/** One workout: its date, its side and counts, then each exercise with every set of it. */
function WorkoutCard({ workout }: { workout: Workout }) {
  return (
    <Card spacing={4} className="history-session workout">
      {/* Takes the focus when its day is tapped in the calendar; never a tab stop. */}
      <h2 id={headingId(workout)} tabIndex={-1} className="history-date title-medium">
        {formatSessionDate(workout.date)}
      </h2>
      <p className="workout-summary eyebrow">{workoutSummary(workout)}</p>
      {workout.exercises.map((exercise) => (
        <div key={exercise.exerciseId} className="history-group workout-exercise">
          <h3 className="workout-exercise-name title-small">{exercise.name}</h3>
          <PastSets sets={exercise.sets} />
        </div>
      ))}
    </Card>
  )
}
