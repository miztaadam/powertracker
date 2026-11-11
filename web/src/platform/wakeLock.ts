let held: WakeLockSentinel | null = null
/** Whether the app still wants the screen awake: a lock granted after it was let go is released at once. */
let wanted = false

/**
 * Keeps the screen on (Screen Wake Lock, iOS 16.4+). Does nothing where the API is missing, and
 * never rejects: the browser may refuse, for example in Low Power Mode or while the page is hidden.
 * The browser drops the lock whenever the page is hidden, so ask again when it is visible.
 */
export async function requestWakeLock(): Promise<void> {
  wanted = true
  if (held != null || !('wakeLock' in navigator)) return
  try {
    const lock = await navigator.wakeLock.request('screen')
    if (!wanted || held != null) {
      await lock.release()
      return
    }
    held = lock
    lock.addEventListener('release', () => {
      if (held === lock) held = null
    })
  } catch {
    // Refused: the rest runs on, the screen just dims as usual.
  }
}

/** Lets the screen sleep again. Never rejects. */
export async function releaseWakeLock(): Promise<void> {
  wanted = false
  const lock = held
  held = null
  try {
    await lock?.release()
  } catch {
    // Already gone.
  }
}
