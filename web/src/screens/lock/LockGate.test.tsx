import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { getSettings, updateSettings, type GritDb } from '../../db/db'
import { CREDENTIAL_KEY } from '../../platform/passkeyLock'
import { todayFixture } from '../../test/todayFixture'
import { CREDENTIAL_ID, authenticatorData, cancelled, clearWebAuthn, mockWebAuthn, pendingUntilAborted } from '../../test/webauthn'
import { LockGateHost } from './LockGateHost'

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let clock = 1_000_000

beforeEach(async () => {
  clock = 1_000_000
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  clearWebAuthn()
  setVisibility('visible')
})

function setVisibility(state: 'visible' | 'hidden') {
  Object.defineProperty(document, 'visibilityState', { value: state, configurable: true })
}
function leave() {
  act(() => {
    setVisibility('hidden')
    document.dispatchEvent(new Event('visibilitychange'))
  })
}
function comeBack() {
  act(() => {
    setVisibility('visible')
    document.dispatchEvent(new Event('visibilitychange'))
  })
}

/** App lock switched on, with its passkey made on this phone. */
async function lockOn() {
  await updateSettings(db, (s) => ({ ...s, appLock: true }))
  await db.meta.put({ key: CREDENTIAL_KEY, value: CREDENTIAL_ID })
}

const host = () => (
  <LockGateHost db={db} now={() => clock}>
    <p>the training log</p>
  </LockGateHost>
)
const show = () => render(host())
/** A passkey the phone made: `flags` 0x05 with its owner verified, 0x01 without. */
const passkey = (bytes: number[], flags = 0x05) => ({ type: 'public-key', rawId: new Uint8Array(bytes).buffer, response: { getAuthenticatorData: () => authenticatorData(flags) } })
const NEW_ID = 'CQkJ'
const content = () => screen.queryByText('the training log')

describe('LockGateHost', () => {
  it('locked_rendersNoContent', async () => {
    const { get } = mockWebAuthn()
    // The Face ID sheet is closed without unlocking.
    get.mockRejectedValue(cancelled())
    await lockOn()
    const { container } = show()

    expect(await screen.findByRole('heading', { name: 'Grit is locked' })).toBeInTheDocument()
    expect(screen.getByText('Use Face ID, Touch ID or your passcode to open your training log.')).toBeInTheDocument()
    expect(content()).toBeNull()
    expect(container.textContent).not.toContain('the training log')
    // Asked once on its own, as on Android; the button asks again.
    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))

    get.mockResolvedValue({ type: 'public-key', rawId: new ArrayBuffer(1) })
    await userEvent.click(screen.getByRole('button', { name: 'Unlock' }))

    expect(await screen.findByText('the training log')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Grit is locked' })).toBeNull()
  })

  it('nothingShows_untilTheSettingIsRead', async () => {
    mockWebAuthn().get.mockRejectedValue(cancelled())
    await lockOn()
    const { container } = show()
    // First paint: neither the log nor the lock screen.
    expect(container.textContent).toBe('')
    await screen.findByRole('heading', { name: 'Grit is locked' })
  })

  it('lockOff_showsTheContent', async () => {
    show()
    expect(await screen.findByText('the training log')).toBeInTheDocument()
  })

  it('aMinuteAway_locksAgain_aShortTripDoesNot', async () => {
    const { get } = mockWebAuthn()
    await lockOn()
    show()
    expect(await screen.findByText('the training log')).toBeInTheDocument()

    leave()
    clock += 59_999
    comeBack()
    expect(content()).not.toBeNull()

    get.mockRejectedValue(cancelled())
    leave()
    clock += 60_000
    comeBack()
    expect(content()).toBeNull()
    expect(screen.getByRole('heading', { name: 'Grit is locked' })).toBeInTheDocument()

    // Leaving again while locked keeps the first time: the lock cannot be waited out.
    leave()
    clock += 1_000
    comeBack()
    expect(content()).toBeNull()
  })

  it('turningTheLockOn_doesNotLockThisVisit', async () => {
    mockWebAuthn()
    show()
    await screen.findByText('the training log')

    await lockOn()
    await new Promise((done) => setTimeout(done, 50))

    expect(content()).not.toBeNull()
  })

  it('unsupported_explainsAndStaysUnlocked', async () => {
    // The lock is on, but this browser has nothing to unlock with.
    await lockOn()
    show()

    expect(await screen.findByRole('heading', { name: "App lock isn't available here" })).toBeInTheDocument()
    expect(screen.getByText(/so your log isn't locked/)).toBeInTheDocument()
    expect(content()).toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Continue' }))
    expect(await screen.findByText('the training log')).toBeInTheDocument()
  })

  it('lockOnWithoutPasskey_opens_andOffersSetUpOrOff', async () => {
    // What a restored backup leaves on a new phone: the setting, but no passkey.
    const { create, get } = mockWebAuthn()
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    show()

    expect(await screen.findByText('the training log')).toBeInTheDocument()
    const dialog = within(await screen.findByRole('dialog', { name: 'Set up App lock again?' }))
    expect(get).not.toHaveBeenCalled()

    await userEvent.click(dialog.getByRole('button', { name: 'Set up' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(create).toHaveBeenCalledTimes(1)
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(CREDENTIAL_ID)
    // Set up in this visit: it locks from the next one.
    expect(content()).not.toBeNull()
  })

  it('lockOnWithoutPasskey_turnOff', async () => {
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    show()
    const dialog = within(await screen.findByRole('dialog', { name: 'Set up App lock again?' }))

    await userEvent.click(dialog.getByRole('button', { name: 'Turn off' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect((await getSettings(db)).appLock).toBe(false)
    expect(content()).not.toBeNull()
  })

  it('lockOnWithoutPasskey_setUpUnsupported_explains', async () => {
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    show()
    const dialog = within(await screen.findByRole('dialog', { name: 'Set up App lock again?' }))

    await userEvent.click(dialog.getByRole('button', { name: 'Set up' }))

    expect(await dialog.findByText(/This browser can't use Face ID, Touch ID or a passcode for Grit/)).toBeInTheDocument()
    expect(content()).not.toBeNull()
  })

  it('aShortTrip_isForgotten_andCannotLockLater', async () => {
    mockWebAuthn()
    await lockOn()
    const { rerender } = show()
    await screen.findByText('the training log')

    // A glance at a message, half a minute.
    leave()
    clock += 30_000
    comeBack()
    // Back in the app for longer than the rest of the minute: any redraw must leave it open.
    clock += 31_000
    rerender(host())
    expect(content()).not.toBeNull()

    leave()
    clock += 2_000
    comeBack()
    expect(content()).not.toBeNull()
  })

  it('unlockTapped_whileThePromptIsUp_withdrawsIt_andAsksAgain', async () => {
    const { get } = mockWebAuthn()
    // The prompt Grit brings up on launch just sits there (Safari may not even show it without a tap).
    get.mockImplementationOnce(pendingUntilAborted)
    await lockOn()
    show()
    await screen.findByRole('heading', { name: 'Grit is locked' })
    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))

    await userEvent.click(screen.getByRole('button', { name: 'Unlock' }))

    expect(get).toHaveBeenCalledTimes(2)
    expect(get.mock.calls[0]?.[0].signal?.aborted).toBe(true)
    expect(await screen.findByText('the training log')).toBeInTheDocument()
  })

  it('cantUnlock_makesANewPasskey_andOpens', async () => {
    // The passkey was deleted from the phone's passwords: unlocking can only fail.
    const { create, get } = mockWebAuthn()
    get.mockRejectedValue(cancelled())
    create.mockResolvedValue(passkey([9, 9, 9]))
    await lockOn()
    show()
    await screen.findByRole('heading', { name: 'Grit is locked' })

    await userEvent.click(screen.getByRole('button', { name: "Can't unlock?" }))

    expect(await screen.findByText('the training log')).toBeInTheDocument()
    expect(create).toHaveBeenCalledTimes(1)
    // The new passkey takes the old one's place.
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(NEW_ID)
  })

  it('cantUnlock_cancelled_staysLocked', async () => {
    const { create, get } = mockWebAuthn()
    get.mockRejectedValue(cancelled())
    create.mockRejectedValue(cancelled())
    await lockOn()
    show()
    await screen.findByRole('heading', { name: 'Grit is locked' })

    await userEvent.click(screen.getByRole('button', { name: "Can't unlock?" }))
    await waitFor(() => expect(create).toHaveBeenCalledTimes(1))
    await new Promise((done) => setTimeout(done, 50))

    expect(content()).toBeNull()
    expect(screen.getByRole('heading', { name: 'Grit is locked' })).toBeInTheDocument()
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(CREDENTIAL_ID)
  })

  it('cantUnlock_ownerNotVerified_staysLocked', async () => {
    const { create, get } = mockWebAuthn()
    get.mockRejectedValue(cancelled())
    create.mockResolvedValue(passkey([9, 9, 9], 0x01))
    await lockOn()
    show()
    await screen.findByRole('heading', { name: 'Grit is locked' })

    await userEvent.click(screen.getByRole('button', { name: "Can't unlock?" }))
    await waitFor(() => expect(create).toHaveBeenCalledTimes(1))
    await new Promise((done) => setTimeout(done, 50))

    expect(content()).toBeNull()
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(CREDENTIAL_ID)
  })

  it('anUnreadableStoredPasskeyId_staysLocked_andCantUnlockGetsIn', async () => {
    const { create, get } = mockWebAuthn()
    create.mockResolvedValue(passkey([9, 9, 9]))
    await updateSettings(db, (s) => ({ ...s, appLock: true }))
    await db.meta.put({ key: CREDENTIAL_KEY, value: 'not base64 !!' })
    show()

    await screen.findByRole('heading', { name: 'Grit is locked' })
    await userEvent.click(screen.getByRole('button', { name: 'Unlock' }))
    expect(content()).toBeNull()
    expect(get).not.toHaveBeenCalled()

    await userEvent.click(screen.getByRole('button', { name: "Can't unlock?" }))
    expect(await screen.findByText('the training log')).toBeInTheDocument()
    expect((await db.meta.get(CREDENTIAL_KEY))?.value).toBe(NEW_ID)
  })

  it('offScreen_withTheLockOn_theLogIsCovered', async () => {
    mockWebAuthn()
    await lockOn()
    const { container } = show()
    await screen.findByText('the training log')
    expect(container.querySelector('.gate-cover')).toBeNull()

    // What the app switcher takes its picture of.
    leave()
    expect(container.querySelector('.gate-cover')).not.toBeNull()

    comeBack()
    expect(container.querySelector('.gate-cover')).toBeNull()
    expect(content()).not.toBeNull()
  })

  it('offScreen_withTheLockOff_nothingIsCovered', async () => {
    const { container } = show()
    await screen.findByText('the training log')
    leave()
    expect(container.querySelector('.gate-cover')).toBeNull()
  })
})
