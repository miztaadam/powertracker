import type { ReactNode } from 'react'
import type { TrainingMode } from '../../db/schema'
import type { Lift } from '../../router'
import type { CardDetail, CoachCard, NewExerciseUi, Prescription, SetAdditionUi } from '../../state/useCoach'
import type { MissPoint } from '../../state/useToday'
import { grit } from '../../domain'
import { dayName } from '../../state/weekdayNames'
import { AdviceCard } from '../../ui/components/AdviceCard'
import { SecondaryButton } from '../../ui/components/Button'
import { formatKg, formatRpe } from '../../ui/components/format'
import { BalanceIcon, BatteryAlertIcon, FitnessCenterIcon, ReplayIcon, SkipNextIcon, TrendingFlatIcon } from '../../ui/components/icons'
import { formatDayMonth } from '../progress/dates'
import { Eyebrow, NumberDisplay } from '../../ui/components/NumberDisplay'
import { RatioBar } from '../../ui/components/RatioBar'
import { Sparkline } from '../../ui/components/Sparkline'
import { StatTile } from '../../ui/components/StatTile'
import type { Tone } from '../../ui/components/tone'
import { InfoButton } from '../library/InfoButton'
import { LIFT_COLOR, LIFT_LABEL } from '../progress/lifts'

/** Where each lift starts, as the coach names it on a card. */
const FLOOR_LABEL: Record<Lift, string> = { SQUAT: 'Out of the hole', BENCH: 'Off the chest', DEADLIFT: 'Off the floor', STRICT_PRESS: 'Off the shoulders' }

/** A stall's trend is scaled to at least 5% of the lift, so it reads as the flat line it is. */
const STALL_TREND_SPAN = 0.05

const missLabel = (miss: MissPoint, lift: Lift) => (miss === 'LOCKOUT' ? 'Lockout' : miss === 'MID' ? 'Mid-range' : FLOOR_LABEL[lift])

/** An estimate is not worth more than half a kilo of precision. */
const toHalfKg = (kg: number) => Math.round(kg * 2) / 2

const dose = (p: Prescription) => (p.repsLow === p.repsHigh ? `${p.sets} × ${p.repsHigh}` : `${p.sets} × ${p.repsLow}–${p.repsHigh}`)

type Look = { tone: Tone; icon: ReactNode; kind: string; title: string; action: string }

function lookFor(type: string, lift: Lift | null, detail: CardDetail): Look {
  const name = lift ? LIFT_LABEL[lift] : null
  switch (type) {
    case 'STALL':
      return { tone: 'warning', icon: <TrendingFlatIcon />, kind: 'Stall', title: `${name ?? 'Progress'} has stalled`, action: 'Add to plan' }
    case 'DELOAD':
      return { tone: 'danger', icon: <BatteryAlertIcon />, kind: 'Deload', title: 'Take a deload week', action: 'Apply deload' }
    case 'RE_ENTRY':
      return { tone: 'info', icon: <ReplayIcon />, kind: 'Re-entry', title: 'Welcome back', action: 'Ease back in' }
    default: {
      const ratio = detail.kind === 'weakPoint' ? detail.ratio : null
      const other = ratio ? (ratio.numerator === lift ? ratio.denominator : ratio.numerator) : null
      const title = `${name ?? 'A lift'} is lagging` + (other ? ` your ${LIFT_LABEL[other].toLowerCase()}` : '')
      return { tone: 'info', icon: <BalanceIcon />, kind: 'Weak point', title, action: `Prioritise ${name?.toLowerCase() ?? 'it'}` }
    }
  }
}

function PrescriptionList({ items }: { items: Prescription[] }) {
  if (items.length === 0) return null
  return (
    <ul className="coach-list">
      {items.map((item) => (
        <li key={item.exerciseName} className="coach-list-row">
          <span className="coach-list-name title-small">{item.exerciseName}</span>
          <span className="coach-dose headline-small">{dose(item)}</span>
          <InfoButton name={item.exerciseName} />
        </li>
      ))}
    </ul>
  )
}

function Detail({ lift, detail, side }: { lift: Lift | null; detail: CardDetail; side: TrainingMode }) {
  switch (detail.kind) {
    case 'stall':
      return (
        <>
          {detail.e1rmKg != null && (
            <div className="coach-trend">
              <NumberDisplay value={formatKg(toHalfKg(detail.e1rmKg))} unit="kg" label={lift ? `${LIFT_LABEL[lift]} e1RM` : 'e1RM'} size="medium" />
              {detail.trend.length >= 2 && (
                <Sparkline className="coach-spark" values={detail.trend} color={lift ? LIFT_COLOR[lift] : 'var(--text-muted)'} minSpan={detail.e1rmKg * STALL_TREND_SPAN} />
              )}
            </div>
          )}
          <PrescriptionList items={detail.prescriptions} />
        </>
      )
    case 'deload':
      return (
        <div className="coach-row">
          <StatTile label="Volume" value={String(detail.volumePct)} unit="%" note="of your usual sets" />
          <StatTile label="Load" value={String(detail.loadPct)} unit="%" note={side === 'BODYBUILDING' ? 'of your last weights' : 'of your last top sets'} />
        </div>
      )
    case 'reEntry':
      return <StatTile label="Load" value={String(detail.loadPct)} unit="%" note={side === 'BODYBUILDING' ? 'of your last weights, for 7 days' : 'of your last top sets, for 7 days'} />
    case 'weakPoint': {
      const { ratio, missPoint, muscles, additions } = detail
      return (
        <>
          {ratio && <RatioBar label={`${LIFT_LABEL[ratio.numerator]} ÷ ${LIFT_LABEL[ratio.denominator]}`} valuePct={ratio.valuePct} lowPct={ratio.lowPct} highPct={ratio.highPct} />}
          {missPoint && lift && (
            <StatTile label="Sticking point" value={missLabel(missPoint, lift)} note={muscles.length > 0 ? `Works ${muscles.map((m) => m.toLowerCase()).join(', ')}` : undefined} />
          )}
          <div className="coach-additions">
            {additions.length === 0 ? (
              <p className="coach-muted body-small">The work for this is already in your plan.</p>
            ) : (
              <>
                <Eyebrow text="Adds to your plan" />
                <PrescriptionList items={additions} />
              </>
            )}
          </div>
        </>
      )
    }
    case 'moreWork':
    case 'swap':
    case 'skip':
      return null
  }
}

export type CoachAdviceCardProps = {
  card: CoachCard
  /** Apply; a swap passes the alternative tapped. */
  onApply: (choice?: string) => void
  onDismiss: () => void
  /** The side the app is on. On the bodybuilding side a deload or easing back in speaks of exercises and weights, not lifts and top sets. */
  side?: TrainingMode
}

/** What a card's tag names after its kind: the lift, else everything it covers. */
function scopeOf(advice: CoachCard['advice'], side: TrainingMode): string {
  if (advice.lift) return LIFT_LABEL[advice.lift]
  return (advice.type === 'DELOAD' || advice.type === 'RE_ENTRY') && side === 'BODYBUILDING' ? 'All exercises' : 'All lifts'
}

/** A body part's name as a title starts it: "Side delts". */
const groupName = (group: string) => grit.muscleGroupLabel(group)

/** "One more set of Incline DB press on Monday (3 → 4)". */
export const setAdditionLine = (a: SetAdditionUi) => `One more set of ${a.name} on ${dayName(a.weekday)} (${a.fromSets} → ${a.toSets})`

/** "Add Pec deck on Thursday, 3 × 8–12 @ 8". */
export const newExerciseLine = (n: NewExerciseUi) => `Add ${n.name} on ${dayName(n.weekday)}, ${n.sets} × ${n.repsLow}–${n.repsHigh} @ ${formatRpe(n.rpe)}`

/**
 * A lagging body part (stage 3): what the coach would add, one line each, then the body part's weekly sets before and
 * after. Nothing changes until "Add to plan".
 */
function MoreWorkCard({ detail, onApply, onDismiss }: { detail: Extract<CardDetail, { kind: 'moreWork' }>; onApply: () => void; onDismiss: () => void }) {
  const lines = detail.newExercise ? [newExerciseLine(detail.newExercise)] : detail.addSets.map(setAdditionLine)
  return (
    <AdviceCard tone="info" icon={<FitnessCenterIcon />} tag="More work" title={`${groupName(detail.group)} is lagging`} primaryAction="Add to plan" onPrimary={onApply} secondaryAction="Dismiss" onSecondary={onDismiss}>
      <ul className="coach-list">
        {lines.map((line) => (
          <li key={line} className="coach-list-row">
            <span className="coach-list-name body-medium">{line}</span>
          </li>
        ))}
      </ul>
      <p className="coach-muted body-medium">{`${groupName(detail.group)}: ${detail.weeklyBefore} → ${detail.weeklyAfter} sets a week`}</p>
    </AdviceCard>
  )
}

/** An exercise that has not moved for three weeks or more (stage 3): one button per alternative, then Dismiss. */
function SwapCard({ detail, onSwap, onDismiss }: { detail: Extract<CardDetail, { kind: 'swap' }>; onSwap: (choice: string) => void; onDismiss: () => void }) {
  const weeks = `${detail.weeks} ${detail.weeks === 1 ? 'week' : 'weeks'}`
  return (
    <AdviceCard
      tone="warning"
      icon={<TrendingFlatIcon />}
      tag="Stuck"
      title={`${detail.name} hasn't moved in ${weeks}`}
      body={`Same weight and no more reps since ${formatDayMonth(detail.since)}. Another exercise for ${groupName(detail.group).toLowerCase()} can get it moving.`}
      secondaryAction="Dismiss"
      onSecondary={onDismiss}
    >
      <div className="coach-swaps">
        {detail.alternatives.map((name) => (
          <SecondaryButton key={name} text={`Swap for ${name}`} onClick={() => onSwap(name)} compact />
        ))}
      </div>
    </AdviceCard>
  )
}

/** The skip card's title (1.13): "Bench skipped". The coach's reason says why and what follows. */
export const skipTitle = (name: string) => `${name} skipped`

/**
 * A planned exercise skipped on Today (1.13), in the stuck exercise's look: one button per alternative, best first, then
 * Dismiss. With no alternatives (a main lift's pain card, or nothing left to swap to) it is Dismiss only.
 */
function SkipCard({ detail, reason, onSwap, onDismiss }: { detail: Extract<CardDetail, { kind: 'skip' }>; reason: string; onSwap: (choice: string) => void; onDismiss: () => void }) {
  return (
    <AdviceCard tone="warning" icon={<SkipNextIcon />} tag="Skipped" title={skipTitle(detail.name)} body={reason} secondaryAction="Dismiss" onSecondary={onDismiss}>
      {detail.alternatives.length > 0 && (
        <div className="coach-swaps">
          {detail.alternatives.map((name) => (
            <SecondaryButton key={name} text={`Swap for ${name}`} onClick={() => onSwap(name)} compact />
          ))}
        </div>
      )}
    </AdviceCard>
  )
}

/** One card of the Coach feed: what kind of advice, what to do, the coach's reason, the figures behind it, Apply and Dismiss. */
export function CoachAdviceCard({ card, onApply, onDismiss, side = 'POWERLIFTING' }: CoachAdviceCardProps) {
  const { advice, detail } = card
  if (detail.kind === 'moreWork') return <MoreWorkCard detail={detail} onApply={() => onApply()} onDismiss={onDismiss} />
  if (detail.kind === 'swap') return <SwapCard detail={detail} onSwap={(choice) => onApply(choice)} onDismiss={onDismiss} />
  if (detail.kind === 'skip') return <SkipCard detail={detail} reason={advice.reason} onSwap={(choice) => onApply(choice)} onDismiss={onDismiss} />
  const look = lookFor(advice.type, advice.lift, detail)
  return (
    <AdviceCard
      tone={look.tone}
      icon={look.icon}
      tag={`${look.kind} · ${scopeOf(advice, side)}`}
      title={look.title}
      body={advice.reason}
      primaryAction={look.action}
      onPrimary={onApply}
      secondaryAction="Dismiss"
      onSecondary={onDismiss}
    >
      <Detail lift={advice.lift} detail={detail} side={side} />
    </AdviceCard>
  )
}
