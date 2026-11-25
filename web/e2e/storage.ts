/*
 * The browser storage every e2e page starts with, in one place (1.16): the config's shared state and the specs that
 * replace it all come from here, so no spec can forget the guide's seam. Plain data: playwright.config.ts imports it.
 *
 * - `grit:motion` 'off' (MOTION_KEY in src/ui/modeHeadStart.ts): every page runs still unless a spec asks for motion.
 * - `grit:guide` 'off' (GUIDE_KEY in src/screens/guide/guideSeen.ts): the guide's first-launch gate stays shut. Every
 *   test starts from an empty database, which is exactly a new user who has not seen the guide.
 */

/** The ports the e2e servers run on (playwright.config.ts). */
const PORTS = [4173, 4174, 4175]

type Item = { name: string; value: string }

/**
 * Only in the WebKit pass (E2E_BROWSER=webkit, CI's by-hand job): Playwright's WebKit windows refuse private file
 * storage, as a private Safari window does, so the app takes them for private browsing and shows its warning instead
 * of itself (seen: every test stopped there). "Continue anyway" as already chosen (`grit:privateModeAccepted`,
 * src/platform/storage.ts), so the tests reach the app. The Chromium runs do not get it.
 */
const PRIVATE_ACCEPTED: Item[] = process.env.E2E_BROWSER === 'webkit' ? [{ name: 'grit:privateModeAccepted', value: '1' }] : []

const withItems = (items: Item[]) => {
  const localStorage = [...items, ...PRIVATE_ACCEPTED]
  return { cookies: [], origins: PORTS.map((port) => ({ origin: `http://localhost:${port}`, localStorage })) }
}

const MOTION_OFF: Item = { name: 'grit:motion', value: 'off' }
const GUIDE_OFF: Item = { name: 'grit:guide', value: 'off' }

/** Every test's: motion off, the first-launch gate shut. */
export const SHARED_STATE = withItems([MOTION_OFF, GUIDE_OFF])

/** For a spec that needs motion on: the system's own setting decides it; the first-launch gate stays shut. */
export const MOTION_ON = withItems([GUIDE_OFF])

/** A new device as a lifter meets it: motion off for steady checks, and the first-launch gate open. Only the guide's own first-launch tests. */
export const NEW_DEVICE = withItems([MOTION_OFF])
