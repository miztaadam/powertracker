import { useRef, useState } from 'react'
import { updateSettings, type GritDb } from '../../db/db'
import { createLock } from '../../platform/passkeyLock'
import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'
import { LOCK_UNSUPPORTED } from './LockScreen'

export type LockSetupDialogProps = {
  db: GritDb
  /** Whether this browser can make a passkey, when already known. */
  supported: boolean | null
  /** Closed without a choice: the question comes back the next time Grit opens. */
  onDismiss: () => void
}

/**
 * App lock is on but this phone has no passkey for it, as after restoring a backup from another
 * phone. Grit is open meanwhile; the lifter either sets the lock up here or turns it off.
 */
export function LockSetupDialog({ db, supported, onDismiss }: LockSetupDialogProps) {
  const [note, setNote] = useState<string | null>(null)
  const busy = useRef(false)
  const exclusive = (work: () => Promise<void>) => {
    if (busy.current) return
    busy.current = true
    work()
      .catch(() => setNote("Couldn't save — try again."))
      .finally(() => {
        busy.current = false
      })
  }
  // Either choice ends the state this dialog is shown for, which closes it.
  const setUp = () =>
    exclusive(async () => {
      // First thing in the tap: Safari shows the Face ID sheet only then.
      const result = await createLock(db, supported)
      setNote(result === 'unsupported' ? `${LOCK_UNSUPPORTED}. Turn App lock off to stop being asked.` : null)
    })
  const turnOff = () => exclusive(async () => void (await updateSettings(db, (s) => ({ ...s, appLock: false }))))
  return (
    <Dialog
      open
      title="Set up App lock again?"
      onDismiss={onDismiss}
      actions={
        <>
          <QuietButton text="Turn off" onClick={turnOff} />
          <PrimaryButton text="Set up" onClick={setUp} compact />
        </>
      }
    >
      App lock is on, but it isn't set up on this phone, so Grit isn't locked. Set it up again, or turn it off.
      {note != null && (
        <p className="gr-dialog-note" role="status">
          {note}
        </p>
      )}
    </Dialog>
  )
}
