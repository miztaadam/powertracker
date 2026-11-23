import { expect, type Locator, type Page } from '@playwright/test'

/** The sample Wednesday the flows run on. */
export const WEDNESDAY = new Date('2026-11-11T09:41:00')

/** How long Plan's add form ignores taps on Add, Cancel and outside once it opens (ADD_FORM_TAP_WINDOW_MS in PlanDialogs). */
const ADD_FORM_TAP_WINDOW_MS = 300

/** Waits until Plan's add form, open now, takes taps: it ignores them at first, as the second tap of a double tap on a row. */
export const pastTheTapWindow = (page: Page) => page.waitForTimeout(ADD_FORM_TAP_WINDOW_MS)

/**
 * Puts the guide's seam back (e2e/storage.ts) after a test cleared the site's storage: the next load is a new user
 * again, and without it the first-launch gate would open the guide.
 */
export const keepTheGuideShut = (page: Page) => page.evaluate(() => localStorage.setItem('grit:guide', 'off'))

export const tabBar = (page: Page) => page.getByRole('navigation', { name: 'Main' })

/**
 * Whether an open dialog's effect has run, seen from its panel: the page around it is wholly inert (every element
 * sibling of its scrim, and of each ancestor up to `body`, has `inert`: what `isolateFromPage` leaves) and the focus is
 * inside the panel. That effect (src/ui/components/Dialog.tsx) moves the focus in, isolates the page, then adds the
 * Escape listener (or registers with the app's close watcher, and on an iPhone or iPad for a history entry), all in one
 * synchronous pass, so from here neither half can be seen to hold before the dialog listens unless something else made
 * it hold at draw. Each half covers the other's gap:
 * - a dialog with an `autoFocus` field (Create your own, Set best, the bodyweight entry) has the focus when drawn, but
 *   the page around a newly drawn scrim is never wholly inert then: its own siblings at least are not yet;
 * - a dialog drawn in the same commit that closes another (the day chooser that the form guide's Add to plan opens in
 *   its place) finds the page still inert from the closing one, whose undo runs only in the same effect pass, but the
 *   focus is not inside it until its own effect moves it there;
 * - a dialog left open when one over it closes (Plan's form under the picker) is listened to again only once that one's
 *   effect cleanup has run; from the moment that one is taken off the page until then, the page around this one is
 *   inert again but the focus is on nothing, and the cleanup gives it back into this one as its last step.
 * Not covered: a dialog with an `autoFocus` field drawn in the same commit that closes another. It would have the focus
 * and an inert page when drawn, before its effect. No dialog is opened that way today; one that is needs its own
 * condition.
 * Runs in the page; kept self-contained for that.
 */
function dialogHasRun(panel: Element): boolean {
  const scrim = panel.isConnected ? panel.closest('.gr-dialog-scrim') : null
  if (scrim == null || !panel.contains(document.activeElement)) return false
  for (let node: Element = scrim; node.parentElement && node !== document.body; node = node.parentElement) {
    for (const sibling of node.parentElement.children) if (sibling !== node && !sibling.hasAttribute('inert')) return false
  }
  return true
}

/**
 * Waits until `dialog` (an open Dialog's `role="dialog"` panel) listens: its effect has run, so Escape, Back on an
 * Android phone or an iPhone, or any other close request reaches it. A dialog takes these only from an effect that runs
 * just after it is drawn, so a close request sent as soon as it is visible can come too early (`dialogHasRun` is the
 * condition). Await it once the dialog is open, before anything takes the focus out of it (a tap that redraws what had
 * the focus, as a muscle tile in the picker does): it listens from then on until it closes. Await it again for a dialog
 * once one over it has closed: that one's close gives the focus back into it as its last step. Before a Back on the
 * iPhone path, wait for the dialog's history entry as well (its depth): the entry is pushed a microtask after the effect,
 * or later while a step back is on its way, so this condition alone does not say the entry is there.
 */
export async function dialogListening(dialog: Locator) {
  await expect.poll(() => dialog.evaluate(dialogHasRun), { message: "the dialog's effect has run: the page around it inert, the focus inside it" }).toBe(true)
}

/**
 * Waits until an open ⋮ menu (`role="menu"`) takes close requests. Where the browser has CloseWatcher (Android), the
 * menu registers with the app's close watcher in an effect, and its key handler leaves Escape to the watcher; the
 * condition is the focus on its first item, which a layout effect of the commit after that one gives
 * (src/ui/components/Menu.tsx: the placement it sets is rendered only once the earlier commit's effects have run). On an
 * iPhone or iPad, Escape is the menu's key handler, there from the moment it is drawn, and the wait costs nothing.
 */
export async function menuListening(menu: Locator) {
  await expect(menu.getByRole('menuitem').first()).toBeFocused()
}

export async function tap(page: Page, name: string, times = 1) {
  for (let i = 0; i < times; i++) await page.getByRole('button', { name, exact: true }).click()
}

/** The exercise card's typed fields, by their spoken names. */
export const weightField = (page: Page) => page.getByRole('textbox', { name: 'Weight, kilograms' })
export const repsField = (page: Page) => page.getByRole('textbox', { name: 'Reps', exact: true })
export const rpeField = (page: Page) => page.getByRole('textbox', { name: 'RPE', exact: true })

/** Taps a field on the exercise card and types over its value, as a thumb on the keypad does. Nothing is committed yet. */
export async function typeInto(page: Page, field: Locator, text: string) {
  await field.tap()
  await expect(field).toBeFocused()
  await page.keyboard.type(text)
  await expect(field).toHaveValue(text)
}

/**
 * Opens Grit on the sample Wednesday, with the database created and the first screen showing. With
 * E2E_TRAINING_MODE=BODYBUILDING in the environment the app is put on the bodybuilding side first, so a run can
 * check that the flows (the @speed checks above all) hold there too; unset, nothing changes.
 */
export async function openOnWednesday(page: Page, hash = '#/today') {
  await page.clock.setFixedTime(WEDNESDAY)
  await page.goto(`/${hash}`)
  await expect(tabBar(page)).toBeVisible()
  if (process.env.E2E_TRAINING_MODE === 'BODYBUILDING') {
    await page.evaluate(async () => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      await new Promise<void>((done, fail) => {
        const tx = db.transaction('settings', 'readwrite')
        tx.objectStore('settings').put({ key: 'trainingMode', value: 'BODYBUILDING' })
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      db.close()
    })
    await page.reload()
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    await expect(tabBar(page)).toBeVisible()
  }
}

/**
 * Writes a bench day on Wednesdays and six flat weeks of bench (100 x 5 @ 8.5) straight into
 * IndexedDB, then reloads: finishing one more such session makes the coach call a stall.
 */
export async function seedFlatBench(page: Page) {
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const exercises = await new Promise<{ id: number; name: string }[]>((done) => {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => done(all.result)
    })
    const bench = exercises.find((e) => e.name === 'Bench')!.id
    // Stamped with their side, as the app writes them (schema version 3).
    tx.objectStore('planItems').add({ weekday: 3, exerciseId: bench, sets: 3, reps: 5, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
    for (let week = 0; week < 6; week++) {
      const date = new Date(Date.UTC(2026, 9, 5 + week * 7)).toISOString().slice(0, 10)
      const session = tx.objectStore('sessions').add({ date, bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })
      session.onsuccess = () => tx.objectStore('sets').add({ sessionId: session.result, exerciseId: bench, weightKg: 100, reps: 5, rpe: 8.5, failed: false, missPoint: null, isTopSet: false })
    }
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
}

/** Every row of every table, as the app stores it: what the "same data" checks compare. */
export function readAllTables(page: Page) {
  return page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const out: Record<string, unknown[]> = {}
    for (const name of ['exercises', 'sessions', 'sets', 'planItems', 'baselines', 'advice', 'settings']) {
      out[name] = await new Promise<unknown[]>((done, fail) => {
        const all = db.transaction(name).objectStore(name).getAll()
        all.onsuccess = () => done(all.result)
        all.onerror = () => fail(all.error)
      })
    }
    db.close()
    return out
  })
}
