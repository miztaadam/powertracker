import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const css = readFileSync(resolve(process.cwd(), 'src/ui/tokens.css'), 'utf8')

function block(source: string, open: RegExp): Record<string, string> {
  const m = open.exec(source)
  if (!m) throw new Error(`block not found: ${open}`)
  let depth = 1
  let i = m.index + m[0].length
  const start = i
  while (depth > 0 && i < source.length) {
    const c = source[i++]
    if (c === '{') depth++
    else if (c === '}') depth--
  }
  const vars: Record<string, string> = {}
  for (const d of source.slice(start, i - 1).matchAll(/(--[a-z0-9-]+)\s*:\s*(#[0-9a-fA-F]{6})\s*;/g)) {
    vars[d[1]!] = d[2]!
  }
  return vars
}

const dark = block(css, /:root\s*\{/)
const light = { ...dark, ...block(css, /@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{/) }

function luminance(hex: string): number {
  const ch = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
  const [r, g, b] = ch.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)) as [number, number, number]
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function ratio(a: string, b: string): number {
  const la = luminance(a) + 0.05
  const lb = luminance(b) + 0.05
  return Math.max(la, lb) / Math.min(la, lb)
}

const surfaces = ['--bg', '--surface', '--container', '--container-high', '--container-highest']
const texts = ['--text', '--text-secondary', '--text-muted', '--accent-text', '--success', '--warning', '--danger', '--info']
const pairs: [string, string][] = [
  ['--accent-ink', '--accent'],
  ['--accent-on-container', '--accent-container'],
  ['--success', '--success-container'],
  ['--warning', '--warning-container'],
  ['--danger', '--danger-container'],
  ['--info', '--info-container'],
  ['--text', '--warning-container'],
  ['--text', '--danger-container'],
]

describe.each([
  ['dark', dark],
  ['light', light],
] as const)('%s tokens', (_name, theme) => {
  const get = (k: string): string => {
    const v = theme[k]
    if (!v) throw new Error(`missing token ${k}`)
    return v
  }
  for (const s of surfaces) {
    for (const t of texts) {
      it(`${t} on ${s} is at least 4.5:1`, () => {
        expect(ratio(get(t), get(s))).toBeGreaterThanOrEqual(4.5)
      })
    }
  }
  for (const [fg, bg] of pairs) {
    it(`${fg} on ${bg} is at least 4.5:1`, () => {
      expect(ratio(get(fg), get(bg))).toBeGreaterThanOrEqual(4.5)
    })
  }
})

describe('token set', () => {
  it('matches the DESIGN.md anchors', () => {
    expect(dark['--bg']).toBe('#0B0C0E')
    expect(dark['--accent']).toBe('#C9F73A')
    expect(light['--bg']).toBe('#F4F4F0')
    expect(light['--accent-text']).toBe('#3F6100')
  })
  it('defines chart and plate colours', () => {
    for (const k of ['--chart-squat', '--chart-bench', '--chart-deadlift', '--plate-25', '--plate-20', '--plate-15', '--plate-10', '--plate-5', '--plate-2p5', '--plate-small']) {
      expect(dark[k], k).toBeDefined()
    }
  })
})
