import { useRef, useState, type ChangeEvent } from 'react'
import { backupFileName, decodeBackup, encodeBackup, exportBackup, markBackedUp, restoreBackup } from '../../backup/backup'
import type { BackupFile } from '../../backup/format'
import { readSafetyCopy, type GritDb } from '../../db/db'
import { safetyCopyDate } from '../../db/safety'
import { saveJsonFile } from '../../platform/saveFile'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { Dialog } from '../../ui/components/Dialog'
import { DownloadIcon, ReplayIcon, UploadIcon } from '../../ui/components/icons'
import './settings.css'

export type BackupCardProps = {
  db: GritDb
  /** The last export as an ISO timestamp, or null. */
  lastBackupAt: string | null
  /** null while the browser has not answered. */
  storageProtected: boolean | null
  safetyCopy: 'backup' | 'raw' | null
  today: () => string
  now: () => number
  /** One line for the snackbar. */
  onMessage: (text: string) => void
}

const TOO_NEW = 'This backup is from a newer version of Grit.'
const NOT_A_BACKUP = "This file isn't a valid Grit backup."

/** What replaces the data once confirmed, and how the question describes it. */
type PendingRestore = { file: BackupFile; what: string }

function lastBackupText(lastBackupAt: string | null): string {
  const date = lastBackupAt != null ? new Date(lastBackupAt) : null
  return date != null && !Number.isNaN(date.getTime()) ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date) : 'never'
}

/**
 * Export, restore, and how safe the data is right now. A restore replaces everything, so it always
 * asks first; it runs in one transaction, so a failed restore leaves the data as it was.
 */
export function BackupCard({ db, lastBackupAt, storageProtected, safetyCopy, today, now, onMessage }: BackupCardProps) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [pending, setPending] = useState<PendingRestore | null>(null)
  // One export or restore at a time: a second tap while one runs does nothing.
  const busy = useRef(false)
  const exclusive = (work: () => Promise<void>) => {
    if (busy.current) return
    busy.current = true
    void work().finally(() => {
      busy.current = false
    })
  }

  const exportNow = () =>
    exclusive(async () => {
      try {
        const nowMs = now()
        const result = await saveJsonFile(backupFileName(today()), encodeBackup(await exportBackup(db, nowMs)))
        if (result === 'cancelled') return
        await markBackedUp(db, nowMs)
        onMessage('Backup saved.')
      } catch {
        onMessage("Couldn't save the backup.")
      }
    })

  /** Checks a backup's text; a valid one waits for the lifter's confirmation. */
  const offer = (text: string | null, what: string) => {
    const decoded = text != null ? decodeBackup(text) : null
    if (decoded?.ok) setPending({ file: decoded.file, what })
    else onMessage(decoded?.kind === 'TOO_NEW' ? TOO_NEW : NOT_A_BACKUP)
  }

  const onFileChosen = (e: ChangeEvent<HTMLInputElement>) => {
    const input = e.target
    const file = input.files?.[0]
    if (!file) return
    file
      .text()
      .catch(() => null)
      .then((text) => offer(text, 'the contents of this backup'))
      .catch(() => onMessage(NOT_A_BACKUP))
      // Cleared so that choosing the same file again is noticed.
      .finally(() => {
        input.value = ''
      })
  }

  const offerSafetyCopy = () =>
    exclusive(async () => {
      try {
        const copy = await readSafetyCopy(db)
        offer(copy?.kind === 'backup' ? copy.json : null, 'the safety copy saved before the last update')
      } catch {
        onMessage(NOT_A_BACKUP)
      }
    })

  const exportSafetyCopy = () =>
    exclusive(async () => {
      try {
        const copy = await readSafetyCopy(db)
        if (copy == null) return
        // Not recorded as a backup: the copy is older than the data it sits beside.
        if ((await saveJsonFile(`grit-safety-copy-${safetyCopyDate(copy) ?? 'undated'}.json`, copy.json)) === 'saved') onMessage('Safety copy saved.')
      } catch {
        onMessage("Couldn't save the safety copy.")
      }
    })

  const restore = (file: BackupFile) =>
    exclusive(async () => {
      try {
        await restoreBackup(db, file)
        onMessage('Backup restored.')
      } catch {
        onMessage("Couldn't restore the backup.")
      }
    })

  return (
    <Card>
      <p className="settings-text body-medium">Your log lives only on this phone. Export a file now and then so you can restore it.</p>
      <SecondaryButton text="Export backup" onClick={exportNow} icon={<UploadIcon />} />
      <SecondaryButton text="Restore backup" onClick={() => fileInput.current?.click()} icon={<DownloadIcon />} />
      <input ref={fileInput} className="sr-only" type="file" accept="application/json" aria-label="Backup file" aria-hidden="true" tabIndex={-1} onChange={onFileChosen} />
      <div className="settings-status body-medium">
        {storageProtected != null && <p>Storage: {storageProtected ? 'protected' : 'not protected'}</p>}
        <p>Last backup: {lastBackupText(lastBackupAt)}</p>
      </div>
      {safetyCopy != null && (
        <>
          <p className="settings-text body-medium">
            {safetyCopy === 'backup' ? 'Grit saved a copy of your data before its last update.' : 'Grit saved a copy of your data before its last update. It can be exported, but not restored here.'}
          </p>
          {safetyCopy === 'backup' ? (
            <SecondaryButton text="Restore safety copy" onClick={offerSafetyCopy} icon={<ReplayIcon />} />
          ) : (
            <SecondaryButton text="Export safety copy" onClick={exportSafetyCopy} icon={<UploadIcon />} />
          )}
        </>
      )}

      {pending && (
        <Dialog
          open
          title="Replace all data?"
          onDismiss={() => setPending(null)}
          actions={
            <>
              <QuietButton text="Cancel" onClick={() => setPending(null)} />
              <PrimaryButton
                text="Replace"
                onClick={() => {
                  setPending(null)
                  restore(pending.file)
                }}
                compact
              />
            </>
          }
        >
          Restoring replaces everything in Grit with {pending.what}.
        </Dialog>
      )}
    </Card>
  )
}
