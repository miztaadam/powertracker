import { useRef, useState } from 'react'
import { exportBackupFile } from '../../backup/exportFile'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { PrimaryButton, SecondaryButton } from '../../ui/components/Button'
import { UploadIcon } from '../../ui/components/icons'
import { GateScreen } from '../lock/GateScreen'

export type PrivateModeScreenProps = {
  db: GritDb
  /** The lifter chose to go on in the private window. */
  onContinue: () => void
}

/**
 * Stands before the app in a private window, where the browser throws the log away with the tab.
 * Whatever is in there now can be exported; going on is the lifter's call, because no browser says
 * for certain that a window is private.
 */
export function PrivateModeScreen({ db, onContinue }: PrivateModeScreenProps) {
  const [note, setNote] = useState<string | null>(null)
  const busy = useRef(false)
  const exportNow = () => {
    if (busy.current) return
    busy.current = true
    exportBackupFile(db, todayIso(), Date.now())
      .then((result) => setNote(result === 'saved' ? 'Backup saved.' : null))
      .catch(() => setNote("Couldn't save the backup."))
      .finally(() => {
        busy.current = false
      })
  }
  return (
    <GateScreen
      title="Private browsing won't keep your log"
      body="In a private window, everything you log is deleted when the tab closes. Open Grit in a normal Safari tab, or add it to your Home Screen."
      note={note}
    >
      <PrimaryButton text="Export backup" onClick={exportNow} icon={<UploadIcon />} />
      <SecondaryButton text="Continue anyway" onClick={onContinue} />
    </GateScreen>
  )
}
