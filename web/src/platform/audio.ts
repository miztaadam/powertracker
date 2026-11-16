const BEEP_HZ = 880
const BEEP_SEC = 0.15
const GAP_SEC = 0.1
/** The gain envelope: a quick rise and fall, so a beep neither clicks nor startles. */
const ATTACK_SEC = 0.01
const RELEASE_SEC = 0.04
const PEAK_GAIN = 0.5

/** A tone whose `resume()` takes longer than this is dropped: it would sound after the moment had passed. */
const LATE_RESUME_MS = 1000

let context: AudioContext | null = null

/** Suspended, or (iOS, after a call, an alarm or the app going to the background) interrupted. */
const stopped = (audio: AudioContext) => (audio.state as string) === 'suspended' || (audio.state as string) === 'interrupted'

/** Plays one silent sample. In a gesture, this is what really unlocks Web Audio on iOS; a resume alone may not. */
function playSilence(audio: AudioContext): void {
  try {
    const source = audio.createBufferSource()
    source.buffer = audio.createBuffer(1, 1, 22050)
    source.connect(audio.destination)
    source.start(0)
  } catch {
    // An older Web Audio: the resume is all there is.
  }
}

/**
 * Makes the app's audio context on a gesture and resumes it whenever it is not running. iOS keeps
 * Web Audio locked until a user gesture and may suspend or interrupt it again later, so this listens
 * for the whole life of the app, not only for the first tap. iOS counts the end of a touch, a click or
 * a key as the gesture, not always a `pointerdown`, so it listens to all four; until the context runs,
 * each gesture also plays a silent sample. When the page comes back into view, a stopped context is
 * resumed. Returns the function that stops listening.
 */
export function unlockAudioOnGesture(): () => void {
  const unlock = () => {
    if (typeof AudioContext === 'undefined') return
    try {
      context ??= new AudioContext()
      if (context.state !== 'running') {
        // iOS rejects a resume it does not count as a gesture; the next one tries again.
        context.resume().catch(() => {})
        playSilence(context)
      }
    } catch {
      // No audio on this device: the flash still marks the end of a rest.
    }
  }
  const onVisible = () => {
    if (document.visibilityState !== 'visible' || context == null || !stopped(context)) return
    try {
      context.resume().catch(() => {})
    } catch {
      // Not resumed: the next gesture tries again.
    }
  }
  const events = ['pointerdown', 'touchend', 'click', 'keydown'] as const
  for (const event of events) window.addEventListener(event, unlock, { capture: true, passive: true })
  document.addEventListener('visibilitychange', onVisible)
  return () => {
    for (const event of events) window.removeEventListener(event, unlock, { capture: true })
    document.removeEventListener('visibilitychange', onVisible)
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

function playTone(audio: AudioContext): void {
  const now = audio.currentTime
  beep(audio, now)
  beep(audio, now + BEEP_SEC + GAP_SEC)
}

/**
 * The end-of-rest tone: two 150 ms beeps at 880 Hz. Silent when audio is missing or still locked.
 * A context that was suspended or interrupted (iOS, after the app was away) is resumed first and the
 * tone plays once it runs again. It never queues a tone for later: a resume that is refused, or that
 * takes longer than a second, drops the tone, since one that sounds long after the rest ended is
 * worse than none.
 */
export function playRestTone(): void {
  const audio = context
  if (audio == null) return
  try {
    if (audio.state === 'running') {
      playTone(audio)
      return
    }
    const askedAt = Date.now()
    audio
      .resume()
      .then(() => {
        if (audio.state === 'running' && Date.now() - askedAt <= LATE_RESUME_MS) playTone(audio)
      })
      .catch(() => {})
  } catch {
    // A tone that cannot play must not break the screen.
  }
}
