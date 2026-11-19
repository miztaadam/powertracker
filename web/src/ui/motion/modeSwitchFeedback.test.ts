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

describe('the format', () => {
  it('canPlayTypeSaysNothingForOgg_theWavCopy_aFallback', async () => {
    vi.spyOn(HTMLMediaElement.prototype, 'canPlayType').mockReturnValue('')
    expect(fb.soundFormats()).toEqual(['wav'])
    fb.prefetchSounds('drop')
    await settle()
    expect(asked.every((u) => u.endsWith('.wav'))).toBe(true)
  })

  it('canPlayTypeSaysOgg_theOgg_andOnlyThat', async () => {
    vi.spyOn(HTMLMediaElement.prototype, 'canPlayType').mockImplementation((type) => (type.includes('ogg') ? 'probably' : ''))
    expect(fb.soundFormats()).toEqual(['ogg', 'wav'])
    fb.prefetchSounds('drop')
    await settle()
    expect(asked.map((u) => u.replace(/^.*\/sounds\//, ''))).toEqual(['grit_hammer_drop.ogg', 'grit_pump.ogg'])
  })

  it('aMissingOgg_isTriedAsWav', async () => {
    vi.spyOn(HTMLMediaElement.prototype, 'canPlayType').mockReturnValue('maybe')
    delete files['grit_pump.ogg']
    fb.unlockSound()
    fb.playSound('pump', () => 0)
    await settle()
    expect(asked.slice(0, 2).map((u) => u.replace(/^.*\/sounds\//, ''))).toEqual(['grit_pump.ogg', 'grit_pump.wav'])
    expect(FakeContext.made[0]!.decoded[0]).toBe(2000)
    expect(FakeContext.made[0]!.started).toHaveLength(1)
  })
})

describe('nothing breaks', () => {
  it('aContextThatStaysSuspended_playsNothing_andThrowsNothing', async () => {
    FakeContext.resumes = 'stay'
    expect(() => fb.unlockSound()).not.toThrow()
    expect(() => fb.playSound('drop', () => 0)).not.toThrow()
    await settle()
    expect(FakeContext.made[0]!.started).toHaveLength(0)
    expect(() => fb.stopSound()).not.toThrow()
  })

  it('aResumeThatIsRefused_playsNothing_andThrowsNothing', async () => {
    FakeContext.resumes = 'fail'
    fb.unlockSound()
    fb.playSound('pump', null)
    await settle()
    expect(FakeContext.made[0]!.started).toHaveLength(0)
  })

  it('aMissingFile_playsNothing_throwsNothing_andIsAskedForAgainNextTime', async () => {
    files = {}
    fb.unlockSound()
    fb.playSound('drop', () => 0)
    await settle()
    const ctx = FakeContext.made[0]!
    expect(ctx.started).toHaveLength(0)
    const before = asked.filter((u) => u.includes('grit_hammer_drop')).length
    expect(before).toBeGreaterThan(0)
    // The file is there again (back online): the next switch fetches it and plays.
    files = { 'grit_hammer_drop.wav': 1900, 'grit_pump.wav': 2000 }
    fb.playSound('drop', () => 0)
    await settle()
    expect(asked.filter((u) => u.includes('grit_hammer_drop')).length).toBeGreaterThan(before)
    expect(ctx.started).toHaveLength(1)
  })

  it('noWebAudio_atAll_throwsNothing', async () => {
    vi.stubGlobal('AudioContext', undefined)
    vi.resetModules()
    const fresh = await import('./modeSwitchFeedback')
    expect(() => fresh.unlockSound()).not.toThrow()
    expect(() => fresh.playSound('drop', () => 0)).not.toThrow()
    expect(() => fresh.followSound(100)).not.toThrow()
    expect(() => fresh.stopSound()).not.toThrow()
  })
})
