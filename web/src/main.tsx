import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { prepareThumbnails } from './screens/library/ExercisePicture'
import { syncThemeColor } from './ui/theme'
import './ui/tokens.css'

syncThemeColor()

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
  // The Exercises tab's thumbnails, worked out while the app is idle, so the tab opens without working them out.
  prepareThumbnails()
}
