// Grit: loaded into the generated service worker (vite.config.ts, workbox `importScripts`).
//
// When a new version of the service worker takes over, every Grit window the previous version controlled is reloaded,
// so no window stays open on old code. A window showing "Grit is open in another tab" or the lock screen never
// registers for updates itself, and without this it could later become the window in use, on old code, beside a log
// written by the new one.
//
// - Controlled windows only (no `includeUncontrolled`, no `clients.claim()`): on a first install no window is
//   controlled yet, so nothing is reloaded, and a first launch is never interrupted while it loads.
// - Once per worker version: `activate` fires once for each new worker, so this cannot loop.
// - A real reload, never a jump within the page: every Grit URL has a #/ route, and a navigation to the same address
//   with only a different (or the same) # part may be taken as a move within the page, which reloads nothing. So each
//   window is sent to its address with a `u` parameter whose value is new on every activation (replacing an earlier
//   one); the path and the # route are kept. The app takes `u` out of the address as it starts (src/platform/
//   reloadMarker.ts), so nothing else ever sees it.
// - The window that tapped "Update ready" reloads itself as well; whichever navigation wins, it loads the new version.
//   A failed or refused navigation is ignored.
// - Not awaited: the navigations' requests wait for this worker to finish activating, so waiting on them here would
//   hold activation up until they time out. Only the lookup of the windows is awaited.
// - Where a browser has no `client.navigate`, nothing happens here and those windows keep the guard in the app
//   (the "older than your log" screen) for protection.
self.addEventListener('activate', function (event) {
  var stamp = Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then(function (windows) {
      windows.forEach(function (client) {
        if (typeof client.navigate !== 'function') return
        try {
          var url = new URL(client.url)
          url.searchParams.set('u', stamp)
          client.navigate(url.href).catch(function () {})
        } catch (e) {
          // Refused outright: that window stays as it is.
        }
      })
    }),
  )
})
