// Writes docs/library/pictures.html: every exercise picture on one self-contained page (inline SVG, no
// scripts or files to fetch), grouped by body area, with a dark and light switch. It is the page the owner reads
// through to flag anything that looks odd. Run: npm run figures:page
//
// The figures are drawn by the app's own component and coloured by the app's own tokens (see figure-sheet.mjs).
import { writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { CARD_CSS, card, escapeHtml, openFigures } from './figure-sheet.mjs'

const here = dirname(fileURLToPath(import.meta.url))
const out = resolve(here, '../../docs/library/pictures.html')

const figures = await openFigures()
try {
  const catalogue = figures.grit.library()
  // Every catalogue exercise has a figure (domain FiguresTest.everyCatalogueExercise_hasFigure); the page shows them all.
  const without = catalogue.filter((e) => figures.info(e.name) == null)
  if (without.length > 0) throw new Error(`No figure for: ${without.map((e) => e.name).join(', ')}`)
  const sections = figures.areas
    .map((area) => {
      const inArea = catalogue.filter((e) => e.area === area.id)
      const id = area.id.toLowerCase().replace(/_/g, '-')
      return {
        id,
        label: area.label,
        count: inArea.length,
        html: `<section aria-labelledby="${id}"><h2 id="${id}">${escapeHtml(area.label)} <span class="count">${inArea.length}</span></h2>${
          inArea.length > 0 ? `<div class="cards">${inArea.map((e) => card(figures, e.name)).join('\n')}</div>` : ''
        }</section>`,
      }
    })

  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light dark">
<title>Grit exercise pictures</title>
<style>
:root { color-scheme: dark; ${figures.tokens.dark} }
@media (prefers-color-scheme: light) { :root:not([data-theme="dark"]) { color-scheme: light; ${figures.tokens.light} } }
:root[data-theme="light"] { color-scheme: light; ${figures.tokens.light} }
* { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; }
body { margin: 0; background: var(--bg); color: var(--text); font: 16px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
main { max-width: 76rem; margin: 0 auto; padding: 16px; }
h1, h2 { line-height: 1.25; overflow-wrap: anywhere; }
h1 { font-size: 1.6rem; margin: 0; }
h2 { font-size: 1.35rem; margin: 2rem 0 1rem; padding-bottom: .25rem; border-bottom: 1px solid var(--hairline); }
p { color: var(--text-secondary); max-width: 46rem; }
a { color: var(--accent); }
code { font-size: .9em; }
header { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; }
.themes { display: flex; gap: 8px; }
.themes button { min-width: 48px; min-height: 48px; padding: 0 16px; border: 1px solid var(--hairline); border-radius: 10px; background: var(--surface); color: var(--text); font: inherit; cursor: pointer; }
.themes button[aria-pressed="true"] { border-color: var(--accent); background: var(--container-high); }
nav ul { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: 6px 16px; }
.count { color: var(--text-muted); font-size: .9rem; font-weight: 400; }
.cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 320px), 1fr)); gap: 16px; }
.card { padding: 12px 14px 14px; border: 1px solid var(--hairline); border-radius: 14px; background: var(--surface); }
${CARD_CSS.trim()}
${figures.roleCss}
</style>
</head>
<body>
<main>
<header>
<h1>Grit exercise pictures</h1>
<div class="themes" role="group" aria-label="Theme">
<button type="button" data-theme-choice="dark">Dark</button>
<button type="button" data-theme-choice="light">Light</button>
</div>
</header>
<p>Generated from the catalogue by <code>npm run figures:page</code> (in <code>web/</code>). Do not edit by hand. All ${catalogue.length} exercises, by body area. Each picture shows the start and the end of the movement, with the small picture the Exercises tab uses beside the name. Look through and flag anything that looks odd; you are not asked to judge technique. The words are in <a href="guides.html">guides.html</a>.</p>
<nav aria-label="Contents"><ul>
${sections.map((s) => `<li><a href="#${s.id}">${escapeHtml(s.label)}</a> <span class="count">${s.count}</span></li>`).join('\n')}
</ul></nav>
${sections.map((s) => s.html).join('\n')}
</main>
<script>
(() => {
  const root = document.documentElement
  const buttons = document.querySelectorAll('[data-theme-choice]')
  const system = matchMedia('(prefers-color-scheme: light)')
  const show = () => {
    const theme = root.dataset.theme ?? (system.matches ? 'light' : 'dark')
    for (const button of buttons) button.setAttribute('aria-pressed', String(button.dataset.themeChoice === theme))
  }
  for (const button of buttons) button.addEventListener('click', () => { root.dataset.theme = button.dataset.themeChoice; show() })
  system.addEventListener('change', show)
  show()
})()
</script>
</body>
</html>
`
  writeFileSync(out, html)
  console.log(`${out}: ${catalogue.length} exercises`)
} finally {
  await figures.close()
}
