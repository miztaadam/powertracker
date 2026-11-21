import { expect, test, type Locator, type Page } from '@playwright/test'
import { openOnWednesday, tabBar } from './helpers'

// 1.17.1: an exercise's ⋮ menu on Today, from the owner's phone. At 306 px a name like "Cable chest press" wraps the open
// card's head, the ⋮ dropped alone to the left under the name, and its menu (anchored at its right edge) opened past the
// left edge of the screen: "Count plates on this machine" cut, "Skip today" out of sight. Now the menu always stays inside
// the screen, and the head's ⓘ and ⋮ stay together at the right.

const LONG = 'Single-arm cable crossover from the low pulley'

/** Cable chest press (catalogue) and a custom accessory with a long name, both planned on Wednesdays on the powerlifting side. */
async function seed(page: Page, plates: boolean) {
  await page.evaluate(
    async ({ plates, long }) => {
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
      const add = (store: string, row: unknown) =>
        new Promise<number>((done) => {
          const request = tx.objectStore(store).add(row)
          request.onsuccess = () => done(request.result as number)
        })
      const setting = plates ? { plateCount: 'WHOLE' } : {}
      const cable = await add('exercises', { name: 'Cable chest press', category: 'ACCESSORY', parentLift: null, muscles: ['PECS'], isBuiltIn: true, archived: false, ...setting })
      const custom = await add('exercises', { name: long, category: 'ACCESSORY', parentLift: null, muscles: ['PECS'], isBuiltIn: false, archived: false, ...setting })
      await add('planItems', { weekday: 3, exerciseId: cable, sets: 3, reps: 10, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
      await add('planItems', { weekday: 3, exerciseId: custom, sets: 3, reps: 10, targetRpe: 8, order: 1, mode: 'POWERLIFTING' })
      await settled
      db.close()
    },
    { plates, long: LONG },
  )
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

/** The part of the screen the menu may use: the window, above the rest bar and the tab bar. */
const room = (page: Page) =>
  page.evaluate(() => {
    const docked = [...document.querySelectorAll('.shell-rest, .gr-tabbar')].map((el) => el.getBoundingClientRect().top)
    return { width: window.innerWidth, bottom: Math.min(window.innerHeight, ...docked) }
  })

/** Opens `name`'s menu (`scope`'s ⋮), checks it is wholly on screen, then picks Count plates on this machine: its sheet opens. */
async function opensInside(page: Page, scope: Locator, name: string, what: string) {
  await scope.getByRole('button', { name: `Options for ${name}` }).click()
  const menu = page.getByRole('menu', { name: `Options for ${name}` })
  await expect(menu).toBeVisible()
  const { width, bottom } = await room(page)
  const box = (await menu.boundingBox())!
  expect(box.x, `${what}: the menu starts inside the screen`).toBeGreaterThanOrEqual(0)
  expect(box.x + box.width, `${what}: the menu ends inside the screen`).toBeLessThanOrEqual(width)
  expect(box.y, `${what}: the menu's top is on screen`).toBeGreaterThanOrEqual(0)
  expect(box.y + box.height, `${what}: the menu is above the docked bars`).toBeLessThanOrEqual(bottom)
  for (const item of await menu.getByRole('menuitem').all()) {
    const b = (await item.boundingBox())!
    expect(b.x >= 0 && b.x + b.width <= width && b.y >= 0 && b.y + b.height <= bottom, `${what}: ${await item.textContent()} is whole on screen`).toBe(true)
  }
  await menu.getByRole('menuitem', { name: 'Count plates on this machine' }).click()
  const sheet = page.getByRole('dialog', { name: `Count plates on ${name}` })
  await expect(sheet).toBeVisible()
  await sheet.getByRole('button', { name: 'Done' }).click()
  await expect(sheet).toBeHidden()
}

for (const [width, height] of [
  [306, 664],
  [320, 568],
  [360, 640],
  [390, 844],
  [160, 284],
] as const)
for (const load of ['kg', 'plates'] as const) {
  test.describe(`the ⋮ menu at ${width} px, ${load}`, () => {
    test.use({ viewport: { width, height } })

    test('opens inside the screen from the start card, the open card and a folded row, and the head keeps ⓘ and ⋮ together', async ({ page }) => {
      await openOnWednesday(page)
      await seed(page, load === 'plates')
      const start = page.locator('.today-plan')
      for (const name of ['Cable chest press', LONG]) await opensInside(page, start, name, `start card, ${name}`)

      await page.getByRole('button', { name: 'Start session', exact: true }).click()
      const head = page.locator('.today-card-head')
      await expect(head.getByRole('heading', { name: 'Cable chest press' })).toBeVisible()
      // ⓘ and ⋮ side by side, the ⋮ at the head's right edge, whatever wraps.
      const [info, more, edge] = await Promise.all([
        head.getByRole('button', { name: /^Form guide for / }).boundingBox(),
        head.getByRole('button', { name: 'Options for Cable chest press' }).boundingBox(),
        head.boundingBox(),
      ])
      expect(Math.abs(info!.y - more!.y), 'ⓘ and ⋮ on one line').toBeLessThanOrEqual(1)
      expect(more!.x, 'ⓘ just before ⋮').toBeGreaterThan(info!.x)
      expect(Math.abs(more!.x + more!.width - (edge!.x + edge!.width)), '⋮ at the right edge').toBeLessThanOrEqual(1)
      await opensInside(page, page.locator('.today-card-head'), 'Cable chest press', 'open card')

      // The folded row, scrolled to the bottom of the screen: the menu goes up rather than under the tab bar.
      const row = page.locator('.today-row-card').filter({ hasText: LONG })
      await row.evaluate((el) => el.scrollIntoView({ block: 'end' }))
      await opensInside(page, row, LONG, 'folded row at the bottom')
    })
  })
}
