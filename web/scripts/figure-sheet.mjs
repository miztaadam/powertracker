// Renders exercise figures to a contact sheet, dark and light, for review: web/.figure-sheets/<label>-dark.png and
// <label>-light.png (not committed). Each figure has its name above it, its thumbnail at the size the Exercises tab
// shows it, and its two panels with their captions underneath; six figures to a row.
//
//   npm run figures:sheet -- all                              every figure drawn so far
//   npm run figures:sheet -- chest                            one body area (its id or its label, any case)
//   npm run figures:sheet -- Bench "Lat pulldown"             exercises by name or alias
//   npm run figures:sheet -- --label batch1 Bench Squat       the sheets are named batch1-dark.png and batch1-light.png
//   npm run figures:sheet -- --columns 3 chest                fewer, larger figures to a row (default 6)
//
// The figures are drawn by the app's own component (src/screens/library/FigureSvg.tsx) and coloured by the app's own
// tokens and role colours (src/ui/tokens.css, src/screens/library/figure.css), so a sheet shows what the app shows.
// Uses the pre-installed Chromium.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'
import react from '@vitejs/plugin-react'
import { createElement } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

const here = dirname(fileURLToPath(import.meta.url))
const web = resolve(here, '..')

/** The colour tokens a picture and its page need. */
const TOKENS = ['--bg', '--surface', '--container-high', '--hairline', '--text', '--text-secondary', '--text-muted', '--accent']

export const escapeHtml = (text) => text.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])

/** The app's colour tokens as CSS declarations, for each theme: tokens.css's dark `:root` block, and its light one over it. */
function themeTokens() {
  const css = readFileSync(resolve(web, 'src/ui/tokens.css'), 'utf8')
  const declarations = (block) => Object.fromEntries(Array.from(block.matchAll(/(--[a-z0-9-]+)\s*:\s*(#[0-9a-fA-F]{6})\s*;/g), (m) => [m[1], m[2]]))
  const dark = declarations(/:root\s*\{([^}]*)\}/.exec(css)[1])
  const light = { ...dark, ...declarations(/@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{([^}]*)\}/.exec(css)[1]) }
  const pick = (theme) =>
    TOKENS.map((token) => {
      if (theme[token] == null) throw new Error(`tokens.css has no ${token}`)
      return `${token}: ${theme[token]};`
    }).join(' ')
  return { dark: pick(dark), light: pick(light) }
}

/**
 * Opens the app's figures for a script: the domain, the body areas, the theme and role CSS, and `svg(name, pose)`,
 * which is the app's own drawing of a pose as SVG markup. Call `close()` when done.
 */
export async function openFigures() {
  // The app's modules are TypeScript: Vite loads them, without the app's own config (no PWA build, no dev server).
  const vite = await createServer({ root: web, configFile: false, plugins: [react()], server: { middlewareMode: true, hmr: false, watch: null }, appType: 'custom', logLevel: 'error', optimizeDeps: { noDiscovery: true } })
  try {
    const { grit } = await vite.ssrLoadModule('/src/domain/index.ts')
    const { FigureSvg } = await vite.ssrLoadModule('/src/screens/library/FigureSvg.tsx')
    const { AREAS } = await vite.ssrLoadModule('/src/screens/library/labels.ts')
    return {
      grit,
      areas: AREAS,
      tokens: themeTokens(),
      roleCss: readFileSync(resolve(web, 'src/screens/library/figure.css'), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '').replace(/\n\s*\n/g, '\n').trim(),
      /** The catalogue's figure for `name`, or null. */
      info: (name) => grit.figureInfo(name) ?? null,
      svg: (name, pose) => {
        const info = grit.figureInfo(name)
        if (info == null) throw new Error(`${name} has no figure`)
        return renderToStaticMarkup(createElement(FigureSvg, { name, pose, viewBox: info.viewBox }))
      },
      close: () => vite.close(),
    }
  } catch (e) {
    await vite.close()
    throw e
  }
}

/** Layout shared by the contact sheet and the pictures page: one figure as a card. */
export const CARD_CSS = `
.card { min-width: 0; }
.card-head { display: flex; align-items: center; gap: 12px; margin: 0 0 8px; }
.card-name { flex: 1; min-width: 0; margin: 0; font: 600 16px/22px system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; color: var(--text); overflow-wrap: anywhere; }
.thumb { flex: none; display: flex; justify-content: center; width: 56px; height: 40px; overflow: hidden; border-radius: 6px; background: var(--container-high); --fig-surface: var(--container-high); }
.thumb .fig { display: block; height: 100%; aspect-ratio: 4 / 3; }
.panels { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.panel { display: flex; flex-direction: column; gap: 4px; min-width: 0; margin: 0; }
.panel .fig { display: block; width: 100%; aspect-ratio: 4 / 3; }
.panel figcaption { font: 400 12px/16px system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; color: var(--text-secondary); text-align: center; overflow-wrap: anywhere; }
`

/** One figure as a card: its name and thumbnail, then its two panels with their captions. */
export function card(figures, name) {
  const info = figures.info(name)
  const panel = (pose, caption) => `<figure class="panel">${figures.svg(name, pose)}<figcaption>${escapeHtml(caption)}</figcaption></figure>`
  return `<article class="card"><div class="card-head"><h3 class="card-name">${escapeHtml(name)}</h3><span class="thumb" title="Thumbnail: the ${info.thumbnail} pose">${figures.svg(name, info.thumbnail)}</span></div><div class="panels">${panel('start', info.startCaption)}${panel('end', info.endCaption)}</div></article>`
}

/** What the arguments ask for: the label, the columns, and the catalogue names to draw. */
function parse(figures, argv) {
  const { grit, areas } = figures
  const asked = []
  let label
  let columns = 6
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--label' || argv[i] === '--columns') {
      const value = argv[++i]
      if (value == null) throw new Error(`${argv[i - 1]} needs a value`)
      if (argv[i - 1] === '--label') label = value
      else columns = Number(value)
    } else asked.push(argv[i])
  }
  if (label != null && !/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(label)) throw new Error(`--label "${label}": letters, digits, dots, dashes and underscores only`)
  if (!Number.isInteger(columns) || columns < 1 || columns > 12) throw new Error('--columns takes a whole number from 1 to 12')
  if (asked.length === 0) throw new Error('Say what to draw: all, a body area, or exercise names. See the top of scripts/figure-sheet.mjs.')

  const names = []
  const skipped = []
  const missing = []
  const add = (name) => {
    if (!names.includes(name)) names.push(name)
  }
  /** A whole group: those drawn so far, with a note of the rest. */
  const addGroup = (entries) => {
    for (const entry of entries) (figures.info(entry.name) != null ? add : (n) => skipped.push(n))(entry.name)
  }
  for (const arg of asked) {
    const key = arg.toLowerCase().replace(/[\s_-]+/g, ' ').trim()
    const area = areas.find((a) => a.id.toLowerCase().replace(/_/g, ' ') === key || a.label.toLowerCase().replace(/[\s_-]+/g, ' ') === key)
    if (key === 'all') addGroup(grit.library())
    else if (area != null) addGroup(grit.library().filter((e) => e.area === area.id))
    else {
      // An exercise, by name, alias or former name. Asked for by name, it has to be there.
      const entry = grit.findInLibrary(arg)
      if (entry == null) missing.push(`"${arg}" is not a body area or a catalogue exercise`)
      else if (figures.info(entry.name) == null) missing.push(`${entry.name} has no figure yet`)
      else add(entry.name)
    }
  }
  if (missing.length > 0) throw new Error(missing.join('\n'))
  if (names.length === 0) throw new Error(`Nothing to draw: none of ${asked.join(', ')} has a figure yet.`)
  if (label == null) {
    const only = asked.length === 1 ? asked[0].toLowerCase().replace(/[\s_-]+/g, ' ').trim() : null
    const area = only == null ? null : areas.find((a) => a.id.toLowerCase().replace(/_/g, ' ') === only || a.label.toLowerCase().replace(/[\s_-]+/g, ' ') === only)
    label = only === 'all' ? 'all' : area != null ? area.id.toLowerCase().replace(/_/g, '-') : 'figures'
  }
  return { label, columns, names, skipped }
}

/** How wide one figure's card is on a sheet, in CSS pixels: two 4:3 panels of 216 × 162. */
const CARD_WIDTH = 440
const GUTTER = 28

function sheetHtml(figures, { label, columns, names }, theme) {
  const across = Math.min(columns, names.length)
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><style>
:root { ${figures.tokens[theme]} }
html, body { margin: 0; background: var(--surface); }
body { width: ${across * CARD_WIDTH + (across + 1) * GUTTER}px; padding: ${GUTTER}px; box-sizing: border-box; }
h1 { margin: 0 0 ${GUTTER}px; font: 600 14px/18px system-ui, sans-serif; color: var(--text-muted); }
.sheet { display: grid; grid-template-columns: repeat(${across}, ${CARD_WIDTH}px); gap: ${GUTTER + 8}px ${GUTTER}px; }
${CARD_CSS}
${figures.roleCss}
</style></head><body><h1>${escapeHtml(label)} · ${names.length} ${names.length === 1 ? 'figure' : 'figures'} · ${theme}</h1><div class="sheet">${names.map((name) => card(figures, name)).join('')}</div></body></html>`
}

async function main() {
  const figures = await openFigures()
  let browser
  try {
    const request = parse(figures, process.argv.slice(2))
    const out = resolve(web, '.figure-sheets')
    mkdirSync(out, { recursive: true })
    const executablePath = existsSync('/opt/pw-browsers/chromium') ? '/opt/pw-browsers/chromium' : undefined
    browser = await chromium.launch({ executablePath })
    // Sharp enough to judge a thumbnail on; a very long sheet is drawn at plain size so it stays a usable image.
    const rows = Math.ceil(request.names.length / request.columns)
    const page = await browser.newPage({ viewport: { width: 800, height: 600 }, deviceScaleFactor: rows > 12 ? 1 : 2 })
    for (const theme of ['dark', 'light']) {
      await page.setContent(sheetHtml(figures, request, theme))
      const file = resolve(out, `${request.label}-${theme}.png`)
      writeFileSync(file, await page.locator('body').screenshot({ type: 'png' }))
      console.log(file)
    }
    console.log(`${request.names.length} ${request.names.length === 1 ? 'figure' : 'figures'}: ${request.names.join(', ')}`)
    // Named in full only while the list is short enough to read.
    if (request.skipped.length > 0) console.log(`Not drawn yet, so left out: ${request.skipped.length}${request.skipped.length <= 30 ? ` (${request.skipped.join(', ')})` : ''}`)
  } finally {
    await browser?.close()
    await figures.close()
  }
}

if (process.argv[1] != null && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((e) => {
    console.error(e instanceof Error ? e.message : e)
    process.exit(1)
  })
}
