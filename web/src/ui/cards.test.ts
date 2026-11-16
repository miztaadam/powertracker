import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

// The soft card (DESIGN.md "Space and shape"): cards take the elevation token and no visible border; row dividers
// stay, as the hairline at 60%; state outlines are not card borders and stay as they were.
const read = (path: string) => readFileSync(resolve(process.cwd(), 'src', path), 'utf8')

/** The declarations of the first rule whose selector is exactly `selector`. */
function rule(css: string, selector: string): string {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = new RegExp(`(?:^|\\})\\s*${escaped}\\s*\\{([^}]*)\\}`, 'm').exec(css)
  if (!match) throw new Error(`no rule ${selector}`)
  return match[1] ?? ''
}

const SOFT_DIVIDER = 'color-mix(in srgb, var(--hairline) 60%, transparent)'

describe('soft cards', () => {
  const cards: [string, string, boolean][] = [
    ['ui/components/Card.css', '.gr-card', true],
    ['ui/components/Menu.css', '.gr-menu-list', true],
    ['screens/library/library.css', '.lib-muscle', true],
    ['ui/shell.css', '.shell-update', false],
  ]
  for (const [file, selector, shadow] of cards) {
    it(`${selector} has no hairline border${shadow ? ' and takes the card elevation' : ''}`, () => {
      const body = rule(read(file), selector)
      expect(body).not.toMatch(/border:[^;]*var\(--hairline\)/)
      expect(body).toMatch(/border:\s*1px solid transparent/)
      if (shadow) expect(body).toMatch(/box-shadow:\s*var\(--elevation-card\)/)
      else expect(body).not.toMatch(/box-shadow/)
    })
  }

  it('gives the picker and Exercises rows no shadow', () => {
    const library = read('screens/library/library.css')
    for (const selector of ['.lib-row', '.lib-row-main', '.lib-rows']) expect(rule(library, selector)).not.toMatch(/box-shadow/)
  })
})

describe('row dividers at 60%', () => {
  const files = [
    'screens/plan/plan.css',
    'screens/plan/builder/builder.css',
    'screens/progress/progress.css',
    'screens/today/today.css',
    'screens/checkin/checkin.css',
    'screens/settings/settings.css',
    'screens/coach/coach.css',
    'screens/library/library.css',
    'ui/components/TabBar.css',
  ]
  for (const file of files) {
    it(`${file} draws every hairline line at 60%`, () => {
      const css = read(file)
      expect(css).not.toMatch(/(?:border(?:-top|-bottom)?|background):\s*[^;]*(?<!\(in srgb, )var\(--hairline\)\s*;/)
      expect(css).toContain(SOFT_DIVIDER)
    })
  }

  it('keeps the dividers between rows', () => {
    expect(rule(read('screens/settings/settings.css'), '.settings-group > * + *')).toContain(`border-top: 1px solid ${SOFT_DIVIDER}`)
    expect(rule(read('screens/plan/plan.css'), '.plan-row')).toContain(`border-top: 1px solid ${SOFT_DIVIDER}`)
    expect(rule(read('screens/library/library.css'), '.lib-row::after')).toContain(`background: ${SOFT_DIVIDER}`)
  })
})

describe('state outlines stay', () => {
  it('keeps the NumberField underline at full hairline', () => {
    expect(rule(read('ui/components/NumberField.css'), '.gr-numfield-sizer')).toMatch(/border-bottom:\s*2px solid var\(--hairline\)/)
  })
})
