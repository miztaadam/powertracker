import { useState } from 'react'
import type { GritDb } from '../../db/db'
import { useWorkoutHistory, type Workout, type WorkoutHistoryState } from '../../state/useWorkoutHistory'
import { IconButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { EmptyState } from '../../ui/components/EmptyState'
import { formatKg } from '../../ui/components/format'
import { ArrowBackIcon, FitnessCenterIcon } from '../../ui/components/icons'
import { PastSets } from '../../ui/components/PastSets'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { formatSessionDate } from './dates'
import './progress.css'

/** How many workouts are drawn at first, and how many more each "Show older workouts" adds: a long log is never drawn whole. */
export const WORKOUTS_PER_PAGE = 20

export type WorkoutHistoryScreenProps = { db: GritDb; onBack: () => void }

/** Every past workout, newest first, from both sides (1.19): opened from Progress's History button. Read-only. */
export function WorkoutHistoryScreen({ db, onBack }: WorkoutHistoryScreenProps) {
  const state = useWorkoutHistory(db)
  return <WorkoutHistoryContent state={state} onBack={onBack} />
}

const count = (n: number, one: string) => `${n} ${n === 1 ? one : `${one}s`}`

/** "Bodybuilding · 3 exercises · 9 sets", and " · 82.5 kg bodyweight" when one was logged that day. */
export function workoutSummary(workout: Workout): string {
  const parts = [workout.side === 'BODYBUILDING' ? 'Bodybuilding' : 'Powerlifting', count(workout.exercises.length, 'exercise'), count(workout.setCount, 'set')]
  if (workout.bodyweightKg != null) parts.push(`${formatKg(workout.bodyweightKg)} kg bodyweight`)
  return parts.join(' · ')
}

/** The History screen without its data source. */
export function WorkoutHistoryContent({ state, onBack }: { state: WorkoutHistoryState; onBack: () => void }) {
  const [shown, setShown] = useState(WORKOUTS_PER_PAGE)
  const total = state.workouts.length
  return (
    <section className="screen">
      <ScreenHeader title="History" eyebrow={state.loading ? undefined : count(total, 'workout')} navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />} />
      <div className="screen-body">
        {!state.loading && total === 0 && <EmptyState icon={<FitnessCenterIcon />} title="No workouts yet" body="Every workout you log on Today will be listed here, with each exercise and its sets." />}
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
      <h2 className="history-date title-medium">{formatSessionDate(workout.date)}</h2>
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
