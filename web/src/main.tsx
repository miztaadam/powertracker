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

syncThemeColor()
// A quick tap still shows its sink (press.css): one listener for the whole page.
installPressHold()

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
  app.render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
  // The catalogue the picker and the Exercises tab open on, worked out once just after the first paint.
  warmCatalogue()
}
