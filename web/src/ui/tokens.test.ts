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
    '--accent': '#93979D',
    '--accent-ink': '#0B0C0E',
    '--accent-text': '#93979D',
    '--accent-container': '#2A2C2F',
    '--accent-on-container': '#DEDFE1',
  },
  light: {
    '--accent': '#4A4D52',
    '--accent-ink': '#F6F7F8',
    '--accent-text': '#4A4D52',
    '--accent-container': '#DCDEE0',
    '--accent-on-container': '#2F3135',
  },
} as const

describe.each([
  ['dark', dark],
  ['light', light],
] as const)('%s accent (grey)', (name, theme) => {
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

/** Every declaration of `name` in the block that `open` starts, whatever its value (the hex-only `block` skips shadows). */
function declared(source: string, open: RegExp, name: string): string | undefined {
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
  const body = source.slice(start, i - 1)
  const d = new RegExp(`${name}\\s*:\\s*([^;]+);`).exec(body)
  return d?.[1]?.replace(/\s+/g, ' ').trim()
}

// The soft card's shadow (DESIGN.md "Space and shape"), as on Android (Color.kt `CardShadow`). The bodybuilding side
// uses the same shadows, so its blocks do not set it.
describe('card elevation', () => {
  const LIGHT = /@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{/
  it('dark: a white top highlight at 5% and a soft black drop shadow', () => {
    expect(declared(css, /:root\s*\{/, '--elevation-card')).toBe('inset 0 1px 0 rgba(255,255,255,.05), 0 8px 24px rgba(0,0,0,.35)')
  })
  it('light: two soft near-black shadows', () => {
    expect(declared(css, LIGHT, '--elevation-card')).toBe('0 1px 2px rgba(17,19,21,.06), 0 8px 24px rgba(17,19,21,.08)')
  })
  it('the bodybuilding side keeps the same shadows', () => {
    expect(declared(css, /:root\[data-mode='bodybuilding'\]\s*\{/, '--elevation-card')).toBeUndefined()
    expect(declared(css, /@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\[data-mode='bodybuilding'\]\s*\{/, '--elevation-card')).toBeUndefined()
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

// The bodybuilding side (bodybuilding mode spec §7.2): a second palette for each scheme, switched on by
// `data-mode="bodybuilding"` on <html>. Everything it does not set is the powerlifting side's.
const BODYBUILDING_DARK = /:root\[data-mode='bodybuilding'\]\s*\{/
const BODYBUILDING_LIGHT = /@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\[data-mode='bodybuilding'\]\s*\{/
const bodybuildingDark = { ...dark, ...block(css, BODYBUILDING_DARK) }
const bodybuildingLight = { ...light, ...block(css, BODYBUILDING_LIGHT) }

// The spec's values. Container highest is one step darker (dark) and five steps lighter (light) than the spec's
// #22314F and #C9D7EC: the least change that gives the outline 3:1 and muted text 4.5:1 against it.
const bodybuilding = {
  dark: {
    '--bg': '#070B14',
    '--surface': '#0D1422',
    '--container': '#131C2E',
    '--container-high': '#1A2640',
    '--container-highest': '#21304E',
    '--hairline': '#1E2A44',
    '--accent': '#8DB8FF',
    '--accent-ink': '#07162E',
    '--accent-text': '#8DB8FF',
    '--accent-container': '#14274A',
    '--accent-on-container': '#D6E6FF',
  },
  light: {
    '--bg': '#EDF2FA',
    '--surface': '#FFFFFF',
    '--container': '#E2EAF6',
    '--container-high': '#D6E1F1',
    '--container-highest': '#CEDCF1',
    '--hairline': '#D0DCEE',
    '--accent': '#1F4FA8',
    '--accent-ink': '#F5F8FF',
    '--accent-text': '#1F4FA8',
    '--accent-container': '#DCE8FB',
    '--accent-on-container': '#173B7D',
  },
} as const

describe.each([
  ['dark', bodybuildingDark],
  ['light', bodybuildingLight],
] as const)('bodybuilding %s palette', (name, theme) => {
  const get = (k: string): string => {
    const v = theme[k]
    if (!v) throw new Error(`missing token ${k}`)
    return v
  }
  for (const [token, hex] of Object.entries(bodybuilding[name])) {
    it(`${token} is ${hex}`, () => {
      expect(theme[token]).toBe(hex)
    })
  }
  it('sets only the surfaces, the hairline and the accent', () => {
    const own = block(css, name === 'dark' ? BODYBUILDING_DARK : BODYBUILDING_LIGHT)
    expect(Object.keys(own).sort()).toEqual(Object.keys(bodybuilding[name]).sort())
  })
  it('leaves text, status, chart and plate colours as they are', () => {
    const base: Record<string, string> = name === 'dark' ? dark : light
    for (const k of [...texts.filter((t) => t !== '--accent-text'), '--outline', '--success-container', '--warning-container', '--danger-container', '--info-container', '--chart-squat', '--chart-bench', '--chart-deadlift', '--chart-press', '--plate-25', '--plate-20']) {
      expect(theme[k], k).toBe(base[k])
    }
  })
  for (const s of surfaces) {
    for (const t of texts) {
      it(`${t} on ${s} is at least 4.5:1`, () => {
        expect(ratio(get(t), get(s))).toBeGreaterThanOrEqual(4.5)
      })
    }
    it(`the fill against ${s} is at least 3:1`, () => {
      expect(ratio(get('--accent'), get(s))).toBeGreaterThanOrEqual(3)
    })
    it(`the fill against the disabled-button fill on ${s} is at least 3:1`, () => {
      expect(ratio(get('--accent'), over(get('--text'), get(s), 0.12))).toBeGreaterThanOrEqual(3)
    })
  }
  for (const [fg, bg] of pairs) {
    it(`${fg} on ${bg} is at least 4.5:1`, () => {
      expect(ratio(get(fg), get(bg))).toBeGreaterThanOrEqual(4.5)
    })
  }
  for (const s of ['--container-high', '--container-highest']) {
    it(`--outline against ${s} is at least 3:1`, () => {
      expect(ratio(get('--outline'), get(s))).toBeGreaterThanOrEqual(3)
    })
  }
  // The check-in's locked tiles (a body part already marked lagging, on step 2) sit on a card (--surface): their name
  // and their box are drawn in --text-muted, so they read as unavailable and still pass, light mode included.
  it('the locked check-in tile on --surface: its name at least 4.5:1 and its box at least 3:1', () => {
    const library = readFileSync(resolve(process.cwd(), 'src/screens/library/library.css'), 'utf8')
    const rule = (selector: string) => library.slice(library.indexOf(`${selector} {`)).split('}')[0]!
    expect(rule('.lib-option-disabled')).toContain('color: var(--text-muted)')
    expect(rule('.lib-option-disabled input')).toContain('border: 2px solid var(--text-muted)')
    expect(ratio(get('--text-muted'), get('--surface'))).toBeGreaterThanOrEqual(4.5)
  })
  for (const s of ['--bg', '--surface', '--container']) {
    it(`--chart-press against ${s} is at least 3:1`, () => {
      expect(ratio(get('--chart-press'), get(s))).toBeGreaterThanOrEqual(3)
    })
  }
})

describe('bodybuilding palette in the cascade', () => {
  it('the light block sets every token the dark one does, so nothing dark leaks into light', () => {
    expect(Object.keys(block(css, BODYBUILDING_LIGHT)).sort()).toEqual(Object.keys(block(css, BODYBUILDING_DARK)).sort())
  })
  it('comes after the powerlifting palettes', () => {
    expect(css.search(BODYBUILDING_DARK)).toBeGreaterThan(css.search(/@media\s*\(prefers-color-scheme:\s*light\)\s*\{\s*:root\s*\{/))
    expect(css.search(BODYBUILDING_LIGHT)).toBeGreaterThan(css.search(BODYBUILDING_DARK))
  })
})
