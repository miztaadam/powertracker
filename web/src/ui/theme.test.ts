import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { applyTrainingMode, syncThemeColor } from './theme'

let meta: HTMLMetaElement
let light = false
const listeners = new Set<() => void>()

beforeEach(() => {
  meta = document.createElement('meta')
  meta.name = 'theme-color'
  document.head.append(meta)
  vi.stubGlobal('matchMedia', (query: string) => ({
    get matches() {
      return query.includes('light') && light
    },
    addEventListener: (_: string, fn: () => void) => listeners.add(fn),
    removeEventListener: (_: string, fn: () => void) => listeners.delete(fn),
  }))
})
afterEach(() => {
  applyTrainingMode('POWERLIFTING')
  meta.remove()
  light = false
  listeners.clear()
  vi.unstubAllGlobals()
})

describe('the look follows the training side', () => {
  it('powerlifting_noAttribute_warmBackground', () => {
    const stop = syncThemeColor()
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(meta.content).toBe('#0B0C0E')
    stop()
  })

  it('bodybuilding_setsTheAttribute_andTheBrowserChromeTurnsBlue', () => {
    const stop = syncThemeColor()
    applyTrainingMode('BODYBUILDING')
    expect(document.documentElement.getAttribute('data-mode')).toBe('bodybuilding')
    expect(meta.content).toBe('#070B14')
    // The system switching to light keeps the side.
    light = true
    for (const fn of listeners) fn()
    expect(meta.content).toBe('#EDF2FA')
    stop()
  })

  it('backToPowerlifting_removesTheAttribute', () => {
    light = true
    applyTrainingMode('BODYBUILDING')
    applyTrainingMode('POWERLIFTING')
    expect(document.documentElement.hasAttribute('data-mode')).toBe(false)
    expect(meta.content).toBe('#F4F4F0')
  })
})
