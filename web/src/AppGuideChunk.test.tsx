import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { App } from './App'
import type { GritDb } from './db/db'
import { todayFixture } from './test/todayFixture'
import { GUIDE_KEY, GUIDE_SEEN_KEY } from './screens/guide/guideSeen'

/*
 * The guide is a chunk of its own (1.16). If it cannot be loaded (offline before the service worker has it, a stale
 * chunk after an update), the guide closes as if skipped and the app shows: never the recovery screen. Its own file,
 * as the failing import is mocked for the whole file.
 */

vi.mock('./screens/guide/GuideScreen', () => Promise.reject(new TypeError('Failed to fetch dynamically imported module')))

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  window.location.hash = ''
  fx = todayFixture()
  db = await fx.open()
})
/** React and jsdom both report the error the boundary caught. */
const swallow = (event: ErrorEvent) => event.preventDefault()
afterEach(async () => {
  window.removeEventListener('error', swallow)
  cleanup()
  await fx.dispose()
  vi.restoreAllMocks()
})

/** Every row of every store, to show nothing was written. */
const everything = async () => Object.fromEntries(await Promise.all(db.tables.map(async (t) => [t.name, await t.toArray()] as const)))

it('aGuideChunkThatFailsToLoad_closesTheGuideAsSkipped_writesNothing_andTheAppShows', async () => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
  window.addEventListener('error', swallow)
  const before = await everything()
  window.location.hash = '#/guide'
  render(<App dbName={fx.name} />)
  // The app, on Today, where a skipped guide goes: its tab bar, and no recovery screen.
  await waitFor(() => expect(window.location.hash).toBe('#/today'))
  expect(await screen.findByRole('navigation', { name: 'Main' })).toBeInTheDocument()
  expect(screen.queryByTestId('guide')).toBeNull()
  expect(screen.queryByRole('heading', { name: /^Grit couldn't/ })).toBeNull()
  expect(await everything()).toEqual(before)
})

it('onAFirstLaunch_aGuideChunkThatFailsToLoad_writesNoSeen_soANewUserIsShownItNextTime', async () => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
  window.addEventListener('error', swallow)
  // A new device: the first-launch gate open (the shared setup shuts it).
  localStorage.removeItem(GUIDE_KEY)
  const before = await everything()
  render(<App dbName={fx.name} />)
  // The gate opens the guide; its chunk fails; the app shows, and the guide does not open again in this page.
  expect(await screen.findByRole('navigation', { name: 'Main' })).toBeInTheDocument()
  await new Promise((done) => setTimeout(done, 300))
  expect(screen.queryByTestId('guide')).toBeNull()
  expect(window.location.hash).not.toBe('#/guide')
  expect(await db.meta.get(GUIDE_SEEN_KEY)).toBeUndefined()
  expect(await everything()).toEqual(before)
})
