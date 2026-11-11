import { liveQuery } from 'dexie'
import { useCallback, useEffect, useRef, useState } from 'react'
import { registerSW } from 'virtual:pwa-register'
import type { GritDb } from '../db/db'
import { ACTIVITY_KEY, sessionInProgress } from '../state/useToday'

/** vite-plugin-pwa's registration: returns the function that activates the waiting version and reloads. */
export type RegisterSW = (options?: {
  onNeedRefresh?: () => void
  onRegisteredSW?: (url: string, registration: ServiceWorkerRegistration | undefined) => void
  onRegisterError?: (error: unknown) => void
}) => (reloadPage?: boolean) => Promise<void>

export const UPDATE_READY = 'Update ready — tap to refresh'

type UpdateStore = { waiting: boolean; listeners: Set<() => void>; apply: (reloadPage?: boolean) => Promise<void> }

// One registration per page, however often the hook mounts.
const stores = new Map<RegisterSW, UpdateStore>()

function storeFor(register: RegisterSW): UpdateStore {
  const existing = stores.get(register)
  if (existing) return existing
  const store: UpdateStore = { waiting: false, listeners: new Set(), apply: async () => {} }
  stores.set(register, store)
  store.apply = register({
    onNeedRefresh() {
      store.waiting = true
      store.listeners.forEach((cb) => cb())
    },
    onRegisteredSW(_url, registration) {
      if (!registration) return
      // An installed app stays open for days: look for a new version each time it comes to the front.
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') registration.update().catch(() => {})
      })
    },
    // Without a service worker Grit still runs; there is just nothing to update.
    onRegisterError() {},
  })
  return store
}

export type UpdatePrompt = {
  /** A new version is installed and waiting, and no session is being logged: show `UPDATE_READY`. */
  ready: boolean
  /** The lifter tapped the prompt: switch to the new version and reload. Does nothing while a session is open. */
  refresh: () => void
}

/**
 * The service worker fetches a new version of Grit in the background. It is switched to only when
 * the lifter taps the prompt, never on its own, and the prompt stays away while a session is being
 * logged: a reload between sets would be an interruption nobody asked for.
 */
export function useUpdatePrompt(db: GritDb, register: RegisterSW = registerSW): UpdatePrompt {
  const [store] = useState(() => storeFor(register))
  const [waiting, setWaiting] = useState(store.waiting)
  useEffect(() => {
    const changed = () => setWaiting(store.waiting)
    store.listeners.add(changed)
    changed()
    return () => {
      store.listeners.delete(changed)
    }
  }, [store])

  // The note the Today screen keeps while a session is open; undefined until it has been read.
  const [note, setNote] = useState<{ value: unknown }>()
  useEffect(() => {
    // A failed read leaves the prompt hidden: the screens report a broken database.
    const subscription = liveQuery(async () => ({ value: (await db.meta.get(ACTIVITY_KEY))?.value })).subscribe({ next: setNote, error: () => setNote(undefined) })
    return () => subscription.unsubscribe()
  }, [db])

  const ready = waiting && note !== undefined && !sessionInProgress(note.value)
  const allowed = useRef(ready)
  allowed.current = ready
  const refresh = useCallback(() => {
    if (allowed.current) store.apply(true).catch(() => {})
  }, [store])
  return { ready, refresh }
}
