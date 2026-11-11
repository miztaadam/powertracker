const BEEP_HZ = 880
const BEEP_SEC = 0.15
const GAP_SEC = 0.1
/** The gain envelope: a quick rise and fall, so a beep neither clicks nor startles. */
const ATTACK_SEC = 0.01
const RELEASE_SEC = 0.04
const PEAK_GAIN = 0.5

let context: AudioContext | null = null

/**
 * Makes the app's audio context on a gesture and resumes it whenever it is not running. iOS keeps
 * Web Audio locked until a user gesture and may suspend it again after an interruption, so this
 * listens for the whole life of the app, not only for the first tap. `touchend` is there because
 * Safari counts the end of a touch as the gesture, not always its `pointerdown`. Returns the function
 * that stops listening.
 */
export function unlockAudioOnGesture(): () => void {
  const unlock = () => {
    if (typeof AudioContext === 'undefined') return
    try {
      context ??= new AudioContext()
      // iOS rejects a resume it does not count as a gesture; the next one tries again.
      if (context.state !== 'running') context.resume().catch(() => {})
    } catch {
      // No audio on this device: the flash still marks the end of a rest.
    }
  }
  const events = ['pointerdown', 'touchend'] as const
  for (const event of events) window.addEventListener(event, unlock, { capture: true, passive: true })
  return () => {
    for (const event of events) window.removeEventListener(event, unlock, { capture: true })
  }
}

function beep(audio: AudioContext, at: number): void {
  const oscillator = audio.createOscillator()
  const gain = audio.createGain()
  oscillator.type = 'sine'
  oscillator.frequency.value = BEEP_HZ
  gain.gain.setValueAtTime(0, at)
  gain.gain.linearRampToValueAtTime(PEAK_GAIN, at + ATTACK_SEC)
  gain.gain.setValueAtTime(PEAK_GAIN, at + BEEP_SEC - RELEASE_SEC)
  gain.gain.linearRampToValueAtTime(0, at + BEEP_SEC)
  oscillator.connect(gain).connect(audio.destination)
  oscillator.start(at)
  oscillator.stop(at + BEEP_SEC)
}

/**
 * The end-of-rest tone: two 150 ms beeps at 880 Hz. Silent when audio is missing or still locked.
 * It never queues a tone for later: one that sounds long after the rest ended is worse than none.
 */
export function playRestTone(): void {
  const audio = context
  if (audio == null) return
  try {
    if (audio.state !== 'running') {
      audio.resume().catch(() => {})
      return
    }
    const now = audio.currentTime
    beep(audio, now)
    beep(audio, now + BEEP_SEC + GAP_SEC)
  } catch {
    // A tone that cannot play must not break the screen.
  }
}
