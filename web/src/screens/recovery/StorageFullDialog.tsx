import { useEffect, useRef, useState } from 'react'
import { exportBackupFile } from '../../backup/exportFile'
import { onQuotaExceeded, type GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { PrimaryButton, QuietButton } from '../../ui/components/Button'
import { Dialog } from '../../ui/components/Dialog'

/**
 * Appears when any write fails because the phone's storage is full, whichever screen was saving.
 * The failed write changed nothing; exporting only reads, so it still works on a full phone.
 */
export function StorageFullDialog({ db }: { db: GritDb }) {
  const [open, setOpen] = useState(false)
  const [note, setNote] = useState<string | null>(null)
  const busy = useRef(false)
  useEffect(
    () =>
      onQuotaExceeded(() => {
        setNote(null)
        setOpen(true)
      }),
    [],
  )
  if (!open) return null
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
    <Dialog
      open
      title="Storage is full"
      onDismiss={() => setOpen(false)}
      actions={
        <>
          <QuietButton text="Close" onClick={() => setOpen(false)} />
          <PrimaryButton text="Export backup" onClick={exportNow} compact />
        </>
      }
    >
      Grit couldn't save that, because this phone is out of space. Nothing was changed. Free up some space, and export a backup to keep your log safe.
      {note != null && (
        <p className="gr-dialog-note" role="status">
          {note}
        </p>
      )}
    </Dialog>
  )
}
