import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { afterDialogEntries, historyBack, historyClosesActive, holdHistoryEntry, installHistoryCloses, resetHistoryCloses } from './historyCloses'

// The iPhone path (1.17.4) against jsdom's own history: its back() and go() move asynchronously and fire popstate, as a
// browser's do.

const IPHONE = { userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148', platform: 'iPhone', maxTouchPoints: 5 }
const IPAD_AS_MAC = { userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15', platform: 'MacIntel', maxTouchPoints: 5 }
const ANDROID = { userAgent: 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Mobile Safari/537.36', platform: 'Linux armv81', maxTouchPoints: 5 }
const DESKTOP = { userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36', platform: 'Linux x86_64', maxTouchPoints: 0 }

const depth = () => ((history.state as { gritDialog?: number } | null)?.gritDialog ?? 0)
/** Lets queued microtasks and jsdom's traversal tasks run. */
const settle = async () => {
  for (let i = 0; i < 5; i++) await new Promise((r) => setTimeout(r, 5))
}
/** A popstate the app's screens would see (added after the module's, as the screens' are). */
let seenByApp: unknown[] = []
const appListener = (e: PopStateEvent) => seenByApp.push(e.state)

beforeEach(async () => {
  resetHistoryCloses()
  // A clean page entry to start from.
  history.replaceState(null, '', '#/today')
  history.pushState(null, '', '#/exercises')
  await settle()
  seenByApp = []
})

afterEach(() => {
  window.removeEventListener('popstate', appListener)
  resetHistoryCloses()
  vi.restoreAllMocks()
})

describe('off iPhone and iPad: a synchronous pass-through', () => {
  for (const [name, nav] of [
    ['Android', ANDROID],
    ['desktop', DESKTOP],
  ] as const) {
    it(`${name}: nothing installs, nothing is pushed, every call runs at once`, async () => {
      const add = vi.spyOn(window, 'addEventListener')
      const push = vi.spyOn(history, 'pushState')
      const go = vi.spyOn(history, 'go')
      const back = vi.spyOn(history, 'back').mockImplementation(() => {})
      expect(installHistoryCloses(nav)).toBeNull()
      expect(historyClosesActive()).toBe(false)
      expect(add).not.toHaveBeenCalled()

      const release = holdHistoryEntry(() => {})
      const ran: string[] = []
      afterDialogEntries(() => ran.push('a'))
      expect(ran).toEqual(['a'])
      historyBack()
      expect(back).toHaveBeenCalledTimes(1)
      release()
      await settle()
      expect(push).not.toHaveBeenCalled()
      expect(go).not.toHaveBeenCalled()
      expect(back).toHaveBeenCalledTimes(1)
    })
  }

  it('not installed at all (jsdom, unit tests): the same', async () => {
    const push = vi.spyOn(history, 'pushState')
    const release = holdHistoryEntry(() => {})
    let ran = false
    afterDialogEntries(() => (ran = true))
    expect(ran).toBe(true)
    release()
    await settle()
    expect(push).not.toHaveBeenCalled()
  })
})

describe('on an iPhone or iPad', () => {
  beforeEach(() => {
    expect(installHistoryCloses(IPHONE)).toBeNull()
    window.addEventListener('popstate', appListener)
  })

  it('also turns on for an iPad that says it is a Mac', () => {
    resetHistoryCloses()
    expect(installHistoryCloses(IPAD_AS_MAC)).toBeNull()
    expect(historyClosesActive()).toBe(true)
  })

  it('keeps one entry per open thing, a microtask after it opens, and drops them when they close', async () => {
    const length = history.length
    const a = holdHistoryEntry(() => {})
    expect(depth()).toBe(0)
    await settle()
    expect(depth()).toBe(1)
    const b = holdHistoryEntry(() => {})
    await settle()
    expect(depth()).toBe(2)
    expect(history.length).toBe(length + 2)
    // Out of order: the older closes first; the count is what matters.
    a()
    await settle()
    expect(depth()).toBe(1)
    b()
    await settle()
    expect(depth()).toBe(0)
    expect(location.hash).toBe('#/exercises')
    // Its own steps back never reach the app.
    expect(seenByApp).toEqual([])
  })

  it('a close and an open in the same tick make no history call', async () => {
    const first = holdHistoryEntry(() => {})
    await settle()
    const push = vi.spyOn(history, 'pushState')
    const go = vi.spyOn(history, 'go')
    first()
    const second = holdHistoryEntry(() => {})
    await settle()
    expect(push).not.toHaveBeenCalled()
    expect(go).not.toHaveBeenCalled()
    expect(depth()).toBe(1)
    second()
    await settle()
  })

  it("the lifter's Back dismisses the newest only, once, and the app's listeners never see it", async () => {
    const older = vi.fn()
    const newer = vi.fn()
    const releaseOlder = holdHistoryEntry(older)
    const releaseNewer = holdHistoryEntry(() => {
      newer()
      releaseNewer()
    })
    await settle()
    expect(depth()).toBe(2)
    history.back()
    await settle()
    expect(newer).toHaveBeenCalledTimes(1)
    expect(older).not.toHaveBeenCalled()
    expect(depth()).toBe(1)
    expect(seenByApp).toEqual([])
    releaseOlder()
    await settle()
  })

  it('a dismiss that leaves it open puts its entry back for the next Back', async () => {
    let steps = 1
    const release = holdHistoryEntry(() => {
      if (steps > 0) steps--
      else release()
    })
    await settle()
    history.back()
    await settle()
    expect(steps).toBe(0)
    expect(depth()).toBe(1)
    history.back()
    await settle()
    expect(depth()).toBe(0)
    expect(location.hash).toBe('#/exercises')
    expect(seenByApp).toEqual([])
  })

  it('queues the app’s history calls until its entries are gone, in order, and they read the page’s own state', async () => {
    history.replaceState({ gritFrom: '#/today' }, '')
    const release = holdHistoryEntry(() => {})
    await settle()
    expect(depth()).toBe(1)
    const ran: unknown[] = []
    release()
    afterDialogEntries(() => ran.push(['a', history.state]))
    afterDialogEntries(() => ran.push(['b', history.state]))
    expect(ran).toEqual([])
    await settle()
    expect(ran).toEqual([
      ['a', { gritFrom: '#/today' }],
      ['b', { gritFrom: '#/today' }],
    ])
    expect(seenByApp).toEqual([])
  })

  it('a route call with something still open: the entries go, the call runs, the entries come back on the new route', async () => {
    const release = holdHistoryEntry(() => {})
    await settle()
    afterDialogEntries(() => {
      location.hash = '#/plan'
    })
    await settle()
    expect(location.hash).toBe('#/plan')
    expect(depth()).toBe(1)
    // Back closes it on the new route, and the next Back is an ordinary one to Exercises.
    release()
    await settle()
    expect(depth()).toBe(0)
    history.back()
    await settle()
    expect(location.hash).toBe('#/exercises')
  })

  it("the app's own step back goes on to its listeners, and calls queued behind it wait for it", async () => {
    history.replaceState({ gritFrom: '#/today' }, '')
    const release = holdHistoryEntry(() => {})
    await settle()
    const ran: string[] = []
    release()
    historyBack()
    afterDialogEntries(() => ran.push(location.hash))
    await settle()
    // One pop reached the app: its own, to Today.
    expect(seenByApp).toEqual([null])
    expect(ran).toEqual(['#/today'])
  })

  it('Forward onto an entry with nothing open steps off it, unseen', async () => {
    const release = holdHistoryEntry(() => release())
    await settle()
    history.back()
    await settle()
    expect(depth()).toBe(0)
    history.forward()
    await settle()
    expect(depth()).toBe(0)
    expect(location.hash).toBe('#/exercises')
    expect(seenByApp).toEqual([])
  })
})

describe('start-up on one of its entries (a reload with something open)', () => {
  it('steps off before the app renders, and says so with a promise', async () => {
    history.pushState({ gritDialog: 1 }, '')
    history.pushState({ gritDialog: 2 }, '')
    await settle()
    const ready = installHistoryCloses(IPHONE)
    // The screens' listeners come after the module's, as in the app (main.tsx installs it before rendering).
    window.addEventListener('popstate', appListener)
    expect(ready).not.toBeNull()
    await ready
    expect(depth()).toBe(0)
    expect(location.hash).toBe('#/exercises')
    expect(seenByApp).toEqual([])
  })

  it('off iPhone, an entry like that is left alone and the app renders at once', () => {
    history.pushState({ gritDialog: 1 }, '')
    expect(installHistoryCloses(ANDROID)).toBeNull()
  })
})
