import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import type { Lift } from '../../router'
import { useProgress, type ChartRange, type ProgressState } from '../../state/useProgress'
import { EmptyState } from '../../ui/components/EmptyState'
import { InsightsIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { BodyweightSection, RatioSection, RecordsSection, SummaryCard, TrendCard, VolumeSection } from './ProgressSections'
import './progress.css'

export type ProgressScreenProps = {
  db: GritDb
  onOpenLift: (lift: Lift) => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
}

/** Where the lifter stands: total and lift e1RMs, their trend, records, weekly volume, ratios and bodyweight. */
export function ProgressScreen({ db, onOpenLift, today = todayIso }: ProgressScreenProps) {
  const { state, setRange } = useProgress(db, today)
  return <ProgressContent state={state} onRange={setRange} onOpenLift={onOpenLift} />
}

export type ProgressContentProps = { state: ProgressState; onRange: (range: ChartRange) => void; onOpenLift: (lift: Lift) => void }

/** The Progress screen without its data source. */
export function ProgressContent({ state, onRange, onOpenLift }: ProgressContentProps) {
  return (
    <section className="screen">
      <ScreenHeader title="Progress" eyebrow="Estimated 1RM" />
      <div className="screen-body">
        {!state.loading && !state.hasData && (
          <EmptyState
            icon={<InsightsIcon />}
            title="No lifts logged yet"
            body={
              state.showStrictPress
                ? 'Log a squat, bench, deadlift or strict press session on Today and your charts will appear here.'
                : 'Log a squat, bench or deadlift session on Today and your charts will appear here.'
            }
          />
        )}
        {state.hasData && (
          <>
            <SummaryCard state={state} onOpenLift={onOpenLift} />
            <TrendCard state={state} onRange={onRange} />
            {state.prs.length > 0 && <RecordsSection prs={state.prs} />}
            <VolumeSection state={state} />
            {state.ratios.length > 0 && <RatioSection ratios={state.ratios} />}
            {state.bodyweight.length > 0 && <BodyweightSection points={state.bodyweight} />}
          </>
        )}
      </div>
    </section>
  )
}
