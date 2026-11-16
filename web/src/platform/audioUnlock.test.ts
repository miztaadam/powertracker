import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// The iPhone side of the rest tone: the silent sample that unlocks Web Audio, every gesture iOS counts,
// a context that iOS interrupted, and a resume before the tone. Each test loads audio.ts afresh, so
// each starts with no context.

type Audio = typeof import('./audio')

let contexts: FakeAudioContext[]
/** Set to make the next `new AudioContext()` throw, as a browser without a free audio device does. */
let failNextCreate = false
let beeps: number[]
let silences: number
/** How the next `resume()` answers: at once, refused, or held until the test lets it go. */
let resumeMode: 'resolve' | 'reject' | 'hold'
let release: () => void

class FakeAudioContext {
  state: string = 'suspended'
  currentTime = 4
  destination = {}
  resume = vi.fn(() => {
    if (resumeMode === 'reject') return Promise.reject(new DOMException('not allowed', 'NotAllowedError'))
    if (resumeMode === 'hold') {
      return new Promise<void>((done) => {
        release = () => {
          this.state = 'running'
          done()
        }
      })
    }
    this.state = 'running'
    return Promise.resolve()
  })
  constructor() {
    if (failNextCreate) {
      failNextCreate = false
      throw new DOMException('no audio device', 'NotSupportedError')
    }
    contexts.push(this)
  }
  createBuffer(channels: number, length: number, rate: number) {
    return { channels, length, rate }
  }
  createBufferSource() {
    const source = {
      buffer: null as { length: number } | null,
      connect: (node: unknown) => node,
      start: () => {
        if (source.buffer?.length === 1) silences += 1
      },
    }
    return source
  }
  createGain() {
    return { connect: (node: unknown) => node, gain: { setValueAtTime() {}, linearRampToValueAtTime() {} } }
  }
  createOscillator() {
    return { type: 'sine', frequency: { value: 0 }, connect: (node: unknown) => node, start: (at: number) => void beeps.push(at), stop() {} }
  }
}

let audio: Audio
let stopListening: () => void
const flush = () => new Promise((done) => setTimeout(done, 0))
let visibility: DocumentVisibilityState = 'visible'

beforeEach(async () => {
  contexts = []
  beeps = []
  silences = 0
  failNextCreate = false
  resumeMode = 'resolve'
  visibility = 'visible'
  vi.stubGlobal('AudioContext', FakeAudioContext)
  Object.defineProperty(document, 'visibilityState', { get: () => visibility, configurable: true })
  vi.resetModules()
  audio = await import('./audio')
  stopListening = audio.unlockAudioOnGesture()
})
afterEach(() => {
  stopListening()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  delete (document as { visibilityState?: unknown }).visibilityState
})

const context = () => contexts[0]!

describe('unlocking on iPhone', () => {
  for (const type of ['pointerdown', 'touchend', 'click', 'keydown']) {
    it(`a ${type} makes the context, resumes it and plays one silent sample`, async () => {
      window.dispatchEvent(new Event(type))
      await flush()
      expect(contexts).toHaveLength(1)
      expect(context().resume).toHaveBeenCalledTimes(1)
      expect(silences).toBe(1)
      expect(context().state).toBe('running')
    })
  }

  it('once it runs, later gestures neither resume nor play silence again', async () => {
    window.dispatchEvent(new Event('touchend'))
    await flush()
    window.dispatchEvent(new Event('click'))
    window.dispatchEvent(new Event('keydown'))
    await flush()
    expect(contexts).toHaveLength(1)
    expect(context().resume).toHaveBeenCalledTimes(1)
    expect(silences).toBe(1)
  })

  it('a refused unlock tries again, silence included, on the next gesture', async () => {
    resumeMode = 'reject'
    window.dispatchEvent(new Event('pointerdown'))
    await flush()
    resumeMode = 'resolve'
    window.dispatchEvent(new Event('touchend'))
    await flush()
    expect(context().resume).toHaveBeenCalledTimes(2)
    expect(silences).toBe(2)
    expect(context().state).toBe('running')
  })

  it('stops listening when asked', async () => {
    stopListening()
    window.dispatchEvent(new Event('click'))
    await flush()
    expect(contexts).toEqual([])
  })
})

describe('coming back to the app', () => {
  for (const state of ['suspended', 'interrupted']) {
    it(`resumes a context that iOS left ${state}`, async () => {
      window.dispatchEvent(new Event('click'))
      await flush()
      context().state = state
      document.dispatchEvent(new Event('visibilitychange'))
      await flush()
      expect(context().resume).toHaveBeenCalledTimes(2)
      expect(context().state).toBe('running')
    })
  }

  it('does nothing while hidden, or when the context already runs', async () => {
    window.dispatchEvent(new Event('click'))
    await flush()
    document.dispatchEvent(new Event('visibilitychange'))
    context().state = 'interrupted'
    visibility = 'hidden'
    document.dispatchEvent(new Event('visibilitychange'))
    await flush()
    expect(context().resume).toHaveBeenCalledTimes(1)
  })

  it('does nothing before any gesture made a context', () => {
    expect(() => document.dispatchEvent(new Event('visibilitychange'))).not.toThrow()
    expect(contexts).toEqual([])
  })
})

describe('the rest tone after an interruption', () => {
  it('resumes an interrupted context, then plays the tone', async () => {
    window.dispatchEvent(new Event('click'))
    await flush()
    context().state = 'interrupted'
    audio.playRestTone()
    expect(beeps).toEqual([])
    await flush()
    expect(context().resume).toHaveBeenCalledTimes(2)
    expect(beeps).toHaveLength(2)
  })

  it('a refused resume drops the tone, with no unhandled rejection', async () => {
    window.dispatchEvent(new Event('click'))
    await flush()
    context().state = 'suspended'
    resumeMode = 'reject'
    audio.playRestTone()
    await flush()
    expect(beeps).toEqual([])
  })

  it('a resume that answers more than a second later drops the tone', async () => {
    window.dispatchEvent(new Event('click'))
    await flush()
    context().state = 'interrupted'
    resumeMode = 'hold'
    vi.useFakeTimers({ toFake: ['Date'] })
    audio.playRestTone()
    vi.setSystemTime(Date.now() + 5_000)
    release()
    await flush()
    expect(context().state).toBe('running')
    expect(beeps).toEqual([])
  })

  it('a resume that answers at once still plays the tone', async () => {
    window.dispatchEvent(new Event('click'))
    await flush()
    context().state = 'suspended'
    resumeMode = 'hold'
    audio.playRestTone()
    release()
    await flush()
    expect(beeps).toHaveLength(2)
  })
})

describe('making the context at start-up, off the tap', () => {
  // A browser's frames, run by hand: jsdom draws none.
  let frames: FrameRequestCallback[]
  const paint = async () => {
    for (const frame of frames.splice(0)) frame(0)
    await flush()
  }
  beforeEach(() => {
    stopListening()
    frames = []
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => frames.push(cb))
    vi.stubGlobal('cancelAnimationFrame', (id: number) => {
      frames[id - 1] = () => {}
    })
    stopListening = audio.unlockAudioOnGesture()
  })

  it('is made after the first paint, without a gesture, and is not resumed or primed then', async () => {
    expect(contexts).toEqual([])
    await paint()
    expect(contexts).toHaveLength(1)
    expect(context().resume).not.toHaveBeenCalled()
    expect(silences).toBe(0)
  })

  it('a gesture then makes no context: it only resumes the one made early and plays the silent sample', async () => {
    await paint()
    const made = vi.spyOn(globalThis, 'AudioContext')
    window.dispatchEvent(new Event('click'))
    await flush()
    expect(made).not.toHaveBeenCalled()
    expect(contexts).toHaveLength(1)
    expect(context().resume).toHaveBeenCalledTimes(1)
    expect(silences).toBe(1)
    expect(context().state).toBe('running')
  })

  it('a gesture before the first paint makes it, and the paint makes no second one', async () => {
    window.dispatchEvent(new Event('touchend'))
    await flush()
    expect(contexts).toHaveLength(1)
    await paint()
    expect(contexts).toHaveLength(1)
    expect(silences).toBe(1)
  })

  it('when the early one fails, the gesture still makes it', async () => {
    failNextCreate = true
    await paint()
    expect(contexts).toEqual([])
    window.dispatchEvent(new Event('pointerdown'))
    await flush()
    expect(contexts).toHaveLength(1)
    expect(context().state).toBe('running')
    expect(silences).toBe(1)
  })

  it('stopping before the first paint makes none', async () => {
    stopListening()
    await paint()
    expect(contexts).toEqual([])
  })
})
