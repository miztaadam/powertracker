import { useRef, useState, type ChangeEvent } from 'react'
import { backupFileName, decodeBackup, encodeBackup, exportBackup, markBackedUp, plansLostBy, restoreBackup } from '../../backup/backup'
import type { BackupFile } from '../../backup/format'
import { readSafetyCopy, writeSafetyCopy, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
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
  /** The day the safety copy was made, `YYYY-MM-DD`; null when unknown. */
  safetyCopyDate?: string | null
  today: () => string
  now: () => number
  /** One line for the snackbar. */
  onMessage: (text: string) => void
}

const TOO_NEW = 'This backup is from a newer version of Grit.'
const NOT_A_BACKUP = "This file isn't a valid Grit backup."

/** What replaces the data once confirmed, and how the question describes it. */
type PendingRestore = {
  file: BackupFile
  what: string
  /** The file is the safety copy itself, which then stays as it is. */
  fromSafetyCopy?: boolean
  /** The copy of the current data could not be saved: this restore cannot be undone. */
  noSafetyCopy?: boolean
  /** The sides with a plan now and none in the file (1.14.1): the question warns of that instead. */
  plansLost?: TrainingMode[]
}

const sideName = (mode: TrainingMode) => (mode === 'BODYBUILDING' ? 'bodybuilding' : 'powerlifting')

/** "This backup has no powerlifting plan", or "no powerlifting or bodybuilding plan" when both sides lose theirs. */
export const noPlanTitle = (sides: readonly TrainingMode[]) => `This backup has no ${sides.map(sideName).join(' or ')} plan`

/** What restoring a backup without the side's plan does; with both sides, "your current powerlifting and bodybuilding plans". */
export const noPlanBody = (sides: readonly TrainingMode[]) =>
  `Restoring it removes your current ${sides.map(sideName).join(' and ')} ${sides.length > 1 ? 'plans' : 'plan'}. Your other data is replaced by the backup's too.`

export const RESTORE_ANYWAY = 'Restore anyway'

const mediumDate = (date: Date) => new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)

/** " on 11 Nov 2026" for the safety copy's day, or nothing when it is not known. */
function onDay(isoDate: string | null | undefined): string {
  const date = isoDate != null ? new Date(`${isoDate}T00:00:00`) : null
  return date != null && !Number.isNaN(date.getTime()) ? ` on ${mediumDate(date)}` : ''
}

function lastBackupText(lastBackupAt: string | null): string {
  const date = lastBackupAt != null ? new Date(lastBackupAt) : null
  return date != null && !Number.isNaN(date.getTime()) ? mediumDate(date) : 'never'
}

/**
 * Export, restore, and how safe the data is right now. A restore replaces everything, so it always
 * asks first; it runs in one transaction, so a failed restore leaves the data as it was. Before it
 * replaces anything, the data as it is goes into the safety copy, so a wrong file can be undone with
 * "Restore safety copy"; if that copy cannot be saved, the question is asked again and says so.
 * Restoring the safety copy itself leaves the copy untouched: it may be the only place the good
 * data is, and must survive a restore that fails.
 */
export function BackupCard({ db, lastBackupAt, storageProtected, safetyCopy, safetyCopyDate: copyDate, today, now, onMessage }: BackupCardProps) {
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

  /**
   * Checks a backup's text; a valid one waits for the lifter's confirmation. When it would remove a side's plan (that
   * side has items now and the file has none), the question says so first (1.14.1).
   */
  const offer = async (text: string | null, what: string, fromSafetyCopy = false) => {
    const decoded = text != null ? decodeBackup(text) : null
    if (!decoded?.ok) {
      onMessage(decoded?.kind === 'TOO_NEW' ? TOO_NEW : NOT_A_BACKUP)
      return
    }
    // Unreadable plan: no warning, and the question as before.
    const plansLost = await plansLostBy(db, decoded.file).catch(() => [])
    setPending({ file: decoded.file, what, fromSafetyCopy, plansLost })
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
        await offer(copy?.kind === 'backup' ? copy.json : null, `the safety copy saved${onDay(copy ? safetyCopyDate(copy) : null)}`, true)
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

  const restore = (confirmed: PendingRestore) =>
    exclusive(async () => {
      if (!confirmed.noSafetyCopy && !confirmed.fromSafetyCopy) {
        try {
          await writeSafetyCopy(db)
        } catch {
          // The restore is still theirs to make, knowing it cannot be undone.
          setPending({ ...confirmed, noSafetyCopy: true })
          return
        }
      }
      try {
        await restoreBackup(db, confirmed.file)
        onMessage('Backup restored.')
      } catch {
        onMessage("Couldn't restore the backup.")
      }
    })

  // The plan warning takes the question's place; asked again without a safety copy, the question is as before.
  const plansLost = pending != null && !pending.noSafetyCopy ? (pending.plansLost ?? []) : []

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
            Grit saved a safety copy of your data{onDay(copyDate)}, before its last update or restore.{safetyCopy === 'raw' && ' It can be exported, but not restored here.'}
          </p>
          {safetyCopy === 'backup' ? (
            <SecondaryButton text="Restore safety copy" onClick={offerSafetyCopy} icon={<ReplayIcon />} />
          ) : (
            <SecondaryButton text="Export safety copy" onClick={exportSafetyCopy} icon={<UploadIcon />} />
          )}
        </>
      )}

      {pending && plansLost.length > 0 && (
        <Dialog
          open
          title={noPlanTitle(plansLost)}
          onDismiss={() => setPending(null)}
          actions={
            <>
              <QuietButton text="Cancel" onClick={() => setPending(null)} />
              <PrimaryButton
                text={RESTORE_ANYWAY}
                onClick={() => {
                  setPending(null)
                  restore(pending)
                }}
                compact
              />
            </>
          }
        >
          {noPlanBody(plansLost)}
        </Dialog>
      )}
      {pending && plansLost.length === 0 && (
        <Dialog
          open
          title="Replace all data?"
          onDismiss={() => setPending(null)}
          actions={
            <>
              <QuietButton text="Cancel" onClick={() => setPending(null)} />
              <PrimaryButton
                text={pending.noSafetyCopy ? 'Replace anyway' : 'Replace'}
                onClick={() => {
                  setPending(null)
                  restore(pending)
                }}
                compact
              />
            </>
          }
        >
          Restoring replaces everything in Grit with {pending.what}.
          {pending.noSafetyCopy && (
            <p className="gr-dialog-note" role="status">
              Grit couldn't save a safety copy of your current data first, so this can't be undone. Export a backup before you go on, if you can.
            </p>
          )}
        </Dialog>
      )}
    </Card>
  )
}
