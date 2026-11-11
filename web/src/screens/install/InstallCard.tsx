import { liveQuery } from 'dexie'
import { useEffect, useState } from 'react'
import type { GritDb } from '../../db/db'
import { isStandalone } from '../../platform/storage'
import { QuietButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import './install.css'

/** In `meta`: true once the lifter answered the card with "Not now". */
export const INSTALL_DISMISSED_KEY = 'install:dismissed'

/**
 * Shown while Grit runs in a browser tab and not from the Home Screen: how to install it, and why
 * (an installed app's storage is not cleared by the browser). Grit works without installing, so
 * "Not now" puts the card away for good.
 */
export function InstallCard({ db }: { db: GritDb }) {
  const [standalone] = useState(isStandalone)
  // Hidden until the stored answer has been read, so the card does not flash for someone who said no.
  const [dismissed, setDismissed] = useState(true)
  useEffect(() => {
    if (standalone) return
    // A failed read only means no card; the screen's own reads report a broken database.
    const subscription = liveQuery(async () => (await db.meta.get(INSTALL_DISMISSED_KEY))?.value === true).subscribe({ next: setDismissed, error: () => setDismissed(true) })
    return () => subscription.unsubscribe()
  }, [db, standalone])
  if (standalone || dismissed) return null
  return (
    <Card className="install-card">
      <div className="install-head">
        <img className="install-icon" src="/icons/icon-192.png" alt="" width={48} height={48} />
        <h2 className="install-title headline-small">Install Grit</h2>
      </div>
      <p className="install-body body-medium">Add Grit to your Home Screen: tap Share, then Add to Home Screen. Installed, your training log is protected from being cleared.</p>
      <div className="install-actions">
        <QuietButton
          text="Not now"
          onClick={() => {
            // Hidden at once, also if remembering the answer fails.
            setDismissed(true)
            db.meta.put({ key: INSTALL_DISMISSED_KEY, value: true }).catch(() => {})
          }}
        />
      </div>
    </Card>
  )
}
