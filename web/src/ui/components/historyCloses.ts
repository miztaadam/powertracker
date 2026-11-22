// Back on an iPhone or iPad (1.17.4): a swipe back, or Back in an installed app, closes the open dialog, sheet or ⋮ menu
// instead of leaving the screen. Safari has no CloseWatcher, so the only way is a history entry of our own: while
// something is open, the page has one extra entry (same address) per open thing, marked with its depth. A Back press pops
// one, and this module dismisses the newest open thing, the same one Escape would.
//
// Off iPhone and iPad (`installHistoryCloses` not called, or called elsewhere), nothing here is active: every function
// is a synchronous pass-through (`afterDialogEntries(fn)` runs `fn` at once, `historyBack()` is `history.back()`,
// `holdHistoryEntry` does nothing), so Android (CloseWatcher, closeRequests.ts) and desktop are exactly as before.
//
// The rules it keeps:
// - A count, not an entry per dialog: things close out of order, and history can only lose its top entry. After any
//   change the entries are brought to the number of open things (`reconcile`), a microtask later, so a close and an
//   open in the same tick make no history call at all.
// - Its own steps back (dropping entries) are told from the lifter's by a count of steps it has asked for. Those
//   popstate events are stopped here, first in line (the listener is added before the app renders), so the screens that
//   keep entries of their own (Templates' draft, the check-in's step 2) never see them. A Back that pops one of our
//   entries is stopped here as well: it dismisses, and nothing else.
// - Every history call of the app goes through `afterDialogEntries`: if entries are on the stack, they are dropped
//   first, then the queued calls run in order (reading `history.state` only then), then whatever is still open gets
//   its entries again on top of where the app now is.
// - An entry with nothing open under it (a reload on it, Forward after a Back-close, a Back onto one left behind by a
//   tab change) is stepped off at once; at start-up, before the app renders.

import { flushSync } from 'react-dom'
import { isIos, type DeviceNavigator } from '../../platform/device'

/** The key on our entries' state; its value is the entry's depth, 1 for the first thing open. */
const DEPTH = 'gritDialog'

/** How long an expected popstate may take before the count is taken again from where the page is. */
const POP_TIMEOUT_MS = 1500

type Held = { onClose: () => void }

let active = false
/** What is open, oldest first. */
const open: Held[] = []
/** The depth of the entry the page was on after the last pop or push this module knows of. */
let lastDepth = 0
/** Steps back this module asked for, not yet arrived. Their popstate is stopped here. */
let ownPending = 0
/** Steps back the app asked for through `historyBack`, not yet arrived. Their popstate goes on to the app. */
let appPending = 0
/** History calls of the app waiting for our entries to be dropped, in the order they were made. */
const queue: (() => void)[] = []
let draining = false
let scheduled = false
let timer: ReturnType<typeof setTimeout> | undefined
let started: (() => void) | null = null

const depthOf = (state: unknown): number => {
  const depth = (state as Record<string, unknown> | null)?.[DEPTH]
  return typeof depth === 'number' && depth > 0 ? depth : 0
}
const here = () => depthOf(window.history.state)
const settled = () => ownPending === 0 && appPending === 0

function expectPop(own: boolean) {
  if (own) ownPending++
  else appPending++
  clearTimeout(timer)
  // A step that never arrives (a browser's own timing, or nothing to go back to) must not hold the app's calls forever.
  timer = setTimeout(() => {
    ownPending = 0
    appPending = 0
    lastDepth = here()
    afterSettle()
  }, POP_TIMEOUT_MS)
}

function stepBack(by: number) {
  expectPop(true)
  window.history.go(-by)
}

/** Brings our entries to the number of open things, when nothing else is moving. */
function reconcile() {
  if (!active || !settled() || queue.length > 0) return
  const at = here()
  const want = open.length
  if (at < want) {
    for (let depth = at + 1; depth <= want; depth++) window.history.pushState({ [DEPTH]: depth }, '')
    lastDepth = want
  } else if (at > want) {
    stepBack(at - want)
  }
}

function scheduleReconcile() {
  if (scheduled) return
  scheduled = true
  queueMicrotask(() => {
    scheduled = false
    reconcile()
  })
}

function drain() {
  draining = true
  try {
    while (queue.length > 0 && settled()) queue.shift()!()
  } finally {
    draining = false
  }
}

function afterSettle() {
  if (!settled()) return
  clearTimeout(timer)
  if (started) {
    const go = started
    started = null
    go()
  }
  drain()
  reconcile()
}

function onPopState(e: PopStateEvent) {
  const before = lastDepth
  const landed = depthOf(e.state)
  lastDepth = landed
  if (ownPending > 0) {
    // Ours: the app never sees it.
    ownPending--
    e.stopImmediatePropagation()
    afterSettle()
    return
  }
  if (appPending > 0) {
    // The app's own step back: it goes on to the app's listeners, and the queue moves on once they have run.
    appPending--
    if (settled()) clearTimeout(timer)
    setTimeout(afterSettle, 0)
    return
  }
  if (landed < before) {
    // The lifter's Back popped our entries: dismiss that many, newest first, and nothing else sees it. Each dismiss is
    // committed now, so the count below is what is really still open (the picker stepping back from a muscle stays).
    e.stopImmediatePropagation()
    for (let i = before - landed; i > 0; i--) {
      const top = open.at(-1)
      if (top == null) break
      flushSync(() => top.onClose())
    }
    reconcile()
    return
  }
  if (landed > before) {
    // Onto one of our entries with nothing open on it (Forward after a Back-close, or a Back onto one left behind):
    // step off it.
    e.stopImmediatePropagation()
    reconcile()
  }
}

/** A tab or other in-page link tapped while our entries are on the stack (a ⋮ menu is open; it is not modal): the
 * entries go first, then the link, so they are never buried under the new screen. */
function onClickCapture(e: MouseEvent) {
  if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
  if (here() === 0 && settled() && queue.length === 0) return
  const link = (e.target as Element | null)?.closest?.('a[href^="#"]')
  const href = link?.getAttribute('href')
  if (href == null) return
  e.preventDefault()
  afterDialogEntries(() => {
    window.location.hash = href
  })
}

/**
 * Turns the iPhone path on, on an iPhone or iPad only; first thing at start-up, before the app renders, so its
 * popstate listener runs before any screen's. Returns a promise when the page opened on one of our entries (a reload
 * with something open): render once it resolves, so no screen reads that entry's state. Otherwise null: render now.
 */
export function installHistoryCloses(nav: DeviceNavigator = navigator): Promise<void> | null {
  if (active || !isIos(nav)) return null
  active = true
  window.addEventListener('popstate', onPopState)
  document.addEventListener('click', onClickCapture, true)
  lastDepth = here()
  if (lastDepth === 0) return null
  return new Promise((resolve) => {
    started = resolve
    stepBack(lastDepth)
  })
}

/** Whether the iPhone path is on in this page. */
export function historyClosesActive(): boolean {
  return active
}

/**
 * Registers something open (a dialog or a ⋮ menu) for Back, newest on top. Returns the undo, for when it closes or
 * unmounts. Does nothing off iPhone and iPad.
 */
export function holdHistoryEntry(onClose: () => void): () => void {
  if (!active) return () => {}
  const held = { onClose }
  open.push(held)
  scheduleReconcile()
  return () => {
    const at = open.indexOf(held)
    if (at < 0) return
    open.splice(at, 1)
    scheduleReconcile()
  }
}

/**
 * Runs a history call of the app (or anything that reads `history.state` to decide one) once our entries are off the
 * stack, after any calls queued before it. Off iPhone and iPad, or with no entries, it runs at once.
 */
export function afterDialogEntries(fn: () => void): void {
  if (!active || draining || (queue.length === 0 && settled() && here() === 0)) {
    fn()
    return
  }
  queue.push(fn)
  if (!settled()) return
  const at = here()
  if (at > 0) stepBack(at)
  else afterSettle()
}

/** `history.back()` for the app, through `afterDialogEntries`, so this module knows the step is the app's own. */
export function historyBack(): void {
  afterDialogEntries(() => {
    if (active) expectPop(false)
    window.history.back()
  })
}

/** Back to the state before `installHistoryCloses`, for tests. */
export function resetHistoryCloses(): void {
  if (active) {
    window.removeEventListener('popstate', onPopState)
    document.removeEventListener('click', onClickCapture, true)
  }
  active = false
  open.length = 0
  queue.length = 0
  lastDepth = 0
  ownPending = 0
  appPending = 0
  draining = false
  scheduled = false
  started = null
  clearTimeout(timer)
}
