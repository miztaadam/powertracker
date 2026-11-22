import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, tabBar } from './helpers'
import { ANDROID_CHROME_UA } from './userAgents'

// 1.17.4: Back on an Android phone closes the open dialog, sheet or ⋮ menu, through the browser's CloseWatcher, and
// leaves the screen where it is. Playwright's goBack() is the toolbar's Back, which is not a close request, so it is
// not used here. Close requests are sent two ways: Escape (a close request wherever CloseWatcher exists), and
// `requestClose()` on the newest watcher the page made, which stands in for the phone's Back button.
//
// The init script records every watcher the app makes. The app keeps one live watcher at most, whatever is open, so
// the browser can never put two in one group (where one Back would close both): each step below checks that.

const rpeGuide = GritDomain.getInstance().rpeGuide()

// Android's path: the default project's iPhone user agent would send Back through a history entry instead
// (historyCloses.ts, tested in backClosesIphone.spec.ts).
test.use({ userAgent: ANDROID_CHROME_UA })

const RECORD = () => {
  const Real = (window as unknown as { CloseWatcher?: new () => EventTarget & { destroy(): void } }).CloseWatcher
  if (!Real) return
  const made: { state: 'live' | 'closed' | 'destroyed'; watcher: unknown }[] = []
  ;(window as unknown as { __watchers: typeof made }).__watchers = made
  class Recorded extends Real {
    constructor() {
      super()
      const entry = { state: 'live' as 'live' | 'closed' | 'destroyed', watcher: this as unknown }
      made.push(entry)
      this.addEventListener('close', () => {
        if (entry.state === 'live') entry.state = 'closed'
      })
    }
    destroy() {
      const entry = made.find((e) => e.watcher === this)
      if (entry && entry.state === 'live') entry.state = 'destroyed'
      super.destroy()
    }
  }
  ;(window as unknown as { CloseWatcher: unknown }).CloseWatcher = Recorded
}

/** How many of the app's watchers are live now. */
const live = (page: Page) => page.evaluate(() => (window as unknown as { __watchers: { state: string }[] }).__watchers.filter((e) => e.state === 'live').length)

/** A close request with no keyboard, as the phone's Back button sends it: to the newest live watcher. */
async function backButton(page: Page) {
  const sent = await page.evaluate(() => {
    const made = (window as unknown as { __watchers: { state: string; watcher: { requestClose(): void } }[] }).__watchers
    const newest = made.filter((e) => e.state === 'live').at(-1)
    newest?.watcher.requestClose()
    return newest != null
  })
  expect(sent, 'a live watcher to send Back to').toBe(true)
}

const escape = (page: Page) => page.keyboard.press('Escape')

/** Puts Squat on Wednesdays straight into IndexedDB, then reloads (as rpe.spec.ts does). */
async function squatOnWednesday(page: Page) {
  await page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const all = tx.objectStore('exercises').getAll()
    all.onsuccess = () => {
      const squat = (all.result as { id: number; name: string }[]).find((e) => e.name === 'Squat')!.id
      tx.objectStore('planItems').add({ weekday: 3, exerciseId: squat, sets: 3, reps: 5, targetRpe: 8.5, order: 0 })
    }
    await settled
    db.close()
  })
  await page.reload()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
}

/** Puts Lat pulldown (a machine with a plate stack) on Wednesdays, as plates.spec.ts seeds it, then reloads. */
async function latPulldownOnWednesday(page: Page) {
  const e = GritDomain.getInstance().findInLibrary('Lat pulldown')!
  const row = { name: e.name, category: e.category, parentLift: e.parentLift ?? null, muscles: [...e.muscles], isBuiltIn: true, archived: false }
  await page.evaluate(async (row) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const added = tx.objectStore('exercises').add(row)
    added.onsuccess = () => tx.objectStore('planItems').add({ weekday: 3, exerciseId: added.result, sets: 3, reps: 8, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
    await settled
    db.close()
  }, row)
  await page.reload()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
}

const editForm = (page: Page) => page.getByRole('dialog', { name: 'Edit Wednesday' })
const picker = (page: Page) => page.getByRole('dialog', { name: 'Change exercise' })
const planMenuButton = (page: Page) => page.getByRole('region', { name: 'Wednesday' }).getByRole('button', { name: 'More' })

/** Plan → Wednesday's Squat → ⋮ → Edit → Change → Quads: the form, the picker over it, a muscle open in the picker. */
async function pickerOnAMuscle(page: Page) {
  await openOnWednesday(page)
  await squatOnWednesday(page)
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await planMenuButton(page).click()
  await page.getByRole('menuitem', { name: 'Edit' }).click()
  await expect(editForm(page)).toBeVisible()
  await editForm(page).getByRole('button', { name: 'Change' }).click()
  await expect(picker(page)).toBeVisible()
  await picker(page).getByRole('group', { name: 'Browse by muscle' }).getByRole('button').filter({ hasText: 'Quads' }).click()
  await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
  expect(await live(page)).toBe(1)
}

test('the browser under test has CloseWatcher, and with this user agent the app does not take its iPhone path', async ({ page }) => {
  await openOnWednesday(page)
  const seen = await page.evaluate(() => ({ watcher: 'CloseWatcher' in window, ua: navigator.userAgent, platform: navigator.platform, touch: navigator.maxTouchPoints }))
  expect(seen.watcher).toBe(true)
  // What isIos() reads (platform/device.ts): no iPhone, iPad or iPod in the user agent, and not a Mac with touch.
  expect(seen.ua).not.toMatch(/iPad|iPhone|iPod/)
  expect(seen.platform).not.toBe('MacIntel')
  // No entry of the iPhone path: opening a menu adds nothing to history.
  await squatOnWednesday(page)
  const before = await page.evaluate(() => history.length)
  await tabBar(page).getByRole('link', { name: 'Plan' }).click()
  await page.getByRole('button', { name: 'Plan options' }).click()
  await expect(page.getByRole('menu')).toBeVisible()
  expect(await page.evaluate(() => history.length)).toBe(before + 1)
  expect(await page.evaluate(() => history.state)).toBeNull()
})

test.describe('with CloseWatcher', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(RECORD)
  })

  for (const [how, press] of [
    ['Escape', escape],
    ['the Back button', backButton],
  ] as const) {
    test(`(a) picker on a muscle over Plan's form: ${how} steps back from the muscle, then closes the picker, then the form`, async ({ page }) => {
      await pickerOnAMuscle(page)
      const url = page.url()

      await press(page)
      await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toHaveCount(0)
      await expect(picker(page).getByRole('group', { name: 'Browse by muscle' })).toBeVisible()
      await expect(editForm(page)).toBeVisible()
      expect(await live(page)).toBe(1)

      await press(page)
      await expect(picker(page)).toHaveCount(0)
      await expect(editForm(page)).toBeVisible()
      expect(await live(page)).toBe(1)

      await press(page)
      await expect(editForm(page)).toHaveCount(0)
      expect(await live(page)).toBe(0)
      expect(page.url()).toBe(url)
      await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
      // Focus back on the row's ⋮, as after Escape before.
      await expect(planMenuButton(page)).toBeFocused()
    })
  }

  test('(b) the form guide over the picker: one press closes the guide only', async ({ page }) => {
    await pickerOnAMuscle(page)
    await picker(page).getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button', { name: 'Form guide for Squat' }).click()
    const guide = page.getByRole('dialog', { name: 'Form guide for Squat' })
    await expect(guide).toBeVisible()
    expect(await live(page)).toBe(1)

    await backButton(page)
    await expect(guide).toHaveCount(0)
    await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
    expect(await live(page)).toBe(1)

    // Again, by Escape this time.
    await picker(page).getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button', { name: 'Form guide for Squat' }).click()
    await expect(guide).toBeVisible()
    await escape(page)
    await expect(guide).toHaveCount(0)
    await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
    await expect(editForm(page)).toBeVisible()
  })

  test("(c) the RPE sheet over Plan's form: one press closes the sheet, the next the form", async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await tabBar(page).getByRole('link', { name: 'Plan' }).click()
    await planMenuButton(page).click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    await editForm(page).getByRole('button', { name: 'What RPE means' }).click()
    const sheet = page.getByRole('dialog', { name: rpeGuide.title })
    await expect(sheet).toBeVisible()
    expect(await live(page)).toBe(1)

    await backButton(page)
    await expect(sheet).toHaveCount(0)
    await expect(editForm(page).getByRole('group', { name: 'RPE' })).toContainText('8.5')
    expect(await live(page)).toBe(1)
    await escape(page)
    await expect(editForm(page)).toHaveCount(0)
    expect(await live(page)).toBe(0)
  })

  test('(d) a dialog that opened by itself at load, then one opened by a tap: each press closes one', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await tabBar(page).getByRole('link', { name: 'Plan' }).click()
    // App lock on with no passkey here: "Set up App lock again?" opens by itself on the next load, before any tap.
    await page.evaluate(async () => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      await new Promise<void>((done, fail) => {
        const tx = db.transaction('settings', 'readwrite')
        tx.objectStore('settings').put({ key: 'appLock', value: true })
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      db.close()
    })
    await page.reload()
    const lockSetup = page.getByRole('dialog', { name: 'Set up App lock again?' })
    await expect(lockSetup).toBeVisible()
    expect(await live(page)).toBe(1)
    const url = page.url()

    await backButton(page)
    await expect(lockSetup).toHaveCount(0)
    expect(await live(page)).toBe(0)
    expect(page.url()).toBe(url)

    // Then a tapped dialog, with the sheet over it: one press each.
    await planMenuButton(page).click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    await editForm(page).getByRole('button', { name: 'What RPE means' }).click()
    const sheet = page.getByRole('dialog', { name: rpeGuide.title })
    await expect(sheet).toBeVisible()
    expect(await live(page)).toBe(1)
    await backButton(page)
    await expect(sheet).toHaveCount(0)
    await expect(editForm(page)).toBeVisible()
    await backButton(page)
    await expect(editForm(page)).toHaveCount(0)
    expect(page.url()).toBe(url)
  })

  test('(d) a dialog that opens by itself over a tapped one: the newer closes first', async ({ page }) => {
    await openOnWednesday(page)
    await latPulldownOnWednesday(page)
    // Tapped: Lat pulldown's ⋮ → Count plates on this machine. The sheet stays open while its choice is saved.
    await page.getByRole('button', { name: 'Options for Lat pulldown' }).click()
    await page.getByRole('menuitem', { name: 'Count plates on this machine' }).click()
    const sheet = page.getByRole('dialog', { name: 'Count plates on Lat pulldown' })
    await expect(sheet).toBeVisible()
    await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
    await page.waitForTimeout(300)
    // The phone's storage fills up: the choice's write fails, and "Storage is full" opens by itself over the sheet.
    await page.evaluate(() => {
      const quota = () => {
        throw new DOMException('The quota has been exceeded.', 'QuotaExceededError')
      }
      IDBObjectStore.prototype.put = quota
      IDBObjectStore.prototype.add = quota
    })
    await sheet.getByRole('radio', { name: 'Whole plates' }).click()
    const full = page.getByRole('dialog', { name: 'Storage is full' })
    await expect(full).toBeVisible()
    await expect(sheet).toBeVisible()
    expect(await live(page)).toBe(1)
    const url = page.url()

    await backButton(page)
    await expect(full).toHaveCount(0)
    await expect(sheet).toBeVisible()
    expect(await live(page)).toBe(1)
    await escape(page)
    await expect(sheet).toHaveCount(0)
    expect(await live(page)).toBe(0)
    expect(page.url()).toBe(url)
    await expect(page.getByRole('button', { name: 'Options for Lat pulldown' })).toBeFocused()
  })

  test('the ⋮ menu: a close request closes it and gives the focus back to its button; an item that opens a dialog still works', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await tabBar(page).getByRole('link', { name: 'Plan' }).click()
    const url = page.url()
    const menu = page.getByRole('menu')

    await planMenuButton(page).click()
    await expect(menu).toBeVisible()
    expect(await live(page)).toBe(1)
    await escape(page)
    await expect(menu).toHaveCount(0)
    await expect(planMenuButton(page)).toBeFocused()
    expect(await live(page)).toBe(0)

    await planMenuButton(page).click()
    await expect(menu).toBeVisible()
    await backButton(page)
    await expect(menu).toHaveCount(0)
    await expect(planMenuButton(page)).toBeFocused()
    expect(page.url()).toBe(url)

    // The header's ⋮ as well.
    await page.getByRole('button', { name: 'Plan options' }).click()
    await expect(menu).toBeVisible()
    await backButton(page)
    await expect(menu).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Plan options' })).toBeFocused()

    // An item that opens a dialog: the menu goes, the dialog opens, and Back then closes the dialog.
    await planMenuButton(page).click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    await expect(menu).toHaveCount(0)
    await expect(editForm(page)).toBeVisible()
    expect(await live(page)).toBe(1)
    await backButton(page)
    await expect(editForm(page)).toHaveCount(0)
    expect(await live(page)).toBe(0)
    expect(page.url()).toBe(url)
    await expect(page.getByRole('heading', { level: 1, name: 'Plan' })).toBeVisible()
  })

  test('with a sheet open, a close request leaves the URL, the tab and the history as they were', async ({ page }) => {
    await openOnWednesday(page, '#/exercises')
    await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
    const guide = page.getByRole('dialog', { name: 'Form guide for Hack squat' })
    await expect(guide).toBeVisible()
    const url = page.url()
    const entries = await page.evaluate(() => history.length)

    await backButton(page)
    await expect(guide).toHaveCount(0)
    expect(page.url()).toBe(url)
    expect(await page.evaluate(() => history.length)).toBe(entries)
    await expect(tabBar(page).getByRole('link', { name: 'Exercises', exact: true })).toHaveAttribute('aria-current', 'page')
    await expect(page.getByRole('heading', { level: 1, name: 'Exercises' })).toBeVisible()
    // Nothing open, no watcher: the next Back is the browser's own.
    expect(await live(page)).toBe(0)
  })
})

test.describe('without CloseWatcher (Safari today)', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
      delete (window as unknown as { CloseWatcher?: unknown }).CloseWatcher
    })
  })

  test('Escape steps back from the muscle, then closes the picker, then the form, as before; the menu closes on Escape', async ({ page }) => {
    await openOnWednesday(page)
    expect(await page.evaluate(() => 'CloseWatcher' in window)).toBe(false)
    await squatOnWednesday(page)
    await tabBar(page).getByRole('link', { name: 'Plan' }).click()
    await planMenuButton(page).click()
    await expect(page.getByRole('menu')).toBeVisible()
    await escape(page)
    await expect(page.getByRole('menu')).toHaveCount(0)
    await expect(planMenuButton(page)).toBeFocused()

    await planMenuButton(page).click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    await editForm(page).getByRole('button', { name: 'Change' }).click()
    await picker(page).getByRole('group', { name: 'Browse by muscle' }).getByRole('button').filter({ hasText: 'Quads' }).click()
    await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()

    await escape(page)
    await expect(picker(page).getByRole('group', { name: 'Browse by muscle' })).toBeVisible()
    await expect(editForm(page)).toBeVisible()
    await escape(page)
    await expect(picker(page)).toHaveCount(0)
    await expect(editForm(page)).toBeVisible()
    await escape(page)
    await expect(editForm(page)).toHaveCount(0)
    await expect(planMenuButton(page)).toBeFocused()
  })
})
