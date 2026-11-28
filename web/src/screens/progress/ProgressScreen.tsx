import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import type { Lift } from '../../router'
import { useProgress, type ChartRange, type ProgressState } from '../../state/useProgress'
import { EmptyState } from '../../ui/components/EmptyState'
import { InsightsIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { BodybuildingProgressContent, SideBalanceSection } from './BodybuildingProgress'
import { HistoryButton } from './HistoryButton'
import { BodyweightSection, RatioSection, RecordsSection, SummaryCard, TrendCard, VolumeSection } from './ProgressSections'
import './progress.css'

export type ProgressScreenProps = {
  db: GritDb
  onOpenLift: (lift: Lift) => void
  /** Opens one exercise's history, from any row on the bodybuilding side (main lifts too). */
  onOpenExercise?: (exerciseId: number) => void
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
  /** Opens a check-in, from the bodybuilding side's Check in button. */
  onCheckIn?: () => void
  /** Opens every past workout (1.19), from the History button at the top right on both sides. */
  onOpenHistory?: () => void
}

/** Where the lifter stands: total and lift e1RMs, their trend, records, weekly volume, ratios and bodyweight. */
export function ProgressScreen({ db, onOpenLift, onOpenExercise = () => {}, today = todayIso, onCheckIn, onOpenHistory }: ProgressScreenProps) {
  const { state, setRange, side, bodybuilding } = useProgress(db, today)
  if (side === 'BODYBUILDING') return <BodybuildingProgressContent state={bodybuilding} onOpenExercise={onOpenExercise} onCheckIn={onCheckIn} onOpenHistory={onOpenHistory} />
  return <ProgressContent state={state} onRange={setRange} onOpenLift={onOpenLift} onOpenHistory={onOpenHistory} />
}

export type ProgressContentProps = { state: ProgressState; onRange: (range: ChartRange) => void; onOpenLift: (lift: Lift) => void; onOpenHistory?: () => void }

/** The Progress screen without its data source. */
export function ProgressContent({ state, onRange, onOpenLift, onOpenHistory }: ProgressContentProps) {
  return (
    <section className="screen">
      <ScreenHeader className="progress-header" title="Progress" eyebrow="Estimated 1RM" label={<ModeLabel />} actions={<HistoryButton onOpen={onOpenHistory} />} />
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
            {state.sideBalances && state.sideBalances.length > 0 && <SideBalanceSection rows={state.sideBalances} />}
            {/* Keyed on the range, as the trend line is: a new range draws the line in again. */}
            {state.bodyweight.length > 0 && <BodyweightSection key={state.range} points={state.bodyweight} />}
          </>
        )}
      </div>
    </section>
  )
}
