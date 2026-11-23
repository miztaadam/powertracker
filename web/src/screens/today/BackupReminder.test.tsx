import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { markBackedUp } from '../../backup/backup'
import type { GritDb } from '../../db/db'
import { TODAY, addDays, startSession, todayFixture } from '../../test/todayFixture'
import { TodayScreen } from './TodayScreen'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

const setProtected = (value: boolean | undefined) =>
  Object.defineProperty(navigator, 'storage', { value: value == null ? undefined : { persisted: async () => value }, configurable: true })
/** Noon, `days` before TODAY. */
const daysAgo = (days: number) => new Date(`${addDays(TODAY, -days)}T12:00:00`).getTime()

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  await startSession(db, addDays(TODAY, -60))
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  setProtected(undefined)
})

const show = (onOpenSettings: () => void = () => {}) => render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} onOpenSettings={onOpenSettings} />)
const reminder = () => screen.queryByRole('heading', { name: 'Back up your log' })
/** The screen has loaded and the reminder has had its chance to appear. */
const settled = async () => {
  await screen.findByRole('button', { name: 'Start session' })
  await new Promise((done) => setTimeout(done, 50))
}

describe('Today backup reminder', () => {
  it('backupReminder_after14Days_or7WhenUnprotected', async () => {
    setProtected(true)
    await markBackedUp(db, daysAgo(13))
    show()
    await settled()
    expect(reminder()).toBeNull()
    cleanup()

    await markBackedUp(db, daysAgo(14))
    show()
    expect(await screen.findByRole('heading', { name: 'Back up your log' })).toBeInTheDocument()
    expect(screen.getByText('Your last backup was 14 days ago. Your log lives only on this phone.')).toBeInTheDocument()
    cleanup()

    // Storage the browser may clear: a week is long enough.
    setProtected(false)
    await markBackedUp(db, daysAgo(6))
    show()
    await settled()
    expect(reminder()).toBeNull()
    cleanup()

    await markBackedUp(db, daysAgo(7))
    show()
    expect(await screen.findByRole('heading', { name: 'Back up your log' })).toBeInTheDocument()
  })

  it('neverBackedUp_remindsOnceThereIsALogToLose', async () => {
    show()
    expect(await screen.findByText('You have not exported a backup yet. Your log lives only on this phone.')).toBeInTheDocument()
    cleanup()

    await db.sessions.clear()
    show()
    await settled()
    expect(reminder()).toBeNull()
  })

  it('notNow_hidesItForTheDay_andBackUpOpensSettings', async () => {
    const onOpenSettings = vi.fn()
    show(onOpenSettings)
    await screen.findByRole('heading', { name: 'Back up your log' })
    await userEvent.click(screen.getByRole('button', { name: 'Back up' }))
    expect(onOpenSettings).toHaveBeenCalledOnce()

    await userEvent.click(screen.getByRole('button', { name: 'Not now' }))
    await waitFor(() => expect(reminder()).toBeNull())
    // Still hidden when the screen is opened again the same day; back the next day.
    cleanup()
    show()
    await settled()
    expect(reminder()).toBeNull()
    cleanup()
    render(<TodayScreen db={db} today={() => addDays(TODAY, 1)} onOpenCoach={() => {}} onOpenSettings={() => {}} />)
    expect(await screen.findByRole('heading', { name: 'Back up your log' })).toBeInTheDocument()
  })

  it('staysOutOfTheWayDuringASession', async () => {
    await startSession(db, TODAY)
    show()
    await screen.findByRole('button', { name: 'Add exercise' })
    await new Promise((done) => setTimeout(done, 50))
    expect(reminder()).toBeNull()
  })
})
