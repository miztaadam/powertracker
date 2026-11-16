// Grit: puts the look on before the first paint. A plain script, loaded in the head, so it runs before anything is drawn.
// - The bodybuilding look, from the side remembered at the last visit (src/ui/modeHeadStart.ts writes it). Only a
//   head start: the app reads the stored settings and corrects it.
// - The browser chrome colour (the one theme-color tag), from the side and the system scheme. Kept in step with
//   tokens.css and src/ui/theme.ts, which keeps it current afterwards.
// - `ios-standalone` on <html> in the iOS Home Screen app, where iOS 26 blurs a band under the status bar and the
//   shell leaves it clear (shell.css). The same checks as isIos (src/platform/device.ts) and isStandalone
//   (src/platform/storage.ts).
// - `data-motion="off"` on <html> while the system asks for reduced motion (kept current when that changes), or
//   while a test run asks: the `grit:motion` key set to `off` (playwright.config.ts sets it for every e2e page, so
//   screenshots and position checks run still). CSS motion rules sit under :root:not([data-motion="off"]).
// Wrapped in a function so that none of its names become globals.
;(function () {
  var bodybuilding = false
  try {
    bodybuilding = localStorage.getItem('grit:trainingMode') === 'BODYBUILDING'
  } catch (e) {
    // Storage blocked: the app puts the look on once the settings are read.
  }
  try {
    var root = document.documentElement
    if (bodybuilding) root.dataset.mode = 'bodybuilding'
    var light = typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: light)').matches
    var meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.setAttribute('content', bodybuilding ? (light ? '#EDF2FA' : '#070B14') : light ? '#F4F4F0' : '#0B0C0E')
    var nav = navigator
    var ios = /iPad|iPhone|iPod/.test(nav.userAgent) || (nav.platform === 'MacIntel' && nav.maxTouchPoints > 1)
    var standalone = (typeof matchMedia === 'function' && matchMedia('(display-mode: standalone)').matches) || nav.standalone === true
    if (ios && standalone) root.classList.add('ios-standalone')
  } catch (e) {
    // Nothing here may stop the page: the app sets the colours again once it runs.
  }
  try {
    var page = document.documentElement
    var testOff = false
    try {
      testOff = localStorage.getItem('grit:motion') === 'off'
    } catch (e) {
      // Storage blocked: only the system setting counts.
    }
    var reduce = typeof matchMedia === 'function' ? matchMedia('(prefers-reduced-motion: reduce)') : null
    var applyMotion = function () {
      if (testOff || (reduce && reduce.matches)) page.setAttribute('data-motion', 'off')
      else page.removeAttribute('data-motion')
    }
    applyMotion()
    if (reduce && typeof reduce.addEventListener === 'function') reduce.addEventListener('change', applyMotion)
    else if (reduce && typeof reduce.addListener === 'function') reduce.addListener(applyMotion)
  } catch (e) {
    // Motion stays on; tokens.css still stops transitions for reduced motion.
  }
})()
