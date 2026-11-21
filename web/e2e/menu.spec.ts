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

/**
 * The open menu's box and the part of the screen it may use (the window, above the rest bar and the tab bar), read
 * together in one go once the menu has settled: the same box on two frames running.
 */
const settled = (page: Page, name: string, label = `Options for ${name}`) =>
  page.evaluate(async (label) => {
    const frame = () => new Promise((done) => requestAnimationFrame(done))
    const read = () => {
      const menu = document.querySelector(`[role="menu"][aria-label="${label}"]`)!
      const box = menu.getBoundingClientRect()
      const docked = [...document.querySelectorAll('.shell-rest, .gr-tabbar')].map((el) => el.getBoundingClientRect().top)
      return {
        x: box.left,
        y: box.top,
        width: box.width,
        height: box.height,
        roomWidth: window.innerWidth,
        roomBottom: Math.min(window.innerHeight, ...docked),
        scrolls: menu.scrollHeight > menu.clientHeight + 1,
        items: [...menu.querySelectorAll('[role="menuitem"]')].map((item) => {
          const r = item.getBoundingClientRect()
          return { text: item.textContent, x: r.left, y: r.top, right: r.right, bottom: r.bottom }
        }),
      }
    }
    let last = JSON.stringify(read())
    for (let i = 0; i < 60; i++) {
      await frame()
      const now = JSON.stringify(read())
      if (now === last) return read()
      last = now
    }
    throw new Error('the menu did not settle')
  }, label)

/** Opens `name`'s menu (`scope`'s ⋮), checks it is wholly on screen, then picks Count plates on this machine: its sheet opens. */
async function opensInside(page: Page, scope: Locator, name: string, what: string) {
  await scope.getByRole('button', { name: `Options for ${name}` }).click()
  await opensInsideOpen(page, name, what)
}

/** The same checks on `name`'s menu, already open. */
async function opensInsideOpen(page: Page, name: string, what: string) {
  const menu = page.getByRole('menu', { name: `Options for ${name}` })
  await expect(menu).toBeVisible()
  const box = await settled(page, name)
  expect(box.x, `${what}: the menu starts inside the screen`).toBeGreaterThanOrEqual(0)
  expect(box.x + box.width, `${what}: the menu ends inside the screen`).toBeLessThanOrEqual(box.roomWidth)
  expect(box.y, `${what}: the menu's top is on screen`).toBeGreaterThanOrEqual(0)
  expect(box.y + box.height, `${what}: the menu is above the docked bars`).toBeLessThanOrEqual(box.roomBottom)
  // Every item whole on screen, unless the menu is taller than its room and scrolls: then each is inside the menu's box.
  for (const item of box.items) {
    if (box.scrolls) continue
    expect(item.x >= 0 && item.right <= box.roomWidth && item.y >= 0 && item.bottom <= box.roomBottom, `${what}: ${item.text} is whole on screen`).toBe(true)
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
    // Generous: three menus and two sheets per test, beside other workers.
    test.describe.configure({ timeout: 90_000 })

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

test.describe('the ⋮ menu, more cases', () => {
  test.describe.configure({ timeout: 90_000 })

  test('a name of one very long word does not push the ⋮ off the open card', async ({ page }) => {
    await page.setViewportSize({ width: 306, height: 664 })
    await openOnWednesday(page)
    const word = 'Supercalifragilisticexpialidociouspulldown'
    await page.evaluate(async (name) => {
      const db = await new Promise<IDBDatabase>((done, fail) => {
        const request = indexedDB.open('grit')
        request.onsuccess = () => done(request.result)
        request.onerror = () => fail(request.error)
      })
      const tx = db.transaction(['exercises', 'planItems'], 'readwrite')
      const done = new Promise<void>((ok) => (tx.oncomplete = () => ok()))
      const request = tx.objectStore('exercises').add({ name, category: 'ACCESSORY', parentLift: null, muscles: ['LATS'], isBuiltIn: false, archived: false })
      request.onsuccess = () => tx.objectStore('planItems').add({ weekday: 3, exerciseId: request.result, sets: 3, reps: 10, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
      await done
      db.close()
    }, word)
    await page.reload()
    await page.getByRole('button', { name: 'Start session', exact: true }).click()
    const head = page.locator('.today-card-head')
    await expect(head.getByRole('heading', { name: word })).toBeVisible()
    const [title, more, card] = await Promise.all([
      head.getByRole('heading', { name: word }).evaluate((h) => ({ right: h.getBoundingClientRect().right, scroll: h.scrollWidth, client: h.clientWidth })),
      head.getByRole('button', { name: `Options for ${word}` }).boundingBox(),
      head.boundingBox(),
    ])
    expect(more!.x + more!.width, 'the ⋮ inside the card').toBeLessThanOrEqual(card!.x + card!.width + 0.5)
    expect(title.scroll, 'the name breaks rather than run past its box').toBeLessThanOrEqual(title.client + 1)
    await opensInside(page, head, word, 'open card, one long word')
  })

  test('a menu taller than its room scrolls, and the arrow keys reach every item', async ({ page }) => {
    // 160 px wide, and short: the two wrapped items do not fit above or below the start card's ⋮.
    await page.setViewportSize({ width: 160, height: 230 })
    await openOnWednesday(page)
    await seed(page, false)
    const row = page.locator('.today-plan-line').first()
    await row.evaluate((el) => el.scrollIntoView({ block: 'center' }))
    await row.getByRole('button', { name: 'Options for Cable chest press' }).click()
    const box = await settled(page, 'Cable chest press')
    expect(box.y).toBeGreaterThanOrEqual(0)
    expect(box.y + box.height).toBeLessThanOrEqual(box.roomBottom)
    expect(box.scrolls, 'the menu scrolls').toBe(true)
    const menu = page.getByRole('menu', { name: 'Options for Cable chest press' })
    await expect(menu.getByRole('menuitem').first()).toBeFocused()
    await page.keyboard.press('ArrowDown')
    const last = menu.getByRole('menuitem').last()
    await expect(last).toBeFocused()
    // The focused item is scrolled into the menu's view.
    const [item, list] = await Promise.all([last.boundingBox(), menu.boundingBox()])
    expect(item!.y + item!.height).toBeLessThanOrEqual(list!.y + list!.height + 1)
    expect(item!.y).toBeGreaterThanOrEqual(list!.y - 1)
  })

  test('a menu closes when the window changes width (turning the phone)', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await openOnWednesday(page)
    await seed(page, false)
    await page.locator('.today-plan-line').first().getByRole('button', { name: 'Options for Cable chest press' }).click()
    await expect(page.getByRole('menu')).toBeVisible()
    await page.setViewportSize({ width: 844, height: 390 })
    await expect(page.getByRole('menu')).toHaveCount(0)
  })

  // The lifter has just typed a weight, so the on-screen keyboard is up, and taps ⋮: the menu takes focus, the field
  // loses it and the keyboard slides away. That changes the window's height only: the menu stays open and on screen.
  for (const [what, from, to] of [
    ['the keyboard going away (taller)', 480, 844],
    ['the keyboard coming up (shorter)', 844, 480],
  ] as const) {
    test(`a change of height only keeps the menu open and on screen: ${what}`, async ({ page }) => {
      await page.setViewportSize({ width: 390, height: from })
      await openOnWednesday(page)
      await seed(page, false)
      await page.getByRole('button', { name: 'Start session', exact: true }).click()
      const head = page.locator('.today-card-head')
      await expect(head.getByRole('heading', { name: 'Cable chest press' })).toBeVisible()
      await page.getByRole('textbox', { name: 'Weight, kilograms' }).focus()
      await head.getByRole('button', { name: 'Options for Cable chest press' }).click()
      const menu = page.getByRole('menu', { name: 'Options for Cable chest press' })
      await expect(menu).toBeVisible()
      await page.setViewportSize({ width: 390, height: to })
      await page.waitForTimeout(200)
      await expect(menu).toBeVisible()
      await expect(menu.getByRole('menuitem').first()).toBeFocused()
      // Placed again for the new height: whole on screen, above the tab bar; Count plates opens its sheet.
      await opensInsideOpen(page, 'Cable chest press', `after ${what}`)
    })
  }

  test('after a change of height only, Skip today opens its sheet', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 480 })
    await openOnWednesday(page)
    await seed(page, false)
    await page.getByRole('button', { name: 'Start session', exact: true }).click()
    await page.getByRole('textbox', { name: 'Weight, kilograms' }).focus()
    await page.locator('.today-card-head').getByRole('button', { name: 'Options for Cable chest press' }).click()
    const menu = page.getByRole('menu', { name: 'Options for Cable chest press' })
    await expect(menu).toBeVisible()
    await page.setViewportSize({ width: 390, height: 844 })
    await page.waitForTimeout(200)
    await menu.getByRole('menuitem', { name: 'Skip today' }).click()
    await expect(page.getByRole('dialog', { name: 'Why skip Cable chest press?' })).toBeVisible()
  })

  test("a change of height only keeps Plan's ⋮ menu open", async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 480 })
    await openOnWednesday(page)
    await seed(page, false)
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Plan options' }).click()
    const menu = page.getByRole('menu', { name: 'Plan options' })
    await expect(menu).toBeVisible()
    await page.setViewportSize({ width: 390, height: 844 })
    await page.waitForTimeout(200)
    await expect(menu).toBeVisible()
    const box = await settled(page, 'Plan options', 'Plan options')
    expect(box.x >= 0 && box.x + box.width <= box.roomWidth && box.y >= 0 && box.y + box.height <= box.roomBottom).toBe(true)
  })
})
