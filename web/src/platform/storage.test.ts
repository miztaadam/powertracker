import Dexie from 'dexie'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { detectPrivateMode, isQuotaError, isStandalone, onStorageProtectionChange, requestPersistence, storageProtected } from './storage'

const setStorage = (value: unknown) => Object.defineProperty(navigator, 'storage', { value, configurable: true })

afterEach(() => {
  setStorage(undefined)
  vi.unstubAllGlobals()
  Object.defineProperty(navigator, 'standalone', { value: undefined, configurable: true })
})

describe('requestPersistence', () => {
  it('asks the browser and tells listeners to read the status again', async () => {
    let granted = false
    const persist = vi.fn(async () => (granted = true))
    setStorage({ persist, persisted: async () => granted })
    const changed = vi.fn()
    const stop = onStorageProtectionChange(changed)

    expect(await storageProtected()).toBe(false)
    expect(await requestPersistence()).toBe(true)

    expect(persist).toHaveBeenCalledTimes(1)
    expect(changed).toHaveBeenCalledTimes(1)
    expect(await storageProtected()).toBe(true)
    stop()
  })

  it('does not ask again once storage is protected', async () => {
    const persist = vi.fn(async () => true)
    setStorage({ persist, persisted: async () => true })
    expect(await requestPersistence()).toBe(true)
    expect(persist).not.toHaveBeenCalled()
  })

  it('is false when the browser refuses, cannot say or throws', async () => {
    setStorage({ persist: async () => false, persisted: async () => false })
    expect(await requestPersistence()).toBe(false)
    setStorage(undefined)
    expect(await requestPersistence()).toBe(false)
    setStorage({
      persisted: async () => false,
      persist: async () => {
        throw new Error('no')
      },
    })
    expect(await requestPersistence()).toBe(false)
  })
})

describe('isStandalone', () => {
  it('is true only for an installed app', () => {
    expect(isStandalone()).toBe(false)
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query === '(display-mode: standalone)' }))
    expect(isStandalone()).toBe(true)
    vi.stubGlobal('matchMedia', () => ({ matches: false }))
    expect(isStandalone()).toBe(false)
    // iPhone Safari's own flag for a Home Screen app.
    Object.defineProperty(navigator, 'standalone', { value: true, configurable: true })
    expect(isStandalone()).toBe(true)
  })
})

describe('isQuotaError', () => {
  it('knows the browser error and the ones Dexie wraps it in', () => {
    const quota = new DOMException('full', 'QuotaExceededError')
    expect(isQuotaError(quota)).toBe(true)
    expect(isQuotaError(new Dexie.QuotaExceededError('full'))).toBe(true)
    expect(isQuotaError({ name: 'AbortError', inner: quota })).toBe(true)
    expect(isQuotaError(new Dexie.BulkError('bulk', { 0: quota }))).toBe(true)
    expect(isQuotaError(new Error('other'))).toBe(false)
    expect(isQuotaError(null)).toBe(false)
  })
})

describe('detectPrivateMode', () => {
  it('is false in an ordinary browser', async () => {
    expect(await detectPrivateMode()).toBe(false)
    setStorage({ getDirectory: async () => ({}) })
    expect(await detectPrivateMode()).toBe(false)
  })

  it('is true when the browser refuses private file storage, as private browsing does', async () => {
    setStorage({ getDirectory: async () => Promise.reject(new DOMException('no', 'SecurityError')) })
    expect(await detectPrivateMode()).toBe(true)
  })

  it('is false when the browser does not answer in time', async () => {
    setStorage({ getDirectory: () => new Promise(() => {}) })
    expect(await detectPrivateMode(20)).toBe(false)
  })

  it('is true without IndexedDB', async () => {
    vi.stubGlobal('indexedDB', undefined)
    expect(await detectPrivateMode()).toBe(true)
  })
})
