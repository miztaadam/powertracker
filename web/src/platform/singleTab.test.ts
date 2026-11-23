import { afterEach, describe, expect, it, vi } from 'vitest'
import { fakeChannels, fakeLocks } from '../test/tabs'
import { singleTabGuard, type TabRole } from './singleTab'

const setLocks = (value: unknown) => Object.defineProperty(navigator, 'locks', { value, configurable: true })
const settle = (ms = 0) => new Promise((done) => setTimeout(done, ms))

afterEach(() => {
  setLocks(undefined)
  vi.unstubAllGlobals()
})

/** A tab: its guard and the roles it was told, newest last. */
function tab() {
  const roles: TabRole[] = []
  const guard = singleTabGuard((role) => roles.push(role))
  return { guard, role: () => roles[roles.length - 1] ?? null }
}

describe('singleTabGuard', () => {
  it('firstTabIsPrimary_secondIsNot_untilTheFirstCloses', async () => {
    setLocks(fakeLocks())
    vi.stubGlobal('BroadcastChannel', fakeChannels())
    const first = tab()
    await settle()
    const second = tab()
    await settle()
    expect(first.role()).toBe('primary')
    expect(second.role()).toBe('secondary')

    first.guard.dispose()
    await settle()
    expect(second.role()).toBe('primary')
    second.guard.dispose()
  })

  it('takeOver_movesGritToThisTab', async () => {
    setLocks(fakeLocks())
    vi.stubGlobal('BroadcastChannel', fakeChannels())
    const first = tab()
    await settle()
    const second = tab()
    await settle()
    expect(second.guard.canTakeOver).toBe(true)

    second.guard.takeOver()
    await settle()

    expect(first.role()).toBe('secondary')
    expect(second.role()).toBe('primary')
    first.guard.dispose()
    second.guard.dispose()
  })

  it('withoutWebLocks_theChannelDecides', async () => {
    vi.stubGlobal('BroadcastChannel', fakeChannels())
    const first = tab()
    await settle(400)
    expect(first.role()).toBe('primary')
    const second = tab()
    await settle(400)
    expect(second.role()).toBe('secondary')

    second.guard.takeOver()
    await settle()
    expect(first.role()).toBe('secondary')
    expect(second.role()).toBe('primary')

    // The tab that has Grit closes: the other one takes it.
    second.guard.dispose()
    await settle(800)
    expect(first.role()).toBe('primary')
    first.guard.dispose()
  })

  it('withoutEitherApi_everyTabWorks', async () => {
    vi.stubGlobal('BroadcastChannel', undefined)
    const only = tab()
    await settle()
    expect(only.role()).toBe('primary')
    expect(only.guard.canTakeOver).toBe(false)
    only.guard.dispose()
  })

  it('aBrokenLockManager_doesNotShutTheAppOut', async () => {
    setLocks({ request: () => Promise.reject(new DOMException('no', 'SecurityError')) })
    vi.stubGlobal('BroadcastChannel', fakeChannels())
    const only = tab()
    await settle()
    expect(only.role()).toBe('primary')
    only.guard.dispose()
  })
})
