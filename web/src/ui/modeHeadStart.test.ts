import { renderHook, waitFor } from '@testing-library/react'
import Dexie from 'dexie'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { openGritDb, setTrainingMode, type GritDb } from '../db/db'
import { useTrainingMode } from '../state/useTrainingMode'
import { MODE_KEY, rememberMode, rememberedMode } from './modeHeadStart'

const SCRIPT = readFileSync(resolve(process.cwd(), 'public/mode-head-start.js'), 'utf8')
const INDEX = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')

/** Runs the head-start script as the browser does: a plain script, before anything else of the app. */
const runScript = () => new Function(SCRIPT)()

const themeColor = () => document.querySelector('meta[name="theme-color"]')?.getAttribute('content')

function resetPage() {
  delete document.documentElement.dataset.mode
  document.head.innerHTML = '<meta name="theme-color" content="#0B0C0E" />'
}

afterEach(() => {
  vi.restoreAllMocks()
  resetPage()
  try {
    localStorage.removeItem(MODE_KEY)
  } catch {
    // Nothing to clear.
  }
})

describe('the remembered side', () => {
  it('rememberedMode_isWhatWasRemembered_elsePowerlifting', () => {
    expect(rememberedMode()).toBe('POWERLIFTING')
    rememberMode('BODYBUILDING')
    expect(rememberedMode()).toBe('BODYBUILDING')
    rememberMode('POWERLIFTING')
    expect(rememberedMode()).toBe('POWERLIFTING')
    expect(localStorage.getItem(MODE_KEY)).toBeNull()
    localStorage.setItem(MODE_KEY, 'CROSSFIT')
    expect(rememberedMode()).toBe('POWERLIFTING')
  })

  it('storageThatThrows_isNoHeadStartAndNoError', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    expect(() => rememberMode('BODYBUILDING')).not.toThrow()
    expect(rememberedMode()).toBe('POWERLIFTING')
    resetPage()
    expect(runScript).not.toThrow()
    expect(document.documentElement.dataset.mode).toBeUndefined()
  })
})

describe('the head-start script', () => {
  it('putsTheBodybuildingLookOnBeforeTheApp', () => {
    resetPage()
    rememberMode('BODYBUILDING')
    runScript()
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
    // jsdom has no matchMedia: the dark background.
    expect(themeColor()).toBe('#070B14')
  })

  it('leavesThePowerliftingLookAlone', () => {
    resetPage()
    runScript()
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(themeColor()).toBe('#0B0C0E')
  })

  it('followsALightSystemScheme', () => {
    resetPage()
    rememberMode('BODYBUILDING')
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('light') }))
    try {
      runScript()
    } finally {
      vi.unstubAllGlobals()
    }
    expect(themeColor()).toBe('#EDF2FA')
  })

  it('setsThePowerliftingLightColourToo_whenTheSystemIsLight', () => {
    resetPage()
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: q.includes('light') }))
    try {
      runScript()
    } finally {
      vi.unstubAllGlobals()
    }
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(themeColor()).toBe('#F4F4F0')
  })

  it('isLoadedInTheHeadAsAPlainScript_beforeTheApp', () => {
    const head = INDEX.slice(0, INDEX.indexOf('</head>'))
    const tag = head.match(/<script[^>]*src="\/mode-head-start\.js"[^>]*><\/script>/)?.[0]
    expect(tag).toBeDefined()
    expect(tag).not.toMatch(/type="module"|defer|async/)
    expect(head.indexOf('name="theme-color"')).toBeLessThan(head.indexOf('mode-head-start.js'))
    expect(INDEX.indexOf('mode-head-start.js')).toBeLessThan(INDEX.indexOf('/src/main.tsx'))
  })
})

describe('useTrainingMode with a head start', () => {
  let db: GritDb | null = null
  const name = 'grit-head-start-test'
  afterEach(async () => {
    db?.close()
    db = null
    await Dexie.delete(name)
  })

  it('keepsTheRememberedLookUntilTheSettingsSayOtherwise', async () => {
    rememberMode('BODYBUILDING')
    document.documentElement.dataset.mode = 'bodybuilding'
    // No database yet: the remembered side, not the powerlifting palette for a moment.
    const { result, rerender } = renderHook(({ d }) => useTrainingMode(d), { initialProps: { d: null as GritDb | null } })
    expect(result.current).toBe('BODYBUILDING')
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')

    // The stored settings are the truth: powerlifting, so the look and the remembered side follow.
    db = await openGritDb(name)
    rerender({ d: db })
    await waitFor(() => expect(result.current).toBe('POWERLIFTING'))
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(rememberedMode()).toBe('POWERLIFTING')

    await setTrainingMode(db, 'BODYBUILDING', '2026-11-13')
    await waitFor(() => expect(rememberedMode()).toBe('BODYBUILDING'))
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
  })
})
