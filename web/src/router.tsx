import { useSyncExternalStore } from 'react'
import type { TrainingMode } from './db/schema'
import { afterDialogEntries, historyBack } from './ui/components/historyCloses'

/** The main lifts, in the domain's order. */
export type Lift = 'SQUAT' | 'BENCH' | 'DEADLIFT' | 'STRICT_PRESS'
export type Tab = 'today' | 'coach' | 'progress' | 'exercises' | 'plan'
export type Route = { tab: Tab } | { page: 'settings' } | { page: 'lift'; lift: Lift } | { page: 'exercise'; exerciseId: number } | { page: 'planBuilder' } | { page: 'templates'; mode: TrainingMode } | { page: 'checkIn' } | { page: 'guide' }

/** The tabs, in the order the bar shows them. */
export const TABS: readonly Tab[] = ['today', 'coach', 'progress', 'exercises', 'plan']
export const LIFTS: readonly Lift[] = ['SQUAT', 'BENCH', 'DEADLIFT', 'STRICT_PRESS']

const TODAY: Route = { tab: 'today' }

export function parseHash(hash: string): Route {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  const [head, arg] = parts
  if (parts.length === 1 && TABS.includes(head as Tab)) return { tab: head as Tab }
  if (parts.length === 1 && head === 'settings') return { page: 'settings' }
  if (parts.length === 1 && head === 'checkin') return { page: 'checkIn' }
  // The guide (1.16): full screen, no tab bar. Nothing opens it by itself yet.
  if (parts.length === 1 && head === 'guide') return { page: 'guide' }
  if (parts.length === 2 && head === 'plan' && arg === 'build') return { page: 'planBuilder' }
  // The templates of one side (1.14): #/plan/templates/powerlifting or #/plan/templates/bodybuilding.
  if (parts.length === 3 && head === 'plan' && arg === 'templates' && (parts[2] === 'powerlifting' || parts[2] === 'bodybuilding'))
    return { page: 'templates', mode: parts[2] === 'bodybuilding' ? 'BODYBUILDING' : 'POWERLIFTING' }
  if (parts.length === 2 && head === 'lift' && LIFTS.includes(arg as Lift)) return { page: 'lift', lift: arg as Lift }
  if (parts.length === 2 && head === 'exercise' && /^[1-9]\d*$/.test(arg ?? '')) return { page: 'exercise', exerciseId: Number(arg) }
  return TODAY
}

export function routeToHash(route: Route): string {
  if ('tab' in route) return `#/${route.tab}`
  if (route.page === 'settings') return '#/settings'
  if (route.page === 'planBuilder') return '#/plan/build'
  if (route.page === 'templates') return `#/plan/templates/${route.mode === 'BODYBUILDING' ? 'bodybuilding' : 'powerlifting'}`
  if (route.page === 'checkIn') return '#/checkin'
  if (route.page === 'guide') return '#/guide'
  if (route.page === 'exercise') return `#/exercise/${route.exerciseId}`
  return `#/lift/${route.lift}`
}

/** Kept on a history entry: the hash of the page it was opened from inside the app. */
type EntryState = { gritFrom?: string } | null

// Each of these runs through `afterDialogEntries` (1.17.4): on an iPhone or iPad, once the entries kept for open dialogs
// and menus are off the stack, so `history.state` is the page's own; elsewhere, at once.

export function navigate(route: Route): void {
  afterDialogEntries(() => {
    const hash = routeToHash(route)
    const from = window.location.hash
    if (from === hash) return
    window.location.hash = hash
    // Remember on the new entry where it was opened from, so `goBack` can return there.
    window.history.replaceState({ gritFrom: from }, '')
  })
}

/**
 * Leaves a page for `to`. When the page was opened from `to` inside the app this is one step back in
 * history, so no entry is added and a later swipe back does not return here. Otherwise (a deep link,
 * a cold start) `to` takes this page's place in history.
 */
export function goBack(to: Route): void {
  afterDialogEntries(() => {
    const hash = routeToHash(to)
    if ((window.history.state as EntryState)?.gritFrom === hash) historyBack()
    else window.location.replace(hash)
  })
}

/**
 * Leaves a page for wherever it was opened from inside the app (one step back in history), or for `fallback` when it
 * was not opened from inside the app (a deep link, a cold start), which then takes its place in history.
 */
export function leave(fallback: Route): void {
  afterDialogEntries(() => {
    if ((window.history.state as EntryState)?.gritFrom != null) historyBack()
    else window.location.replace(routeToHash(fallback))
  })
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
