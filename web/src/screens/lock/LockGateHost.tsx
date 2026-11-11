import { liveQuery } from 'dexie'
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { flushSync } from 'react-dom'
import { getSettings, type GritDb } from '../../db/db'
import { CREDENTIAL_KEY, RELOCK_AFTER_MS, createLockState, lockSupported, makePasskey, readCredentialId, unlock } from '../../platform/passkeyLock'
import { BlankScreen } from './GateScreen'
import { LockCover, LockScreen } from './LockScreen'
import { LockSetupDialog } from './LockSetupDialog'

export type LockGateHostProps = {
  db: GritDb
  /** The real clock in milliseconds. */
  now?: () => number
  children: ReactNode
}

type LockConfig = { appLock: boolean; credentialId: string | null }

const isVisible = () => document.visibilityState === 'visible'

/**
 * Shows `children` only while the app is open. While it is locked they are not rendered at all, so
 * nothing of the log (dialogs included) is in the page behind the lock; where the lifter was is in
 * the address and the database, and comes back after unlocking.
 *
 * App lock on without a passkey on this phone (a backup restored from another one) never locks:
 * the app opens and asks whether to set the lock up again or turn it off.
 */
export function LockGateHost({ db, now = Date.now, children }: LockGateHostProps) {
  const [lock] = useState(createLockState)
  const clock = useRef(now)
  clock.current = now
  const [config, setConfig] = useState<LockConfig | null>(null)
  const [readError, setReadError] = useState<unknown>(null)
  const [supported, setSupported] = useState<boolean | null>(null)
  const [visible, setVisible] = useState(isVisible)
  // Redraws when the lock's own state changed (it lives outside React).
  const [, setTick] = useState(0)
  const redraw = useCallback(() => setTick((n) => n + 1), [])
  const [setupDismissed, setSetupDismissed] = useState(false)

  useEffect(() => {
    const read = async (): Promise<LockConfig> => ({ appLock: (await getSettings(db)).appLock, credentialId: await readCredentialId(db) })
    const subscription = liveQuery(read).subscribe({ next: setConfig, error: setReadError })
    return () => subscription.unsubscribe()
  }, [db])

  useEffect(() => {
    let stale = false
    lockSupported()
      .then((value) => {
        if (!stale) setSupported(value)
      })
      .catch(() => {})
    return () => {
      stale = true
    }
  }, [])

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined
    const onChange = () => {
      clearTimeout(timer)
      const shown = isVisible()
      if (!shown) {
        lock.markHidden(clock.current())
        // If the page is still running by then, take the log out of it before anyone comes back.
        timer = setTimeout(redraw, RELOCK_AFTER_MS)
        // At once, not on the next frame: the app switcher takes its picture as the app leaves.
        flushSync(() => setVisible(false))
        return
      }
      // Back within the minute: as on Android the trip is over and forgotten, so it cannot lock the
      // app later, in the middle of a workout.
      if (!lock.needsUnlock(clock.current())) lock.markUnlocked()
      setVisible(true)
      redraw()
    }
    document.addEventListener('visibilitychange', onChange)
    window.addEventListener('pagehide', onChange)
    window.addEventListener('pageshow', onChange)
    return () => {
      clearTimeout(timer)
      document.removeEventListener('visibilitychange', onChange)
      window.removeEventListener('pagehide', onChange)
      window.removeEventListener('pageshow', onChange)
    }
  }, [lock, redraw])

  const enabled = config == null ? null : config.appLock && config.credentialId != null
  // With the lock off the visit counts as unlocked, so switching it on in Settings does not lock the
  // visit it was switched on in.
  useEffect(() => {
    if (enabled === false) lock.markUnlocked()
  }, [enabled, lock])

  // The prompt on screen, if any. A new one withdraws it: a tap on Unlock is never ignored.
  const prompt = useRef<AbortController | null>(null)
  const credentialId = config?.credentialId ?? null
  /**
   * Asks the phone to verify its owner. Nothing is awaited before the request itself (the passkey
   * id is already in hand), because Safari shows the Face ID sheet only while a tap is being handled.
   */
  const ask = useCallback(() => {
    prompt.current?.abort()
    const mine = new AbortController()
    prompt.current = mine
    unlock(credentialId, mine.signal)
      .then((result) => {
        // Withdrawn for a newer prompt: that one decides.
        if (prompt.current !== mine) return
        prompt.current = null
        if (result === 'ok') lock.markUnlocked()
        // Nothing to unlock with any more: explain, and let the lifter in.
        else if (result === 'unsupported') setSupported(false)
        redraw()
      })
      // A cancelled or failed prompt leaves the lock screen up, where the Unlock button asks again.
      .catch(() => {})
  }, [credentialId, lock, redraw])

  /**
   * The way back in when the passkey is gone (deleted from the phone's passwords) or unreadable: the
   * phone verifies its owner by making a new passkey, which takes the old one's place.
   */
  const remake = useCallback(() => {
    prompt.current?.abort()
    prompt.current = null
    makePasskey(supported)
      .then(async (made) => {
        if (made.result === 'unsupported') setSupported(false)
        if (made.result !== 'ok') return redraw()
        // The phone verified its owner: that opens the app, even if the new id cannot be kept (a full
        // phone). The next launch then finds the old id and "Can't unlock?" is there again.
        lock.markUnlocked()
        redraw()
        await db.table('meta').put({ key: CREDENTIAL_KEY, value: made.credentialId })
      })
      // Not verified, or not stored: the app is open if it was verified, still locked if not.
      .catch(() => {})
  }, [db, supported, lock, redraw])

  const gate = lock.lockGate(now(), enabled, supported)

  // Becoming locked while on screen brings up the system prompt right away, as on Android.
  useEffect(() => {
    if (gate === 'locked' && visible) ask()
  }, [gate, visible, ask])
  useEffect(() => () => prompt.current?.abort(), [])

  // A failed read is for the app's error boundary: without the setting there is no telling whether to lock.
  if (readError != null) throw readError
  if (gate === 'loading') return <BlankScreen />
  if (gate !== 'open') {
    return (
      <LockScreen
        unavailable={gate === 'unavailable'}
        onUnlock={ask}
        onCantUnlock={remake}
        onContinue={() => {
          lock.markUnlocked()
          redraw()
        }}
      />
    )
  }
  const needsSetup = config?.appLock === true && config.credentialId == null && !setupDismissed
  return (
    <>
      {children}
      {needsSetup && <LockSetupDialog db={db} supported={supported} onDismiss={() => setSetupDismissed(true)} />}
      {enabled && !visible && <LockCover />}
    </>
  )
}
