import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../db/db'
import { todayFixture } from '../test/todayFixture'
import { CREDENTIAL_BYTES, CREDENTIAL_ID, authenticatorData, cancelled, clearWebAuthn, mockWebAuthn, pendingUntilAborted } from '../test/webauthn'
import { CREDENTIAL_KEY, clearLock, createLock, createLockState, lockSupported, unlock } from './passkeyLock'

describe('lockGate_rules', () => {
  // Ported from the Android AppLockTest.
  let clock = 1_000_000
  let lock: ReturnType<typeof createLockState>
  beforeEach(() => {
    clock = 1_000_000
    lock = createLockState()
  })

  it('a new page load is locked', () => {
    expect(lock.needsUnlock(clock)).toBe(true)
    expect(lock.lockGate(clock, true, true)).toBe('locked')
  })

  it('unlocking opens the app', () => {
    lock.markUnlocked()
    expect(lock.needsUnlock(clock)).toBe(false)
    expect(lock.lockGate(clock, true, true)).toBe('open')
  })

  it('a short trip to the background does not lock again', () => {
    lock.markUnlocked()
    lock.markHidden(clock)
    clock += 59_999
    expect(lock.needsUnlock(clock)).toBe(false)
  })

  it('sixty seconds in the background locks again', () => {
    lock.markUnlocked()
    lock.markHidden(clock)
    clock += 60_000
    expect(lock.needsUnlock(clock)).toBe(true)
  })

  it('the lock stays until the next unlock, then the timer starts afresh', () => {
    lock.markUnlocked()
    lock.markHidden(clock)
    clock += 120_000
    expect(lock.needsUnlock(clock)).toBe(true)
    // Leaving again while locked must not open the app.
    lock.markHidden(clock)
    expect(lock.needsUnlock(clock)).toBe(true)

    lock.markUnlocked()
    expect(lock.needsUnlock(clock)).toBe(false)
    lock.markHidden(clock)
    clock += 1_000
    expect(lock.needsUnlock(clock)).toBe(false)
  })

  it('the gate waits until the setting is known', () => {
    expect(lock.lockGate(clock, null, true)).toBe('loading')
    expect(lock.lockGate(clock, true, null)).toBe('loading')
  })

  it('app lock off is always open', () => {
    expect(lock.lockGate(clock, false, true)).toBe('open')
    expect(lock.lockGate(clock, false, null)).toBe('open')
  })

  it('a phone that cannot unlock is not shut out', () => {
    expect(lock.lockGate(clock, true, false)).toBe('unavailable')
  })

  it('turning app lock on does not lock the session it was turned on in', () => {
    expect(lock.lockGate(clock, false, true)).toBe('open')
    lock.markUnlocked()
    expect(lock.lockGate(clock, true, true)).toBe('open')
  })

  it('a clock set back while hidden locks rather than opens', () => {
    lock.markUnlocked()
    lock.markHidden(clock)
    expect(lock.needsUnlock(clock - 1)).toBe(true)
  })
})

describe('passkey lock', () => {
  let fx: ReturnType<typeof todayFixture>
  let db: GritDb
  beforeEach(async () => {
    fx = todayFixture()
    db = await fx.open()
  })
  afterEach(async () => {
    await fx.dispose()
    clearWebAuthn()
  })
  const storedId = async () => (await db.meta.get(CREDENTIAL_KEY))?.value

  it('createLock_makesAPlatformPasskey_andStoresItsId', async () => {
    const { create } = mockWebAuthn()

    expect(await createLock(db)).toBe('ok')

    const options = create.mock.calls[0]?.[0].publicKey
    expect(options?.authenticatorSelection).toMatchObject({ authenticatorAttachment: 'platform', userVerification: 'required' })
    expect(options?.rp).toEqual({ id: window.location.hostname, name: 'Grit' })
    expect(options?.pubKeyCredParams.map((p) => p.alg)).toEqual([-7, -257])
    expect(await storedId()).toBe(CREDENTIAL_ID)
  })

  it('createLock_cancelled_storesNothing', async () => {
    const { create } = mockWebAuthn()
    create.mockRejectedValueOnce(cancelled())

    expect(await createLock(db)).toBe('cancelled')
    expect(await storedId()).toBeUndefined()
  })

  it('createLock_unsupported_withoutWebAuthnOrAuthenticator', async () => {
    expect(await lockSupported()).toBe(false)
    expect(await createLock(db)).toBe('unsupported')

    // No Face ID, Touch ID or passcode: the browser refuses, and that is not the lifter saying no.
    const none = mockWebAuthn({ available: false })
    none.create.mockRejectedValueOnce(cancelled())
    expect(await createLock(db)).toBe('unsupported')
    // Known beforehand: not even asked.
    expect(await createLock(db, false)).toBe('unsupported')
    expect(none.create).toHaveBeenCalledTimes(1)

    // A host a passkey cannot belong to (an IP address, say).
    const ok = mockWebAuthn()
    ok.create.mockRejectedValueOnce(new DOMException('bad rp id', 'SecurityError'))
    expect(await createLock(db)).toBe('unsupported')
    expect(await storedId()).toBeUndefined()
  })

  it('createLock_asksBeforeAnythingElse_soSafariKeepsTheTap', async () => {
    const { create } = mockWebAuthn()
    const pending = createLock(db)
    // Already asked, with nothing awaited first.
    expect(create).toHaveBeenCalledTimes(1)
    await pending
  })

  it('createLock_sameOwnerEveryTime_soThePhoneReplacesItsEntry', async () => {
    const { create } = mockWebAuthn()
    await createLock(db)
    await createLock(db)
    const owner = (call: number) => new TextDecoder().decode(create.mock.calls[call]?.[0].publicKey?.user.id as Uint8Array)
    expect(owner(0)).toBe('grit-owner')
    expect(owner(1)).toBe('grit-owner')
  })

  it('createLock_refusesAPasskeyMadeWithoutVerifyingTheOwner', async () => {
    const { create } = mockWebAuthn()
    const made = (flags: number) => ({ type: 'public-key', rawId: CREDENTIAL_BYTES.buffer, response: { getAuthenticatorData: () => authenticatorData(flags) } })

    create.mockResolvedValueOnce(made(0x01))
    expect(await createLock(db)).toBe('cancelled')
    expect(await storedId()).toBeUndefined()

    create.mockResolvedValueOnce(made(0x05))
    expect(await createLock(db)).toBe('ok')
    expect(await storedId()).toBe(CREDENTIAL_ID)
  })

  it('unlock_asksForTheStoredPasskeyWithUserVerification', async () => {
    const { get } = mockWebAuthn()

    const pending = unlock(CREDENTIAL_ID)
    // Asked with nothing awaited first, so Safari keeps the tap.
    expect(get).toHaveBeenCalledTimes(1)
    expect(await pending).toBe('ok')

    const options = get.mock.calls[0]?.[0].publicKey
    expect(options?.userVerification).toBe('required')
    expect(options?.rpId).toBe(window.location.hostname)
    expect(options?.allowCredentials).toHaveLength(1)
    expect(new Uint8Array(options?.allowCredentials?.[0]?.id as ArrayBuffer)).toEqual(CREDENTIAL_BYTES)
  })

  it('unlock_cancelledOrUnsupported', async () => {
    const { get } = mockWebAuthn()
    // Nothing to unlock with: no passkey was made on this phone.
    expect(await unlock(null)).toBe('unsupported')
    expect(get).not.toHaveBeenCalled()

    get.mockRejectedValueOnce(cancelled())
    expect(await unlock(CREDENTIAL_ID)).toBe('cancelled')

    // Verified by nobody: not an unlock.
    get.mockResolvedValueOnce({ type: 'public-key', rawId: CREDENTIAL_BYTES.buffer, response: { authenticatorData: authenticatorData(0x01) } })
    expect(await unlock(CREDENTIAL_ID)).toBe('cancelled')

    clearWebAuthn()
    expect(await unlock(CREDENTIAL_ID)).toBe('unsupported')
  })

  it('unlock_anUnreadableStoredId_isAFailedAttempt_notAnError', async () => {
    const { get } = mockWebAuthn()
    expect(await unlock('not base64 !!')).toBe('cancelled')
    expect(get).not.toHaveBeenCalled()
  })

  it('unlock_withdrawnThroughItsSignal', async () => {
    const { get } = mockWebAuthn()
    get.mockImplementationOnce(pendingUntilAborted)
    const prompt = new AbortController()
    const pending = unlock(CREDENTIAL_ID, prompt.signal)
    prompt.abort()
    expect(await pending).toBe('cancelled')
  })

  it('clearLock_forgetsThePasskey', async () => {
    mockWebAuthn()
    await createLock(db)
    await clearLock(db)
    expect(await storedId()).toBeUndefined()
  })
})
