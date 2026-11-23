import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { dialogListening, openOnWednesday, tabBar } from './helpers'
import { IPHONE_SAFARI_UA } from './userAgents'

// 1.17.4 on an iPhone or iPad: Back (a swipe back, or Back in the installed app) closes the open dialog, sheet or ⋮ menu
// through a history entry of the app's own (src/ui/components/historyCloses.ts), as Escape would, and leaves the screen
// where it is. Here that Back is Playwright's goBack(), which is the history traversal a swipe makes; goForward() and
// reload() as well. This is Chromium with Safari's user agent: it proves the app's logic, not Safari's own timing or the
// swipe's look.
//
// Each case starts on Today and opens its screen from there, so "an ordinary Back" afterwards is Back to Today, as it
// was before this release. After every close, the app's own entries must be gone again (`depth` 0).

test.use({ userAgent: IPHONE_SAFARI_UA })

const rpeGuide = GritDomain.getInstance().rpeGuide()

/** Where history is: its length, the depth of the app's entry it is on (0: none), and the route. */
const where = (page: Page) =>
  page.evaluate(() => ({ length: history.length, depth: ((history.state as { gritDialog?: number } | null)?.gritDialog ?? 0) as number, hash: location.hash }))
const depth = async (page: Page) => (await where(page)).depth
/** Waits until the app has settled its entries: `want` of them under the page. */
const entries = (page: Page, want: number) => expect.poll(() => depth(page)).toBe(want)

const back = (page: Page) => page.goBack()

/** Counts the page's own history calls from now on (pushState, replaceState, go, back, forward). */
const countHistoryCalls = (page: Page) =>
  page.evaluate(() => {
    const calls: string[] = []
    ;(window as unknown as { __historyCalls: string[] }).__historyCalls = calls
    for (const name of ['pushState', 'replaceState', 'go', 'back', 'forward'] as const) {
      const real = (history[name] as (...args: unknown[]) => void).bind(history)
      ;(history as unknown as Record<string, unknown>)[name] = (...args: unknown[]) => {
        calls.push(name)
        real(...args)
      }
    }
  })
/** A slow browser: each `history.go` the page asks for is carried out `ms` late. */
const slowGo = (page: Page, ms: number) =>
  page.evaluate((ms) => {
    const real = history.go.bind(history)
    history.go = (delta?: number) => void setTimeout(() => real(delta), ms)
  }, ms)
const historyCalls = (page: Page) => page.evaluate(() => (window as unknown as { __historyCalls: string[] }).__historyCalls)
const onTab = async (page: Page, tab: string) => {
  await expect(tabBar(page).getByRole('link', { name: tab, exact: true })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { level: 1, name: tab })).toBeVisible()
}
/** Back with nothing open: to Today, where every case came from. */
async function ordinaryBackToToday(page: Page) {
  await entries(page, 0)
  await back(page)
  await expect(page).toHaveURL(/#\/today$/)
  await onTab(page, 'Today')
}
async function goToTab(page: Page, tab: string) {
  await tabBar(page).getByRole('link', { name: tab, exact: true }).click()
  await onTab(page, tab)
}

async function squatOnWednesday(page: Page, mode: 'POWERLIFTING' | 'BODYBUILDING' = 'POWERLIFTING') {
  await page.evaluate(async (mode) => {
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
      const item = mode === 'BODYBUILDING' ? { sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, mode } : { sets: 3, reps: 5, targetRpe: 8.5 }
      tx.objectStore('planItems').add({ weekday: 3, exerciseId: squat, order: 0, ...item })
    }
    await settled
    db.close()
  }, mode)
  await page.reload()
  if (mode === 'POWERLIFTING') await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
  else await expect(tabBar(page)).toBeVisible()
}

/** Through the builder's steps to its draft, whatever each step's main button says. */
async function throughTheBuilder(page: Page) {
  const use = page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' })
  for (let i = 0; i < 6 && !(await use.isVisible()); i++) {
    await page.locator('.pb-actions .gr-btn').last().click()
    await page.waitForTimeout(100)
  }
  await expect(use).toBeVisible()
}

async function putSetting(page: Page, key: string, value: unknown) {
  await page.evaluate(
    async ({ key, value }) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      await new Promise<void>((done, fail) => {
        const tx = db.transaction('settings', 'readwrite')
        tx.objectStore('settings').put({ key, value })
        tx.oncomplete = () => done()
        tx.onerror = () => fail(tx.error)
      })
      db.close()
    },
    { key, value },
  )
}

const guideSheet = (page: Page, name: string) => page.getByRole('dialog', { name: `Form guide for ${name}` })
const editForm = (page: Page) => page.getByRole('dialog', { name: 'Edit Wednesday' })
const picker = (page: Page) => page.getByRole('dialog', { name: 'Change exercise' })
const rowMenuButton = (page: Page) => page.getByRole('region', { name: 'Wednesday' }).getByRole('button', { name: 'More' })

/** Today → Plan → Wednesday's ⋮ → Edit: Plan's form open. */
async function editFormOpen(page: Page) {
  await openOnWednesday(page)
  await squatOnWednesday(page)
  await goToTab(page, 'Plan')
  await rowMenuButton(page).click()
  await page.getByRole('menuitem', { name: 'Edit' }).click()
  await expect(editForm(page)).toBeVisible()
  // The menu's entry stays for the form, which opens in the same commit that closes the menu: the depth is 1 before the
  // form has taken it over, so the wait is on the form itself (dialogListening, e2e/helpers.ts).
  await dialogListening(editForm(page))
  await entries(page, 1)
}

test('this run has the iPhone user agent, so the app takes its iPhone path', async ({ page }) => {
  await openOnWednesday(page)
  expect(await page.evaluate(() => navigator.userAgent)).toMatch(/iPhone/)
})

test('1. a sheet on Exercises: Back closes it and stays on Exercises; the next Back leaves as before', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  const start = await where(page)
  await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
  await expect(guideSheet(page, 'Hack squat')).toBeVisible()
  await entries(page, 1)
  expect((await where(page)).length).toBe(start.length + 1)

  // A Back-close makes no history call of the app's: the Back took the entry off, and nothing is pushed or popped after.
  await countHistoryCalls(page)
  await back(page)
  await expect(guideSheet(page, 'Hack squat')).toHaveCount(0)
  await expect(page).toHaveURL(/#\/exercises$/)
  await onTab(page, 'Exercises')
  await entries(page, 0)
  await page.waitForTimeout(300)
  expect(await historyCalls(page)).toEqual([])
  await ordinaryBackToToday(page)
})

test("2a. picker on a muscle over Plan's form: Back steps back from the muscle, then closes the picker, then the form", async ({ page }) => {
  await editFormOpen(page)
  await editForm(page).getByRole('button', { name: 'Change' }).click()
  await expect(picker(page)).toBeVisible()
  await picker(page).getByRole('group', { name: 'Browse by muscle' }).getByRole('button').filter({ hasText: 'Quads' }).click()
  await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
  await entries(page, 2)

  await back(page)
  await expect(picker(page).getByRole('group', { name: 'Browse by muscle' })).toBeVisible()
  await expect(editForm(page)).toBeVisible()
  // The picker is still open, so its entry is back for the next press.
  await entries(page, 2)

  await back(page)
  await expect(picker(page)).toHaveCount(0)
  await expect(editForm(page)).toBeVisible()
  await entries(page, 1)
  // The form again, once the picker's close has run its course (dialogListening, e2e/helpers.ts).
  await dialogListening(editForm(page))

  await back(page)
  await expect(editForm(page)).toHaveCount(0)
  await expect(page).toHaveURL(/#\/plan$/)
  await expect(rowMenuButton(page)).toBeFocused()
  await ordinaryBackToToday(page)
})

test('2b. the form guide over the picker: Back closes the guide only', async ({ page }) => {
  await editFormOpen(page)
  await editForm(page).getByRole('button', { name: 'Change' }).click()
  await picker(page).getByRole('group', { name: 'Browse by muscle' }).getByRole('button').filter({ hasText: 'Quads' }).click()
  await picker(page).getByRole('listitem', { name: 'Squat', exact: true }).getByRole('button', { name: 'Form guide for Squat' }).click()
  await expect(guideSheet(page, 'Squat')).toBeVisible()
  await entries(page, 3)

  await back(page)
  await expect(guideSheet(page, 'Squat')).toHaveCount(0)
  await expect(picker(page).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
  await expect(editForm(page)).toBeVisible()
  await entries(page, 2)
  await dialogListening(picker(page))
  // Each Back once the one before has settled, as swipes are (see the notes on a Back within a few ms of the last).
  await back(page)
  await expect(picker(page).getByRole('group', { name: 'Browse by muscle' })).toBeVisible()
  await entries(page, 2)
  await back(page)
  await expect(picker(page)).toHaveCount(0)
  await entries(page, 1)
  await dialogListening(editForm(page))
  await back(page)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page).toHaveURL(/#\/plan$/)
  await ordinaryBackToToday(page)
})

test("2c. the RPE sheet over Plan's form: Back closes the sheet, then the form", async ({ page }) => {
  await editFormOpen(page)
  await editForm(page).getByRole('button', { name: 'What RPE means' }).click()
  const sheet = page.getByRole('dialog', { name: rpeGuide.title })
  await expect(sheet).toBeVisible()
  await entries(page, 2)
  await back(page)
  await expect(sheet).toHaveCount(0)
  await expect(editForm(page).getByRole('group', { name: 'RPE' })).toContainText('8.5')
  await entries(page, 1)
  await dialogListening(editForm(page))
  await back(page)
  await expect(editForm(page)).toHaveCount(0)
  await expect(page).toHaveURL(/#\/plan$/)
  await ordinaryBackToToday(page)
})

test.describe('3. menus', () => {
  test('a ⋮ menu: Back closes it and gives the focus back to its button; the next Back leaves', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await goToTab(page, 'Plan')
    await rowMenuButton(page).click()
    await expect(page.getByRole('menu')).toBeVisible()
    await entries(page, 1)
    await back(page)
    await expect(page.getByRole('menu')).toHaveCount(0)
    await expect(rowMenuButton(page)).toBeFocused()
    await expect(page).toHaveURL(/#\/plan$/)
    await ordinaryBackToToday(page)
  })

  test('a menu item that opens a dialog: the menu goes, the dialog opens, Back closes the dialog', async ({ page }) => {
    await editFormOpen(page)
    await back(page)
    await expect(editForm(page)).toHaveCount(0)
    await expect(page).toHaveURL(/#\/plan$/)
    await ordinaryBackToToday(page)
  })

  test('Plan options → Choose a template: the menu closes and Templates opens; Back to Plan, then to Today', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await goToTab(page, 'Plan')
    await page.getByRole('button', { name: 'Plan options' }).click()
    await entries(page, 1)
    await page.getByRole('menuitem', { name: 'Choose a template' }).click()
    await expect(page).toHaveURL(/#\/plan\/templates\/powerlifting$/)
    await entries(page, 0)
    await back(page)
    await expect(page).toHaveURL(/#\/plan$/)
    await onTab(page, 'Plan')
    await ordinaryBackToToday(page)
  })

  test('a tab tapped with a ⋮ menu open: the new tab, and Back returns to the screen, not to the menu', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await goToTab(page, 'Plan')
    await rowMenuButton(page).click()
    await entries(page, 1)
    await goToTab(page, 'Coach')
    await expect(page.getByRole('menu')).toHaveCount(0)
    await entries(page, 0)
    await back(page)
    await expect(page).toHaveURL(/#\/plan$/)
    await onTab(page, 'Plan')
    await expect(page.getByRole('menu')).toHaveCount(0)
    await ordinaryBackToToday(page)
  })
})

test('4. closed by its button, by a tap outside or by Escape, then Back: one ordinary step', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  const start = await where(page)
  const sheet = guideSheet(page, 'Hack squat')
  const open = async () => {
    await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
    await expect(sheet).toBeVisible()
    await entries(page, 1)
  }
  await open()
  await sheet.getByRole('button', { name: 'Close' }).click()
  await expect(sheet).toHaveCount(0)
  await entries(page, 0)
  await open()
  // A tap on the strip above the sheet.
  const top = (await sheet.boundingBox())!.y
  await page.touchscreen.tap(200, top / 2)
  await expect(sheet).toHaveCount(0)
  await entries(page, 0)
  await open()
  await page.keyboard.press('Escape')
  await expect(sheet).toHaveCount(0)
  await entries(page, 0)
  expect((await where(page)).hash).toBe(start.hash)
  await ordinaryBackToToday(page)
})

test('5. "Use this plan?" → Replace lands on Plan; Back from Plan then goes where it went before', async ({ page }) => {
  await openOnWednesday(page)
  await putSetting(page, 'trainingMode', 'BODYBUILDING')
  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
  await goToTab(page, 'Plan')
  // A first plan, from the builder (no question over an empty plan).
  await page.getByRole('button', { name: 'Build my plan' }).click()
  await throughTheBuilder(page)
  await page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' }).click()
  await expect(page).toHaveURL(/#\/plan$/)
  await entries(page, 0)
  // Over a plan: Plan options → Rebuild my plan → … → Use this plan? → Back closes only the question.
  await page.getByRole('button', { name: 'Plan options' }).click()
  await page.getByRole('menuitem', { name: 'Rebuild my plan' }).click()
  await expect(page).toHaveURL(/#\/plan\/build$/)
  await throughTheBuilder(page)
  await page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' }).click()
  const confirm = page.getByRole('dialog', { name: 'Use this plan?' })
  await expect(confirm).toBeVisible()
  await entries(page, 1)
  await back(page)
  await expect(confirm).toHaveCount(0)
  await expect(page).toHaveURL(/#\/plan\/build$/)
  await entries(page, 0)
  // Again, and Replace.
  await page.locator('.pb-actions').getByRole('button', { name: 'Use this plan' }).click()
  await confirm.getByRole('button', { name: 'Replace' }).click()
  await expect(page).toHaveURL(/#\/plan$/)
  await onTab(page, 'Plan')
  await ordinaryBackToToday(page)
})

test('a slow step back (2.2 s, past the 1.5 s check): it is not asked for twice, the screen stays, and the next Back is ordinary', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  const sheet = guideSheet(page, 'Hack squat')
  await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
  await expect(sheet).toBeVisible()
  await entries(page, 1)
  await slowGo(page, 2200)
  await sheet.getByRole('button', { name: 'Close' }).click()
  await expect(sheet).toHaveCount(0)
  // The step arrives late; then nothing more happens.
  await expect.poll(() => depth(page), { timeout: 6000 }).toBe(0)
  await page.waitForTimeout(2500)
  await expect(page).toHaveURL(/#\/exercises$/)
  await onTab(page, 'Exercises')
  await ordinaryBackToToday(page)
})

test.describe('6. Templates', () => {
  async function draftOverAPlan(page: Page) {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await goToTab(page, 'Plan')
    await page.getByRole('button', { name: 'Plan options' }).click()
    await page.getByRole('menuitem', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await expect(page.getByRole('heading', { level: 1, name: '3-day powerlifting' })).toBeVisible()
  }
  const confirm = (page: Page) => page.getByRole('dialog', { name: 'Replace your powerlifting plan?' })

  test('Back with "Replace your plan?" open closes only the question and stays on the draft; Replace then lands on Plan', async ({ page }) => {
    await draftOverAPlan(page)
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(confirm(page)).toBeVisible()
    await entries(page, 1)
    await countHistoryCalls(page)
    await back(page)
    await expect(confirm(page)).toHaveCount(0)
    await expect(page.getByRole('heading', { level: 1, name: '3-day powerlifting' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Use this plan' })).toBeVisible()
    await entries(page, 0)
    // No history call of the app's after the Back: Templates did not step back from its draft either.
    await page.waitForTimeout(300)
    expect(await historyCalls(page)).toEqual([])
    expect(await page.evaluate(() => (history.state as { templatesStep?: string } | null)?.templatesStep)).toBe('draft')

    await page.getByRole('button', { name: 'Use this plan' }).click()
    await confirm(page).getByRole('button', { name: 'Replace' }).click()
    await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
    await expect(page).toHaveURL(/#\/plan$/)
    await ordinaryBackToToday(page)
  })

  test('Replace while the question\'s entry is still being taken off (a slow step): Templates waits for it, then lands on Plan', async ({ page }) => {
    // The save is quicker than the step back here, so Templates' own step off the draft must wait for the dialog's
    // entry to be gone (afterDialogEntries): run at once, it would read the dialog's entry as where it is.
    await draftOverAPlan(page)
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(confirm(page)).toBeVisible()
    await entries(page, 1)
    await slowGo(page, 800)
    await confirm(page).getByRole('button', { name: 'Replace' }).click()
    await expect(page.getByText('Plan set: 3-day powerlifting.')).toBeVisible()
    await expect(page).toHaveURL(/#\/plan$/)
    await onTab(page, 'Plan')
    await ordinaryBackToToday(page)
  })

  test('draft → Back → the list; Back → Plan; and Close from the list after a question was closed', async ({ page }) => {
    await draftOverAPlan(page)
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await confirm(page).getByRole('button', { name: 'Cancel' }).click()
    await expect(confirm(page)).toHaveCount(0)
    await entries(page, 0)
    await back(page)
    await expect(page.getByRole('heading', { level: 1, name: 'Choose a template' })).toBeVisible()
    await back(page)
    await expect(page).toHaveURL(/#\/plan$/)
    await ordinaryBackToToday(page)
  })

  test('Close from the draft with a question closed before: lands on Plan', async ({ page }) => {
    await draftOverAPlan(page)
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(confirm(page)).toBeVisible()
    await entries(page, 1)
    await back(page)
    await expect(confirm(page)).toHaveCount(0)
    await page.getByRole('button', { name: 'Close' }).click()
    await expect(page).toHaveURL(/#\/plan$/)
    await onTab(page, 'Plan')
    await ordinaryBackToToday(page)
  })
})

test('7. the form guide → Add to plan → day chooser: Back closes the chooser; a day, then "Open Plan", lands on Plan', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
  await guideSheet(page, 'Hack squat').getByRole('button', { name: 'Add to plan' }).click()
  const chooser = page.getByRole('dialog', { name: 'Add to plan' })
  await expect(chooser).toBeVisible()
  // The guide's entry stays for the chooser, which opens in the same commit that closes the guide: the depth is 1 before
  // the chooser has taken it over, so the wait is on the chooser itself.
  await dialogListening(chooser)
  await entries(page, 1)
  await back(page)
  await expect(chooser).toHaveCount(0)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await entries(page, 0)
  await expect(page).toHaveURL(/#\/exercises$/)

  await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
  await guideSheet(page, 'Hack squat').getByRole('button', { name: 'Add to plan' }).click()
  await chooser.getByRole('button', { name: 'Wednesday' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await entries(page, 0)
  await page.getByRole('button', { name: 'Open Plan' }).click()
  await onTab(page, 'Plan')
  await back(page)
  await onTab(page, 'Exercises')
  await ordinaryBackToToday(page)
})

test.describe('8. the check-in', () => {
  async function checkIn(page: Page) {
    await openOnWednesday(page)
    await putSetting(page, 'trainingMode', 'BODYBUILDING')
    // A plan to check in on (an empty one has nothing to ask about).
    await squatOnWednesday(page, 'BODYBUILDING')
    await expect(page.locator('html')).toHaveAttribute('data-mode', 'bodybuilding')
    await goToTab(page, 'Progress')
    await page.getByRole('button', { name: 'Check in' }).click()
    await expect(page).toHaveURL(/#\/checkin$/)
    await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
  }

  test('its two steps behave as before', async ({ page }) => {
    await checkIn(page)
    await page.getByRole('button', { name: 'Next' }).click()
    await expect(page.getByRole('heading', { name: 'Which are growing well?' })).toBeVisible()
    await back(page)
    await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
    await page.getByRole('button', { name: 'Next' }).click()
    await page.locator('.ci-actions').getByRole('button', { name: 'Back' }).click()
    await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
    await back(page)
    await onTab(page, 'Progress')
    await ordinaryBackToToday(page)
  })

  /** What a reload on step 2 leads to, and each Back after it: the same with or without a dialog open over it. */
  async function afterAReloadOnStep2(page: Page) {
    // The page's own state: step 1 shown, on step 2's entry (how a reload there has always ended).
    await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
    expect(await page.evaluate(() => history.state)).toEqual({ gritFrom: '#/progress', checkInStep: 'growing' })
    await back(page)
    await expect(page.getByRole('heading', { name: 'Which body parts are lagging?' })).toBeVisible()
    await expect.poll(() => page.evaluate(() => history.state)).toEqual({ gritFrom: '#/progress' })
    await back(page)
    await onTab(page, 'Progress')
    await ordinaryBackToToday(page)
  }

  test('a reload on step 2 with nothing open: the baseline', async ({ page }) => {
    await checkIn(page)
    await page.getByRole('button', { name: 'Next' }).click()
    await expect(page.getByRole('heading', { name: 'Which are growing well?' })).toBeVisible()
    await page.reload()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await afterAReloadOnStep2(page)
  })

  test('a reload on step 2 with "Set up App lock again?" open over it: Back closes it, then as the baseline', async ({ page }) => {
    await checkIn(page)
    await page.getByRole('button', { name: 'Next' }).click()
    await expect(page.getByRole('heading', { name: 'Which are growing well?' })).toBeVisible()
    // App lock on with no passkey here: the dialog opens by itself on each load.
    await putSetting(page, 'appLock', true)
    await page.reload()
    const lockSetup = page.getByRole('dialog', { name: 'Set up App lock again?' })
    await expect(lockSetup).toBeVisible()
    await entries(page, 1)
    // The reload that matters: with the dialog open, on its entry.
    await page.reload()
    await expect(lockSetup).toBeVisible()
    await entries(page, 1)
    await back(page)
    await expect(lockSetup).toHaveCount(0)
    await entries(page, 0)
    await afterAReloadOnStep2(page)
  })
})

test.describe('9. reload with a dialog open', () => {
  test('on a plain screen: the page opens with nothing open and no entry of ours; Back leaves as before', async ({ page }) => {
    await openOnWednesday(page)
    await goToTab(page, 'Exercises')
    await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
    await entries(page, 1)
    await page.reload()
    await onTab(page, 'Exercises')
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await entries(page, 0)
    await ordinaryBackToToday(page)
  })

  test('on the Templates draft with "Replace your plan?" open: as a reload on the draft, the list; Back to Plan', async ({ page }) => {
    await openOnWednesday(page)
    await squatOnWednesday(page)
    await goToTab(page, 'Plan')
    await page.getByRole('button', { name: 'Plan options' }).click()
    await page.getByRole('menuitem', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(page.getByRole('dialog', { name: 'Replace your powerlifting plan?' })).toBeVisible()
    await entries(page, 1)
    await page.reload()
    await expect(page.getByRole('heading', { level: 1, name: 'Choose a template' })).toBeVisible()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    await entries(page, 0)
    expect(await page.evaluate(() => (history.state as { templatesStep?: string } | null)?.templatesStep)).toBeUndefined()
    // Back from the list lands on Plan, as from a list never left; then Today.
    await back(page)
    await expect(page).toHaveURL(/#\/plan$/)
    await onTab(page, 'Plan')
    await ordinaryBackToToday(page)
  })
})

test('10. Forward after a Back-close: nothing opens and no entry is left; Back then leaves as before', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
  await entries(page, 1)
  await back(page)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.goForward()
  await entries(page, 0)
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await onTab(page, 'Exercises')
  await ordinaryBackToToday(page)
})

test('11. open and close quickly several times, then Back: one ordinary step, and history does not grow', async ({ page }) => {
  await openOnWednesday(page)
  await goToTab(page, 'Exercises')
  const start = await where(page)
  const sheet = guideSheet(page, 'Hack squat')
  for (let i = 0; i < 6; i++) {
    await page.getByRole('button', { name: 'Open Hack squat', exact: true }).click()
    await expect(sheet).toBeVisible()
    await dialogListening(sheet)
    await page.keyboard.press('Escape')
    await expect(sheet).toHaveCount(0)
  }
  await entries(page, 0)
  // At most the one entry ahead of this one, which the next entry or navigation replaces.
  expect((await where(page)).length).toBeLessThanOrEqual(start.length + 1)
  await ordinaryBackToToday(page)
  // Forward goes back to Exercises (the entry ahead of it is ours, stepped off), never a dead press.
  await page.goForward()
  await expect.poll(async () => (await where(page)).hash).toBe('#/exercises')
  await entries(page, 0)
})
