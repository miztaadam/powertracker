import { rememberRoute, stripReloadMarker } from './platform/reloadMarker'
import { installHistoryCloses } from './ui/components/historyCloses'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { warmCatalogue } from './state/exercisePicker'
import { installPressHold } from './ui/pressHold'
import { syncThemeColor } from './ui/theme'
import './ui/tokens.css'
// Press feedback for every tappable component (stage 2); its own sheet, after the tokens.
import './ui/press.css'
// Things slide into place (stage 3): the names and timing of the few changes that move.
import './ui/vt.css'

// First: the address the service worker reloaded this window at loses its `u` parameter (and gets back the route this
// window was on) before anything reads it. Then the route is kept from here on, for the next such reload.
stripReloadMarker()
rememberRoute()
syncThemeColor()
// A quick tap still shows its sink (press.css): one listener for the whole page.
installPressHold()
// On an iPhone or iPad (1.17.4), Back closes what is open through history entries of its own: its popstate listener
// goes first, before any screen's. A page reloaded on one of those entries steps off it before anything renders.
// Elsewhere this does nothing and the app renders at once, as before.
const historyReady = installHistoryCloses()

const root = document.getElementById('root')
if (!root) throw new Error('missing #root')
const app = createRoot(root)

// `#/showcase` is the design-system page. It exists only in the dev server: in a production
// build `import.meta.env.DEV` is false, so the import below is dropped from the bundle.
if (import.meta.env.DEV && window.location.hash === '#/showcase') {
  void import('./ui/showcase/Showcase').then(({ default: Showcase }) =>
    app.render(
      <StrictMode>
        <Showcase />
      </StrictMode>,
    ),
  )
} else {
  const start = () => {
    app.render(
      <StrictMode>
        <App />
      </StrictMode>,
    )
    // The catalogue the picker and the Exercises tab open on, worked out once just after the first paint.
    warmCatalogue()
  }
  if (historyReady == null) start()
  else void historyReady.then(start)
}
