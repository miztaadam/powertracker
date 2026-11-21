import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it, vi } from 'vitest'

// public/sw-reload-clients.js, run against a stand-in for the worker's global scope: on activate it reloads each window
// the previous worker controlled, at its own URL with a `u` parameter that differs on every activation, so the
// navigation is never taken for a jump within the page (every Grit URL has a #/ route).

const source = readFileSync(resolve(process.cwd(), 'public/sw-reload-clients.js'), 'utf8')

type Win = { url: string; navigate?: (url: string) => Promise<unknown> }

/** Loads the script into a fresh fake worker scope; `activate()` fires its activate event and waits for it. */
function worker(windows: () => Win[]) {
  let listener: ((event: { waitUntil: (p: Promise<unknown>) => void }) => void) | undefined
  const matchAll = vi.fn(async (_options: unknown) => windows())
  const self = {
    addEventListener: (type: string, cb: typeof listener) => {
      if (type === 'activate') listener = cb
    },
    clients: { matchAll },
  }
  new Function('self', source)(self)
  return {
    matchAll,
    async activate() {
      let pending: Promise<unknown> = Promise.resolve()
      listener!({ waitUntil: (p) => (pending = p) })
      await pending
    },
  }
}

const win = (url: string) => ({ url, navigate: vi.fn(async (_url: string) => undefined) })

describe('sw-reload-clients.js', () => {
  it('asks for controlled windows only', async () => {
    const w = worker(() => [])
    await w.activate()
    expect(w.matchAll).toHaveBeenCalledWith({ type: 'window' })
  })

  it('navigates each window to its own path and route, with a u parameter', async () => {
    const a = win('https://grit.example/#/today')
    const b = win('https://grit.example/?keep=1#/plan')
    const w = worker(() => [a, b])
    await w.activate()
    const toA = new URL(a.navigate.mock.calls[0]![0])
    const toB = new URL(b.navigate.mock.calls[0]![0])
    expect([toA.pathname, toA.hash, toA.searchParams.getAll('u').length]).toEqual(['/', '#/today', 1])
    expect([toB.pathname, toB.hash, toB.searchParams.get('keep'), toB.searchParams.getAll('u').length]).toEqual(['/', '#/plan', '1', 1])
  })

  it('replaces an earlier u rather than adding a second, and the value differs on every activation', async () => {
    const a = win('https://grit.example/?u=old#/today')
    const w = worker(() => [a])
    await w.activate()
    await w.activate()
    const [first, second] = a.navigate.mock.calls.map(([url]) => new URL(url))
    expect(first!.searchParams.getAll('u')).toHaveLength(1)
    expect(first!.searchParams.get('u')).not.toBe('old')
    expect(second!.searchParams.get('u')).not.toBe(first!.searchParams.get('u'))
  })

  it('a refused navigation, or a browser without navigate, changes nothing else', async () => {
    const refused = { url: 'https://grit.example/#/today', navigate: vi.fn(async () => Promise.reject(new TypeError('no'))) }
    const thrown = {
      url: 'https://grit.example/#/today',
      navigate: vi.fn(() => {
        throw new TypeError('no')
      }),
    }
    const missing = { url: 'https://grit.example/#/today' }
    const fine = win('https://grit.example/#/coach')
    const w = worker(() => [refused, thrown, missing, fine])
    await expect(w.activate()).resolves.toBeUndefined()
    expect(fine.navigate).toHaveBeenCalledTimes(1)
  })
})
