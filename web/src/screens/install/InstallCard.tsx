import { liveQuery } from 'dexie'
import { useEffect, useRef, useState } from 'react'
import { exportBackupFile } from '../../backup/exportFile'
import type { GritDb } from '../../db/db'
import { todayIso } from '../../domain'
import { isAndroid } from '../../platform/device'
import { isStandalone } from '../../platform/storage'
import { QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { UploadIcon } from '../../ui/components/icons'
import './install.css'

/** How to install: Safari's Share sheet on iPhone (and as the default), the browser's menu on Android. */
export const INSTALL_STEPS_SAFARI = 'In Safari, tap Share, then Add to Home Screen, and open Grit from the new icon.'
export const INSTALL_STEPS_ANDROID = "Open your browser's menu (⋮), then tap Install app or Add to Home screen."

/** In `meta`: true once the lifter answered the card with "Not now". */
export const INSTALL_DISMISSED_KEY = 'install:dismissed'

type Shown = { dismissed: boolean; hasLog: boolean }

/** Whether there is anything here that an installed copy would start without: a session, a plan or a baseline. */
async function read(db: GritDb): Promise<Shown> {
  const [answer, sessions, planItems, baselines] = await Promise.all([db.meta.get(INSTALL_DISMISSED_KEY), db.sessions.count(), db.planItems.count(), db.baselines.count()])
  return { dismissed: answer?.value === true, hasLog: sessions + planItems + baselines > 0 }
}

/**
 * Shown while Grit runs in a browser tab and not from the Home Screen: how to install it, and why
 * (an installed app's storage is not cleared by the browser). On iPhone the installed app has its
 * own storage, apart from Safari's, so when there is already a log here the card also says to export
 * it first and restore it in the installed app. On Android, Chrome's installed app shares this
 * browser's storage and opens with the log, so the card only suggests keeping a backup now and then.
 * Either way, with a log here it offers the export.
 * Grit works without installing, so "Not now" puts the card away for good.
 */
export function InstallCard({ db }: { db: GritDb }) {
  const [standalone] = useState(isStandalone)
  const [android] = useState(isAndroid)
  const steps = android ? INSTALL_STEPS_ANDROID : INSTALL_STEPS_SAFARI
  // Hidden until the stored answer has been read, so the card does not flash for someone who said no.
  const [shown, setShown] = useState<Shown>({ dismissed: true, hasLog: false })
  const [note, setNote] = useState<string | null>(null)
  // "Not now" in this page, its write settled (saved or not).
  const [answered, setAnswered] = useState(false)
  const exporting = useRef(false)
  useEffect(() => {
    if (standalone) return
    // A failed read only means no card; the screen's own reads report a broken database.
    const subscription = liveQuery(() => read(db)).subscribe({ next: setShown, error: () => setShown({ dismissed: true, hasLog: false }) })
    return () => subscription.unsubscribe()
  }, [db, standalone])
  if (standalone || shown.dismissed || answered) return null
  const exportNow = () => {
    if (exporting.current) return
    exporting.current = true
    exportBackupFile(db, todayIso(), Date.now())
      .then((result) => setNote(result === 'saved' ? 'Backup saved.' : null))
      .catch(() => setNote("Couldn't save the backup."))
      .finally(() => {
        exporting.current = false
      })
  }
  return (
    <Card className="install-card">
      <div className="install-head">
        <img className="install-icon" src="/icons/icon-192.png" alt="" width={48} height={48} />
        <h2 className="install-title headline-small">Install Grit</h2>
      </div>
      <p className="install-body body-medium">{`${steps} Installed, your training log is protected from being cleared.`}</p>
      {shown.hasLog && (
        <p className="install-body body-medium">
          {android
            ? 'Your log stays in this browser, so the installed app opens with it. Export a backup now and then to keep a copy.'
            : "The installed app keeps its own log, apart from Safari's, and starts empty. You have already logged here: tap Export backup before you add Grit to the Home Screen, then restore that file in the installed app (Plan, Settings, Restore backup)."}
        </p>
      )}
      {note != null && (
        <p className="install-body body-medium" role="status">
          {note}
        </p>
      )}
      <div className="install-actions">
        <QuietButton
          text="Not now"
          onClick={() => {
            // Hidden once remembering the answer has settled, so a hidden card is a saved answer; hidden also if it
            // fails, and kept hidden while this page is open (the stored answer's reads do not bring it back).
            const hide = () => setAnswered(true)
            db.meta.put({ key: INSTALL_DISMISSED_KEY, value: true }).then(hide, hide)
          }}
        />
        {shown.hasLog && <SecondaryButton text="Export backup" onClick={exportNow} icon={<UploadIcon />} compact />}
      </div>
    </Card>
  )
}
