import type { GritDb } from '../../db/db'
import { useExerciseHistory, type ExerciseHistoryState } from '../../state/useLiftHistory'
import { IconButton } from '../../ui/components/Button'
import { EmptyState } from '../../ui/components/EmptyState'
import { ArrowBackIcon, FitnessCenterIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { InfoButton } from '../library/InfoButton'
import { SessionCard } from './LiftHistoryScreen'
import './progress.css'

export type ExerciseHistoryScreenProps = {
  db: GritDb
  exerciseId: number
  onBack: () => void
}

/** Every session of one exercise, newest first, from both sides: the lift history's layout for any exercise. */
export function ExerciseHistoryScreen({ db, exerciseId, onBack }: ExerciseHistoryScreenProps) {
  const state = useExerciseHistory(db, exerciseId)
  return <ExerciseHistoryContent state={state} onBack={onBack} />
}

export function ExerciseHistoryContent({ state, onBack }: { state: ExerciseHistoryState; onBack: () => void }) {
  const count = state.sessions.length
  const name = state.name ?? 'Exercise'
  return (
    <section className="screen">
      <ScreenHeader
        title={`${name} history`}
        eyebrow={state.loading ? undefined : count === 1 ? '1 session' : `${count} sessions`}
        navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />}
        actions={state.name != null ? <InfoButton name={state.name} /> : undefined}
      />
      <div className="screen-body">
        {!state.loading && count === 0 && <EmptyState icon={<FitnessCenterIcon />} title={`No ${name.toLowerCase()} sessions yet`} body="Every set you log for this exercise will be listed here." />}
        {state.sessions.map((session) => (
          <SessionCard key={session.sessionId} session={session} />
        ))}
      </div>
    </section>
  )
}
