import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { App } from './App'
import { updateSettings, type GritDb } from './db/db'
import { REST_TIMER_KEY } from './state/useRest'
import { todayFixture } from './test/todayFixture'

/*
 * The guide (1.16) is drawn in place of the app's screens. The flash that marks a rest's end must not replay when it
 * closes, and a rest that ends while it is up flashes once.
 */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
/** How many times a flash was put on the page. */
let flashes: number
let watcher: MutationObserver

beforeEach(async () => {
  window.location.hash = ''
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

/** A rest saved as running, ending `inMs` from now: the app picks it up as it opens. */
const restEndingIn = (inMs: number) => db.meta.put({ key: REST_TIMER_KEY, value: { endsAtMs: Date.now() + inMs, totalSec: 90 } })

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
  await restEndingIn(300)
  window.location.hash = '#/today'
  render(<App dbName={fx.name} />)
  await tabBar()
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
  await restEndingIn(1000)
  window.location.hash = '#/guide'
  render(<App dbName={fx.name} />)
  expect(await screen.findByTestId('guide')).toBeInTheDocument()
  expect(flashes).toBe(0)
  await waitFor(() => expect(flashes).toBe(1), { timeout: 3000 })
  await flashGone()
  await closeTheGuide()
  await aMoment()
  expect(flash()).toBeNull()
  expect(flashes).toBe(1)
}, 10_000)
