import { afterEach, describe, expect, it, vi } from 'vitest'
import { releaseWakeLock, requestWakeLock } from './wakeLock'

class FakeSentinel extends EventTarget {
  release = vi.fn(async () => {
    this.dispatchEvent(new Event('release'))
  })
}

function stubWakeLock(request: () => Promise<FakeSentinel>) {
  const spy = vi.fn(request)
  Object.defineProperty(navigator, 'wakeLock', { configurable: true, value: { request: spy } })
  return spy
}

afterEach(async () => {
  await releaseWakeLock()
  Reflect.deleteProperty(navigator, 'wakeLock')
})

describe('wake lock', () => {
  it('is a no-op where the API is missing', async () => {
    expect('wakeLock' in navigator).toBe(false)
    await expect(requestWakeLock()).resolves.toBeUndefined()
    await expect(releaseWakeLock()).resolves.toBeUndefined()
  })

  it('asks for a screen lock once and lets it go', async () => {
    const sentinel = new FakeSentinel()
    const request = stubWakeLock(async () => sentinel)
    await requestWakeLock()
    await requestWakeLock()
    expect(request).toHaveBeenCalledExactlyOnceWith('screen')
    await releaseWakeLock()
    expect(sentinel.release).toHaveBeenCalledOnce()
    await releaseWakeLock()
    expect(sentinel.release).toHaveBeenCalledOnce()
  })

  it('asks again after the system took the lock away', async () => {
    const request = stubWakeLock(async () => new FakeSentinel())
    await requestWakeLock()
    const first = (await request.mock.results[0]!.value) as FakeSentinel
    // The page was hidden: the browser releases the lock itself.
    first.dispatchEvent(new Event('release'))
    await requestWakeLock()
    expect(request).toHaveBeenCalledTimes(2)
  })

  it('a refused request is swallowed', async () => {
    stubWakeLock(() => Promise.reject(new DOMException('low power mode', 'NotAllowedError')))
    await expect(requestWakeLock()).resolves.toBeUndefined()
  })

  it('a lock granted after it was let go is released straight away', async () => {
    const sentinel = new FakeSentinel()
    let grant: (s: FakeSentinel) => void = () => {}
    stubWakeLock(() => new Promise<FakeSentinel>((done) => (grant = done)))
    const asked = requestWakeLock()
    await releaseWakeLock()
    grant(sentinel)
    await asked
    expect(sentinel.release).toHaveBeenCalledOnce()
  })
})
