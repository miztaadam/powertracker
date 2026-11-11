import { describe, expect, it } from 'vitest'
import { addDays } from './isoDate'
import { backupReminder } from './backupReminder'

const TODAY = '2026-11-11'
/** Noon local time `days` before TODAY, as `lastBackupAt` stores it. */
const backedUp = (days: number) => new Date(`${addDays(TODAY, -days)}T12:00:00`).toISOString()

describe('backupReminder', () => {
  it('backupReminder_after14Days_or7WhenUnprotected', () => {
    const due = (days: number, storageProtected: boolean) => backupReminder({ lastBackupAt: backedUp(days), firstSessionDate: '2026-01-05', storageProtected, today: TODAY }).due
    expect(due(13, true)).toBe(false)
    expect(due(14, true)).toBe(true)
    expect(due(6, false)).toBe(false)
    expect(due(7, false)).toBe(true)
    expect(due(0, false)).toBe(false)
  })

  it('neverBackedUp_countsFromTheFirstSession', () => {
    const due = (firstSessionDate: string | null, storageProtected: boolean) => backupReminder({ lastBackupAt: null, firstSessionDate, storageProtected, today: TODAY })
    // Nothing logged: nothing to lose yet.
    expect(due(null, false)).toEqual({ due: false, daysSinceBackup: null })
    expect(due(addDays(TODAY, -6), false).due).toBe(false)
    expect(due(addDays(TODAY, -7), false)).toEqual({ due: true, daysSinceBackup: null })
    expect(due(addDays(TODAY, -13), true).due).toBe(false)
    expect(due(addDays(TODAY, -14), true).due).toBe(true)
  })

  it('saysHowLongAgoTheLastBackupWas', () => {
    expect(backupReminder({ lastBackupAt: backedUp(20), firstSessionDate: null, storageProtected: true, today: TODAY })).toEqual({ due: true, daysSinceBackup: 20 })
  })

  it('unreadableDate_countsAsNeverBackedUp', () => {
    expect(backupReminder({ lastBackupAt: 'yesterday-ish', firstSessionDate: '2026-10-01', storageProtected: true, today: TODAY })).toEqual({ due: true, daysSinceBackup: null })
  })
})
