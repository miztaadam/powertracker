import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { LockOpenIcon } from '../../ui/components/icons'
import { GateScreen } from './GateScreen'

export type LockScreenProps = {
  /** App lock is on but this browser has nothing to unlock with. */
  unavailable?: boolean
  onUnlock: () => void
  /** The passkey is gone or no longer works: have the phone verify its owner by making a new one. */
  onCantUnlock: () => void
  onContinue: () => void
}

/** Why Grit cannot be locked in this browser; also shown where the lock is switched on. */
export const LOCK_UNSUPPORTED = "This browser can't use Face ID, Touch ID or a passcode for Grit"

/** What stands in for the app while it is locked: the Grit icon, one line on why, and the one action. */
export function LockScreen({ unavailable = false, onUnlock, onCantUnlock, onContinue }: LockScreenProps) {
  if (unavailable) {
    return (
      <GateScreen title="App lock isn't available here" body={`${LOCK_UNSUPPORTED}, so your log isn't locked. You can turn App lock off in Plan › Settings.`}>
        <PrimaryButton text="Continue" onClick={onContinue} />
      </GateScreen>
    )
  }
  return (
    <GateScreen title="Grit is locked" body="Use Face ID, Touch ID or your passcode to open your training log.">
      <PrimaryButton text="Unlock" onClick={onUnlock} icon={<LockOpenIcon />} />
      <QuietButton text="Can't unlock?" onClick={onCantUnlock} />
    </GateScreen>
  )
}

/** Laid over the open app while it is off screen with App lock on, so the app switcher's picture of it shows no log. */
export function LockCover() {
  return (
    <div className="gate-cover" aria-hidden="true">
      <img className="gate-icon" src="/icons/icon-192.png" alt="" width={96} height={96} />
    </div>
  )
}
