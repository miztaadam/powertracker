import { statSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'
import type { Plugin } from 'vite'
import type { Shape } from '../src/screens/library/FigureSvg'
import { encodeShapes } from '../src/screens/library/thumbnailFormat'

/**
 * The Exercises tab's thumbnails, worked out when the app is built, so the browser never solves a pose for one.
 *
 * Provides `virtual:figure-thumbnails` (typed in src/screens/library/figure-thumbnails.d.ts) to `vite build`,
 * `vite dev` and vitest: for every catalogue exercise with a figure, its view box and its thumbnail pose's shapes,
 * written as src/screens/library/thumbnailFormat.ts describes. They come from the generated domain bundle in the tree
 * (`:domain:syncWebDomain`), loaded straight into Node, so they always match it; nothing generated is committed.
 * The module is one `JSON.parse` of a string literal: the quickest way for a browser to read this much data.
 */
const ID = 'virtual:figure-thumbnails'
const RESOLVED = `\0${ID}`
const DOMAIN = fileURLToPath(new URL('../src/domain/generated/powertracker-domain.mjs', import.meta.url))

type Domain = {
  library(): readonly { name: string }[]
  figureInfo(name: string): { thumbnail: string; viewBox: readonly number[] } | null | undefined
  figureShapes(name: string, which: string): readonly Shape[] | null | undefined
}

/** Every catalogue figure's thumbnail, by catalogue name: its view box, then its shapes written down. */
export async function figureThumbnails(): Promise<Record<string, number[]>> {
  // A changed bundle (a new figure while the dev server runs) is a new module to Node, so it is read afresh.
  const url = `${pathToFileURL(DOMAIN).href}?v=${statSync(DOMAIN).mtimeMs}`
  const { GritDomain } = (await import(url)) as { GritDomain: { getInstance(): Domain } }
  const grit = GritDomain.getInstance()
  const out: Record<string, number[]> = {}
  for (const { name } of grit.library()) {
    const info = grit.figureInfo(name)
    if (info == null) continue
    const shapes = grit.figureShapes(name, info.thumbnail)
    if (shapes == null) throw new Error(`${name}: a figure with no ${info.thumbnail} pose`)
    out[name] = [info.viewBox[0]!, info.viewBox[1]!, ...encodeShapes(shapes)]
  }
  return out
}

export function figureThumbnailsPlugin(): Plugin {
  return {
    name: 'grit-figure-thumbnails',
    resolveId: (id) => (id === ID ? RESOLVED : null),
    async load(id) {
      if (id !== RESOLVED) return null
      this.addWatchFile(DOMAIN)
      return `export default JSON.parse(${JSON.stringify(JSON.stringify(await figureThumbnails()))})\n`
    },
  }
}
