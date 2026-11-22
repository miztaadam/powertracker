// Back on an Android phone (and Escape) as a "close request" (1.17.4), through the browser's CloseWatcher API.
//
// One owner for the whole app: while any dialog, sheet or ⋮ menu is open, exactly one CloseWatcher is alive, and its
// close event goes to the newest of them only. A watcher per dialog would not do: the browser puts watchers made with no
// user activation between them into one group, and one Back closes a whole group (seen in Chromium on a probe page:
// three watchers made at load, and one Escape closed two of them). With a single live watcher there is never a group of
// more than one.
//
// A watcher is spent once it has fired close. If something is still open afterwards (a dialog whose dismiss left it
// open, or the one under it), a fresh one is made straight away, so the next Back has one too.
//
// Where the API does not exist (Safari, jsdom), `closeRequestsSupported()` is false and nothing here is used: Dialog
// and Menu keep their Escape keydown handling exactly as before. On an iPhone or iPad it is false even once Safari has
// the API: there Back goes through a history entry instead (historyCloses.ts), whatever the browser offers.

import { isIos } from '../../platform/device'

/** The part of the API used here (TypeScript's DOM lib does not have it yet). */
type Watcher = {
  addEventListener(type: 'close', listener: () => void): void
  destroy(): void
}
type WatcherConstructor = new () => Watcher

function watcherConstructor(): WatcherConstructor | undefined {
  return (globalThis as { CloseWatcher?: WatcherConstructor }).CloseWatcher
}

/** Whether close requests come through CloseWatcher here. Read when a dialog or menu opens, not once at load. */
export function closeRequestsSupported(): boolean {
  return typeof watcherConstructor() === 'function' && !isIos()
}

/** What is open, oldest first; a close request goes to the last one. */
const open: { onClose: () => void }[] = []
let live: Watcher | null = null

function arm() {
  const Make = watcherConstructor()
  if (live != null || open.length === 0 || Make == null) return
  const watcher = new Make()
  watcher.addEventListener('close', () => {
    // Spent: the browser has already let go of it.
    if (live === watcher) live = null
    open.at(-1)?.onClose()
    // Whatever is still open (React unmounts the closed one later) gets a watcher now; one that then closes takes it
    // down with it in `release`.
    arm()
  })
  live = watcher
}

/**
 * Registers something open (a dialog or a menu) for close requests, newest on top. `onClose` is read at the time of
 * the request. Returns the undo, to call when it closes or unmounts.
 */
export function holdCloseRequests(onClose: () => void): () => void {
  const entry = { onClose }
  open.push(entry)
  arm()
  return () => {
    const at = open.indexOf(entry)
    if (at >= 0) open.splice(at, 1)
    if (open.length === 0 && live != null) {
      live.destroy()
      live = null
    }
  }
}
