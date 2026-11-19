import { grit } from '../../domain'

/**
 * The vibration and the sound of the mode switch (1.15): the hammer drop and the pump.
 *
 * - **Vibration** is always on, whatever "Vibrate on taps" says (owner): it goes straight to `navigator.vibrate`,
 *   never through platform/haptics.ts. iPhones give web apps none; a browser that refuses is simply quiet.
 * - **Sound** follows the phone's media volume and silent mode; there is no setting. The two files
 *   (public/sounds/, rendered from the approved prototype) are fetched and decoded on the first switch, never at app
 *   start. The switch tap creates and resumes the `AudioContext`, the gesture browsers ask for. Each sound is played
 *   from the animation clock's offset, as the prototype does, and started again from the clock if the two drift
 *   apart. Nothing here ever throws: no Web Audio, a blocked context or a missing file only means no sound.
 */
export type ModeSwitchKind = 'drop' | 'pump'

/** A buzz, straight to the browser. Never throws. */
export function buzz(pattern: number | number[]): void {
  try {
    const nav = navigator as Navigator & { vibrate?: (p: number | number[]) => boolean }
    if (typeof nav.vibrate === 'function') nav.vibrate(pattern)
  } catch {
    // Not allowed here (no gesture, a strict browser): fine.
  }
}

/** With motion off the drop's pattern still lands on the impact, as it does in the sound: still until then. */
export function dropBuzzWithoutMotion(): number[] {
  return [0, grit.modeSwitchDropTimeline().impact, ...grit.modeSwitchVibration().dropPattern]
}

/** With motion off the pump's three beats still land on their onsets: one pattern, pauses between the pulses. */
export function pumpBuzzWithoutMotion(): number[] {
  const beats = grit.modeSwitchPumpTimeline().beats
  const pulses = grit.modeSwitchVibration().pumpPulses
  const pattern = [0, beats[0]!]
  for (let i = 0; i < pulses.length; i++) {
    pattern.push(pulses[i]!)
    if (i + 1 < pulses.length) pattern.push(beats[i + 1]! - beats[i]! - pulses[i]!)
  }
  return pattern
}

const FILES: Record<ModeSwitchKind, string> = {
  drop: 'sounds/grit_hammer_drop.ogg',
  pump: 'sounds/grit_pump.ogg',
}

type Playing = {
  src: AudioBufferSourceNode
  gain: GainNode
  kind: ModeSwitchKind
  tied: boolean
  lat: number
  off: number
  t0: number
}

let ctx: AudioContext | null = null
let master: GainNode | null = null
let unlocked: Promise<unknown> | null = null
const buffers: Partial<Record<ModeSwitchKind, Promise<AudioBuffer | null>>> = {}
let current: Playing | null = null
let token = 0
let lastSync = 0
/** Where the clock is for a tied sound: ms from the tap, or null once its animation has gone. */
let tiedClock: (() => number | null) | null = null

function audioContextClass(): typeof AudioContext | null {
  if (typeof window === 'undefined') return null
  return window.AudioContext ?? (window as Window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext ?? null
}

function load(kind: ModeSwitchKind): Promise<AudioBuffer | null> {
  const context = ctx
  if (context == null) return Promise.resolve(null)
  const cached = buffers[kind]
  if (cached != null) return cached
  const base = typeof import.meta.env?.BASE_URL === 'string' ? import.meta.env.BASE_URL : '/'
  const loading = fetch(`${base}${FILES[kind]}`)
    .then((response) => (response.ok ? response.arrayBuffer() : Promise.reject(new Error(String(response.status)))))
    .then((bytes) => context.decodeAudioData(bytes))
    .catch(() => {
      // Offline before the first fetch, or a browser that cannot decode it: no sound this time; try again next switch.
      delete buffers[kind]
      return null
    })
  buffers[kind] = loading
  return loading
}

/**
 * From the switch tap only: makes the `AudioContext` (once) and resumes it, inside the gesture, and starts fetching
 * both sounds. Never throws.
 */
export function unlockSound(): void {
  try {
    const AC = audioContextClass()
    if (AC == null) return
    if (ctx == null) {
      ctx = new AC({ latencyHint: 'interactive' })
      master = ctx.createGain()
      master.connect(ctx.destination)
    }
    if (ctx.state !== 'running') unlocked = ctx.resume().catch(() => {})
    void load('drop')
    void load('pump')
  } catch {
    ctx = null
  }
}

/** Stops the sound playing now on a quick fade, never a click. */
export function stopSound(fade = 0.04): void {
  token++
  tiedClock = null
  const playing = current
  if (playing == null || ctx == null) return
  current = null
  try {
    const now = ctx.currentTime
    playing.gain.gain.cancelScheduledValues(now)
    playing.gain.gain.setValueAtTime(playing.gain.gain.value, now)
    playing.gain.gain.linearRampToValueAtTime(0, now + fade)
    playing.src.stop(now + fade + 0.02)
  } catch {
    // Already ended.
  }
}

/**
 * Plays `kind`. Tied (`clock` given), it starts where the animation clock is when the sound is ready, and plays only
 * while `clock` says the animation runs; untied (motion off), from the start.
 */
export function playSound(kind: ModeSwitchKind, clock: (() => number | null) | null): void {
  const replacing = current != null
  stopSound(0.03)
  tiedClock = clock
  const context = ctx
  if (context == null) return
  const mine = token
  void Promise.all([load(kind), context.state === 'running' ? null : unlocked])
    .then(([buffer]) => {
      if (buffer == null || mine !== token || context.state !== 'running' || master == null) return
      const at = clock?.()
      if (clock != null && at == null) return
      // What is scheduled now is heard this much later.
      const lat = Math.min(0.08, context.outputLatency || context.baseLatency || 0)
      const off = (at ?? 0) / 1000 + lat
      if (off >= buffer.duration - 0.01) return
      const src = context.createBufferSource()
      const gain = context.createGain()
      const now = context.currentTime
      src.buffer = buffer
      gain.gain.setValueAtTime(0, now)
      gain.gain.linearRampToValueAtTime(1, now + (replacing ? 0.03 : 0.004))
      src.connect(gain)
      gain.connect(master)
      src.start(now, off)
      const me: Playing = {
        src,
        gain,
        kind,
        tied: clock != null,
        lat,
        off,
        t0: now,
      }
      current = me
      src.onended = () => {
        if (current === me) current = null
      }
    })
    .catch(() => {})
}

/** Each frame: if the sound and the picture have drifted apart, starts the sound again from the clock. */
export function followSound(tMs: number): void {
  const playing = current
  if (playing == null || !playing.tied || ctx == null) return
  const pos = playing.off + (ctx.currentTime - playing.t0)
  const want = tMs / 1000 + playing.lat
  const now = performance.now()
  if (Math.abs(pos - want) > 0.06 && now - lastSync > 250) {
    lastSync = now
    playSound(playing.kind, tiedClock)
  }
}
