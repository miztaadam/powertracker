import type { Workout } from '../../state/useWorkoutHistory'
import { formatKg } from '../../ui/components/format'

const count = (n: number, one: string) => `${n} ${n === 1 ? one : `${one}s`}`

/** "Bodybuilding · 3 exercises · 9 sets", and " · 82.5 kg bodyweight" when one was logged that day. */
export function workoutSummary(workout: Workout): string {
  const parts = [workout.side === 'BODYBUILDING' ? 'Bodybuilding' : 'Powerlifting', count(workout.exercises.length, 'exercise'), count(workout.setCount, 'set')]
  if (workout.bodyweightKg != null) parts.push(`${formatKg(workout.bodyweightKg)} kg bodyweight`)
  return parts.join(' · ')
}
