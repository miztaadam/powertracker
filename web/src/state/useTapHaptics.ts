import { liveQuery } from 'dexie'
import { useEffect } from 'react'
import { getSettings, type GritDb } from '../db/db'
import { canVibrate } from '../platform/device'
import { setTapHaptics } from '../platform/haptics'

/**
 * Keeps `haptic()` following the "Vibrate on taps" setting, read live, while the app is open. Where the browser cannot
 * vibrate (iPhone) there is nothing to follow, so nothing is read. Off again when the app goes.
 */
export function useTapHaptics(db: GritDb | null): void {
  useEffect(() => {
    if (db == null || !canVibrate()) return
    // A failed read leaves the buzz as it was; the screens that need the settings report it.
    const subscription = liveQuery(() => getSettings(db)).subscribe({ next: (s) => setTapHaptics(s.vibrateOnTaps), error: () => {} })
    return () => {
      subscription.unsubscribe()
      setTapHaptics(false)
    }
  }, [db])
}
