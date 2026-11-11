import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { todayFixture } from '../../test/todayFixture'
import { InstallCard } from './InstallCard'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  vi.unstubAllGlobals()
})

const COPY = 'Add Grit to your Home Screen: tap Share, then Add to Home Screen. Installed, your training log is protected from being cleared.'
const settled = () => new Promise((done) => setTimeout(done, 50))

describe('InstallCard', () => {
  it('installCard_onlyInBrowserNotStandalone', async () => {
    // In a browser tab: the card explains how to install.
    render(<InstallCard db={db} />)
    expect(await screen.findByText(COPY)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Install Grit' })).toBeInTheDocument()
    cleanup()

    // Opened from the Home Screen: nothing to explain.
    vi.stubGlobal('matchMedia', (query: string) => ({ matches: query === '(display-mode: standalone)' }))
    render(<InstallCard db={db} />)
    await settled()
    expect(screen.queryByText(COPY)).toBeNull()
  })

  it('installCard_notNow_staysAway', async () => {
    render(<InstallCard db={db} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Not now' }))
    await waitFor(() => expect(screen.queryByText(COPY)).toBeNull())
    cleanup()

    render(<InstallCard db={db} />)
    await settled()
    expect(screen.queryByText(COPY)).toBeNull()
  })
})
