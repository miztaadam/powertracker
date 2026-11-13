import type { WelcomeBack } from '../../state/useWelcomeBack'
import { PrimaryButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import './coach.css'

export const WELCOME_BACK_TITLE = 'Welcome back to the better side.'

export type WelcomeBackCardProps = {
  welcome: WelcomeBack
  /** Opens the side's Plan for editing, after "My goals have changed". */
  onOpenPlan?: () => void
}

/**
 * Back on a side after more than a month on the other (spec §7.7): resume the plan as it was left, or change it.
 * Shown on Today and at the top of Coach; nothing changes until a button is tapped.
 */
export function WelcomeBackCard({ welcome, onOpenPlan }: WelcomeBackCardProps) {
  if (!welcome.show) return null
  return (
    <Card className="welcome-back">
      <section aria-label="Welcome back">
        <h2 className="headline-small">{WELCOME_BACK_TITLE}</h2>
        <p className="welcome-back-body body-medium">Have your goals changed, or do you want to resume the same plan?</p>
      </section>
      <div className="welcome-back-actions">
        <PrimaryButton text="Resume my plan" onClick={() => void welcome.resume().catch(() => {})} />
        <SecondaryButton
          text="My goals have changed"
          onClick={() => {
            void welcome.goalsChanged().catch(() => {})
            onOpenPlan?.()
          }}
        />
      </div>
    </Card>
  )
}
