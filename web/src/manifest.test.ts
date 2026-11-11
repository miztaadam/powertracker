import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

interface Icon { src: string; sizes: string; purpose?: string }
const manifest = JSON.parse(readFileSync(resolve(process.cwd(), 'public/manifest.webmanifest'), 'utf8')) as {
  name: string; display: string; theme_color: string; background_color: string; icons: Icon[]
}

describe('web manifest', () => {
  it('names the app and runs standalone on the graphite background', () => {
    expect(manifest.name).toBe('Grit')
    expect(manifest.display).toBe('standalone')
    expect(manifest.theme_color).toBe('#0B0C0E')
    expect(manifest.background_color).toBe('#0B0C0E')
  })
  it('lists 192, 512 and maskable icons that exist on disk', () => {
    expect(manifest.icons.some((i) => i.sizes === '192x192')).toBe(true)
    expect(manifest.icons.some((i) => i.sizes === '512x512' && (i.purpose ?? 'any') === 'any')).toBe(true)
    expect(manifest.icons.some((i) => i.purpose === 'maskable')).toBe(true)
    for (const i of manifest.icons) {
      expect(existsSync(resolve(process.cwd(), `public${i.src}`)), i.src).toBe(true)
    }
  })
})
