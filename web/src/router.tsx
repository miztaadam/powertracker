import { useSyncExternalStore } from 'react'

export type Lift = 'SQUAT' | 'BENCH' | 'DEADLIFT'
export type Tab = 'today' | 'coach' | 'progress' | 'plan'
export type Route = { tab: Tab } | { page: 'settings' } | { page: 'lift'; lift: Lift }

export const TABS: readonly Tab[] = ['today', 'coach', 'progress', 'plan']
const LIFTS: readonly Lift[] = ['SQUAT', 'BENCH', 'DEADLIFT']

const TODAY: Route = { tab: 'today' }

export function parseHash(hash: string): Route {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  const [head, arg] = parts
  if (parts.length === 1 && TABS.includes(head as Tab)) return { tab: head as Tab }
  if (parts.length === 1 && head === 'settings') return { page: 'settings' }
  if (parts.length === 2 && head === 'lift' && LIFTS.includes(arg as Lift)) return { page: 'lift', lift: arg as Lift }
  return TODAY
}

export function routeToHash(route: Route): string {
  if ('tab' in route) return `#/${route.tab}`
  if (route.page === 'settings') return '#/settings'
  return `#/lift/${route.lift}`
}

export function navigate(route: Route): void {
  const hash = routeToHash(route)
  if (window.location.hash !== hash) window.location.hash = hash
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
