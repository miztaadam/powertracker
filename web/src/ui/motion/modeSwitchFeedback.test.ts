import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

/*
 * The real sound code of the mode switch (modeSwitchFeedback.ts), not the controller's fake: a fake AudioContext and
 * fetch stand in for the browser's. The module keeps its context and its files between calls, so each test loads a
 * fresh copy.
 */

type Feedback = typeof import('./modeSwitchFeedback')

/** One AudioBufferSourceNode: when it was started, and from where in the sound. */
type Started = { at: number; offset: number }

class FakeContext {
  static made: FakeContext[] = []
  static resumes: 'run' | 'stay' | 'fail' = 'run'
  state: AudioContextState = 'suspended'
  currentTime = 10
  outputLatency = 0.02
  baseLatency = 0.01
  destination = {}
  started: Started[] = []
  decoded: number[] = []
  constructor() {
    FakeContext.made.push(this)
  }
  resume() {
    if (FakeContext.resumes === 'fail') return Promise.reject(new Error('not allowed'))
    if (FakeContext.resumes === 'run') this.state = 'running'
    return Promise.resolve()
  }
  createGain() {
    return {
      gain: { value: 1, cancelScheduledValues() {}, setValueAtTime() {}, linearRampToValueAtTime() {} },
      connect() {},
    }
  }
  createBufferSource() {
    const started = this.started
    return {
      buffer: null as unknown,
      onended: null as unknown,
      connect() {},
      start(at: number, offset: number) {
        started.push({ at, offset })
      },
      stop() {},
    }
  }
  decodeAudioData(bytes: ArrayBuffer) {
    this.decoded.push(bytes.byteLength)
    return Promise.resolve({ duration: 1.9 } as AudioBuffer)
  }
}

/** The files the server has, and every URL asked for, in order. */
let files: Record<string, number>
let asked: string[]
/** Held answers: a fetch for a URL here waits until the test lets it go. */
let held: Record<string, () => void>

function fakeFetch(url: string) {
  asked.push(url)
  const name = url.replace(/^.*\/sounds\//, '')
  const answer = () => {
    const size = files[name]
    return size == null ? ({ ok: false, status: 404 } as Response) : ({ ok: true, arrayBuffer: () => Promise.resolve(new ArrayBuffer(size)) } as unknown as Response)
  }
  if (name in held) return new Promise<Response>((done) => (held[name] = () => done(answer())))
  return Promise.resolve(answer())
}

/** Lets every promise already queued run. */
const settle = () => new Promise((done) => setTimeout(done, 0))

let fb: Feedback

beforeEach(async () => {
  FakeContext.made = []
  FakeContext.resumes = 'run'
  files = { 'grit_hammer_drop.wav': 1900, 'grit_pump.wav': 2000, 'grit_hammer_drop.ogg': 190, 'grit_pump.ogg': 200 }
  asked = []
  held = {}
  vi.stubGlobal('fetch', vi.fn(fakeFetch))
  vi.stubGlobal('AudioContext', FakeContext)
  vi.resetModules()
  fb = await import('./modeSwitchFeedback')
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('prefetch', () => {
  it('fetchesTheNextSwitchsSoundFirst_thenTheOther_onlyTheBytes', async () => {
    fb.prefetchSounds('pump')
    await settle()
    expect(asked.map((u) => u.replace(/^.*\/sounds\//, ''))).toEqual(['grit_pump.wav', 'grit_hammer_drop.wav'])
    // No AudioContext before the tap: the browser asks for a gesture for that.
    expect(FakeContext.made).toHaveLength(0)
    // The tap: nothing fetched again, both decoded from what was fetched.
    fb.unlockSound()
    fb.playSound('pump', () => 0)
    await settle()
    expect(asked).toHaveLength(2)
    expect(FakeContext.made[0]!.decoded).toEqual([2000, 1900])
    expect(FakeContext.made[0]!.started).toHaveLength(1)
  })

  it('theOtherWay_theDropFirst', async () => {
    fb.prefetchSounds('drop')
    await settle()
    expect(asked[0]).toMatch(/grit_hammer_drop\.wav$/)
  })
})

describe('the clock', () => {
  it('aSoundReadyLate_joinsWhereTheClockIsThen', async () => {
    // Nothing fetched ahead (Settings was opened just now): the file arrives 300 ms into the animation.
    held['grit_hammer_drop.wav'] = () => {}
    let clock = 0
    fb.unlockSound()
    fb.playSound('drop', () => clock)
    await settle()
    const ctx = FakeContext.made[0]!
    expect(ctx.started).toHaveLength(0)
    clock = 300
    held['grit_hammer_drop.wav']!()
    await settle()
    // At the clock's offset, plus what the output adds: heard where the picture is.
    expect(ctx.started).toHaveLength(1)
    expect(ctx.started[0]!.offset).toBeCloseTo(0.3 + 0.02, 6)
  })

  it('aSoundReadyAfterItsAnimationHasEnded_neverPlays', async () => {
    held['grit_pump.wav'] = () => {}
    let clock: number | null = 0
    fb.unlockSound()
    fb.playSound('pump', () => clock)
    clock = null
    held['grit_pump.wav']!()
    await settle()
    expect(FakeContext.made[0]!.started).toHaveLength(0)
  })

  it('untied_withMotionOff_playsFromTheStart', async () => {
    fb.unlockSound()
    fb.playSound('pump', null)
    await settle()
    expect(FakeContext.made[0]!.started[0]!.offset).toBeCloseTo(0.02, 6)
  })
})
