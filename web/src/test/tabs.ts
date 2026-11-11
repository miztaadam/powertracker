type LockOptions = { ifAvailable?: boolean; signal?: AbortSignal }
type LockCallback = (lock: { name: string } | null) => unknown

/** One Web Locks manager shared by the "tabs" of a test: one holder at a time, the rest queue in order. */
export function fakeLocks() {
  let held = false
  const queue: (() => void)[] = []
  const run = async (callback: LockCallback) => {
    held = true
    try {
      return await callback({ name: 'lock' })
    } finally {
      held = false
      queue.shift()?.()
    }
  }
  return {
    request(_name: string, optionsOrCallback: LockOptions | LockCallback, maybeCallback?: LockCallback): Promise<unknown> {
      const options = typeof optionsOrCallback === 'function' ? {} : optionsOrCallback
      const callback = (typeof optionsOrCallback === 'function' ? optionsOrCallback : maybeCallback) as LockCallback
      if (!held) return run(callback)
      if (options.ifAvailable) return Promise.resolve(callback(null))
      return new Promise((resolve, reject) => {
        const start = () => run(callback).then(resolve, reject)
        queue.push(start)
        options.signal?.addEventListener('abort', () => {
          const at = queue.indexOf(start)
          if (at >= 0) queue.splice(at, 1)
          reject(new DOMException('aborted', 'AbortError'))
        })
      })
    },
  }
}

/** A lock manager whose lock another tab holds for good. */
export function locksHeldElsewhere() {
  return {
    request(_name: string, optionsOrCallback: LockOptions | LockCallback, maybeCallback?: LockCallback): Promise<unknown> {
      const options = typeof optionsOrCallback === 'function' ? {} : optionsOrCallback
      const callback = (typeof optionsOrCallback === 'function' ? optionsOrCallback : maybeCallback) as LockCallback
      return options.ifAvailable ? Promise.resolve(callback(null)) : new Promise(() => {})
    },
  }
}

/** A BroadcastChannel class whose instances reach each other (never themselves), a tick later, as the real one does. */
export function fakeChannels() {
  const open = new Set<FakeChannel>()
  class FakeChannel {
    onmessage: ((event: { data: unknown }) => void) | null = null
    constructor(readonly name: string) {
      open.add(this)
    }
    postMessage(data: unknown) {
      for (const other of open) {
        if (other !== this && other.name === this.name) setTimeout(() => open.has(other) && other.onmessage?.({ data }), 0)
      }
    }
    close() {
      open.delete(this)
    }
  }
  return FakeChannel
}
