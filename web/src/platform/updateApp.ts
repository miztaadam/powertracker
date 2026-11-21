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
export function updateApp({
  container = typeof navigator !== 'undefined' ? navigator.serviceWorker : undefined,
  reload = () => window.location.reload(),
  timeoutMs = TIMEOUT_MS,
}: { container?: ServiceWorkerContainer; reload?: () => void; timeoutMs?: number } = {}): Promise<UpdateResult> {
  if (container == null) return Promise.resolve(reloadNow(reload))
  // A second tap while one is working joins it: no second check, listener or wait.
  const running = inFlight.get(container)
  if (running != null) return running
  const attempt = tryUpdate(container, reload, timeoutMs).finally(() => inFlight.delete(container))
  inFlight.set(container, attempt)
  return attempt
}

/** The attempt under way, per service worker container. */
const inFlight = new WeakMap<ServiceWorkerContainer, Promise<UpdateResult>>()

/**
 * The newer version taking over, per container: one `controllerchange` listener for the page's life, which reloads it
 * once. It outlives a timed-out attempt, so a takeover that comes late still reloads, and a later tap adds no second one.
 */
const takeovers = new WeakMap<ServiceWorkerContainer, Promise<void>>()

function takeover(container: ServiceWorkerContainer, reload: () => void): Promise<void> {
  let taken = takeovers.get(container)
  if (taken == null) {
    taken = new Promise<void>((done) => container.addEventListener('controllerchange', () => done()))
    taken.then(reload, () => {})
    takeovers.set(container, taken)
  }
  return taken
}

async function tryUpdate(container: ServiceWorkerContainer, reload: () => void, timeoutMs: number): Promise<UpdateResult> {
  let registration: ServiceWorkerRegistration | undefined
  try {
    registration = await container.getRegistration()
  } catch {
    return reloadNow(reload)
  }
  // Not served by a worker: nothing old is cached in front of this page.
  if (registration == null || container.controller == null) return reloadNow(reload)

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
    const takenOver = takeover(container, reload)
    ;(registration.waiting ?? next).postMessage(SKIP_WAITING)
    await takenOver
    return 'reloading'
  })
}

function reloadNow(reload: () => void): UpdateResult {
  reload()
  return 'reloading'
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
