import { Fragment } from 'react'
import { grit } from '../../domain'
import type { BodybuildingProgressState, BodybuildingRow, BodyPartRow, Rating } from '../../state/useProgress'
import { SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { Pill } from '../../ui/components/Pill'
import type { Tone } from '../../ui/components/tone'
import { EmptyState } from '../../ui/components/EmptyState'
import { formatKg } from '../../ui/components/format'
import { ChevronRightIcon, InsightsIcon, TrendingFlatIcon, TrendingUpIcon } from '../../ui/components/icons'
import { ScreenHeader, SectionHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { HistoryButton } from './HistoryButton'
import './progress.css'

export type BodybuildingProgressContentProps = {
  state: BodybuildingProgressState
  /**
   * Every row opens that exercise's history, which has its sets from both sides: a squat, bench, deadlift or strict
   * press too, as no lift is special on this side. The powerlifting lift history is never opened from here.
   */
  onOpenExercise: (exerciseId: number) => void
  /** Opens a check-in, any time (stage 3). */
  onCheckIn?: () => void
  /** Opens every past workout (1.19): the History button at the top right, as on the powerlifting side. */
  onOpenHistory?: () => void
}

export const FIRST_CHECK_IN_LINE = 'Your first check-in is four weeks after your first bodybuilding session. You can check in any time.'

/**
 * Progress on the bodybuilding side (spec §7.6): each exercise of the plan with its best set from each of its last
 * four sessions on this side, and whether it is going up. The powerlifting charts, records and total are not shown.
 */
export function BodybuildingProgressContent({ state, onOpenExercise, onCheckIn = () => {}, onOpenHistory }: BodybuildingProgressContentProps) {
  return (
    <section className="screen">
      <ScreenHeader className="progress-header" title="Progress" eyebrow="Best set per session" label={<ModeLabel />} actions={<HistoryButton onOpen={onOpenHistory} />} />
      <div className="screen-body">
        {/* An empty plan has nothing to check in on (coordinator's ruling, as on Android). */}
        {!state.loading && state.rows.length > 0 && (
          <div className="bb-check-in">
            <SecondaryButton text="Check in" onClick={onCheckIn} />
            {!state.checkedIn && <p className="bb-check-in-line body-medium">{FIRST_CHECK_IN_LINE}</p>}
          </div>
        )}
        {state.bodyParts.length > 0 && (
          <section aria-label="Body parts" className="bb-section">
            <SectionHeader title="Body parts" />
            <Card contentPadding={0} spacing={0}>
              <ul className="bb-rows">
                {state.bodyParts.map((row) => (
                  <BodyPart key={row.group} row={row} />
                ))}
              </ul>
            </Card>
          </section>
        )}
        {state.sideBalances && state.sideBalances.length > 0 && <SideBalanceSection rows={state.sideBalances} />}
        {!state.loading && state.rows.length === 0 && (
          <EmptyState icon={<InsightsIcon />} title="No exercises in your plan yet" body="Add exercises on the Plan tab and log them on Today. Each one's best sets will show here." />
        )}
        {state.rows.length > 0 && (
          <section aria-label="Exercises" className="bb-section">
          {state.bodyParts.length > 0 && <SectionHeader title="Exercises" />}
          <Card contentPadding={0} spacing={0}>
            <ul className="bb-rows">
              {state.rows.map((row) => (
                <li key={row.exerciseId}>
                  <TrendRow row={row} onClick={() => onOpenExercise(row.exerciseId)} />
                </li>
              ))}
            </ul>
          </Card>
          </section>
        )}
      </div>
    </section>
  )
}

const RATING: Record<Rating, { text: string; tone: Tone }> = {
  LAGGING: { text: 'Lagging', tone: 'warning' },
  GROWING_WELL: { text: 'Growing well', tone: 'success' },
  ON_TRACK: { text: 'On track', tone: 'neutral' },
}

/** "11 of 14 planned sets in the last 7 days"; for a body part the plan does not work, "3 sets in the last 7 days". */
export function setsLine(row: Pick<BodyPartRow, 'setsDone' | 'setsPlanned'>): string {
  if (row.setsPlanned > 0) return `${row.setsDone} of ${row.setsPlanned} planned sets in the last 7 days`
  // None planned: only what was done (coordinator's ruling, as on Android).
  return `${row.setsDone} ${row.setsDone === 1 ? 'set' : 'sets'} in the last 7 days`
}

/** "2 of 3 exercises going up"; null when none of its exercises has enough sessions to tell. */
export function goingUpLine(row: Pick<BodyPartRow, 'exercisesUp' | 'exercisesTracked'>): string | null {
  if (row.exercisesTracked === 0) return null
  return `${row.exercisesUp} of ${row.exercisesTracked} ${row.exercisesTracked === 1 ? 'exercise' : 'exercises'} going up`
}

/** Visual intensity level for the weekly muscle-group volume heat map bar. */
function volumeHeat(done: number, planned: number): { pct: number; level: 'low' | 'mid' | 'hit' | 'high' } {
  const target = planned > 0 ? planned : 12
  const ratio = target > 0 ? done / target : 0
  const pct = Math.min(100, Math.round(ratio * 100))
  if (done === 0 || ratio < 0.45) return { pct, level: 'low' }
  if (ratio < 0.9) return { pct, level: 'mid' }
  if (ratio <= 1.25) return { pct, level: 'hit' }
  return { pct: 100, level: 'high' }
}

/** Left vs. Right balance card shown on Progress once any single-sided sets have been logged. */
export function SideBalanceSection({ rows }: { rows: readonly import('../../state/useProgress').SideBalanceUi[] }) {
  return (
    <section aria-label="Side balance" className="bb-section">
      <SectionHeader title="Side balance (L / R)" />
      <Card contentPadding={0} spacing={0}>
        <ul className="bb-rows">
          {rows.map((row) => (
            <li key={row.exerciseId} className="bb-part">
              <div className="bb-part-head">
                <h3 className="bb-part-name title-medium">{row.name}</h3>
                {row.weakerSide == null ? (
                  <Pill text="Balanced" tone="success" />
                ) : (
                  <Pill text={`${row.weakerSide === 'LEFT' ? 'Left' : 'Right'} -${Math.abs(row.avgRepGap)}`} tone="warning" />
                )}
              </div>
              <p className="bb-part-line body-medium">{row.summary}</p>
            </li>
          ))}
        </ul>
      </Card>
    </section>
  )
}

/**
 * One body part: its name and its rating at the latest check-in, the sets done against those planned, a weekly volume
 * heat map bar, how many of its exercises are going up, and its earlier ratings.
 */
function BodyPart({ row }: { row: BodyPartRow }) {
  const latest = row.ratings.at(-1)
  const earlier = row.ratings.slice(0, -1)
  const goingUp = goingUpLine(row)
  const heat = volumeHeat(row.setsDone, row.setsPlanned)
  return (
    <li className="bb-part">
      <div className="bb-part-head">
        <h3 className="bb-part-name title-medium">{grit.muscleGroupLabel(row.group)}</h3>
        {latest && <Pill text={RATING[latest.rating].text} tone={RATING[latest.rating].tone} />}
      </div>
      <div className="bb-heat-track" aria-hidden="true">
        <div className={`bb-heat-fill bb-heat-${heat.level}`} style={{ width: `${heat.pct}%` }} />
      </div>
      <p className="bb-part-line body-medium">{setsLine(row)}</p>
      {goingUp != null && <p className="bb-part-line body-medium">{goingUp}</p>}
      {earlier.length > 0 && <p className="bb-part-earlier body-small">{`Earlier: ${earlier.map((r) => RATING[r.rating].text).join(' · ')}`}</p>}
    </li>
  )
}

type BestSet = BodybuildingRow['bestSets'][number]

/** "26 × 11"; "plate 9 × 10" on a plate-counted machine (1.17). */
const bestText = (set: BestSet) => `${set.unit === 'PLATES' ? grit.plateText(set.weightKg) : formatKg(set.weightKg)} × ${set.reps}`

/** As a screen reader says it: "26 × 11", "plate 7 and a half × 10". */
const bestSpoken = (set: BestSet) => `${set.unit === 'PLATES' ? grit.plateSpoken(set.weightKg) : formatKg(set.weightKg)} × ${set.reps}`

/** Shown between best sets: "26 × 10 · 26 × 11". */
const SEPARATOR = ' · '

const trendName = (goingUp: boolean | null) => (goingUp === true ? 'Going up' : goingUp === false ? 'Not going up' : null)

/**
 * What a screen reader hears for a row, and what it does: "DB press. 26 × 10, 26 × 11. Going up. Open history"
 * (Android's row says the same, with "Open history" as its click label).
 */
export function trendRowName(row: BodybuildingRow): string {
  const sets = row.bestSets.length === 0 ? 'Not logged yet' : row.bestSets.map(bestSpoken).join(', ')
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
