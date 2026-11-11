import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { decodeBackup } from './backup/backup'
import { getSettings, updateSettings, type GritDb } from './db/db'
import { SCHEMA_VERSIONS, type SchemaVersion } from './db/schema'
import { CREDENTIAL_KEY } from './platform/passkeyLock'
import type { RegisterSW } from './platform/update'
import { locksHeldElsewhere } from './test/tabs'
import { TODAY, addDays, baseline, exerciseNamed, logSet, plan, set, startSession, targets, todayFixture } from './test/todayFixture'
import { todayIso } from './domain'
import { weekdayOf } from './state/useToday'
import { CREDENTIAL_ID, cancelled, clearWebAuthn, mockWebAuthn } from './test/webauthn'
import { RecoveryScreen } from './screens/recovery/RecoveryScreen'
import { UpgradeFailedError } from './db/db'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

function setNavigator(over: { share?: unknown; canShare?: unknown; storage?: unknown; locks?: unknown }) {
  for (const [key, value] of Object.entries(over)) Object.defineProperty(navigator, key, { value, configurable: true })
}

/** A share sheet that takes any file; resolves with the files it was handed. */
function shareSheet() {
  const share = vi.fn(async (_data: { files: File[] }) => {})
  setNavigator({ share, canShare: () => true })
  return share
}
const sharedFile = (share: ReturnType<typeof shareSheet>) => share.mock.calls[0]?.[0].files[0] as File

/** A service worker registration the test drives: `needRefresh()` is the new version arriving. */
function updates() {
  let needRefresh = () => {}
  const reload = vi.fn(async (_reloadPage?: boolean) => {})
  const register: RegisterSW = (options) => {
    needRefresh = () => options?.onNeedRefresh?.()
    return reload
  }
  return { register, reload, arrive: () => act(() => needRefresh()) }
}

/** A test in which the app throws while rendering: React and jsdom would both print the error. */
const swallow = (event: ErrorEvent) => event.preventDefault()
function expectRenderError() {
  vi.spyOn(console, 'error').mockImplementation(() => {})
  window.addEventListener('error', swallow)
}

beforeEach(async () => {
  window.location.hash = ''
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  setNavigator({ share: undefined, canShare: undefined, storage: undefined, locks: undefined })
  clearWebAuthn()
  window.localStorage.clear()
  window.removeEventListener('error', swallow)
  vi.restoreAllMocks()
})

const show = (props: Partial<Parameters<typeof App>[0]> = {}) => render(<App dbName={fx.name} {...props} />)
const nav = () => screen.queryByRole('navigation', { name: 'Main' })
const UPDATE_READY = 'Update ready — tap to refresh'

describe('App safeguards', () => {
  it('locked_rendersNoContent', async () => {
    mockWebAuthn().get.mockRejectedValue(cancelled())
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await db.meta.put({ key: CREDENTIAL_KEY, value: CREDENTIAL_ID })
    const { container } = show()

    expect(await screen.findByRole('heading', { name: 'Grit is locked' })).toBeInTheDocument()
    // Nothing of the app is in the page behind the lock: no screen, no tab bar.
    expect(nav()).toBeNull()
    expect(container.querySelector('.shell')).toBeNull()
    expect(screen.getAllByRole('heading')).toHaveLength(1)
  })

  it('persistence_requestedOnFirstLaunch_statusShown', async () => {
    let granted = false
    const persist = vi.fn(async () => (granted = true))
    setNavigator({ storage: { persist, persisted: async () => granted } })
    window.location.hash = '#/settings'
    show()

    expect(await screen.findByText('Storage: protected')).toBeInTheDocument()
    expect(persist).toHaveBeenCalledTimes(1)
  })

  it('persistence_refused_statusShown', async () => {
    setNavigator({ storage: { persist: async () => false, persisted: async () => false } })
    window.location.hash = '#/settings'
    show()
    expect(await screen.findByText('Storage: not protected')).toBeInTheDocument()
  })

  it('updatePrompt_neverReloadsDuringSession', async () => {
    const sw = updates()
    // A session is being logged.
    await startSession(db, TODAY)
    await db.meta.put({ key: 'today:active', value: { date: TODAY, at: Date.now() } })
    show({ registerSW: sw.register })
    await waitFor(() => expect(nav()).not.toBeNull())

    sw.arrive()
    await new Promise((done) => setTimeout(done, 50))

    // No prompt and no reload while the session is open.
    expect(screen.queryByText(UPDATE_READY)).toBeNull()
    expect(sw.reload).not.toHaveBeenCalled()

    // The session is finished: now the prompt shows, and still nothing reloads until it is tapped.
    await db.meta.delete('today:active')
    const prompt = await screen.findByRole('button', { name: UPDATE_READY })
    expect(sw.reload).not.toHaveBeenCalled()

    await userEvent.click(prompt)
    expect(sw.reload).toHaveBeenCalledTimes(1)
    expect(sw.reload).toHaveBeenCalledWith(true)
  })

  it('updatePrompt_aSessionStartedAfterThePromptShowed_isNotReloaded', async () => {
    const sw = updates()
    show({ registerSW: sw.register })
    await waitFor(() => expect(nav()).not.toBeNull())
    sw.arrive()
    await screen.findByRole('button', { name: UPDATE_READY })

    await db.meta.put({ key: 'today:active', value: { date: TODAY, at: Date.now() } })

    await waitFor(() => expect(screen.queryByText(UPDATE_READY)).toBeNull())
    expect(sw.reload).not.toHaveBeenCalled()
  })

  it('secondTab_showsOpenElsewhere', async () => {
    setNavigator({ locks: locksHeldElsewhere() })
    show()

    expect(await screen.findByRole('heading', { name: 'Grit is open in another tab' })).toBeInTheDocument()
    expect(nav()).toBeNull()
    expect(screen.getAllByRole('heading')).toHaveLength(1)
  })

  it('quotaExceeded_showsErrorAndOffersExport', async () => {
    const share = shareSheet()
    await startSession(db, '2026-11-04')
    window.location.hash = '#/settings'
    show()
    const toggle = await screen.findByRole('switch', { name: 'Change plates' })

    // The phone is full: the browser refuses the write.
    const put = vi.spyOn(IDBObjectStore.prototype, 'put').mockImplementation(() => {
      throw new DOMException('The quota has been exceeded.', 'QuotaExceededError')
    })
    await userEvent.click(toggle)

    const dialog = within(await screen.findByRole('dialog', { name: 'Storage is full' }))
    expect(dialog.getByText(/Nothing was changed/)).toBeInTheDocument()
    put.mockRestore()
    // Nothing was half-written.
    expect((await getSettings(db)).changePlates).toBe(false)

    await userEvent.click(dialog.getByRole('button', { name: 'Export backup' }))

    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))
    const file = sharedFile(share)
    expect(file.name).toMatch(/^grit-backup-\d{4}-\d{2}-\d{2}\.json$/)
    const decoded = decodeBackup(await file.text())
    expect(decoded.ok && decoded.file.sessions).toHaveLength(1)
  })

  it('quotaInCoachPass_showsStorageFullNotRecovery', async () => {
    // A plan for today with a known strength: opening Today makes the coach store a target.
    const squat = await exerciseNamed(db, 'Squat')
    await plan(db, squat.id, { weekday: weekdayOf(todayIso()) })
    await baseline(db, squat.id, 190, addDays(todayIso(), -30))
    await logSet(db, await startSession(db, addDays(todayIso(), -7)), set(squat.id, 150, { rpe: 7 }))
    // The phone is full: the browser refuses every write.
    const refuse = () => {
      throw new DOMException('The quota has been exceeded.', 'QuotaExceededError')
    }
    const add = vi.spyOn(IDBObjectStore.prototype, 'add').mockImplementation(refuse)
    const put = vi.spyOn(IDBObjectStore.prototype, 'put').mockImplementation(refuse)
    show()

    expect(await screen.findByRole('dialog', { name: 'Storage is full' })).toBeInTheDocument()
    // Today is still there, without targets; the app did not fall back to the recovery screen.
    expect(await screen.findByRole('button', { name: 'Start session' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: "Grit couldn't open your log" })).toBeNull()
    expect(nav()).not.toBeNull()
    add.mockRestore()
    put.mockRestore()
    expect(await targets(db)).toHaveLength(0)
    // The rest of the app is reachable.
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Storage is full' })).getByRole('button', { name: 'Close' }))
    await userEvent.click(within(nav()!).getByRole('link', { name: 'Plan' }))
    expect(await screen.findByRole('heading', { name: 'Plan' })).toBeInTheDocument()
  })

  it('privateMode_warns', async () => {
    const share = shareSheet()
    setNavigator({ storage: { getDirectory: async () => Promise.reject(new DOMException('no', 'SecurityError')) } })
    show()

    expect(await screen.findByRole('heading', { name: "Private browsing won't keep your log" })).toBeInTheDocument()
    expect(nav()).toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Export backup' }))
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))

    await userEvent.click(screen.getByRole('button', { name: 'Continue anyway' }))
    await waitFor(() => expect(nav()).not.toBeNull())
    expect(screen.queryByRole('heading', { name: "Private browsing won't keep your log" })).toBeNull()

    // The answer is remembered: the next launch goes straight in.
    cleanup()
    show()
    await waitFor(() => expect(nav()).not.toBeNull())
    expect(screen.queryByRole('heading', { name: "Private browsing won't keep your log" })).toBeNull()
  })

  it('upgradeFailure_opensRecovery', async () => {
    // Review Focus 3: a schema upgrade throws when the new version of Grit first opens.
    const share = shareSheet()
    expectRenderError()
    await startSession(db, '2026-11-04')
    db.close()
    const failing: SchemaVersion[] = [
      ...SCHEMA_VERSIONS,
      {
        version: SCHEMA_VERSIONS.length + 1,
        stores: { extra: '++id' },
        upgrade: async (tx) => {
          await tx.table('sessions').toCollection().modify({ date: '1999-01-01' })
          throw new Error('boom')
        },
      },
    ]
    show({ schemaVersions: failing })

    expect(await screen.findByRole('heading', { name: "Grit couldn't update its storage" })).toBeInTheDocument()
    expect(await screen.findByText(/Your log is unchanged, and Grit saved a safety copy of it before the update/)).toBeInTheDocument()
    expect(nav()).toBeNull()

    // The safety copy written before the upgrade can be taken out of the phone.
    await userEvent.click(await screen.findByRole('button', { name: 'Export safety copy' }))
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))
    const file = sharedFile(share)
    expect(file.name).toMatch(/^grit-safety-copy-\d{4}-\d{2}-\d{2}\.json$/)
    const decoded = decodeBackup(await file.text())
    expect(decoded.ok && decoded.file.sessions).toHaveLength(1)

    // Recovery deleted nothing: the old version still opens the data as it was.
    cleanup()
    const again = await fx.open()
    expect((await again.sessions.toArray()).map((s) => s.date)).toEqual(['2026-11-04'])
  })

  it('recovery_withAppLockOn_asksBeforeExporting', async () => {
    const share = shareSheet()
    const { get } = mockWebAuthn()
    get.mockRejectedValueOnce(cancelled())
    expectRenderError()
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await db.meta.put({ key: CREDENTIAL_KEY, value: CREDENTIAL_ID })
    db.close()
    const failing: SchemaVersion[] = [...SCHEMA_VERSIONS, { version: SCHEMA_VERSIONS.length + 1, stores: {}, upgrade: () => Promise.reject(new Error('boom')) }]
    show({ schemaVersions: failing })

    await userEvent.click(await screen.findByRole('button', { name: 'Export safety copy' }))
    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))
    expect(share).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: 'Export safety copy' }))
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))
  })

  it('recovery_passkeyGone_cantUnlockVerifiesTheOwner_thenExportIsAllowed', async () => {
    const share = shareSheet()
    const { create, get } = mockWebAuthn()
    // The passkey was deleted from the phone: unlocking can only fail.
    get.mockRejectedValue(cancelled())
    create.mockResolvedValue({ type: 'public-key', rawId: new Uint8Array([9, 9, 9]).buffer })
    expectRenderError()
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await db.meta.put({ key: CREDENTIAL_KEY, value: CREDENTIAL_ID })
    db.close()
    const failing: SchemaVersion[] = [...SCHEMA_VERSIONS, { version: SCHEMA_VERSIONS.length + 1, stores: {}, upgrade: () => Promise.reject(new Error('boom')) }]
    show({ schemaVersions: failing })

    await userEvent.click(await screen.findByRole('button', { name: 'Export safety copy' }))
    expect(await screen.findByText('Unlock Grit to export your data.')).toBeInTheDocument()
    expect(share).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: "Can't unlock?" }))
    expect(await screen.findByText('Unlocked. You can export now.')).toBeInTheDocument()
    expect(create).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: "Can't unlock?" })).toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Export safety copy' }))
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))

    // Recovery wrote nothing, the new passkey's id included.
    cleanup()
    const again = await fx.open()
    expect((await again.meta.get(CREDENTIAL_KEY))?.value).toBe(CREDENTIAL_ID)
  })

  it('recovery_cantUnlockCancelled_exportStaysBlocked', async () => {
    const share = shareSheet()
    const { create, get } = mockWebAuthn()
    get.mockRejectedValue(cancelled())
    create.mockRejectedValue(cancelled())
    expectRenderError()
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await db.meta.put({ key: CREDENTIAL_KEY, value: CREDENTIAL_ID })
    db.close()
    show({ schemaVersions: [...SCHEMA_VERSIONS, { version: SCHEMA_VERSIONS.length + 1, stores: {}, upgrade: () => Promise.reject(new Error('boom')) }] })

    await userEvent.click(await screen.findByRole('button', { name: "Can't unlock?" }))
    expect(await screen.findByText('Unlock Grit to export your data.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Export current data' }))
    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))
    expect(share).not.toHaveBeenCalled()
  })

  it('recovery_saysASafetyCopyWasSaved_onlyWhenOneWas', async () => {
    const share = shareSheet()
    // An older copy from an earlier update is in the database; this time writing the copy is what failed.
    await db.meta.put({ key: 'safetyCopy', value: JSON.stringify({ version: 1, exportedAt: new Date(2026, 8, 3, 12).getTime() }) })
    render(<RecoveryScreen error={new UpgradeFailedError(new Error('full'), false)} dbName={fx.name} />)

    const button = await screen.findByRole('button', { name: 'Export safety copy' })
    expect(screen.getByText('Your log is unchanged. Export a copy to keep it safe, then try again.')).toBeInTheDocument()
    expect(screen.queryByText(/saved a safety copy/)).toBeNull()

    // The file is named for the day the copy was made, not for today.
    await userEvent.click(button)
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))
    expect(sharedFile(share).name).toBe('grit-safety-copy-2026-09-03.json')
  })

  it('aFailedOpen_opensRecovery_notABlankPage', async () => {
    expectRenderError()
    // The browser fails to open the database this once.
    vi.spyOn(indexedDB, 'open').mockImplementationOnce(() => {
      throw new DOMException('Internal error opening backing store.', 'UnknownError')
    })
    show()

    expect(await screen.findByRole('heading', { name: "Grit couldn't open your log" })).toBeInTheDocument()
    expect(screen.getByText(/Nothing has been deleted/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Export current data' })).toBeInTheDocument()
    expect(nav()).toBeNull()
  })
})
