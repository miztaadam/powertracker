import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { playRestTone, unlockAudioOnGesture } from './audio'

type Beep = { frequency: number; startedAt: number; stoppedAt: number; envelope: [string, number, number][] }
let beeps: Beep[]
let contexts: FakeAudioContext[]
/** True while the browser counts the running event as a user gesture. */
let inGesture = false

/** A Web Audio context as iOS gives it: suspended until `resume()` runs in a gesture. */
class FakeAudioContext {
  state = 'suspended'
  currentTime = 4
  destination = {}
  resume = vi.fn(() => {
    if (!inGesture) return Promise.reject(new DOMException('not allowed', 'NotAllowedError'))
    this.state = 'running'
    return Promise.resolve()
  })
  constructor() {
    contexts.push(this)
  }
  createGain() {
    const envelope: Beep['envelope'] = []
    return {
      envelope,
      connect: vi.fn((node: unknown) => node),
      gain: {
        setValueAtTime: (value: number, at: number) => envelope.push(['set', value, at]),
        linearRampToValueAtTime: (value: number, at: number) => envelope.push(['ramp', value, at]),
      },
    }
  }
  createOscillator() {
    const beep: Beep = { frequency: 0, startedAt: -1, stoppedAt: -1, envelope: [] }
    const frequency = { value: 0 }
    return {
      type: 'sine',
      frequency,
      connect: (gain: { envelope: Beep['envelope'] }) => {
        beep.envelope = gain.envelope
        return gain
      },
      start: (at: number) => {
        beep.frequency = frequency.value
        beep.startedAt = at
        beeps.push(beep)
      },
      stop: (at: number) => {
        beep.stoppedAt = at
      },
    }
  }
}

let stopListening: () => void
const flush = () => new Promise((done) => setTimeout(done, 0))

beforeEach(() => {
  beeps = []
  contexts = []
  vi.stubGlobal('AudioContext', FakeAudioContext)
  stopListening = unlockAudioOnGesture()
})
afterEach(() => {
  stopListening()
  vi.unstubAllGlobals()
})

function gesture(type: string) {
  inGesture = true
  window.dispatchEvent(new Event(type))
  inGesture = false
}

// One module, one context: these run in order, as a session does.
describe('rest tone', () => {
  it('is silent before any gesture, and makes no context for it', async () => {
    playRestTone()
    await flush()
    expect(contexts).toEqual([])
    expect(beeps).toEqual([])
  })

  it('stays silent when the unlock was refused, without an unhandled rejection', async () => {
    // A touch pointerdown that the browser does not count as a gesture.
    window.dispatchEvent(new Event('pointerdown'))
    await flush()
    expect(contexts).toHaveLength(1)
    playRestTone()
    await flush()
    expect(beeps).toEqual([])
  })

  it('unlocks on the first pointerdown that counts', async () => {
    gesture('pointerdown')
    await flush()
    expect(contexts).toEqual([])
    playRestTone()
    expect(beeps).toHaveLength(2)
  })

  it('plays two 150 ms beeps at 880 Hz through a short gain envelope', () => {
    playRestTone()
    expect(beeps.map((b) => b.frequency)).toEqual([880, 880])
    const [first, second] = beeps as [Beep, Beep]
    expect(first.startedAt).toBe(4)
    expect(first.stoppedAt - first.startedAt).toBeCloseTo(0.15)
    expect(second.stoppedAt - second.startedAt).toBeCloseTo(0.15)
    // The second beep starts after the first has ended.
    expect(second.startedAt).toBeGreaterThan(first.stoppedAt)
    for (const beep of beeps) {
      // Up from silence and back to it, so there is no click.
      expect(beep.envelope[0]).toEqual(['set', 0, beep.startedAt])
      const last = beep.envelope[beep.envelope.length - 1]!
      expect(last[0]).toBe('ramp')
      expect(last[1]).toBe(0)
      expect(last[2]).toBeCloseTo(beep.stoppedAt)
      expect(Math.max(...beep.envelope.map((step) => step[1]))).toBeGreaterThan(0)
    }
  })

  it('does nothing without Web Audio', () => {
    vi.unstubAllGlobals()
    expect(() => playRestTone()).not.toThrow()
  })
})
