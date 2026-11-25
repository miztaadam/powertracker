import { SecondaryButton } from '../../ui/components/Button'
import './progress.css'

/**
 * The History button at the top right of Progress (1.19), the same on both sides and there whether or not this side
 * has anything logged: the list it opens is every workout, from both sides. Nothing when there is nowhere to open it
 * (the guide's pictures of Progress).
 */
export function HistoryButton({ onOpen }: { onOpen?: () => void }) {
  return onOpen ? <SecondaryButton className="progress-history" text="History" onClick={onOpen} compact /> : null
}
