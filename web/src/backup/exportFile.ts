import type Dexie from 'dexie'
import { saveJsonFile, type SaveResult } from '../platform/saveFile'
import { backupFileName, encodeBackup, exportBackup, markBackedUp } from './backup'

/**
 * Exports everything as a backup file, for the places that offer it when storage is in trouble.
 * `date` is the device's local date, for the file name. Recording the backup date is a write, which
 * may be the very thing that fails: the file is what matters.
 */
export async function exportBackupFile(db: Dexie, date: string, nowMs: number): Promise<SaveResult> {
  const result = await saveJsonFile(backupFileName(date), encodeBackup(await exportBackup(db, nowMs)))
  if (result === 'saved') await markBackedUp(db, nowMs).catch(() => {})
  return result
}
