import { useEffect, useRef, useState } from 'react'
import { updateApp, type UpdateResult } from '../../platform/updateApp'
import { PrimaryButton } from '../../ui/components/Button'
import { rememberedMode } from '../../ui/modeHeadStart'
import { applyTrainingMode } from '../../ui/theme'
import { GateScreen } from '../lock/GateScreen'
import './OlderCopyScreen.css'

export type OlderCopyScreenProps = {
  /** Fetches the newer Grit and reloads into it. For tests. */
  update?: () => Promise<UpdateResult>
}

/**
 * Shown instead of the app when the log was written by a newer Grit than this copy (1.17). It never opens the
 * database: this copy would read plate counts as kilograms. Its one action fetches and switches to the newer version;
 * there is no export, because this copy's backup would drop the plate markers. Like the other pre-database screens,
 * it takes its look from the system (dark or light) and from the side remembered at the last visit (the head script).
 */
export function OlderCopyScreen({ update = updateApp }: OlderCopyScreenProps) {
  const [working, setWorking] = useState(false)
  const [failed, setFailed] = useState(false)
  const busy = useRef(false)
  // The app that gave way to this screen put the powerlifting look back as it went: the side remembered at the last
  // visit is all that is known without the database.
  useEffect(() => applyTrainingMode(rememberedMode()), [])
  const tap = () => {
    if (busy.current) return
    busy.current = true
    setWorking(true)
    setFailed(false)
    update()
      .catch((): UpdateResult => 'failed')
      .then((result) => {
        // Reloading: the button stays on "Updating…" until the page goes.
        if (result === 'reloading') return
        busy.current = false
        setWorking(false)
        setFailed(true)
      })
      .catch(() => {})
  }
  return (
    <GateScreen
      title="Update Grit to carry on"
      body="This copy of Grit is older than your log, so it can't open it safely. Nothing has been changed."
      // The status region (polite): "Updating…" while it works, so a screen reader hears it, not only the button.
      note={working ? 'Updating…' : failed ? "Couldn't update. Check your connection and try again." : null}
    >
      <PrimaryButton text={working ? 'Updating…' : 'Update and reload'} onClick={tap} enabled={!working} className="older-copy-action" />
    </GateScreen>
  )
}
