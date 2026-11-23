import { vi } from 'vitest'

export const CREDENTIAL_BYTES = new Uint8Array([1, 2, 3, 250, 251, 252])
/** `CREDENTIAL_BYTES` as the base64url text Grit stores. */
export const CREDENTIAL_ID = 'AQID-vv8'

/** A browser with a platform authenticator (Face ID, Touch ID): both calls succeed unless a test replaces them. */
export function mockWebAuthn({ available = true }: { available?: boolean } = {}) {
  const credential = { type: 'public-key', rawId: CREDENTIAL_BYTES.buffer }
  const create = vi.fn(async (_options: CredentialCreationOptions): Promise<unknown> => credential)
  const get = vi.fn(async (_options: CredentialRequestOptions): Promise<unknown> => credential)
  Object.defineProperty(navigator, 'credentials', { value: { create, get }, configurable: true })
  vi.stubGlobal('PublicKeyCredential', { isUserVerifyingPlatformAuthenticatorAvailable: async () => available })
  return { create, get }
}

/** Back to jsdom, which has no WebAuthn. */
export function clearWebAuthn() {
  Object.defineProperty(navigator, 'credentials', { value: undefined, configurable: true })
  vi.unstubAllGlobals()
}

/** What the browser throws when the lifter closes the Face ID sheet. */
export const cancelled = () => new DOMException('The operation was cancelled.', 'NotAllowedError')

/** Authenticator data as a phone returns it: 32 bytes of hash, then the flags byte. 0x05 = present and verified, 0x01 = present only. */
export function authenticatorData(flags: number): ArrayBuffer {
  const bytes = new Uint8Array(37)
  bytes[32] = flags
  return bytes.buffer
}

/** A prompt that stays on screen until it is withdrawn through its signal, as the Face ID sheet does. */
export function pendingUntilAborted(options: CredentialRequestOptions): Promise<unknown> {
  return new Promise((_, reject) => options.signal?.addEventListener('abort', () => reject(new DOMException('withdrawn', 'AbortError'))))
}
