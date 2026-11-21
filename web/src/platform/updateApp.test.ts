import { describe, expect, it, vi } from 'vitest'
import { SKIP_WAITING, updateApp } from './updateApp'

// The "Update and reload" button of the older-copy screen (1.17): a plain reload would be served the same old copy
// from the service worker's cache, so it fetches the newer version, switches to it, and reloads once it is in charge.

type Listener = () => void

class FakeWorker {
  state: ServiceWorkerState
  posted: unknown[] = []
  private listeners: Listener[] = []
  constructor(state: ServiceWorkerState) {
    this.state = state
  }
  addEventListener(_type: 'statechange', cb: Listener) {
    this.listeners.push(cb)
  }
  removeEventListener(_type: 'statechange', cb: Listener) {
    this.listeners = this.listeners.filter((l) => l !== cb)
  }
  postMessage(message: unknown) {
    this.posted.push(message)
  }
  become(state: ServiceWorkerState) {
    this.state = state
    this.listeners.forEach((cb) => cb())
  }
}

function fakeContainer(options: { waiting?: FakeWorker | null; found?: FakeWorker | null; updateFails?: boolean; controlled?: boolean; registered?: boolean } = {}) {
  const listeners: Listener[] = []
  const registration = {
    waiting: (options.waiting ?? null) as FakeWorker | null,
    installing: null as FakeWorker | null,
    update: vi.fn(async () => {
      if (options.updateFails) throw new TypeError('Failed to update a ServiceWorker: network error')
      if (options.found) registration.installing = options.found
    }),
  }
  const container = {
    controller: options.controlled === false ? null : {},
    getRegistration: vi.fn(async () => (options.registered === false ? undefined : registration)),
    addEventListener: vi.fn((_type: 'controllerchange', cb: Listener) => listeners.push(cb)),
    removeEventListener: () => {},
  }
  return { container: container as unknown as ServiceWorkerContainer, registration, takeOver: () => listeners.forEach((cb) => cb()) }
}

describe('updateApp', () => {
  it('no service worker (dev, or not supported): reloads', async () => {
    const reload = vi.fn()
    expect(await updateApp({ container: undefined, reload })).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('no registration: reloads', async () => {
    const reload = vi.fn()
    const { container } = fakeContainer({ registered: false })
    expect(await updateApp({ container, reload })).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('a page no worker controls (not served from the old cache): reloads', async () => {
    const reload = vi.fn()
    const { container } = fakeContainer({ controlled: false })
    expect(await updateApp({ container, reload })).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('offline: the update check fails, nothing reloads', async () => {
    const reload = vi.fn()
    const { container } = fakeContainer({ updateFails: true })
    expect(await updateApp({ container, reload })).toBe('failed')
    expect(reload).not.toHaveBeenCalled()
  })

  it('no newer version found: fails, nothing reloads', async () => {
    const reload = vi.fn()
    const { container, registration } = fakeContainer()
    expect(await updateApp({ container, reload })).toBe('failed')
    expect(registration.update).toHaveBeenCalledTimes(1)
    expect(reload).not.toHaveBeenCalled()
  })

  it('a newer version found: waits for it to install, tells it to take over, and reloads once it is in charge, once', async () => {
    const reload = vi.fn()
    const found = new FakeWorker('installing')
    const { container, takeOver } = fakeContainer({ found })
    const result = updateApp({ container, reload })
    await vi.waitFor(() => expect(found.posted).toEqual([]))
    found.become('installed')
    await vi.waitFor(() => expect(found.posted).toEqual([SKIP_WAITING]))
    expect(reload).not.toHaveBeenCalled()
    // The worker also reloads the windows it takes over (public/sw-reload-clients.js); the page reloads itself once.
    takeOver()
    takeOver()
    expect(await result).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('a version already waiting: no check needed, it is told to take over', async () => {
    const reload = vi.fn()
    const waiting = new FakeWorker('installed')
    const { container, registration, takeOver } = fakeContainer({ waiting })
    const result = updateApp({ container, reload })
    await vi.waitFor(() => expect(waiting.posted).toEqual([SKIP_WAITING]))
    expect(registration.update).not.toHaveBeenCalled()
    takeOver()
    expect(await result).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('the newer version fails to install: fails, nothing reloads', async () => {
    const reload = vi.fn()
    const found = new FakeWorker('installing')
    const { container } = fakeContainer({ found })
    const result = updateApp({ container, reload })
    await vi.waitFor(() => expect(container.getRegistration).toHaveBeenCalled())
    found.become('redundant')
    expect(await result).toBe('failed')
    expect(reload).not.toHaveBeenCalled()
  })

  it('the newer version never takes over: fails after a while, so the button comes back', async () => {
    vi.useFakeTimers()
    try {
      const reload = vi.fn()
      const waiting = new FakeWorker('installed')
      const { container } = fakeContainer({ waiting })
      const result = updateApp({ container, reload, timeoutMs: 10_000 })
      await vi.advanceTimersByTimeAsync(10_000)
      expect(await result).toBe('failed')
      expect(reload).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })

  it('a second tap while one is working starts nothing new: one check, one listener, one wait', async () => {
    const reload = vi.fn()
    const waiting = new FakeWorker('installed')
    const { container, takeOver } = fakeContainer({ waiting })
    const first = updateApp({ container, reload })
    const second = updateApp({ container, reload })
    await vi.waitFor(() => expect(waiting.posted).toEqual([SKIP_WAITING]))
    expect(container.getRegistration).toHaveBeenCalledTimes(1)
    expect(container.addEventListener).toHaveBeenCalledTimes(1)
    takeOver()
    expect(await first).toBe('reloading')
    expect(await second).toBe('reloading')
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('a takeover after the timeout still reloads, once; a tap after the timeout adds no second listener', async () => {
    vi.useFakeTimers()
    try {
      const reload = vi.fn()
      const waiting = new FakeWorker('installed')
      const { container, takeOver } = fakeContainer({ waiting })
      const first = updateApp({ container, reload, timeoutMs: 10_000 })
      await vi.advanceTimersByTimeAsync(10_000)
      expect(await first).toBe('failed')

      const again = updateApp({ container, reload, timeoutMs: 10_000 })
      await vi.advanceTimersByTimeAsync(0)
      expect(container.addEventListener).toHaveBeenCalledTimes(1)

      takeOver()
      takeOver()
      expect(await again).toBe('reloading')
      expect(reload).toHaveBeenCalledTimes(1)
    } finally {
      vi.useRealTimers()
    }
  })
})
