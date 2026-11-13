import { useSyncExternalStore } from 'react'

/** The main lifts, in the domain's order. */
export type Lift = 'SQUAT' | 'BENCH' | 'DEADLIFT' | 'STRICT_PRESS'
export type Tab = 'today' | 'coach' | 'progress' | 'exercises' | 'plan'
export type Route = { tab: Tab } | { page: 'settings' } | { page: 'lift'; lift: Lift } | { page: 'exercise'; exerciseId: number } | { page: 'planBuilder' }

/** The tabs, in the order the bar shows them. */
export const TABS: readonly Tab[] = ['today', 'coach', 'progress', 'exercises', 'plan']
export const LIFTS: readonly Lift[] = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS']

const TODAY: Route = { tab: 'today' }

export function parseHash(hash: string): Route {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  const [head, arg] = parts
  if (parts.length === 1 && TABS.includes(head as Tab)) return { tab: head as Tab }
  if (parts.length === 1 && head === 'settings') return { page: 'settings' }
  if (parts.length === 2 && head === 'plan' && arg === 'build') return { page: 'planBuilder' }
  if (parts.length === 2 && head === 'lift' && LIFTS.includes(arg as Lift)) return { page: 'lift', lift: arg as Lift }
  if (parts.length === 2 && head === 'exercise' && /^[1-9]\d*$/.test(arg ?? '')) return { page: 'exercise', exerciseId: Number(arg) }
  return TODAY
}

export function routeToHash(route: Route): string {
  if ('tab' in route) return `#/${route.tab}`
  if (route.page === 'settings') return '#/settings'
  if (route.page === 'planBuilder') return '#/plan/build'
  if (route.page === 'exercise') return `#/exercise/${route.exerciseId}`
  return `#/lift/${route.lift}`
}

/** Kept on a history entry: the hash of the page it was opened from inside the app. */
type EntryState = { gritFrom?: string } | null

export function navigate(route: Route): void {
  const hash = routeToHash(route)
  const from = window.location.hash
  if (from === hash) return
  window.location.hash = hash
  // Remember on the new entry where it was opened from, so `goBack` can return there.
  window.history.replaceState({ gritFrom: from }, '')
}

/**
 * Leaves a page for `to`. When the page was opened from `to` inside the app this is one step back in
 * history, so no entry is added and a later swipe back does not return here. Otherwise (a deep link,
 * a cold start) `to` takes this page's place in history.
 */
export function goBack(to: Route): void {
  const hash = routeToHash(to)
  if ((window.history.state as EntryState)?.gritFrom === hash) window.history.back()
  else window.location.replace(hash)
}

/**
 * Leaves a page for wherever it was opened from inside the app (one step back in history), or for `fallback` when it
 * was not opened from inside the app (a deep link, a cold start), which then takes its place in history.
 */
export function leave(fallback: Route): void {
  if ((window.history.state as EntryState)?.gritFrom != null) window.history.back()
  else window.location.replace(routeToHash(fallback))
}

function subscribe(onChange: () => void): () => void {
  window.addEventListener('hashchange', onChange)
  return () => window.removeEventListener('hashchange', onChange)
}

const snapshot = () => window.location.hash

export function useRoute(): Route {
  const hash = useSyncExternalStore(subscribe, snapshot, () => '')
  return parseHash(hash)
}
