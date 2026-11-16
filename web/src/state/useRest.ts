import { liveQuery } from 'dexie'
import { useCallback, useEffect, useRef, useState } from 'react'
import { onDataReplaced } from '../backup/backup'
import { getSettings, type AppSettings, type GritDb } from '../db/db'
import { playRestTone, unlockAudioOnGesture } from '../platform/audio'
import { vibrateRestEnd } from '../platform/device'
import { RestTimerStore, useRestTimer, type RestTimerState, type RestTimerStorage } from '../platform/restTimer'
import { releaseWakeLock, requestWakeLock } from '../platform/wakeLock'

/** The `meta` row that holds the running rest. */
export const REST_TIMER_KEY = 'restTimer'

/**
 * A rest found over longer ago than this ends without an alert. The page is suspended behind a
 * locked screen, so zero can be noticed minutes late; a tone then would only confuse.
 */
export const STALE_ALERT_MS = 5000

function savedRest(value: unknown): RestTimerState | null {
  if (typeof value !== 'object' || value === null) return null
  const { endsAtMs, totalSec } = value as Record<string, unknown>
  return typeof endsAtMs === 'number' && typeof totalSec === 'number' ? { endsAtMs, totalSec } : null
}

/** Keeps the running rest in the database's `meta` table. */
function restStorage(db: GritDb): RestTimerStorage {
  return {
    load: async () => savedRest((await db.meta.get(REST_TIMER_KEY))?.value),
    save: async (state) => {
      if (state.endsAtMs == null) await db.meta.delete(REST_TIMER_KEY)
      else await db.meta.put({ key: REST_TIMER_KEY, value: state })
    },
  }
}

export type Rest = {
  /** Call after each saved set, failed ones too (true for a main lift or variation): starts the rest. */
  onSetLogged: (isMainOrVariation: boolean) => void
  /** The rest itself, for the bar that counts it down. */
  store: RestTimerStore
  /** True while a rest runs. Changes only when one starts or ends, not as the seconds pass. */
  running: boolean
  /** Goes up by one each time a rest ends with a flash; 0 until then. */
  flashId: number
}

/**
 * The rest timer of the app, wired to Settings: a logged set starts a rest of the main or accessory
 * length, zero is announced as the Rest alert setting says, and the screen is kept awake meanwhile
 * when "Keep screen awake during rests" is on. Null `db`: nothing starts until the database is open.
 */
export function useRest(db: GritDb | null): Rest {
  const [store] = useState(() => new RestTimerStore())
  const [settings, setSettings] = useState<AppSettings | null>(null)
  const latest = useRef(settings)
  latest.current = settings
  const [flashId, setFlashId] = useState(0)

  useEffect(() => {
    if (db == null) return
    // A failed read leaves the last settings: the timer is not what reports a broken database.
    const subscription = liveQuery(() => getSettings(db)).subscribe({ next: setSettings, error: () => {} })
    const detach = store.attach(restStorage(db))
    return () => {
      subscription.unsubscribe()
      detach()
    }
  }, [db, store])

  // A restore replaces the session the rest belonged to: the rest ends with it, without an alert.
  useEffect(() => onDataReplaced(() => store.skip()), [store])

  useEffect(() => unlockAudioOnGesture(), [])

  const timer = useRestTimer(
    store,
    useCallback(
      (lateMs: number) => {
        if (lateMs > STALE_ALERT_MS) return
        const announce = ({ restAlert }: AppSettings) => {
          if (restAlert === 'OFF') return
          if (restAlert === 'SOUND' || restAlert === 'SOUND_AND_VIBRATE') playRestTone()
          // Only Android browsers can vibrate; elsewhere Sound + vibrate is just the sound.
          if (restAlert === 'SOUND_AND_VIBRATE') vibrateRestEnd()
          setFlashId((id) => id + 1)
        }
        // A rest picked up after a reload can reach zero before the settings have been read: read them for it.
        if (latest.current != null) announce(latest.current)
        else if (db != null) getSettings(db).then(announce).catch(() => {})
      },
      [db],
    ),
  )

  // Held only while a rest runs. The browser drops the lock when the page is hidden, so it is asked
  // for again each time the page comes back.
  const keepAwake = timer.running && settings?.keepAwake === true
  useEffect(() => {
    if (!keepAwake) return
    const acquire = () => {
      if (document.visibilityState === 'visible') void requestWakeLock()
    }
    acquire()
    document.addEventListener('visibilitychange', acquire)
    return () => {
      document.removeEventListener('visibilitychange', acquire)
      void releaseWakeLock()
    }
  }, [keepAwake])

  const { start } = timer
  const onSetLogged = useCallback(
    (isMainOrVariation: boolean) => {
      if (db == null) return
      // Read at the moment of the set, as Android does: main lifts and their variations rest longer than accessories.
      getSettings(db)
        .then((s) => start(isMainOrVariation ? s.restMainSec : s.restAccessorySec))
        .catch(() => {})
    },
    [db, start],
  )

  return { onSetLogged, store, running: timer.running, flashId }
}
