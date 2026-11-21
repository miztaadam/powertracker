/** The message vite-plugin-pwa's worker takes as "take over now" (the same one the "Update ready" prompt sends). */
export const SKIP_WAITING = { type: 'SKIP_WAITING' }

/** How long a newer version may take to install and take over before the button is offered again. */
const TIMEOUT_MS = 30_000

export type UpdateResult = 'reloading' | 'failed'

/**
 * Fetches the newest Grit and switches to it, for the older-copy screen (1.17). A plain reload would not do: the
 * service worker would serve the same old copy from its cache. So: ask the server for a newer worker (or take the one
 * already waiting), wait for it to install, tell it to take over, and reload once it is in charge. 'failed' when
 * offline or when no newer version is found; the screen then says so. Without a service worker (the dev server, or a
 * browser without one), a reload is all there is.
 *
 * The new worker also reloads every window it takes over (public/sw-reload-clients.js). This page reloads itself once
 * on `controllerchange` as well; both load the new version, and neither runs again, so they cannot loop.
 */
export async function updateApp({
  container = typeof navigator !== 'undefined' ? navigator.serviceWorker : undefined,
  reload = () => window.location.reload(),
  timeoutMs = TIMEOUT_MS,
}: { container?: ServiceWorkerContainer; reload?: () => void; timeoutMs?: number } = {}): Promise<UpdateResult> {
  const reloadOnce = once(reload)
  if (container == null) return reloadNow(reloadOnce)
  let registration: ServiceWorkerRegistration | undefined
  try {
    registration = await container.getRegistration()
  } catch {
    return reloadNow(reloadOnce)
  }
  // Not served by a worker: nothing old is cached in front of this page.
  if (registration == null || container.controller == null) return reloadNow(reloadOnce)

  let next = registration.waiting
  if (next == null) {
    try {
      await registration.update()
    } catch {
      return 'failed'
    }
    next = registration.waiting ?? registration.installing
  }
  if (next == null) return 'failed'

  return withTimeout(timeoutMs, async () => {
    if (!(await installed(next))) return 'failed'
    const takenOver = new Promise<void>((done) => container.addEventListener('controllerchange', () => done()))
    ;(registration.waiting ?? next).postMessage(SKIP_WAITING)
    await takenOver
    reloadOnce()
    return 'reloading'
  })
}

function reloadNow(reload: () => void): UpdateResult {
  reload()
  return 'reloading'
}

function once(fn: () => void): () => void {
  let done = false
  return () => {
    if (done) return
    done = true
    fn()
  }
}

/** Resolves true once `worker` has installed (or gone on to activate), false if it was thrown away. */
function installed(worker: ServiceWorker): Promise<boolean> {
  const settled = () => (worker.state === 'redundant' ? false : worker.state === 'installing' || worker.state === 'parsed' ? null : true)
  const now = settled()
  if (now != null) return Promise.resolve(now)
  return new Promise((done) => {
    const changed = () => {
      const result = settled()
      if (result == null) return
      worker.removeEventListener('statechange', changed)
      done(result)
    }
    worker.addEventListener('statechange', changed)
  })
}

function withTimeout(ms: number, work: () => Promise<UpdateResult>): Promise<UpdateResult> {
  return new Promise((done) => {
    const timer = setTimeout(() => done('failed'), ms)
    work().then(
      (result) => {
        clearTimeout(timer)
        done(result)
      },
      () => {
        clearTimeout(timer)
        done('failed')
      },
    )
  })
}
