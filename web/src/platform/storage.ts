/** True when the browser has promised not to clear Grit's storage on its own. False when it cannot say. */
export async function storageProtected(): Promise<boolean> {
  try {
    return (await navigator.storage?.persisted?.()) === true
  } catch {
    return false
  }
}

const protectionListeners = new Set<() => void>()

/** Calls `cb` when the answer of `storageProtected` may have changed. Returns an unsubscribe. */
export function onStorageProtectionChange(cb: () => void): () => void {
  protectionListeners.add(cb)
  return () => protectionListeners.delete(cb)
}

/**
 * Asks the browser to keep Grit's storage, unless it already does. Called on every launch: Safari
 * says no in a tab and yes once Grit is on the Home Screen. True when storage is protected afterwards.
 */
export async function requestPersistence(): Promise<boolean> {
  let granted = false
  try {
    granted = (await storageProtected()) || (await navigator.storage?.persist?.()) === true
  } catch {
    granted = false
  }
  protectionListeners.forEach((cb) => cb())
  return granted
}

/** True when Grit runs as an installed app (from the Home Screen), not in a browser tab. */
export function isStandalone(): boolean {
  const displayMode = typeof window.matchMedia === 'function' && window.matchMedia('(display-mode: standalone)').matches
  // `navigator.standalone` is iPhone Safari's own flag.
  return displayMode || (navigator as Navigator & { standalone?: boolean }).standalone === true
}

/** Whether a failed write means the phone's storage is full. Dexie wraps the browser's error in its own, or in a bulk error. */
export function isQuotaError(error: unknown, depth = 0): boolean {
  if (typeof error !== 'object' || error === null || depth > 3) return false
  const e = error as { name?: unknown; inner?: unknown; failures?: unknown }
  if (e.name === 'QuotaExceededError') return true
  if (isQuotaError(e.inner, depth + 1)) return true
  return Array.isArray(e.failures) && e.failures.some((failure) => isQuotaError(failure, depth + 1))
}

/** In localStorage: the lifter chose to use Grit in a window that looked private. */
const PRIVATE_ACCEPTED_KEY = 'grit:privateModeAccepted'

/** Whether "Continue anyway" was chosen before. A browser without localStorage has no memory of it. */
export function privateModeAccepted(): boolean {
  try {
    return window.localStorage.getItem(PRIVATE_ACCEPTED_KEY) === '1'
  } catch {
    return false
  }
}

export function acceptPrivateMode(): void {
  try {
    window.localStorage.setItem(PRIVATE_ACCEPTED_KEY, '1')
  } catch {
    // Not remembered: the warning comes back on the next launch.
  }
}

/**
 * Whether this looks like private browsing, where everything stored is thrown away with the tab.
 * No browser says so outright: no IndexedDB at all, or a refusal of private file storage (which
 * Safari and Firefox give only in private windows), is taken as the sign. A browser that takes
 * longer than `timeoutMs` to answer is taken as not private, so the app is never kept waiting.
 */
export async function detectPrivateMode(timeoutMs = 1000): Promise<boolean> {
  if (typeof indexedDB === 'undefined' || indexedDB == null) return true
  const storage = navigator.storage as StorageManager | undefined
  if (typeof storage?.getDirectory !== 'function') return false
  let timer: ReturnType<typeof setTimeout> | undefined
  const refused = storage.getDirectory().then(
    () => false,
    () => true,
  )
  const tooSlow = new Promise<boolean>((done) => {
    timer = setTimeout(() => done(false), timeoutMs)
  })
  try {
    return await Promise.race([refused, tooSlow])
  } catch {
    return false
  } finally {
    clearTimeout(timer)
  }
}
