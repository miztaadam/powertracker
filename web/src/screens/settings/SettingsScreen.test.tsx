import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { decodeBackup, encodeBackup, exportBackup } from '../../backup/backup'
import { getSettings, readSafetyCopy, updateSettings, writeSafetyCopy, type GritDb } from '../../db/db'
import { CREDENTIAL_KEY } from '../../platform/passkeyLock'
import { TODAY, exerciseNamed, plan, todayFixture } from '../../test/todayFixture'
import { CREDENTIAL_ID, cancelled, clearWebAuthn, mockWebAuthn } from '../../test/webauthn'
import { SettingsScreen } from './SettingsScreen'

const WEB_BACKUP = readFileSync(resolve(__dirname, '../../backup/__fixtures__/web-v1.json'), 'utf8')
const NOW = new Date(2026, 10, 11, 9, 41).getTime()

let fx: ReturnType<typeof todayFixture>
let db: GritDb

/** What the browser offers for sharing and for keeping storage; absent unless a test sets it. */
function setNavigator(over: { share?: unknown; canShare?: unknown; storage?: unknown }) {
  for (const [key, value] of Object.entries(over)) Object.defineProperty(navigator, key, { value, configurable: true })
}

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  // The data a restore would replace.
  await plan(db, (await exerciseNamed(db, 'Squat')).id, { weekday: 1 })
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  setNavigator({ share: undefined, canShare: undefined, storage: undefined })
  clearWebAuthn()
  vi.restoreAllMocks()
})

const show = (onBack: () => void = () => {}) => render(<SettingsScreen db={db} onBack={onBack} today={() => TODAY} now={() => NOW} />)
const dialog = () => within(screen.getByRole('dialog'))
const pick = async (text: string) => userEvent.upload(await screen.findByLabelText('Backup file'), new File([text], 'grit-backup-2026-11-01.json', { type: 'application/json' }))
/** Everything a backup holds, as one comparable string. */
const data = async () => encodeBackup(await exportBackup(db, 0))
const stepper = (name: string) => within(screen.getByRole('group', { name }))
// `restoreBackup` and `exportBackup` reach the tables through `db.table(name)`.
const table = (name: string) => db.table(name)

describe('SettingsScreen', () => {
  it('settings_toggleChangePlates_persists', async () => {
    show()
    expect(screen.getByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
    const toggle = await screen.findByRole('switch', { name: 'Change plates' })
    expect(toggle).toHaveAttribute('aria-checked', 'false')
    expect(screen.getByText('Adds 0.5 and 0.25 kg plates, so loads round to 0.5 kg')).toBeInTheDocument()

    // The whole row toggles, as on Android.
    await userEvent.click(screen.getByText('Change plates'))

    await waitFor(async () => expect((await getSettings(db)).changePlates).toBe(true))
    await waitFor(() => expect(toggle).toHaveAttribute('aria-checked', 'true'))
  })

  it('settings_everySettingPersists', async () => {
    mockWebAuthn()
    show()
    await screen.findByRole('switch', { name: 'Change plates' })

    await userEvent.click(stepper('Default RPE').getByRole('button', { name: 'Increase Default RPE' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Mon' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Sat' }))
    await userEvent.click(stepper('Main lift rest').getByRole('button', { name: 'Increase Main lift rest' }))
    await userEvent.click(stepper('Accessory rest').getByRole('button', { name: 'Decrease Accessory rest' }))
    expect(screen.getByRole('radio', { name: 'Sound' })).toBeChecked()
    await userEvent.click(screen.getByRole('radio', { name: 'Silent' }))
    await userEvent.click(screen.getByRole('switch', { name: 'Keep screen awake during rests' }))
    await userEvent.click(screen.getByRole('switch', { name: 'App lock' }))

    await waitFor(async () =>
      expect(await getSettings(db)).toMatchObject({
        defaultRpe: 8.5,
        trainingDays: [3, 5, 6],
        restMainSec: 210,
        restAccessorySec: 75,
        restAlert: 'SILENT',
        keepAwake: false,
        appLock: true,
      }),
    )
    await waitFor(() => expect(screen.getByRole('radio', { name: 'Silent' })).toBeChecked())
    expect(stepper('Main lift rest').getByText('210 s')).toBeInTheDocument()
    expect(screen.getAllByRole('radio').map((r) => r.parentElement?.textContent)).toEqual(['Sound', 'Silent', 'Off'])
  })

  it('appLock_on_makesThePasskey_off_forgetsIt', async () => {
    const { create } = mockWebAuthn()
    show()
    const toggle = await screen.findByRole('switch', { name: 'App lock' })

    await userEvent.click(toggle)
    await waitFor(() => expect(toggle).toHaveAttribute('aria-checked', 'true'))
    expect(create).toHaveBeenCalledTimes(1)
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(CREDENTIAL_ID)
    expect((await getSettings(db)).appLock).toBe(true)

    await userEvent.click(toggle)
    await waitFor(() => expect(toggle).toHaveAttribute('aria-checked', 'false'))
    await waitFor(async () => expect(await db.meta.get(CREDENTIAL_KEY)).toBeUndefined())
    expect((await getSettings(db)).appLock).toBe(false)
  })

  it('appLock_cancelled_staysOff', async () => {
    mockWebAuthn().create.mockRejectedValueOnce(cancelled())
    show()
    const toggle = await screen.findByRole('switch', { name: 'App lock' })

    await userEvent.click(toggle)

    expect(await screen.findByText('App lock stays off.')).toBeInTheDocument()
    expect(toggle).toHaveAttribute('aria-checked', 'false')
    expect((await getSettings(db)).appLock).toBe(false)
    expect(await db.meta.get(CREDENTIAL_KEY)).toBeUndefined()
  })

  it('unsupported_explainsAndStaysUnlocked', async () => {
    // No WebAuthn here, as in a browser without Face ID, Touch ID or a passcode.
    show()
    const toggle = await screen.findByRole('switch', { name: 'App lock' })

    await userEvent.click(toggle)

    expect(await screen.findByText("This browser can't use Face ID, Touch ID or a passcode for Grit, so App lock stays off.")).toBeInTheDocument()
    expect(toggle).toHaveAttribute('aria-checked', 'false')
    expect((await getSettings(db)).appLock).toBe(false)
  })

  it('settings_steppersStopAtTheirLimits', async () => {
    await updateSettings(db, (s) => ({ ...s, defaultRpe: 10, restMainSec: 60, restAccessorySec: 300 }))
    show()
    await screen.findByRole('switch', { name: 'Change plates' })
    expect(stepper('Default RPE').getByText('10')).toBeInTheDocument()
    expect(stepper('Default RPE').getByRole('button', { name: 'Increase Default RPE' })).toBeDisabled()
    expect(stepper('Main lift rest').getByRole('button', { name: 'Decrease Main lift rest' })).toBeDisabled()
    expect(stepper('Accessory rest').getByRole('button', { name: 'Increase Accessory rest' })).toBeDisabled()
  })

  it('restore_tooNew_showsMessage', async () => {
    show()
    const before = await data()
    await pick('{"version":2}')
    expect(await screen.findByText('This backup is from a newer version of Grit.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await data()).toBe(before)
  })

  it('restore_corrupt_showsMessage', async () => {
    show()
    const before = await data()
    await pick('not json')
    expect(await screen.findByText("This file isn't a valid Grit backup.")).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await data()).toBe(before)
  })

  it('restore_ok_confirmsReplaceAllData', async () => {
    show()
    const before = await data()
    await pick(WEB_BACKUP)

    // Nothing is replaced until the lifter says so.
    expect(await screen.findByRole('dialog', { name: 'Replace all data?' })).toBeInTheDocument()
    expect(dialog().getByText('Restoring replaces everything in Grit with the contents of this backup.')).toBeInTheDocument()
    expect(await data()).toBe(before)
    await userEvent.click(dialog().getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await data()).toBe(before)

    // The same file can be chosen again.
    await pick(WEB_BACKUP)
    await screen.findByRole('dialog')
    await userEvent.click(dialog().getByRole('button', { name: 'Replace' }))
    expect(await screen.findByText('Backup restored.')).toBeInTheDocument()
    const restored = decodeBackup(WEB_BACKUP)
    if (!restored.ok) throw new Error('fixture must decode')
    expect((await exportBackup(db, 0)).planItems).toEqual(restored.file.planItems)
    expect(await getSettings(db)).toMatchObject({ changePlates: true, defaultRpe: 7.5, restAlert: 'SILENT' })
    await waitFor(() => expect(screen.getByRole('switch', { name: 'Change plates' })).toHaveAttribute('aria-checked', 'true'))
  })

  it('restore_confirmButtonSaysReplace', async () => {
    show()
    await pick(WEB_BACKUP)
    await screen.findByRole('dialog')
    expect(dialog().getAllByRole('button').map((b) => b.textContent)).toEqual(['Cancel', 'Replace'])
  })

  it('restore_failure_keepsExistingData', async () => {
    show()
    const before = await data()
    await pick(WEB_BACKUP)
    await screen.findByRole('dialog')
    // The write fails part-way: after the old rows were cleared and some new ones written.
    vi.spyOn(table('sets'), 'bulkAdd').mockRejectedValue(new Error('disk full'))
    await userEvent.click(dialog().getByRole('button', { name: 'Replace' }))

    expect(await screen.findByText("Couldn't restore the backup.")).toBeInTheDocument()
    expect(await data()).toBe(before)
  })

  it('export_sharesTheFile_andRecordsTheBackup', async () => {
    const share = vi.fn(async (_data: ShareData) => {})
    setNavigator({ share, canShare: () => true })
    show()
    expect(await screen.findByText('Last backup: never')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Export backup' }))

    expect(await screen.findByText('Backup saved.')).toBeInTheDocument()
    const file = share.mock.calls[0]![0].files![0]!
    expect(file.name).toBe('grit-backup-2026-11-11.json')
    expect(file.type).toBe('application/json')
    const decoded = decodeBackup(await file.text())
    expect(decoded.ok && decoded.file.exportedAt).toBe(NOW)
    expect(decoded.ok && decoded.file.planItems).toHaveLength(1)
    expect((await getSettings(db)).lastBackupAt).toBe(new Date(NOW).toISOString())
    expect(await screen.findByText('Last backup: Nov 11, 2026')).toBeInTheDocument()
  })

  it('export_cancelledShare_recordsNothing', async () => {
    setNavigator({ share: vi.fn().mockRejectedValue(new DOMException('cancelled', 'AbortError')), canShare: () => true })
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Export backup' }))
    await waitFor(() => expect(navigator.share).toHaveBeenCalled())
    expect(screen.queryByText('Backup saved.')).toBeNull()
    expect(screen.queryByText("Couldn't save the backup.")).toBeNull()
    expect((await getSettings(db)).lastBackupAt).toBeNull()
  })

  it('export_withoutShare_downloadsTheFile', async () => {
    const urls = { createObjectURL: vi.fn(() => 'blob:grit'), revokeObjectURL: vi.fn() }
    Object.assign(URL, urls)
    const clicked: { download: string; href: string }[] = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      clicked.push({ download: this.download, href: this.href })
    })
    show()

    await userEvent.click(await screen.findByRole('button', { name: 'Export backup' }))

    expect(await screen.findByText('Backup saved.')).toBeInTheDocument()
    expect(clicked).toEqual([{ download: 'grit-backup-2026-11-11.json', href: 'blob:grit' }])
    expect((await getSettings(db)).lastBackupAt).toBe(new Date(NOW).toISOString())
  })

  it('export_failure_saysSo', async () => {
    show()
    const exportButton = await screen.findByRole('button', { name: 'Export backup' })
    vi.spyOn(table('sessions'), 'toArray').mockRejectedValue(new Error('unreadable'))
    await userEvent.click(exportButton)
    expect(await screen.findByText("Couldn't save the backup.")).toBeInTheDocument()
    expect((await getSettings(db)).lastBackupAt).toBeNull()
  })

  it('storageStatus_protectedOrNot', async () => {
    show()
    expect(await screen.findByText('Storage: not protected')).toBeInTheDocument()
    cleanup()
    setNavigator({ storage: { persisted: async () => true } })
    show()
    expect(await screen.findByText('Storage: protected')).toBeInTheDocument()
  })

  it('safetyCopy_rowOnlyWhenACopyExists_andRestoreAsksFirst', async () => {
    show()
    await screen.findByText('Last backup: never')
    expect(screen.queryByRole('button', { name: 'Restore safety copy' })).toBeNull()
    cleanup()

    await writeSafetyCopy(db)
    // Changed after the copy was made.
    await db.planItems.clear()
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Restore safety copy' }))
    expect(screen.getByRole('dialog', { name: 'Replace all data?' })).toBeInTheDocument()
    expect(await db.planItems.count()).toBe(0)
    await userEvent.click(dialog().getByRole('button', { name: 'Replace' }))
    expect(await screen.findByText('Backup restored.')).toBeInTheDocument()
    expect(await db.planItems.count()).toBe(1)
    // The copy stays, so it can be used again.
    expect(await readSafetyCopy(db)).not.toBeNull()
  })

  it('safetyCopy_rawDump_canOnlyBeExported', async () => {
    await db.meta.put({ key: 'safetyCopy', value: JSON.stringify({ kind: 'raw', schemaVersion: 1, tables: {} }) })
    const share = vi.fn(async (_data: ShareData) => {})
    setNavigator({ share, canShare: () => true })
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Export safety copy' }))
    await waitFor(() => expect(share).toHaveBeenCalled())
    expect(share.mock.calls[0]![0].files![0]!.name).toBe('grit-safety-copy-2026-11-11.json')
    expect(screen.queryByRole('button', { name: 'Restore safety copy' })).toBeNull()
    // Not a backup the lifter made of the current data.
    expect((await getSettings(db)).lastBackupAt).toBeNull()
  })

  it('back_leavesSettings', async () => {
    const onBack = vi.fn()
    show(onBack)
    await userEvent.click(screen.getByRole('button', { name: 'Back' }))
    expect(onBack).toHaveBeenCalledOnce()
  })
})
