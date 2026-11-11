/** Whether this tab is the one that runs Grit ('primary') or must stand aside for another ('secondary'). */
export type TabRole = 'primary' | 'secondary'

export type SingleTabGuard = {
  /** False when the browser gives no way to tell the other tab to stand aside. */
  canTakeOver: boolean
  /** Makes this tab the one that runs Grit; the other shows "open in another tab". */
  takeOver: () => void
  /** Gives the place up (the tab closes, the app unmounts). */
  dispose: () => void
}

const NAME = 'grit-single-tab'
/** Without Web Locks: how long a tab that is already running Grit has to answer a new one. */
const ANSWER_MS = 250

type Message = { type: 'hello'; id: number } | { type: 'taken' } | { type: 'takeover' } | { type: 'bye' }

/**
 * Keeps Grit to one tab, so two copies never write the same log. Web Locks decide who runs: the lock
 * is held for as long as the tab lives, and a second tab waits in line for it. A BroadcastChannel
 * carries "use Grit here" to the tab that holds it. Without Web Locks the channel alone decides (a
 * new tab asks, a running one answers); without either, every tab runs. `onRole` is called with each
 * change; until its first call the tab should show nothing.
 */
export function singleTabGuard(onRole: (role: TabRole) => void): SingleTabGuard {
  const locks = typeof navigator !== 'undefined' ? navigator.locks : undefined
  const channel = typeof BroadcastChannel === 'function' ? new BroadcastChannel(NAME) : null
  let disposed = false
  let role: TabRole | null = null
  const setRole = (next: TabRole) => {
    if (disposed || role === next) return
    role = next
    onRole(next)
  }
  const post = (message: Message) => {
    try {
      channel?.postMessage(message)
    } catch {
      // A closed channel: the tab is going away.
    }
  }

  if (locks == null && channel == null) {
    setRole('primary')
    return { canTakeOver: false, takeOver: () => {}, dispose: () => (disposed = true) }
  }

  if (locks != null) {
    let release: (() => void) | null = null
    const waiting = new AbortController()
    /** Holds the lock until `release` is called. */
    const hold = () =>
      new Promise<void>((done) => {
        if (disposed) return done()
        release = done
        setRole('primary')
      })
    const queue = () => {
      locks.request(NAME, { signal: waiting.signal }, hold).catch(() => {})
    }
    locks
      .request(NAME, { ifAvailable: true }, (lock) => {
        if (lock != null) return hold()
        setRole('secondary')
        queue()
      })
      // A lock manager that refuses to work must not shut the lifter out.
      .catch(() => setRole('primary'))
    if (channel)
      channel.onmessage = (event: MessageEvent<Message>) => {
        if (event.data?.type !== 'takeover' || release == null) return
        // Hand the lock to the tab that asked (it is first in line), then wait for it in turn.
        release()
        release = null
        setRole('secondary')
        queue()
      }
    return {
      canTakeOver: channel != null,
      takeOver: () => post({ type: 'takeover' }),
      dispose() {
        disposed = true
        waiting.abort()
        release?.()
        channel?.close()
      },
    }
  }

  // The channel alone. The tab with the lowest id wins when several ask at once.
  const id = Date.now() + Math.random()
  let timer: ReturnType<typeof setTimeout> | undefined
  const ask = () => {
    clearTimeout(timer)
    role = null
    post({ type: 'hello', id })
    timer = setTimeout(() => setRole('primary'), ANSWER_MS)
  }
  const standAside = () => {
    clearTimeout(timer)
    setRole('secondary')
  }
  if (channel)
    channel.onmessage = (event: MessageEvent<Message>) => {
      const message = event.data
      if (message?.type === 'hello') {
        if (role === 'primary') post({ type: 'taken' })
        else if (role == null && message.id < id) standAside()
      } else if (message?.type === 'taken') {
        if (role == null) standAside()
      } else if (message?.type === 'takeover') {
        standAside()
      } else if (message?.type === 'bye' && role === 'secondary') {
        // The tab that ran Grit closed. Not all at once, so that waiting tabs do not all take its place.
        timer = setTimeout(ask, Math.random() * ANSWER_MS)
      }
    }
  const bye = () => {
    if (role === 'primary') post({ type: 'bye' })
  }
  window.addEventListener('pagehide', bye)
  ask()
  return {
    canTakeOver: true,
    takeOver() {
      clearTimeout(timer)
      post({ type: 'takeover' })
      setRole('primary')
    },
    dispose() {
      bye()
      disposed = true
      clearTimeout(timer)
      window.removeEventListener('pagehide', bye)
      channel?.close()
    },
  }
}
