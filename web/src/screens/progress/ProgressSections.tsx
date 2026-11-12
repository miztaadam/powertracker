import type { ReactNode } from 'react'
import type { Lift } from '../../router'
import { addDays } from '../../state/isoDate'
import type { RatioUi } from '../../state/useCoach'
import { CHART_RANGES, toHalfKg, type ChartRange, type PrUi, type ProgressState } from '../../state/useProgress'
import { BarStrip } from '../../ui/components/BarStrip'
import { Card } from '../../ui/components/Card'
import { cx, formatKg, formatRpe } from '../../ui/components/format'
import { EmojiEventsIcon, TrendingDownIcon, TrendingFlatIcon, TrendingUpIcon } from '../../ui/components/icons'
import { LineChart } from '../../ui/components/LineChart'
import { Eyebrow, NumberDisplay } from '../../ui/components/NumberDisplay'
import { Pill } from '../../ui/components/Pill'
import { RatioBar } from '../../ui/components/RatioBar'
import { SectionHeader } from '../../ui/components/ScreenHeader'
import { SegmentedChoice } from '../../ui/components/SegmentedChoice'
import { Sparkline } from '../../ui/components/Sparkline'
import { StatTile } from '../../ui/components/StatTile'
import type { Tone } from '../../ui/components/tone'
import { axisLabels } from './axisLabels'
import { formatDate, formatDayMonth } from './dates'
import { LIFT_COLOR, LIFT_LABEL } from './lifts'

const RANGE_LABEL: Record<ChartRange, string> = { '8w': '8 wk', '6m': '6 mo', '1y': '1 yr', all: 'All' }

/** "+5 kg", "−2.5 kg" or "No change", with the tone and icon that go with it. */
function change(kg: number): { text: string; tone: Tone; icon: ReactNode } {
  if (kg > 0) return { text: `+${formatKg(kg)} kg`, tone: 'success', icon: <TrendingUpIcon /> }
  if (kg < 0) return { text: `−${formatKg(-kg)} kg`, tone: 'warning', icon: <TrendingDownIcon /> }
  return { text: 'No change', tone: 'neutral', icon: <TrendingFlatIcon /> }
}

const TILES_IN_ONE_ROW = 3

/** The total as the hero figure with its trend, then each lift as a tile that opens its history. */
export function SummaryCard({ state, onOpenLift }: { state: ProgressState; onOpenLift: (lift: Lift) => void }) {
  const totalChange = state.totalChangeKg == null ? null : change(state.totalChangeKg)
  // The total always says what it adds up.
  const totalLabel = state.showStrictPress ? 'Total (S+B+D+P)' : 'Total (S+B+D)'
  return (
    <Card spacing={16}>
      {state.totalKg != null ? (
        <div className="progress-total">
          <div className="progress-total-figure">
            <NumberDisplay value={formatKg(state.totalKg)} unit="kg" label={totalLabel} size="hero" />
            {totalChange && <Pill text={`${totalChange.text} in 4 weeks`} tone={totalChange.tone} icon={totalChange.icon} />}
          </div>
          {state.totalTrend.length >= 2 && <Sparkline values={state.totalTrend.map((p) => p.kg)} color="var(--accent-text)" className="progress-total-spark" />}
        </div>
      ) : (
        <div>
          <Eyebrow text={totalLabel} />
          <p className="progress-note body-medium">{state.showStrictPress ? 'Log all four lifts, strict press included, to see your total.' : 'Log all three lifts to see your total.'}</p>
        </div>
      )}
      <div className="progress-lifts">
        {/* Four tiles do not fit side by side on a phone: with the fourth lift they go two to a row. */}
        <div className={cx('progress-tiles', state.lifts.length > TILES_IN_ONE_ROW && 'progress-tiles-pairs')}>
          {state.lifts.map((summary) => {
            const moved = summary.changeKg == null ? null : change(summary.changeKg)
            const name = LIFT_LABEL[summary.lift]
            return (
              <StatTile
                key={summary.lift}
                label={name}
                value={summary.e1rmKg == null ? '–' : formatKg(summary.e1rmKg)}
                unit={summary.e1rmKg == null ? undefined : 'kg'}
                note={moved?.text}
                noteTone={moved?.tone}
                onClick={() => onOpenLift(summary.lift)}
                onClickLabel={`Open ${name.toLowerCase()} history`}
              />
            )
          })}
        </div>
        <p className="progress-caption body-small">Change over the last 4 weeks. Tap a lift for its history.</p>
      </div>
    </Card>
  )
}

/** The shown lifts on one axis over the chosen range. */
export function TrendCard({ state, onRange }: { state: ProgressState; onRange: (range: ChartRange) => void }) {
  const series = state.lifts.flatMap(({ lift }) => {
    const values = state.e1rm[lift] ?? []
    return values.some((v) => v != null) ? [{ name: LIFT_LABEL[lift], color: LIFT_COLOR[lift], values }] : []
  })
  return (
    <Card>
      <SegmentedChoice
        label="Chart range"
        options={CHART_RANGES.map((r) => RANGE_LABEL[r])}
        selectedIndex={CHART_RANGES.indexOf(state.range)}
        onSelect={(i) => onRange(CHART_RANGES[i] ?? state.range)}
      />
      <Eyebrow text="Weekly best e1RM" />
      {series.length > 0 ? (
        <LineChart series={series} xLabels={axisLabels(state.weeks)} />
      ) : (
        // The range selector stays, so a longer range is one tap away.
        <p className="progress-note body-medium">No sessions in this range. Try a longer one.</p>
      )}
    </Card>
  )
}

/** Each lift's standing record: the set that made it and the e1RM it stands for. */
export function RecordsSection({ prs }: { prs: PrUi[] }) {
  const thisYear = prs.reduce((latest, p) => (p.pr.date > latest ? p.pr.date : latest), '').slice(0, 4)
  return (
    <section>
      <SectionHeader title="Records" />
      <Card contentPadding={0} spacing={0}>
        <ul className="progress-records">
          {prs.map(({ lift, pr }) => {
            const date = pr.date.startsWith(thisYear) ? formatDayMonth(pr.date) : formatDate(pr.date)
            const set = `${formatKg(pr.weightKg)} × ${pr.reps}${pr.rpe == null ? '' : ` @ ${formatRpe(pr.rpe)}`}`
            const e1rm = formatKg(toHalfKg(pr.e1rmKg))
            return (
              <li key={lift} className="progress-record">
                <span className="sr-only">{`${LIFT_LABEL[lift]} record: estimated 1RM ${e1rm} kg, from ${set} on ${date}`}</span>
                <span className="progress-record-visual" aria-hidden="true">
                  <span className="progress-record-icon">
                    <EmojiEventsIcon size={20} />
                  </span>
                  <span className="progress-record-text">
                    <span className="title-medium">{LIFT_LABEL[lift]}</span>
                    <span className="progress-caption body-small">{`${date} · ${set}`}</span>
                  </span>
                  <NumberDisplay value={e1rm} unit="kg" size="medium" />
                </span>
              </li>
            )
          })}
        </ul>
      </Card>
    </section>
  )
}

/** "12,450": tonnage has no use for decimals. */
const formatVolume = (kg: number) => Math.round(kg).toLocaleString('en-US')

/** One strip of eight weekly bars per lift, each on its own scale, with this week's figure beside it. */
export function VolumeSection({ state }: { state: ProgressState }) {
  return (
    // "Strict press · this week" needs a wider label column than the other three lifts' names.
    <section className={state.showStrictPress ? 'progress-volume-wide' : undefined}>
      <SectionHeader title="Weekly volume" />
      <Card spacing={16}>
        {state.volume.map(({ lift, weeks }) => {
          const thisWeek = formatVolume(weeks[weeks.length - 1] ?? 0)
          return (
            <div key={lift}>
              <span className="sr-only">{`${LIFT_LABEL[lift]} volume this week: ${thisWeek} kg`}</span>
              <div className="progress-volume" aria-hidden="true">
                <div className="progress-volume-label">
                  <span className="progress-volume-name body-small">
                    <span className="progress-dot" style={{ background: LIFT_COLOR[lift] }} />
                    {`${LIFT_LABEL[lift]} · this week`}
                  </span>
                  <NumberDisplay value={thisWeek} unit="kg" size="medium" />
                </div>
                <BarStrip values={weeks} color={LIFT_COLOR[lift]} className="progress-volume-bars" />
              </div>
            </div>
          )
        })}
        <div className="progress-volume-foot">
          <div className="progress-volume progress-caption body-small">
            <span className="progress-volume-label" />
            <span className="progress-volume-span">
              <span>{state.volumeFrom == null ? '' : `Week of ${formatDayMonth(state.volumeFrom)}`}</span>
              <span>This week</span>
            </span>
          </div>
          <p className="progress-caption body-small">Weight × reps of completed sets, last 8 weeks.</p>
        </div>
      </Card>
    </section>
  )
}

export function RatioSection({ ratios }: { ratios: RatioUi[] }) {
  return (
    <section>
      <SectionHeader title="Lift ratios" />
      <Card spacing={16}>
        {ratios.map((r) => (
          <RatioBar key={r.numerator} label={`${LIFT_LABEL[r.numerator]} ÷ ${LIFT_LABEL[r.denominator]}`} valuePct={r.valuePct} lowPct={r.lowPct} highPct={r.highPct} />
        ))}
      </Card>
    </section>
  )
}

/** Bodyweight over time. The points sit at their dates, so an uneven gap between them reads as one. */
export function BodyweightSection({ points }: { points: { date: string; kg: number }[] }) {
  const byDate = new Map(points.map((p) => [p.date, p.kg]))
  const first = points[0]!.date
  const last = points[points.length - 1]!.date
  const days: string[] = []
  for (let day = first; day <= last; day = addDays(day, 1)) days.push(day)
  return (
    <section>
      <SectionHeader title="Bodyweight" />
      <Card>
        <LineChart series={[{ name: 'Latest', color: 'var(--text-secondary)', values: days.map((d) => byDate.get(d) ?? null) }]} xLabels={axisLabels(days)} />
      </Card>
    </section>
  )
}
