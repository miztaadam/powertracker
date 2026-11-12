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
  // The outline is the only edge of a selected chip or segment, which is filled with container-highest.
  for (const s of ['--container-high', '--container-highest']) {
    it(`--outline against ${s} is at least 3:1`, () => {
      expect(ratio(get('--outline'), get(s))).toBeGreaterThanOrEqual(3)
    })
  }
})

/** `fg` at `alpha` over an opaque `bg`, as the browser composites it (Button.css's disabled fill). */
function over(fg: string, bg: string, alpha: number): string {
  return (
    '#' +
    [1, 3, 5]
      .map((i) => {
        const f = parseInt(fg.slice(i, i + 2), 16)
        const b = parseInt(bg.slice(i, i + 2), 16)
        return Math.round(f * alpha + b * (1 - alpha))
          .toString(16)
          .padStart(2, '0')
      })
      .join('')
  )
}

const accent = {
  dark: {
    '--accent': '#DCD6C8',
    '--accent-ink': '#1A1812',
    '--accent-text': '#DCD6C8',
    '--accent-container': '#332F26',
    '--accent-on-container': '#EAE4D5',
  },
  light: {
    '--accent': '#46423B',
    '--accent-ink': '#FBF8F1',
    '--accent-text': '#4B463E',
    '--accent-container': '#E8E2D4',
    '--accent-on-container': '#39342B',
  },
} as const

describe.each([
  ['dark', dark],
  ['light', light],
] as const)('%s accent (warm chalk)', (name, theme) => {
  const get = (k: string): string => {
    const v = theme[k]
    if (!v) throw new Error(`missing token ${k}`)
    return v
  }
  for (const [token, hex] of Object.entries(accent[name])) {
    it(`${token} is ${hex}`, () => {
      expect(theme[token]).toBe(hex)
    })
  }
  it('button label on the fill is at least 4.5:1', () => {
    expect(ratio(get('--accent-ink'), get('--accent'))).toBeGreaterThanOrEqual(4.5)
  })
  it('chip tag on the chip is at least 4.5:1', () => {
    expect(ratio(get('--accent-on-container'), get('--accent-container'))).toBeGreaterThanOrEqual(4.5)
  })
  for (const s of surfaces) {
    it(`the fill against ${s} is at least 3:1`, () => {
      expect(ratio(get('--accent'), get(s))).toBeGreaterThanOrEqual(3)
    })
    it(`accent text on ${s} is at least 4.5:1`, () => {
      expect(ratio(get('--accent-text'), get(s))).toBeGreaterThanOrEqual(4.5)
    })
    it(`the fill against the disabled-button fill on ${s} is at least 3:1`, () => {
      expect(ratio(get('--accent'), over(get('--text'), get(s), 0.12))).toBeGreaterThanOrEqual(3)
    })
  }
  it('the fill against the secondary-button fill is at least 3:1', () => {
    expect(ratio(get('--accent'), get('--container-highest'))).toBeGreaterThanOrEqual(3)
  })
})

// The strict press chart colour, as on Android (Color.kt `press`): a line and a dot must stand out from what they are drawn on.
const press = { dark: '#A98BF0', light: '#7C52CC' } as const

describe.each([
  ['dark', dark],
  ['light', light],
] as const)('%s strict press chart colour', (name, theme) => {
  it(`--chart-press is ${press[name]}`, () => {
    expect(theme['--chart-press']).toBe(press[name])
  })
  for (const s of ['--bg', '--surface', '--container']) {
    it(`against ${s} is at least 3:1`, () => {
      expect(ratio(theme['--chart-press'] ?? '#000000', theme[s]!)).toBeGreaterThanOrEqual(3)
      expect(theme['--chart-press']).toBeDefined()
    })
  }
  it('is not one of the other lifts\' colours', () => {
    for (const k of ['--chart-squat', '--chart-bench', '--chart-deadlift']) expect(theme['--chart-press']).not.toBe(theme[k])
  })
})

describe('token set', () => {
  it('matches the DESIGN.md anchors', () => {
    expect(dark['--bg']).toBe('#0B0C0E')
    expect(light['--bg']).toBe('#F4F4F0')
  })
  it('gives the light theme its own accent fill and ink', () => {
    const lightOnly = block(css, /@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{/)
    expect(lightOnly['--accent']).toBeDefined()
    expect(lightOnly['--accent-ink']).toBeDefined()
  })
  it('defines chart and plate colours', () => {
    for (const k of ['--chart-squat', '--chart-bench', '--chart-deadlift', '--chart-press', '--plate-25', '--plate-20', '--plate-15', '--plate-10', '--plate-5', '--plate-2p5', '--plate-small']) {
      expect(dark[k], k).toBeDefined()
    }
  })
})
