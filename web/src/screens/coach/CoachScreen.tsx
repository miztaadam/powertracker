import type { ReactNode } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { useCoach, type CoachActions, type CoachState } from '../../state/useCoach'
import { useWelcomeBack } from '../../state/useWelcomeBack'
import { EmptyState } from '../../ui/components/EmptyState'
import { PrimaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { BoltIcon } from '../../ui/components/icons'
import { ScreenHeader } from '../../ui/components/ScreenHeader'
import { ModeLabel } from '../../ui/components/ModeLabel'
import { Snackbar } from '../../ui/components/Snackbar'
import { CoachAdviceCard } from './CoachAdviceCard'
import { WelcomeBackCard } from './WelcomeBackCard'
import './coach.css'

export type CoachScreenProps = {
  db: GritDb
  /** The real clock, as `YYYY-MM-DD`. Default is the device's local date. */
  today?: () => string
  /** Opens Plan, after "My goals have changed" on the welcome-back card. */
  onOpenPlan?: () => void
  /** Opens the plan builder, after "My goals have changed" on the bodybuilding side's welcome-back card. */
  onOpenBuilder?: () => void
  /** Opens the check-in, from its card on the bodybuilding side. */
  onOpenCheckIn?: () => void
}

export const CHECK_IN_TITLE = 'Time for your check-in'
export const CHECK_IN_LINE = 'Four weeks on this plan. Mark the body parts that stand out; everything else stays on track.'

/**
 * A check-in is due (bodybuilding side, stage 3). It has no dismiss: a check-in with nothing marked is a valid answer
 * and takes two taps.
 */
export function CheckInDueCard({ onCheckIn }: { onCheckIn: () => void }) {
  return (
    <Card className="checkin-due">
      <section aria-label="Check-in">
        <h2 className="headline-small">{CHECK_IN_TITLE}</h2>
        <p className="body-medium">{CHECK_IN_LINE}</p>
      </section>
      <PrimaryButton text="Check in" onClick={onCheckIn} />
    </Card>
  )
}

/** The coach's open advice for the side the app is on, most urgent first. Nothing changes until the lifter taps Apply. */
export function CoachScreen({ db, today = todayIso, onOpenPlan, onOpenBuilder, onOpenCheckIn = () => {} }: CoachScreenProps) {
  const coach = useCoach(db, today)
  const welcome = useWelcomeBack(db, today)
  return (
    <CoachContent
      state={coach}
      actions={coach}
      welcome={<WelcomeBackCard welcome={welcome} onOpenPlan={onOpenPlan} onOpenBuilder={onOpenBuilder} />}
      checkIn={coach.checkInDue ? <CheckInDueCard onCheckIn={onOpenCheckIn} /> : undefined}
    />
  )
}

export type CoachContentProps = {
  state: CoachState
  actions: CoachActions
  /** The welcome-back card, at the top of the feed. */
  welcome?: ReactNode
  /** The check-in card when one is due, under the welcome-back card and above the advice. */
  checkIn?: ReactNode
}

/** The Coach screen without its data source. */
export function CoachContent({ state, actions, welcome, checkIn }: CoachContentProps) {
  const count = state.cards.length
  const eyebrow = state.loading ? undefined : count === 0 ? 'Nothing open' : count === 1 ? '1 open suggestion' : `${count} open suggestions`
  return (
    <section className="screen">
      <ScreenHeader title="Coach" eyebrow={eyebrow} label={<ModeLabel />} />
      <div className="screen-body">
        {welcome}
        {checkIn}
        {!state.loading && count === 0 && checkIn == null && (
          <EmptyState icon={<BoltIcon />} title="No advice right now. Keep logging." body="The coach looks at your training each time you finish a session." />
        )}
        {state.cards.map((card) => (
          <CoachAdviceCard key={card.advice.id} card={card} side={state.side} onApply={(choice) => void actions.apply(card.advice.id, choice)} onDismiss={() => void actions.dismiss(card.advice.id)} />
        ))}
      </div>
      {/* The newest confirmation replaces the one on screen. */}
      <Snackbar message={state.message?.text ?? null} showId={state.message?.id} onDismiss={actions.clearMessage} />
    </section>
  )
}
