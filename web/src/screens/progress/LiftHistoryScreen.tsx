import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import type { Lift } from '../../router'
import { useLiftHistory, type HistorySession, type LiftHistoryState } from '../../state/useLiftHistory'
import { toHalfKg } from '../../state/useProgress'
import { IconButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { EmptyState } from '../../ui/components/EmptyState'
import { formatKg } from '../../ui/components/format'
import { ArrowBackIcon, FitnessCenterIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { SetRow } from '../../ui/components/SetRow'
import { StatTile } from '../../ui/components/StatTile'
import { InfoButton } from '../library/InfoButton'
import { formatDate, formatSessionDate } from './dates'
import { LIFT_LABEL } from './lifts'
import './progress.css'

export type LiftHistoryScreenProps = {
  db: GritDb
  lift: Lift
  onBack: () => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/** Every session of one lift, newest first. */
export function LiftHistoryScreen({ db, lift, onBack, today = todayIso }: LiftHistoryScreenProps) {
  const state = useLiftHistory(db, lift, today)
  return <LiftHistoryContent lift={lift} state={state} onBack={onBack} />
}

export type LiftHistoryContentProps = { lift: Lift; state: LiftHistoryState; onBack: () => void }

/** The lift history screen without its data source. */
export function LiftHistoryContent({ lift, state, onBack }: LiftHistoryContentProps) {
  const count = state.sessions.length
  const name = LIFT_LABEL[lift]
  return (
    <section className="screen">
      <ScreenHeader
        title={`${name} history`}
        eyebrow={state.loading ? undefined : count === 1 ? '1 session' : `${count} sessions`}
        navigation={<IconButton label="Back" icon={<ArrowBackIcon />} onClick={onBack} />}
        actions={<InfoButton name={name} />}
      />
      <div className="screen-body">
        {!state.loading && count === 0 && (
          <EmptyState icon={<FitnessCenterIcon />} title={`No ${name.toLowerCase()} sessions yet`} body="Every set you log for this lift and its variations will be listed here." />
        )}
        {(state.currentE1rmKg != null || state.best != null) && (
          <div className="progress-tiles">
            {state.currentE1rmKg != null && <StatTile label="Current e1RM" value={formatKg(toHalfKg(state.currentE1rmKg))} unit="kg" note="Best of the last 4 weeks" />}
            {state.best != null && <StatTile label="Best e1RM" value={formatKg(toHalfKg(state.best.e1rmKg))} unit="kg" note={formatDate(state.best.date)} noteTone="success" />}
          </div>
        )}
        {state.sessions.map((session) => (
          <SessionCard key={session.sessionId} session={session} />
        ))}
      </div>
    </section>
  )
}

function SessionCard({ session }: { session: HistorySession }) {
  return (
    <Card spacing={4} className="history-session">
      <h2 className="history-date title-medium">{formatSessionDate(session.date)}</h2>
      {session.groups.map((group) => (
        <div key={group.exerciseName} className="history-group">
          {group.isVariation && <p className="history-variation title-small">{group.exerciseName}</p>}
          {group.sets.map((row) => (
            <SetRow
              key={row.index}
              index={row.index}
              weightKg={row.set.weightKg}
              reps={row.set.reps}
              rpe={row.set.rpe ?? null}
              state={row.set.failed ? 'failed' : 'done'}
              isTopSet={row.set.isTopSet}
              isPr={row.isPr}
            />
          ))}
        </div>
      ))}
    </Card>
  )
}
