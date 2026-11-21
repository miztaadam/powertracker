import { describe, expect, it, vi } from 'vitest'
import { RELOAD_PARAM, ROUTE_KEY, rememberRoute, stripReloadMarker } from './reloadMarker'

// The `u` parameter the service worker adds when it reloads a window (public/sw-reload-clients.js) is taken out of the
// address before anything reads it, so the address bar and an installed app's URL stay as they were.

const at = (href: string) => {
  const replaceState = vi.fn()
  stripReloadMarker({ href }, { state: { kept: true }, replaceState })
  return replaceState
}

describe('stripReloadMarker', () => {
  it('is the parameter the worker adds', () => {
    expect(RELOAD_PARAM).toBe('u')
  })

  it('takes u out and keeps the path, the other parameters and the route', () => {
    expect(at('https://grit.example/?u=abc#/plan').mock.calls).toEqual([[{ kept: true }, '', '/#/plan']])
    expect(at('https://grit.example/app/?keep=1&u=abc#/coach').mock.calls).toEqual([[{ kept: true }, '', '/app/?keep=1#/coach']])
  })

  it('leaves an address without it alone', () => {
    expect(at('https://grit.example/#/today')).not.toHaveBeenCalled()
    expect(at('https://grit.example/?keep=1#/today')).not.toHaveBeenCalled()
  })
})

// The worker's `client.url` is the address the window loaded at (Chromium, 2026-11-21: a window that moved to #/plan
// after loading at #/today was reported, and reloaded, at #/today). So each window keeps its current route in its own
// session storage, and a reload by the worker puts it back on that route.
describe('the route a window was on, kept through a reload by the worker', () => {
  const storage = () => {
    const items = new Map<string, string>()
    return { getItem: (k: string) => items.get(k) ?? null, setItem: (k: string, v: string) => void items.set(k, v), items }
  }

  it('a reload by the worker goes back to the route the window last had', () => {
    const session = storage()
    rememberRoute({ hash: '#/plan' }, session, () => {})
    const replaceState = vi.fn()
    stripReloadMarker({ href: 'https://grit.example/?u=abc#/today' }, { state: null, replaceState }, session)
    expect(replaceState.mock.calls).toEqual([[null, '', '/#/plan']])
  })

  it('follows each route change', () => {
    const session = storage()
    const location = { hash: '#/today' }
    let changed = () => {}
    rememberRoute(location, session, (cb) => (changed = cb))
    location.hash = '#/coach'
    changed()
    expect(session.items.get(ROUTE_KEY)).toBe('#/coach')
  })

  it('an ordinary load does not jump to a remembered route', () => {
    const session = storage()
    session.setItem(ROUTE_KEY, '#/plan')
    const replaceState = vi.fn()
    stripReloadMarker({ href: 'https://grit.example/#/today' }, { state: null, replaceState }, session)
    expect(replaceState).not.toHaveBeenCalled()
  })

  it('nothing remembered (a window from before 1.17): stays on the address it was sent to', () => {
    const replaceState = vi.fn()
    stripReloadMarker({ href: 'https://grit.example/?u=abc#/today' }, { state: null, replaceState }, storage())
    expect(replaceState.mock.calls).toEqual([[null, '', '/#/today']])
  })

  it('storage that throws changes nothing but the u parameter', () => {
    const broken = {
      getItem: () => {
        throw new Error('blocked')
      },
      setItem: () => {
        throw new Error('blocked')
      },
    }
    expect(() => rememberRoute({ hash: '#/plan' }, broken, () => {})).not.toThrow()
    const replaceState = vi.fn()
    stripReloadMarker({ href: 'https://grit.example/?u=abc#/today' }, { state: null, replaceState }, broken)
    expect(replaceState.mock.calls).toEqual([[null, '', '/#/today']])
  })
})
