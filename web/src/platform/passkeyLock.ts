import type Dexie from 'dexie'

/** As on Android: a shorter trip away (the share sheet, a glance at a message) does not lock again. */
export const RELOCK_AFTER_MS = 60_000

/** The `meta` row with this phone's passkey id (base64url). It is not part of a backup: a passkey belongs to one device. */
export const CREDENTIAL_KEY = 'lock:credentialId'

/** What the app shows instead of, or as, its content. */
export type LockGate =
  /** The setting has not been read yet: show nothing. */
  | 'loading'
  | 'open'
  | 'locked'
  /** App lock is on but this browser has nothing to unlock with: say so and let the lifter in. */
  | 'unavailable'

export type LockState = {
  /** True on a new page load, and after `RELOCK_AFTER_MS` or more hidden. */
  needsUnlock: (nowMs: number) => boolean
  /** `enabled` is whether the lock is on and has a passkey, `canUnlock` whether the browser can ask for it; null while unknown. */
  lockGate: (nowMs: number, enabled: boolean | null, canUnlock: boolean | null) => LockGate
  markUnlocked: () => void
  /** The app left the screen. Leaving again while locked keeps the first time, so the lock cannot be waited out. */
  markHidden: (nowMs: number) => void
}

/**
 * The Android `AppLock` rules. The unlocked state lives in memory only, so a new page load always asks
 * again. Times are wall-clock milliseconds (a suspended page's own clock stands still), so a clock set
 * back while the app was hidden counts as locked.
 */
export function createLockState(): LockState {
  let unlocked = false
  let hiddenAt: number | null = null
  const needsUnlock = (nowMs: number) => !unlocked || (hiddenAt != null && (nowMs < hiddenAt || nowMs - hiddenAt >= RELOCK_AFTER_MS))
  return {
    needsUnlock,
    lockGate(nowMs, enabled, canUnlock) {
      if (enabled == null) return 'loading'
      if (!enabled || !needsUnlock(nowMs)) return 'open'
      if (canUnlock == null) return 'loading'
      // A browser that lost its authenticator opens the app, rather than shutting the lifter out for good.
      return canUnlock ? 'locked' : 'unavailable'
    },
    markUnlocked() {
      unlocked = true
      hiddenAt = null
    },
    markHidden(nowMs) {
      if (hiddenAt == null) hiddenAt = nowMs
    },
  }
}

export type CreateLockResult = 'ok' | 'unsupported' | 'cancelled'
export type UnlockResult = 'ok' | 'cancelled' | 'unsupported'

const TIMEOUT_MS = 60_000
/** Errors that mean "this browser or address cannot do it", as opposed to the lifter closing the sheet. */
const UNSUPPORTED_ERRORS = ['NotSupportedError', 'SecurityError', 'TypeError']

function toBase64Url(bytes: ArrayBuffer): string {
  let text = ''
  for (const byte of new Uint8Array(bytes)) text += String.fromCharCode(byte)
  return btoa(text).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function fromBase64Url(text: string): Uint8Array<ArrayBuffer> {
  const binary = atob(text.replace(/-/g, '+').replace(/_/g, '/'))
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

const randomBytes = (length: number) => crypto.getRandomValues(new Uint8Array(length))
const errorName = (e: unknown) => (typeof e === 'object' && e !== null && 'name' in e ? String(e.name) : '')

/**
 * The passkey's owner, always the same, so that making a passkey again replaces the phone's entry
 * for Grit instead of adding another one beside it.
 */
const OWNER_ID = new TextEncoder().encode('grit-owner')

/** The "user verified" bit of the flags byte, which follows the 32-byte relying-party hash in authenticator data. */
const UV_FLAG = 0x04

/** Whether the authenticator says it verified its owner. Data a browser does not hand out cannot be checked and passes. */
function userVerified(authenticatorData: ArrayBuffer | null | undefined): boolean {
  if (authenticatorData == null) return true
  const flags = new Uint8Array(authenticatorData)[32]
  return flags != null && (flags & UV_FLAG) !== 0
}

/** Whether this browser speaks WebAuthn at all. Known at once, unlike `lockSupported`. */
const hasWebAuthn = () => typeof PublicKeyCredential !== 'undefined' && navigator.credentials != null

/** Whether this browser has a platform authenticator (Face ID, Touch ID, the passcode) for a passkey. */
export async function lockSupported(): Promise<boolean> {
  try {
    if (!hasWebAuthn()) return false
    return (await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()) === true
  } catch {
    return false
  }
}

/** This phone's passkey id, or null when none was made here. */
export async function readCredentialId(db: Dexie): Promise<string | null> {
  const value: unknown = (await db.table('meta').get(CREDENTIAL_KEY))?.value
  return typeof value === 'string' && value !== '' ? value : null
}

/** A failed prompt: the browser cannot do it at all, or the lifter (or the phone) said no. */
async function failure(e: unknown): Promise<'unsupported' | 'cancelled'> {
  return UNSUPPORTED_ERRORS.includes(errorName(e)) || !(await lockSupported()) ? 'unsupported' : 'cancelled'
}

/**
 * Has the phone make a passkey for Grit, which verifies its owner on the way. Safari only shows
 * the Face ID sheet while a tap is being handled, so the request is the first thing awaited: call
 * this straight from the tap. `supported` is `lockSupported()`'s answer when already known. A
 * passkey made without the owner being verified is refused.
 */
export async function makePasskey(supported?: boolean | null): Promise<{ result: CreateLockResult; credentialId?: string }> {
  if (supported === false || !hasWebAuthn()) return { result: 'unsupported' }
  let credential: Credential | null
  try {
    credential = await navigator.credentials.create({
      publicKey: {
        challenge: randomBytes(32),
        rp: { id: window.location.hostname, name: 'Grit' },
        user: { id: OWNER_ID, name: 'Grit', displayName: 'Grit app lock' },
        pubKeyCredParams: [
          { type: 'public-key', alg: -7 },
          { type: 'public-key', alg: -257 },
        ],
        authenticatorSelection: { authenticatorAttachment: 'platform', userVerification: 'required' },
        attestation: 'none',
        timeout: TIMEOUT_MS,
      },
    })
  } catch (e) {
    return { result: await failure(e) }
  }
  const made = credential as (PublicKeyCredential & { response?: Partial<AuthenticatorAttestationResponse> }) | null
  if (made?.rawId == null) return { result: 'cancelled' }
  if (!userVerified(made.response?.getAuthenticatorData?.())) return { result: 'cancelled' }
  return { result: 'ok', credentialId: toBase64Url(made.rawId) }
}

/**
 * Makes the passkey that unlocks Grit and remembers its id, in place of any earlier one. Nothing
 * is sent anywhere: the passkey is only ever used to have the phone verify its owner. Call it
 * straight from the tap (see `makePasskey`). A failed write to the database throws.
 */
export async function createLock(db: Dexie, supported?: boolean | null): Promise<CreateLockResult> {
  const made = await makePasskey(supported)
  if (made.result === 'ok') await db.table('meta').put({ key: CREDENTIAL_KEY, value: made.credentialId })
  return made.result
}

/**
 * Has the phone verify its owner with the passkey made by `createLock`. `credentialId` is the
 * stored id, read beforehand, so that the request is the first thing awaited (see `makePasskey`).
 * 'unsupported' when there is nothing to ask with; an id that cannot be read counts as a failed
 * attempt, which "Can't unlock?" gets past. `signal` withdraws the prompt.
 */
export async function unlock(credentialId: string | null, signal?: AbortSignal): Promise<UnlockResult> {
  if (credentialId == null || !hasWebAuthn()) return 'unsupported'
  let id: Uint8Array<ArrayBuffer>
  try {
    id = fromBase64Url(credentialId)
  } catch {
    return 'cancelled'
  }
  try {
    const assertion = await navigator.credentials.get({
      publicKey: {
        challenge: randomBytes(32),
        rpId: window.location.hostname,
        allowCredentials: [{ type: 'public-key', id, transports: ['internal'] }],
        userVerification: 'required',
        timeout: TIMEOUT_MS,
      },
      signal,
    })
    const response = (assertion as (PublicKeyCredential & { response?: Partial<AuthenticatorAssertionResponse> }) | null)?.response
    return assertion != null && userVerified(response?.authenticatorData) ? 'ok' : 'cancelled'
  } catch (e) {
    return signal?.aborted ? 'cancelled' : failure(e)
  }
}

/** Forgets this phone's passkey id, when App lock is turned off. */
export async function clearLock(db: Dexie): Promise<void> {
  await db.table('meta').delete(CREDENTIAL_KEY)
}
