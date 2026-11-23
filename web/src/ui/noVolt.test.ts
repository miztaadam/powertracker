import { readdirSync, readFileSync } from 'node:fs'
import { join, relative, resolve, sep } from 'node:path'
import { describe, expect, it } from 'vitest'

/** The retired volt accent. Split so this file doesn't trip its own check. */
const VOLT = ['C9F73A', '121700', '25300A', 'DDFB86', '3F6100', 'E6F9A8', '2A3F00'].map((h) => `#${h}`.toLowerCase())

const root = process.cwd()
const generated = join('src', 'domain', 'generated') + sep

function sources(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const path = join(dir, e.name)
    if (e.isDirectory()) return sources(path)
    return /\.(ts|tsx|css)$/.test(e.name) ? [path] : []
  })
}

const files = [
  ...sources(resolve(root, 'src')).filter((f) => !(relative(root, f) + sep).startsWith(generated)),
  resolve(root, 'index.html'),
  resolve(root, 'public/manifest.webmanifest'),
]

describe('no volt left in the web app', () => {
  it('scans the sources', () => {
    expect(files.length).toBeGreaterThan(50)
    expect(files.some((f) => f.endsWith(join('ui', 'tokens.css')))).toBe(true)
    expect(files.some((f) => (relative(root, f) + sep).startsWith(generated))).toBe(false)
  })
  it('has no volt hex value in any source file', () => {
    const hits = files.flatMap((f) => {
      const text = readFileSync(f, 'utf8').toLowerCase()
      return VOLT.filter((v) => text.includes(v)).map((v) => `${relative(root, f)}: ${v}`)
    })
    expect(hits).toEqual([])
  })
})
