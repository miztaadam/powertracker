import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { decodeBackup } from '../../backup/backup'
import { TODAY, startSession, todayFixture } from '../../test/todayFixture'
import { INSTALL_DISMISSED_KEY, InstallCard } from './InstallCard'

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
  for (const key of ['share', 'canShare']) Object.defineProperty(navigator, key, { value: undefined, configurable: true })
})

const COPY = 'In Safari, tap Share, then Add to Home Screen, and open Grit from the new icon. Installed, your training log is protected from being cleared.'
const MOVE = /The installed app keeps its own log, apart from Safari's, and starts empty\. You have already logged here: tap Export backup before you add Grit to the Home Screen, then restore that file in the installed app \(Plan, Settings, Restore backup\)\./
/** Android: Chrome's installed app shares this browser's storage, so there is nothing to move, only a copy to keep. */
const SHARED_ANDROID = 'Your log stays in this browser, so the installed app opens with it. Export a backup now and then to keep a copy.'
const settled = () => new Promise((done) => setTimeout(done, 50))
/** A write of `meta` held until the test lets it land (Dexie's own promise type, which a plain one stands in for). */
type Held = ReturnType<GritDb['meta']['put']>

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

  it('installCard_notNow_hidesOnlyOnceTheAnswerIsSaved', async () => {
    let land!: () => void
    const put = db.meta.put.bind(db.meta)
    const spy = vi.spyOn(db.meta, 'put').mockImplementationOnce(
      (row) =>
        new Promise<string>((done, fail) => {
          land = () => put(row).then(done, fail)
        }) as Held,
    )
    render(<InstallCard db={db} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Not now' }))
    expect(spy).toHaveBeenCalledTimes(1)
    // The write has not landed: the card is still there.
    await settled()
    expect(screen.getByText(COPY)).toBeInTheDocument()
    land()
    await waitFor(() => expect(screen.queryByText(COPY)).toBeNull())
    expect((await db.meta.get(INSTALL_DISMISSED_KEY))?.value).toBe(true)
  })

  it('installCard_notNow_stillHides_whenSavingTheAnswerFails', async () => {
    let fail!: () => void
    vi.spyOn(db.meta, 'put').mockImplementationOnce(
      () =>
        new Promise<string>((_done, reject) => {
          fail = () => reject(new Error('quota'))
        }) as Held,
    )
    render(<InstallCard db={db} />)
    await userEvent.click(await screen.findByRole('button', { name: 'Not now' }))
    await settled()
    expect(screen.getByText(COPY)).toBeInTheDocument()
    fail()
    await waitFor(() => expect(screen.queryByText(COPY)).toBeNull())
    // Nothing was saved, and the card stays away while this page is open, also when the log changes.
    expect(await db.meta.get(INSTALL_DISMISSED_KEY)).toBeUndefined()
    await startSession(db, TODAY)
    await settled()
    expect(screen.queryByText(COPY)).toBeNull()
  })

  it('installCard_emptyLog_hasNothingToMove', async () => {
    render(<InstallCard db={db} />)
    expect(await screen.findByText(COPY)).toBeInTheDocument()
    await settled()
    expect(screen.queryByText(MOVE)).toBeNull()
    expect(screen.queryByRole('button', { name: 'Export backup' })).toBeNull()
  })

  it('installCard_withALogInSafari_saysToExportFirst_andExports', async () => {
    const share = vi.fn(async (_data: { files: File[] }) => {})
    Object.defineProperty(navigator, 'share', { value: share, configurable: true })
    Object.defineProperty(navigator, 'canShare', { value: () => true, configurable: true })
    await startSession(db, TODAY)
    render(<InstallCard db={db} />)

    // The original line first, then the step that keeps the log.
    expect(await screen.findByText(COPY)).toBeInTheDocument()
    expect(await screen.findByText(MOVE)).toBeInTheDocument()
    // A double tap exports once.
    const button = screen.getByRole('button', { name: 'Export backup' })
    await userEvent.dblClick(button)

    expect(await screen.findByText('Backup saved.')).toBeInTheDocument()
    expect(share).toHaveBeenCalledTimes(1)
    const file = share.mock.calls[0]![0].files[0]!
    expect(file.name).toMatch(/^grit-backup-\d{4}-\d{2}-\d{2}\.json$/)
    const decoded = decodeBackup(await file.text())
    expect(decoded.ok && decoded.file.sessions).toHaveLength(1)
  })

  it('installCard_aPlanAlone_isWorthMovingToo', async () => {
    await db.planItems.add({ weekday: 1, exerciseId: 1, sets: 3, reps: 5, targetRpe: 8, order: 0 })
    render(<InstallCard db={db} />)
    expect(await screen.findByText(MOVE)).toBeInTheDocument()
  })
})

describe('InstallCard wording by phone', () => {
  const ANDROID_UA = 'Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36'
  const IPHONE_UA = 'Mozilla/5.0 (iPhone; CPU iPhone OS 26_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Mobile/15E148 Safari/604.1'
  const ANDROID_COPY = "Open your browser's menu (⋮), then tap Install app or Add to Home screen. Installed, your training log is protected from being cleared."
  const fakeAgent = (ua: string) => Object.defineProperty(navigator, 'userAgent', { value: ua, configurable: true })
  afterEach(() => {
    delete (navigator as unknown as Record<string, unknown>).userAgent
  })

  it('installCard_android_saysToUseTheBrowsersMenu_andKeepsTheRest', async () => {
    fakeAgent(ANDROID_UA)
    await startSession(db, TODAY)
    render(<InstallCard db={db} />)
    expect(await screen.findByText(ANDROID_COPY)).toBeInTheDocument()
    expect(screen.queryByText(COPY)).toBeNull()
    // No separate-storage note: the installed app opens with this log. The export stays.
    expect(await screen.findByText(SHARED_ANDROID)).toBeInTheDocument()
    expect(screen.queryByText(MOVE)).toBeNull()
    expect(screen.getByRole('button', { name: 'Export backup' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Not now' })).toBeInTheDocument()
  })

  it('installCard_iPhone_keepsTheSafariSteps_andTheSeparateStorageNote', async () => {
    fakeAgent(IPHONE_UA)
    await startSession(db, TODAY)
    render(<InstallCard db={db} />)
    expect(await screen.findByText(COPY)).toBeInTheDocument()
    expect(screen.queryByText(ANDROID_COPY)).toBeNull()
    expect(await screen.findByText(MOVE)).toBeInTheDocument()
    expect(screen.queryByText(SHARED_ANDROID)).toBeNull()
    expect(screen.getByRole('button', { name: 'Export backup' })).toBeInTheDocument()
  })
})
