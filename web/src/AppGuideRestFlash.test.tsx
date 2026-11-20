import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { App } from './App'
import { updateSettings, type GritDb } from './db/db'
import { REST_TIMER_KEY } from './state/useRest'
import { todayFixture } from './test/todayFixture'

/*
 * The guide (1.16) is drawn in place of the app's screens. The flash that marks a rest's end must not replay when it
 * closes, and a rest that ends while it is up flashes once.
 *
 * The rest's end is reached by moving the app's clock (`Date.now`, which the rest timer reads on its ticks), not by
 * waiting: a rest due a fixed time after it is saved could end before the app and the guide's chunk had loaded, on a
 * busy machine. Timers run in real time.
 */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
/** How many times a flash was put on the page. */
let flashes: number
let watcher: MutationObserver
/** How far the app's clock is ahead of the real one. */
let aheadMs: number

beforeEach(async () => {
  window.location.hash = ''
  aheadMs = 0
  const realNow = Date.now.bind(Date)
  vi.spyOn(Date, 'now').mockImplementation(() => realNow() + aheadMs)
  fx = todayFixture()
  db = await fx.open()
  // The flash alone: no tone to play.
  await updateSettings(db, (s) => ({ ...s, restAlert: 'SILENT' }))
  flashes = 0
  watcher = new MutationObserver((records) => {
    for (const r of records) for (const node of r.addedNodes) if (node instanceof HTMLElement && node.matches('[data-testid="rest-flash"]')) flashes++
  })
  watcher.observe(document.body, { childList: true, subtree: true })
})
afterEach(async () => {
  watcher.disconnect()
  cleanup()
  await fx.dispose()
  vi.restoreAllMocks()
})

/** Far enough ahead that no load, however slow, reaches it before the clock is moved there. */
const REST_MS = 60_000

/** A rest saved as running, ending [REST_MS] from now: the app picks it up as it opens. */
const restRunning = () => db.meta.put({ key: REST_TIMER_KEY, value: { endsAtMs: Date.now() + REST_MS, totalSec: 90 } })

/** Moves the app's clock just past the rest's end: its next tick (every 200 ms) notices zero, about on time. */
const restEnds = () => {
  aheadMs = REST_MS + 50
}

const flash = () => screen.queryByTestId('rest-flash')
const flashGone = () => waitFor(() => expect(flash()).toBeNull(), { timeout: 3000 })
const tabBar = () => screen.findByRole('navigation', { name: 'Main' })

/** Long enough for a flash, had one been put back, to show. */
const aMoment = () => act(async () => void (await new Promise((done) => setTimeout(done, 300))))

async function openTheGuide() {
  window.location.hash = '#/guide'
  expect(await screen.findByTestId('guide')).toBeInTheDocument()
}

async function closeTheGuide() {
  fireEvent.keyDown(window, { key: 'Escape' })
  await tabBar()
  expect(screen.queryByTestId('guide')).toBeNull()
}

it('aRestThatEndedBeforeTheGuide_doesNotFlashAgainWhenTheGuideCloses', async () => {
  await restRunning()
  window.location.hash = '#/today'
  render(<App dbName={fx.name} />)
  await tabBar()
  restEnds()
  await waitFor(() => expect(flash()).toBeInTheDocument(), { timeout: 3000 })
  await flashGone()
  expect(flashes).toBe(1)
  await openTheGuide()
  await closeTheGuide()
  await aMoment()
  expect(flash()).toBeNull()
  expect(flashes).toBe(1)
}, 10_000)

it('aRestThatEndsWhileTheGuideIsUp_flashesOnce', async () => {
  await restRunning()
  window.location.hash = '#/guide'
  render(<App dbName={fx.name} />)
  expect(await screen.findByTestId('guide')).toBeInTheDocument()
  expect(flashes).toBe(0)
  restEnds()
  await waitFor(() => expect(flashes).toBe(1), { timeout: 3000 })
  await flashGone()
  await closeTheGuide()
  await aMoment()
  expect(flash()).toBeNull()
  expect(flashes).toBe(1)
}, 10_000)
