// Grit: puts the bodybuilding look on before the first paint, from the side remembered at the last visit
// (src/ui/modeHeadStart.ts writes it). Only a head start: the app reads the stored settings and corrects it.
// A plain script, loaded in the head, so it runs before anything is drawn. Kept in step with tokens.css and theme.ts.
try {
  if (localStorage.getItem('grit:trainingMode') === 'BODYBUILDING') {
    document.documentElement.dataset.mode = 'bodybuilding'
    var light = typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: light)').matches
    var meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.setAttribute('content', light ? '#EDF2FA' : '#070B14')
  }
} catch (e) {
  // Storage blocked: the app puts the look on once the settings are read.
}
