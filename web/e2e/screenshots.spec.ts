import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, seedFlatBench, tabBar, tap } from './helpers'

// The screens that need a particular state to be seen, dark and light. The rest (today, coach, progress,
// lift history, plan, settings and the showcase) are written by the specs that exercise them. A normal run writes to test-results/screens/;
// UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/. Look at them after a visual change.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

const grit = GritDomain.getInstance()

/**
 * Waits until the exercise list has at least `rows` rows. The picker and the Exercises tab draw their first screenful
 * at once and the rest straight after, so a shot waits for the rest.
 */
const wholeList = (page: Page, rows: number) => expect.poll(() => page.locator('.lib-row').count()).toBeGreaterThanOrEqual(rows)

async function shoot(page: Page, name: string) {
  mkdirSync(outDir, { recursive: true })
  await page.evaluate(() => document.fonts.ready)
  await page.screenshot({ path: resolve(outDir, `${name}.png`), animations: 'disabled' })
}

for (const scheme of ['dark', 'light'] as const) {
  test(`screenshots: lock (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    const cdp = await page.context().newCDPSession(page)
    await cdp.send('WebAuthn.enable')
    const { authenticatorId } = await cdp.send('WebAuthn.addVirtualAuthenticator', {
      options: { protocol: 'ctap2', transport: 'internal', hasResidentKey: true, hasUserVerification: true, isUserVerified: true, automaticPresenceSimulation: true },
    })
    await page.goto('/#/settings')
    await page.getByRole('switch', { name: 'App lock' }).click()
    await expect(page.getByRole('switch', { name: 'App lock' })).toHaveAttribute('aria-checked', 'true')
    // The next launch, with a face the phone does not know.
    await cdp.send('WebAuthn.setUserVerified', { authenticatorId, isUserVerified: false })
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Grit is locked' })).toBeVisible()
    await shoot(page, `lock-${scheme}`)
  })

  test(`screenshots: today with the install card (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await page.clock.setFixedTime(new Date('2026-11-11T09:41:00'))
    await page.goto('/#/today')
    await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeVisible()
    await shoot(page, `today-install-${scheme}`)
  })

  test(`screenshots: today in a session with the rest bar (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    await seedFlatBench(page)
    await tap(page, 'Start session')
    await tap(page, 'Log set 1')
    await expect(page.getByRole('timer', { name: 'Rest, 3:00 left' })).toBeVisible()
    await shoot(page, `today-rest-${scheme}`)
  })

  test(`screenshots: the exercise picker, one muscle and a form guide (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    await seedFlatBench(page)
    await tap(page, 'Start session')
    await tap(page, 'Add exercise')
    await expect(page.getByRole('dialog', { name: 'Add exercise' }).getByRole('heading', { name: 'All exercises' })).toBeVisible()
    await wholeList(page, grit.library().length)
    await shoot(page, `exercise-picker-${scheme}`)

    // One muscle chosen from the grid.
    await page.getByRole('group', { name: 'Browse by muscle' }).getByRole('button', { name: 'Quads' }).click()
    await expect(page.getByRole('dialog', { name: 'Add exercise' }).getByRole('heading', { name: 'Quads exercises' })).toBeVisible()
    await wholeList(page, grit.exercisesByMuscle('QUADS', []).length)
    await shoot(page, `exercise-picker-muscle-${scheme}`)
    await page.getByRole('button', { name: 'All muscles' }).click()
    await wholeList(page, grit.library().length)

    await page.getByRole('button', { name: 'Form guide for Squat' }).click()
    await expect(page.getByRole('dialog', { name: 'Form guide for Squat' }).getByRole('heading', { name: 'How to do it' })).toBeVisible()
    await shoot(page, `form-guide-${scheme}`)
    // And its end: the last section and the safety line.
    await page.getByRole('dialog', { name: 'Form guide for Squat' }).evaluate((el) => el.scrollTo(0, el.scrollHeight))
    await shoot(page, `form-guide-end-${scheme}`)
  })

  test(`screenshots: the Exercises tab, its muscles, a guide with actions and the day chooser (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    await seedFlatBench(page)
    await tap(page, 'Start session')
    await tabBar(page).getByRole('link', { name: 'Exercises' }).click()
    await expect(page.getByRole('heading', { name: 'Main lifts' })).toBeVisible()
    await wholeList(page, grit.library().length)
    await shoot(page, `exercises-${scheme}`)

    // Two muscle groups ticked, with the drop-down still open over the tab.
    await page.getByRole('button', { name: 'Muscles' }).click()
    const muscles = page.getByRole('dialog', { name: 'Muscles' })
    await muscles.getByRole('checkbox', { name: 'Quads' }).check()
    await muscles.getByRole('checkbox', { name: 'Calves' }).check()
    await expect(page.getByRole('button', { name: 'Muscles · 2' })).toBeAttached()
    await wholeList(page, grit.exercisesByMuscles(['QUADS', 'CALVES'], []).length)
    await shoot(page, `exercises-muscles-${scheme}`)
    await muscles.getByRole('button', { name: 'Close' }).click()
    await shoot(page, `exercises-muscles-chosen-${scheme}`)

    // A session is open: the guide offers today as well as the plan.
    await page.getByRole('button', { name: 'Open Goblet squat', exact: true }).click()
    await expect(page.getByRole('dialog', { name: 'Form guide for Goblet squat' }).getByRole('button', { name: 'Add to today' })).toBeVisible()
    await shoot(page, `exercise-guide-actions-${scheme}`)
    await page.getByRole('dialog', { name: 'Form guide for Goblet squat' }).getByRole('button', { name: 'Add to plan' }).click()
    await expect(page.getByRole('dialog', { name: 'Add to plan' }).getByText('Which day?')).toBeVisible()
    await shoot(page, `exercise-day-chooser-${scheme}`)
  })

  test(`screenshots: a form guide with its picture (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page, '#/exercises')
    await page.getByRole('button', { name: 'Open Bench', exact: true }).click()
    const guide = page.getByRole('dialog', { name: 'Form guide for Bench' })
    await expect(guide.getByRole('img', { name: /^Picture: / })).toBeVisible()
    await guide.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)))
    await shoot(page, `form-guide-picture-${scheme}`)
  })

  test(`screenshots: the lat pulldown's form guide with its picture (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page, '#/exercises')
    await page.getByRole('textbox', { name: 'Search exercises' }).fill('lat pulldown')
    await page.getByRole('button', { name: 'Open Lat pulldown', exact: true }).click()
    const guide = page.getByRole('dialog', { name: 'Form guide for Lat pulldown' })
    await expect(guide.getByRole('img', { name: /^Picture: / })).toBeVisible()
    await guide.evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)))
    await shoot(page, `form-guide-lat-pulldown-${scheme}`)
  })

  test(`screenshots: session saved (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    await seedFlatBench(page)
    await tap(page, 'Start session')
    await tap(page, 'Log set 1')
    await tap(page, 'Finish')
    await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
    await tap(page, 'Skip')
    await shoot(page, `session-saved-${scheme}`)
  })

  test(`screenshots: recovery (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    // A database from a newer Grit cannot be opened by this one.
    await page.evaluate(async () => {
      await new Promise<void>((done, fail) => {
        const del = indexedDB.deleteDatabase('grit')
        del.onsuccess = () => done()
        del.onerror = () => fail(del.error)
        del.onblocked = () => done()
      })
      await new Promise<void>((done, fail) => {
        const open = indexedDB.open('grit', 9999)
        open.onsuccess = () => {
          open.result.close()
          done()
        }
        open.onerror = () => fail(open.error)
      })
    })
    await page.reload()
    await expect(page.getByRole('heading', { name: /^Grit couldn't/ })).toBeVisible()
    await shoot(page, `recovery-${scheme}`)
  })

  test(`screenshots: private window (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    // What Safari's private windows do: no file storage.
    await page.addInitScript(() => {
      Object.defineProperty(navigator.storage, 'getDirectory', { value: () => Promise.reject(new DOMException('no', 'SecurityError')) })
    })
    await page.goto('/#/today')
    await expect(page.getByRole('heading', { name: "Private browsing won't keep your log" })).toBeVisible()
    await shoot(page, `private-mode-${scheme}`)
  })

  test(`screenshots: set up App lock again (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await openOnWednesday(page)
    // App lock on in a backup restored from another phone: no passkey here.
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
    await expect(page.getByRole('dialog', { name: 'Set up App lock again?' })).toBeVisible()
    await shoot(page, `lock-setup-${scheme}`)
  })

  test(`screenshots: replace all data (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await page.goto('/#/settings')
    await page.getByLabel('Backup file').setInputFiles(resolve(process.cwd(), 'src/backup/__fixtures__/web-v1.json'))
    await expect(page.getByRole('dialog', { name: 'Replace all data?' })).toBeVisible()
    await shoot(page, `restore-confirm-${scheme}`)
  })

  test(`screenshots: another tab (${scheme})`, async ({ page, context }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await page.goto('/#/today')
    await expect(tabBar(page)).toBeVisible()
    const second = await context.newPage()
    await second.emulateMedia({ colorScheme: scheme })
    await second.goto('/#/today')
    await expect(second.getByRole('heading', { name: 'Grit is open in another tab' })).toBeVisible()
    await shoot(second, `other-tab-${scheme}`)
  })
}
