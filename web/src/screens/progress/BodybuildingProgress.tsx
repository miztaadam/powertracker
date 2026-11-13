import { Fragment } from 'react'
import type { BodybuildingProgressState, BodybuildingRow } from '../../state/useProgress'
import { Card } from '../../ui/components/Card'
import { EmptyState } from '../../ui/components/EmptyState'
import { formatKg } from '../../ui/components/format'
import { ChevronRightIcon, InsightsIcon, TrendingFlatIcon, TrendingUpIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import './progress.css'

export type BodybuildingProgressContentProps = {
  state: BodybuildingProgressState
  /**
   * Every row opens that exercise's history, which has its sets from both sides: a squat, bench, deadlift or strict
   * press too, as no lift is special on this side. The powerlifting lift history is never opened from here.
   */
  onOpenExercise: (exerciseId: number) => void
}

/**
 * Progress on the bodybuilding side (spec §7.6): each exercise of the plan with its best set from each of its last
 * four sessions on this side, and whether it is going up. The powerlifting charts, records and total are not shown.
 */
export function BodybuildingProgressContent({ state, onOpenExercise }: BodybuildingProgressContentProps) {
  return (
    <section className="screen">
      <ScreenHeader title="Progress" eyebrow="Best set per session" label={<ModeLabel />} />
      <div className="screen-body">
        {!state.loading && state.rows.length === 0 && (
          <EmptyState icon={<InsightsIcon />} title="No exercises in your plan yet" body="Add exercises on the Plan tab and log them on Today. Each one's best sets will show here." />
        )}
        {state.rows.length > 0 && (
          <Card contentPadding={0} spacing={0}>
            <ul className="bb-rows">
              {state.rows.map((row) => (
                <li key={row.exerciseId}>
                  <TrendRow row={row} onClick={() => onOpenExercise(row.exerciseId)} />
                </li>
              ))}
            </ul>
          </Card>
        )}
      </div>
    </section>
  )
}

/** "26 × 11". */
const bestText = (set: { weightKg: number; reps: number }) => `${formatKg(set.weightKg)} × ${set.reps}`

/** Shown between best sets: "26 × 10 · 26 × 11". */
const SEPARATOR = ' · '

const trendName = (goingUp: boolean | null) => (goingUp === true ? 'Going up' : goingUp === false ? 'Not going up' : null)

/**
 * What a screen reader hears for a row, and what it does: "DB press. 26 × 10, 26 × 11. Going up. Open history"
 * (Android's row says the same, with "Open history" as its click label).
 */
export function trendRowName(row: BodybuildingRow): string {
  const sets = row.bestSets.length === 0 ? 'Not logged yet' : row.bestSets.map(bestText).join(', ')
  const trend = trendName(row.goingUp)
  return [row.name, sets, ...(trend ? [trend] : []), 'Open history'].join('. ')
}

function TrendRow({ row, onClick }: { row: BodybuildingRow; onClick: () => void }) {
  return (
    <button type="button" className="bb-row" onClick={onClick} aria-label={trendRowName(row)}>
      <span className="bb-row-text">
        <span className="bb-row-name title-medium">{row.name}</span>
        {row.bestSets.length === 0 ? (
          <span className="bb-row-sets bb-row-muted body-medium">Not logged yet</span>
        ) : (
          <span className="bb-row-sets body-medium">
            {row.bestSets.map((b, i) => (
              <Fragment key={b.date}>
                {i > 0 && SEPARATOR}
                <span className="bb-row-set">{bestText(b)}</span>
              </Fragment>
            ))}
          </span>
        )}
      </span>
      {row.goingUp === true && (
        <span className="bb-row-trend bb-row-up" role="img" aria-label="Going up">
          <TrendingUpIcon />
        </span>
      )}
      {row.goingUp === false && (
        <span className="bb-row-trend bb-row-level" role="img" aria-label="Not going up">
          <TrendingFlatIcon />
        </span>
      )}
      <ChevronRightIcon className="bb-row-chevron" />
    </button>
  )
}
