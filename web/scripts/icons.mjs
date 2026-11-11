// Renders docs/design/logo/b-barbell-grey-icon.svg (flattened, full-bleed, graphite background)
// to the PNGs under public/icons using the pre-installed Chromium. Run: npm run icons
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'

const here = dirname(fileURLToPath(import.meta.url))
const svg = readFileSync(resolve(here, '../../docs/design/logo/b-barbell-grey-icon.svg'), 'utf8')
const out = resolve(here, '../public/icons')
mkdirSync(out, { recursive: true })

// The icon already keeps its glyphs inside the 66/108 safe zone and fills the full square with
// the graphite background, so the same artwork serves as the maskable icon.
const targets = [
  ['apple-touch-icon.png', 180],
  ['icon-192.png', 192],
  ['icon-512.png', 512],
  ['icon-maskable-512.png', 512],
]

const executablePath = existsSync('/opt/pw-browsers/chromium') ? '/opt/pw-browsers/chromium' : undefined
const browser = await chromium.launch({ executablePath })
try {
  for (const [name, size] of targets) {
    const page = await browser.newPage({ viewport: { width: size, height: size } })
    const body = svg.replace('width="108" height="108"', `width="${size}" height="${size}"`)
    await page.setContent(`<style>html,body{margin:0;background:#131518}</style>${body}`)
    writeFileSync(resolve(out, name), await page.screenshot({ type: 'png', clip: { x: 0, y: 0, width: size, height: size } }))
    await page.close()
  }
} finally {
  await browser.close()
}
