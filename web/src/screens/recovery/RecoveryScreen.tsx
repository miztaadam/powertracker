import Dexie from 'dexie'
import { useEffect, useRef, useState } from 'react'
import { backupFileName } from '../../backup/backup'
import { UpgradeFailedError } from '../../db/db'
import { readSafetyCopy, safetyCopyDate, snapshotForSafety } from '../../db/safety'
import { todayIso } from '../../domain'
import { makePasskey, readCredentialId, unlock } from '../../platform/passkeyLock'
import { saveJsonFile } from '../../platform/saveFile'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { ReplayIcon, UploadIcon } from '../../ui/components/icons'
import { GateScreen } from '../lock/GateScreen'

export type RecoveryScreenProps = {
  /** What stopped the app. */
  error: unknown
  /** The database to rescue the data from. */
  dbName: string
}

/** What recovery found in the database, read without Grit's schema so that a failed upgrade does not matter. */
type Found = {
  /** The stored safety copy's date for its file name; null when there is no copy. */
  safetyCopy: { date: string } | null
  /** The passkey to verify the owner with, when App lock is on. */
  lockedBy: string | null
}

/** Opens the database as it is stored, whatever its version, and closes it again. Nothing here writes. */
async function withStoredDb<T>(name: string, work: (db: Dexie) => Promise<T>): Promise<T> {
  const db = new Dexie(name)
  try {
    await db.open()
    return await work(db)
  } finally {
    db.close()
  }
}

async function look(db: Dexie): Promise<Found> {
  // Each read starts inside a promise. A database without the table throws as soon as it is asked for it,
  // and thrown while the list is being built that would leave the reads already started rejected with nobody listening.
  const read = <T,>(work: () => PromiseLike<T>): Promise<T> => Promise.resolve().then(work)
  const [copy, appLock, credentialId] = await Promise.all([
    read(() => readSafetyCopy(db)),
    read(() => db.table('settings').get('appLock')),
    read(() => readCredentialId(db)),
  ])
  return { safetyCopy: copy && { date: safetyCopyDate(copy) ?? 'undated' }, lockedBy: appLock?.value === true ? credentialId : null }
}

/**
 * Shown instead of the app when its database cannot be opened or read, a failed schema upgrade
 * above all. It only reads: the stored safety copy, or the data as it is now, can be exported as a
 * file, and nothing is ever deleted or changed here. With App lock on, the phone's owner is
 * verified before anything is handed out: by the passkey, or, if that is gone, by making a new one
 * (which is not stored here, so the lock screen will offer "Can't unlock?" once more).
 */
export function RecoveryScreen({ error, dbName }: RecoveryScreenProps) {
  const upgradeFailed = error instanceof UpgradeFailedError
  // Null while looking, or when the database cannot even be read as stored.
  const [found, setFound] = useState<Found | null>(null)
  const [note, setNote] = useState<string | null>(null)
  const [verified, setVerified] = useState(false)
  const busy = useRef(false)

  useEffect(() => {
    let stale = false
    withStoredDb(dbName, look)
      .then((value) => {
        if (!stale) setFound(value)
      })
      // Nothing readable: the screen offers only to try again.
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [dbName])

  const locked = found?.lockedBy != null && !verified
  const exclusive = (work: () => Promise<void>) => {
    if (busy.current) return
    busy.current = true
    setNote(null)
    work()
      .catch(() => setNote("Couldn't export. Try again."))
      .finally(() => {
        busy.current = false
      })
  }

  /** Runs one export at a time; `pick` chooses the file's name and text. */
  const exportFile = (pick: (db: Dexie) => Promise<{ name: string; json: string } | null>) =>
    exclusive(async () => {
      if (locked) {
        // First thing in the tap, before the database is touched: Safari shows the Face ID sheet only then.
        // 'unsupported' lets the lifter through, as the lock screen does.
        if ((await unlock(found.lockedBy)) === 'cancelled') return setNote('Unlock Grit to export your data.')
        setVerified(true)
      }
      const file = await withStoredDb(dbName, pick)
      if (file == null) return setNote('There is nothing to export.')
      if ((await saveJsonFile(file.name, file.json)) === 'saved') setNote('Saved. Keep this file safe.')
    })

  const exportSafetyCopy = () =>
    exportFile(async (db) => {
      const copy = await readSafetyCopy(db)
      // Named for the day the copy was made: that is how old the data in it is.
      return copy && { name: `grit-safety-copy-${safetyCopyDate(copy) ?? 'undated'}.json`, json: copy.json }
    })
  const exportCurrent = () =>
    exportFile(async (db) => {
      // A backup when the data can be read as one, otherwise every table as it is stored.
      const copy = await snapshotForSafety(db)
      return { name: copy.kind === 'backup' ? backupFileName(todayIso()) : `grit-raw-data-${todayIso()}.json`, json: copy.json }
    })
  const cantUnlock = () =>
    exclusive(async () => {
      const made = await makePasskey()
      if (made.result === 'cancelled') return setNote('Unlock Grit to export your data.')
      setVerified(true)
      setNote('Unlocked. You can export now.')
    })

  // Only claimed when the copy was written for this very failure, and is there.
  const copySaved = upgradeFailed && error.safetyCopySaved && found?.safetyCopy != null
  return (
    <GateScreen
      title={upgradeFailed ? "Grit couldn't update its storage" : "Grit couldn't open your log"}
      body={
        copySaved
          ? 'Your log is unchanged, and Grit saved a safety copy of it before the update. Export a copy to keep it safe, then try again.'
          : upgradeFailed
            ? 'Your log is unchanged. Export a copy to keep it safe, then try again.'
            : 'Something went wrong while reading your log. Nothing has been deleted. Export a copy to keep it safe, then try again. In a private window, open Grit in a normal one instead.'
      }
      note={note}
    >
      <PrimaryButton text="Try again" onClick={() => window.location.reload()} icon={<ReplayIcon />} />
      {found?.safetyCopy != null && <SecondaryButton text="Export safety copy" onClick={exportSafetyCopy} icon={<UploadIcon />} />}
      {found != null && <SecondaryButton text="Export current data" onClick={exportCurrent} icon={<UploadIcon />} />}
      {locked && <QuietButton text="Can't unlock?" onClick={cantUnlock} />}
    </GateScreen>
  )
}
