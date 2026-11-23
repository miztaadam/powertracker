import type { GritDb } from '../../db/db'

/*
 * The guide's first launch (1.16). Kept out of the guide's lazy chunk: the app reads it at start, and nothing else of
 * the guide. "Seen" is a `meta` key, which a restore keeps and the export leaves out.
 */

/** In `meta`: true once the guide opened by the first launch has been left, by any way out. */
export const GUIDE_SEEN_KEY = 'guide:seen'

/**
 * In localStorage: 'off' keeps the first-launch gate shut (the tests' seam, as MOTION_KEY is the motion's). Every e2e
 * page and every vitest test starts with it; the guide's own first-launch tests remove it.
 */
export const GUIDE_KEY = 'grit:guide'

export async function guideSeen(db: GritDb): Promise<boolean> {
  return (await db.meta.get(GUIDE_SEEN_KEY))?.value === true
}

export async function markGuideSeen(db: GritDb): Promise<void> {
  await db.meta.put({ key: GUIDE_SEEN_KEY, value: true })
}

/** A new user: no session and no plan item on either side. */
export async function isNewUser(db: GritDb): Promise<boolean> {
  return (await db.sessions.count()) === 0 && (await db.planItems.count()) === 0
}

/** Whether the first-launch gate is shut by the seam (read at once, before any query). */
export function guideGateOff(): boolean {
  try {
    return window.localStorage.getItem(GUIDE_KEY) === 'off'
  } catch {
    return false
  }
}
