import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { dialogListening, openOnWednesday, rpeField, tap, typeInto, weightField } from './helpers'

// The "What RPE means" button and its sheet, and a strict press on Today, on the narrowest iPhone:
// the button is a full 48 px target that leaves the RPE field easy to hit, nothing overflows, and the
// sheet can be read to its last line at 130% zoom. A normal run writes the screenshots to test-results/;
// UPDATE_SCREENSHOTS=1 rewrites the committed PNGs in docs/design/web/screens/.
const outDir = resolve(process.cwd(), process.env.UPDATE_SCREENSHOTS === '1' ? '../docs/design/web/screens' : 'test-results/screens')

const guide = GritDomain.getInstance().rpeGuide()
const SAFETY = GritDomain.getInstance().safetyLine()

const narrowPhone = { width: 375, height: 812 }
test.use({ viewport: narrowPhone })

const info = (page: Page) => page.getByRole('button', { name: 'What RPE means' })
const sheet = (page: Page) => page.getByRole('dialog', { name: guide.title })

type Box = { x: number; y: number; width: number; height: number }
const box = async (page: Page, name: string, role: 'button' | 'textbox' = 'button'): Promise<Box> => (await page.getByRole(role, { name, exact: true }).boundingBox())!
const right = (b: Box) => b.x + b.width
const bottom = (b: Box) => b.y + b.height

/** Plans `exercise` for Wednesdays, straight into IndexedDB, then reloads. Strict press gets its row first: a new install has none. */
async function planOnWednesday(page: Page, exercise: 'Squat' | 'Strict press') {
  await page.evaluate(async (exercise) => {
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
    const plan = (exerciseId: IDBValidKey) => tx.objectStore('planItems').add({ weekday: 3, exerciseId, sets: 3, reps: 5, targetRpe: 8.5, order: 0 })
    if (exercise === 'Strict press') {
      const added = tx.objectStore('exercises').add({ name: 'Strict press', category: 'STRICT_PRESS', parentLift: null, muscles: ['Front delts', 'Side delts', 'Triceps'], isBuiltIn: true, archived: false })
      added.onsuccess = () => plan(added.result)
    } else {
      const all = tx.objectStore('exercises').getAll()
      all.onsuccess = () => plan((all.result as { id: number; name: string }[]).find((e) => e.name === exercise)!.id)
    }
    await settled
    db.close()
  }, exercise)
  await page.reload()
  await expect(page.getByText('Wednesday · 1 exercise')).toBeVisible()
}

/** Things wider than the phone. */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<HTMLElement>('.shell-main *, .gr-dialog *')) {
      const b = el.getBoundingClientRect()
      if (b.width > 0 && (b.right > window.innerWidth + 0.5 || b.left < -0.5) && !el.closest('.sr-only')) found.push(`${el.tagName.toLowerCase()}.${el.className}`)
    }
    return found
  })

const sets = (page: Page) =>
  page.evaluate(async () => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const all = await new Promise<{ weightKg: number; failed: boolean; missPoint: string | null }[]>((done, fail) => {
      const request = db.transaction('sets').objectStore('sets').getAll()
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    db.close()
    return all.map((s) => [s.weightKg, s.failed, s.missPoint])
  })

test('today: a strict press miss row and the RPE info button fit, and the RPE field stays easy to hit', async ({ page }) => {
  await openOnWednesday(page)
  await planOnWednesday(page, 'Strict press')
  await tap(page, 'Start session')
  await typeInto(page, weightField(page), '62.5')
  await tap(page, 'Missed')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()

  // A press stalls off the shoulders: the three points sit side by side, whole, under the question.
  const row = page.getByTestId('miss-row-1')
  const faces = await row.locator('.gr-segment-face').evaluateAll((els) => els.map((el) => ({ text: el.textContent, left: el.getBoundingClientRect().left, right: el.getBoundingClientRect().right, clipped: el.scrollWidth > el.clientWidth + 1 })))
  expect(faces.map((f) => f.text)).toEqual(['Off the shoulders', 'Mid', 'Lockout'])
  expect(faces.every((f) => !f.clipped)).toBe(true)
  expect(faces[0]!.right).toBeLessThanOrEqual(faces[1]!.left)
  expect(faces[1]!.right).toBeLessThanOrEqual(faces[2]!.left)
  const segments = await row.locator('.gr-segment').evaluateAll((els) => els.map((el) => el.getBoundingClientRect().toJSON() as DOMRect))
  expect(Math.min(...segments.map((s) => s.height))).toBeGreaterThanOrEqual(47.5)
  expect(segments[0]!.top).toBeGreaterThanOrEqual((await row.getByText('Where did it stall?').boundingBox())!.y + 10)
  await row.getByText('Off the shoulders').click()
  // The same stored point as the other lifts' "Floor".
  await expect.poll(() => sets(page)).toEqual([[62.5, true, 'FLOOR']])
  expect(await overflowing(page)).toEqual([])

  // The info button is a full-size target beside the RPE caption, on the screen.
  const target = (await info(page).boundingBox())!
  expect(Math.min(target.width, target.height)).toBeGreaterThanOrEqual(47.5)
  expect(target.x).toBeGreaterThanOrEqual(0)
  expect(right(target)).toBeLessThanOrEqual(narrowPhone.width)
  // The three figures stay on one line, clear of each other: the button adds nothing to its column.
  const fields = [await box(page, 'Weight, kilograms', 'textbox'), await box(page, 'Reps', 'textbox'), await box(page, 'RPE', 'textbox')]
  expect(new Set(fields.map((f) => Math.round(f.y))).size).toBe(1)
  expect(right(fields[0]!)).toBeLessThanOrEqual(fields[1]!.x)
  expect(right(fields[1]!)).toBeLessThanOrEqual(fields[2]!.x)
  expect(target.x).toBeGreaterThanOrEqual(right(fields[1]!))
  // It is centred on the caption line. Its target reaches the strip above the digits (10 px of the figure's 46) and no further.
  const caption = (await page.locator('.gr-stepper-field-action .eyebrow').first().boundingBox())!
  expect(Math.abs(target.y + target.height / 2 - (caption.y + caption.height / 2))).toBeLessThanOrEqual(1)
  expect(bottom(target) - fields[2]!.y).toBeLessThanOrEqual(10.5)
  for (const name of ['Decrease RPE', 'Increase RPE', 'Decrease Reps', 'Increase Reps']) expect(bottom(target)).toBeLessThanOrEqual((await box(page, name)).y)
  // Nor does it reach the set rows above the editor.
  expect(target.y).toBeGreaterThanOrEqual(Math.max(...segments.map((s) => s.bottom)))

  // Tapping the figure puts the cursor in the field: its middle, its caption, and right under the info target.
  const rpe = rpeField(page)
  const points: [string, number, number][] = [
    ['the middle of the figure', fields[2]!.x + fields[2]!.width / 2, fields[2]!.y + fields[2]!.height / 2],
    ['the caption', caption.x + 6, caption.y + caption.height / 2],
    ['just under the info target', target.x + target.width / 2, bottom(target) + 3],
  ]
  for (const [where, x, y] of points) {
    await page.touchscreen.tap(x, y)
    await expect(rpe, where).toBeFocused()
    await expect(sheet(page), where).toHaveCount(0)
    await rpe.blur()
  }
  await expect(rpe).toHaveValue('8.5')

  // Tapping the info button opens the sheet, out to each edge of its target, and changes nothing.
  const edges: [string, number, number][] = [
    ['its middle', target.x + target.width / 2, target.y + target.height / 2],
    ['its top', target.x + target.width / 2, target.y + 3],
    ['its bottom, over the figure', target.x + target.width / 2, bottom(target) - 3],
    ['its left', target.x + 3, target.y + target.height / 2],
    ['its right', right(target) - 3, target.y + target.height / 2],
  ]
  for (const [where, x, y] of edges) {
    await page.touchscreen.tap(x, y)
    await expect(sheet(page), where).toBeVisible()
    await expect(rpe, where).not.toBeFocused()
    await sheet(page).getByRole('button', { name: 'Close' }).click()
    await expect(sheet(page), where).toHaveCount(0)
  }
  await expect(rpe).toHaveValue('8.5')
  await expect.poll(() => sets(page)).toEqual([[62.5, true, 'FLOOR']])
})

test('plan: the RPE info button in the item dialog fits beside the stepper', async ({ page }) => {
  await openOnWednesday(page)
  await planOnWednesday(page, 'Squat')
  await page.getByRole('link', { name: 'Plan' }).click()
  await page.getByRole('region', { name: 'Wednesday' }).getByRole('button', { name: 'More' }).click()
  await page.getByRole('menuitem', { name: 'Edit' }).click()
  const dialog = page.getByRole('dialog', { name: 'Edit Wednesday' })
  await expect(dialog).toBeVisible()

  const target = (await dialog.getByRole('button', { name: 'What RPE means' }).boundingBox())!
  const minus = (await dialog.getByRole('button', { name: 'Decrease RPE' }).boundingBox())!
  const plus = (await dialog.getByRole('button', { name: 'Increase RPE' }).boundingBox())!
  const panel = (await dialog.boundingBox())!
  expect(Math.min(target.width, target.height)).toBeGreaterThanOrEqual(47.5)
  // Clear of the stepper, and the row stays inside the dialog.
  expect(right(target)).toBeLessThanOrEqual(minus.x + 0.5)
  expect(target.x).toBeGreaterThanOrEqual(panel.x)
  expect(right(plus)).toBeLessThanOrEqual(right(panel))
  // The label is still "RPE", on the button's line.
  const label = (await dialog.getByText('RPE', { exact: true }).boundingBox())!
  expect(right(label)).toBeLessThanOrEqual(target.x + 0.5)
  expect(await overflowing(page)).toEqual([])

  // The sheet opens over the dialog and leaves it as it was.
  await dialog.getByRole('button', { name: 'What RPE means' }).click()
  await expect(sheet(page)).toBeVisible()
  await dialogListening(sheet(page))
  await page.keyboard.press('Escape')
  await expect(sheet(page)).toHaveCount(0)
  await expect(dialog.getByRole('group', { name: 'RPE' })).toContainText('8.5')
})

// What 130% zoom does to a phone: the page is laid out 1.3 times narrower and shorter. The whole phone, and the
// half of it that is left above the keyboard, where the sheet is longer than the screen and has to scroll.
for (const [name, height, mustScroll] of [
  ['the whole phone', 812, false],
  ['half the phone', 420, true],
] as const) {
  test(`the RPE sheet can be read to its last line at 130% zoom (${name})`, async ({ page }) => {
    await page.setViewportSize({ width: Math.floor(375 / 1.3), height: Math.floor(height / 1.3) })
    await page.goto('/#/settings')
    await info(page).click()
    await expect(sheet(page).getByRole('heading', { level: 2, name: guide.title })).toBeVisible()

    const last = sheet(page).getByText(guide.lines.at(-1)!, { exact: true })
    if (mustScroll) {
      expect(await sheet(page).evaluate((el) => el.scrollHeight - el.clientHeight)).toBeGreaterThan(0)
      await expect(last).not.toBeInViewport()
    }

    // Every line is 16 px or more, as wide as the sheet allows and not clipped, down to the last one.
    for (const line of guide.lines) {
      const el = sheet(page).getByText(line, { exact: true })
      await el.scrollIntoViewIfNeeded()
      await expect(el, line).toBeInViewport()
      const fit = await el.evaluate((node) => {
        const b = node.getBoundingClientRect()
        return { left: b.left, right: b.right, clipped: node.scrollWidth > node.clientWidth + 1, fontSize: parseFloat(getComputedStyle(node).fontSize) }
      })
      expect(fit.left, line).toBeGreaterThanOrEqual(0)
      expect(fit.right, line).toBeLessThanOrEqual(page.viewportSize()!.width)
      expect(fit.clipped, line).toBe(false)
      expect(fit.fontSize, line).toBeGreaterThanOrEqual(16)
    }
    // Scrolled to the end, the last line is whole and nothing comes after it: no safety line.
    await sheet(page).evaluate((el) => el.scrollTo(0, el.scrollHeight))
    await expect(last).toBeInViewport({ ratio: 1 })
    await expect(page.getByText(SAFETY)).toHaveCount(0)
    // The title and Close stay in reach.
    await expect(sheet(page).getByRole('button', { name: 'Close' })).toBeInViewport({ ratio: 1 })
    const close = (await sheet(page).getByRole('button', { name: 'Close' }).boundingBox())!
    expect(Math.min(close.width, close.height)).toBeGreaterThanOrEqual(47.5)
    expect(await overflowing(page)).toEqual([])
  })
}

for (const scheme of ['dark', 'light'] as const) {
  test(`screenshots: the RPE sheet (${scheme})`, async ({ page }) => {
    await page.emulateMedia({ colorScheme: scheme })
    await page.setViewportSize({ width: 390, height: 844 })
    await openOnWednesday(page)
    await planOnWednesday(page, 'Squat')
    await tap(page, 'Start session')
    await info(page).click()
    await expect(sheet(page).getByText(guide.lines.at(-1)!, { exact: true })).toBeInViewport({ ratio: 1 })
    expect(await overflowing(page)).toEqual([])
    await page.evaluate(() => document.fonts.ready)
    mkdirSync(outDir, { recursive: true })
    await page.screenshot({ path: resolve(outDir, `rpe-sheet-${scheme}.png`), animations: 'disabled' })
  })
}
