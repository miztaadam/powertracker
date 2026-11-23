// @vitest-environment node
import { fileURLToPath } from 'node:url'
import type { ModuleNode, Plugin } from 'vite'
import { figureThumbnailsPlugin } from '../../../plugins/figureThumbnails'

// The thumbnails plugin itself (Task 14): it resolves and loads virtual:figure-thumbnails from the domain bundle, and
// while `vite dev` runs a re-synced bundle invalidates the loaded data.
const DOMAIN = fileURLToPath(new URL('../../domain/generated/powertracker-domain.mjs', import.meta.url))

type Hook = (this: unknown, ...args: unknown[]) => unknown
const hook = (plugin: Plugin, name: 'resolveId' | 'load' | 'handleHotUpdate'): Hook => {
  const h = plugin[name] as Hook | { handler: Hook }
  return typeof h === 'function' ? h : h.handler
}

describe('figureThumbnailsPlugin', () => {
  it('resolves and loads the thumbnail data, one entry per catalogue figure', async () => {
    const plugin = figureThumbnailsPlugin()
    const resolved = hook(plugin, 'resolveId').call({}, 'virtual:figure-thumbnails') as string
    expect(resolved).toBe('\0virtual:figure-thumbnails')
    expect(hook(plugin, 'resolveId').call({}, 'react')).toBeNull()

    const watched: string[] = []
    const code = (await hook(plugin, 'load').call({ addWatchFile: (f: string) => watched.push(f) }, resolved)) as string
    expect(watched).toEqual([DOMAIN])
    const match = /^export default JSON\.parse\((".*")\)\n$/s.exec(code)
    expect(match).not.toBeNull()
    const data = JSON.parse(JSON.parse(match![1]!) as string) as Record<string, number[]>
    expect(Object.keys(data)).toHaveLength(225)
    expect(data['Bench']?.slice(0, 2)).toEqual([400, 300])
    expect(data['Not an exercise']).toBeUndefined()
    expect(await hook(plugin, 'load').call({ addWatchFile: () => {} }, 'some-other-module')).toBeNull()
  }, 30_000)

  it('invalidates the loaded data when the domain bundle changes in the dev server', () => {
    const plugin = figureThumbnailsPlugin()
    const data = { id: '\0virtual:figure-thumbnails' } as ModuleNode
    const domain = { id: DOMAIN } as ModuleNode
    const invalidated: ModuleNode[] = []
    const server = {
      moduleGraph: {
        getModuleById: (id: string) => (id === data.id ? data : undefined),
        invalidateModule: (m: ModuleNode) => invalidated.push(m),
      },
    }
    const update = hook(plugin, 'handleHotUpdate')
    expect(update.call({}, { file: DOMAIN, server, modules: [domain] })).toEqual([domain, data])
    expect(invalidated).toEqual([data])
    // Any other file leaves the data alone.
    expect(update.call({}, { file: fileURLToPath(new URL('./thumbnails.ts', import.meta.url)), server, modules: [] })).toBeUndefined()
    expect(invalidated).toEqual([data])
  })
})
