import { useState } from 'react'
import type { GritDb } from '../../db/db'
import { useBackupReminder } from '../../state/backupReminder'
import { AdviceCard } from '../../ui/components/AdviceCard'
import { WarningAmberIcon } from '../../ui/components/icons'

export type BackupReminderCardProps = {
  db: GritDb
  /** The real clock's date, `YYYY-MM-DD`. */
  today: string
  /** Opens Settings, where the backup is exported. */
  onBackUp: () => void
}

/** A reminder to export a backup when the last one is too long ago. "Not now" hides it until tomorrow. */
export function BackupReminderCard({ db, today, onBackUp }: BackupReminderCardProps) {
  const reminder = useBackupReminder(db, today)
  // Hidden at once, also if remembering the answer fails.
  const [hiddenOn, setHiddenOn] = useState<string | null>(null)
  if (!reminder.due || hiddenOn === today) return null
  const since = reminder.daysSinceBackup != null ? `Your last backup was ${reminder.daysSinceBackup} days ago.` : 'You have not exported a backup yet.'
  return (
    <AdviceCard
      tone="warning"
      icon={<WarningAmberIcon />}
      tag="Backup"
      title="Back up your log"
      body={`${since} Your log lives only on this phone.`}
      primaryAction="Back up"
      onPrimary={onBackUp}
      secondaryAction="Not now"
      onSecondary={() => {
        setHiddenOn(today)
        reminder.snooze().catch(() => {})
      }}
    />
  )
}
