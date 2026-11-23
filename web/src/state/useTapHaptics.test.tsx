import { render, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { updateSettings, type GritDb } from '../db/db'
import { haptic, setTapHaptics } from '../platform/haptics'
import { todayFixture } from '../test/todayFixture'
import { useTapHaptics } from './useTapHaptics'

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let vibrate: ReturnType<typeof vi.fn>

function Root({ db }: { db: GritDb | null }) {
  useTapHaptics(db)
  return null
}

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  vibrate = vi.fn(() => true)
  Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
})

afterEach(async () => {
  setTapHaptics(false)
  Reflect.deleteProperty(navigator, 'vibrate')
  await fx.dispose()
})

/** Taps once, then reports whether that tap buzzed. */
function buzzes(): boolean {
  vibrate.mockClear()
  haptic('tick')
  return vibrate.mock.calls.length > 0
}

it('follows "Vibrate on taps" live, and buzzes nothing before the settings are read or after the app is gone', async () => {
  expect(buzzes()).toBe(false)
  const { unmount } = render(<Root db={db} />)
  // On by default.
  await waitFor(() => expect(buzzes()).toBe(true))
  await updateSettings(db, (s) => ({ ...s, vibrateOnTaps: false }))
  await waitFor(() => expect(buzzes()).toBe(false))
  await updateSettings(db, (s) => ({ ...s, vibrateOnTaps: true }))
  await waitFor(() => expect(buzzes()).toBe(true))
  unmount()
  expect(buzzes()).toBe(false)
})

it('stays off with no database', () => {
  render(<Root db={null} />)
  expect(buzzes()).toBe(false)
})
