import type { ReactNode } from 'react'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { useCoach, type CoachActions, type CoachState } from '../../state/useCoach'
import { useWelcomeBack } from '../../state/useWelcomeBack'
import { EmptyState } from '../../ui/components/EmptyState'
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
}

/** The coach's open advice for the side the app is on, most urgent first. Nothing changes until the lifter taps Apply. */
export function CoachScreen({ db, today = todayIso, onOpenPlan }: CoachScreenProps) {
  const coach = useCoach(db, today)
  const welcome = useWelcomeBack(db, today)
  return <CoachContent state={coach} actions={coach} welcome={<WelcomeBackCard welcome={welcome} onOpenPlan={onOpenPlan} />} />
}

export type CoachContentProps = {
  state: CoachState
  actions: CoachActions
  /** The welcome-back card, at the top of the feed. */
  welcome?: ReactNode
}

/** The Coach screen without its data source. */
export function CoachContent({ state, actions, welcome }: CoachContentProps) {
  const count = state.cards.length
  const eyebrow = state.loading ? undefined : count === 0 ? 'Nothing open' : count === 1 ? '1 open suggestion' : `${count} open suggestions`
  return (
    <section className="screen">
      <ScreenHeader title="Coach" eyebrow={eyebrow} label={<ModeLabel />} />
      <div className="screen-body">
        {welcome}
        {!state.loading && count === 0 && (
          <EmptyState icon={<BoltIcon />} title="No advice right now. Keep logging." body="The coach looks at your training each time you finish a session." />
        )}
        {state.cards.map((card) => (
          <CoachAdviceCard key={card.advice.id} card={card} side={state.side} onApply={() => void actions.apply(card.advice.id)} onDismiss={() => void actions.dismiss(card.advice.id)} />
        ))}
      </div>
      {/* The newest confirmation replaces the one on screen. */}
      <Snackbar message={state.message?.text ?? null} showId={state.message?.id} onDismiss={actions.clearMessage} />
    </section>
  )
}
