/** The parameter public/sw-reload-clients.js adds to a window's address when it reloads it after an update. */
export const RELOAD_PARAM = 'u'

/** Session-storage key: the # route this window is on, so a reload by the worker can put it back there. */
export const ROUTE_KEY = 'grit:route'

type Session = Pick<Storage, 'getItem' | 'setItem'>

function sessionOrNull(): Session | null {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

/**
 * Takes the service worker's `u` parameter out of the address, before anything reads it: the address bar, a
 * bookmark and an installed app's address stay as they were. Path, other parameters and the # route are kept, and
 * the history entry is replaced, not added.
 *
 * The worker sends a window to the address it loaded at (its `client.url`; Chromium does not follow later # changes),
 * so when `u` is there the window goes back to the route it last had (`rememberRoute`), if it kept one. A window from
 * before 1.17 kept none and stays where it was sent. Arguments are for tests.
 */
export function stripReloadMarker(
  location: { href: string } = window.location,
  history: Pick<History, 'state' | 'replaceState'> = window.history,
  session: Session | null = sessionOrNull(),
): void {
  const url = new URL(location.href)
  if (!url.searchParams.has(RELOAD_PARAM)) return
  url.searchParams.delete(RELOAD_PARAM)
  let route: string | null = null
  try {
    route = session?.getItem(ROUTE_KEY) ?? null
  } catch {
    // Storage blocked: the address the worker sent is kept.
  }
  const hash = route != null && route.startsWith('#/') ? route : url.hash
  history.replaceState(history.state, '', `${url.pathname}${url.search}${hash}`)
}

/** Keeps the window's current # route in its session storage, now and on every change. Arguments are for tests. */
export function rememberRoute(
  location: { hash: string } = window.location,
  session: Session | null = sessionOrNull(),
  onChange: (cb: () => void) => void = (cb) => window.addEventListener('hashchange', cb),
): void {
  const keep = () => {
    try {
      session?.setItem(ROUTE_KEY, location.hash)
    } catch {
      // Storage blocked or full: a reload by the worker lands on the address it loaded at.
    }
  }
  keep()
  onChange(keep)
}
