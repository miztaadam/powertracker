import { liveQuery } from 'dexie'
import { useCallback, useEffect, useState } from 'react'
import { getSettings, type GritDb } from '../db/db'
import { storageProtected } from '../platform/storage'
import { daysBetween } from './isoDate'

/** Days without an export before Grit reminds; sooner when the browser may clear its storage. */
export const REMIND_AFTER_DAYS = 14
export const REMIND_AFTER_DAYS_UNPROTECTED = 7

/** In `meta`: the day the lifter last answered the reminder with "Not now". */
export const REMINDER_SNOOZED_KEY = 'backupReminder:snoozedOn'

export type BackupReminderInput = {
  /** The last export, as an ISO timestamp; null when there has been none. */
  lastBackupAt: string | null
  /** The earliest logged session (`YYYY-MM-DD`); null when nothing is logged. */
  firstSessionDate: string | null
  storageProtected: boolean
  /** The real clock's date, `YYYY-MM-DD`. */
  today: string
}

export type BackupReminder = { due: boolean; daysSinceBackup: number | null }

/** The device's calendar date of a timestamp; null when it cannot be read. */
function localDate(timestamp: string): string | null {
  const d = new Date(timestamp)
  if (Number.isNaN(d.getTime())) return null
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * Whether it is time to remind about a backup. The days count from the last export; with no export
 * yet, from the first logged session (an empty log has nothing to lose).
 */
export function backupReminder({ lastBackupAt, firstSessionDate, storageProtected, today }: BackupReminderInput): BackupReminder {
  const limit = storageProtected ? REMIND_AFTER_DAYS : REMIND_AFTER_DAYS_UNPROTECTED
  const backedUpOn = lastBackupAt != null ? localDate(lastBackupAt) : null
  if (backedUpOn != null) {
    const days = daysBetween(backedUpOn, today)
    return { due: days >= limit, daysSinceBackup: days }
  }
  return { due: firstSessionDate != null && daysBetween(firstSessionDate, today) >= limit, daysSinceBackup: null }
}

type Stored = { lastBackupAt: string | null; firstSessionDate: string | null; snoozedOn: unknown }

async function readStored(db: GritDb): Promise<Stored> {
  const [settings, first, snoozed] = await Promise.all([getSettings(db), db.sessions.orderBy('date').first(), db.meta.get(REMINDER_SNOOZED_KEY)])
  return { lastBackupAt: settings.lastBackupAt, firstSessionDate: first?.date ?? null, snoozedOn: snoozed?.value ?? null }
}

export type BackupReminderState = BackupReminder & {
  /** Hides the reminder for the rest of today. */
  snooze: () => Promise<void>
}

/** The backup reminder for the Today screen. Not due until everything it depends on has been read. */
export function useBackupReminder(db: GritDb, today: string): BackupReminderState {
  const [stored, setStored] = useState<Stored | null>(null)
  const [isProtected, setProtected] = useState<boolean | null>(null)
  useEffect(() => {
    // A failed read only means no reminder; the screen's own reads report a broken database.
    const subscription = liveQuery(() => readStored(db)).subscribe({ next: setStored, error: () => setStored(null) })
    return () => subscription.unsubscribe()
  }, [db])
  useEffect(() => {
    let stale = false
    storageProtected()
      .then((value) => {
        if (!stale) setProtected(value)
      })
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [])
  const snooze = useCallback(async () => {
    await db.meta.put({ key: REMINDER_SNOOZED_KEY, value: today })
  }, [db, today])

  if (stored == null || isProtected == null || stored.snoozedOn === today) return { due: false, daysSinceBackup: null, snooze }
  return { ...backupReminder({ ...stored, storageProtected: isProtected, today }), snooze }
}
