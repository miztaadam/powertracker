import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Locator, type Page } from '@playwright/test'
import postcss, { type ChildNode, type Rule } from 'postcss'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { openOnWednesday, seedFlatBench, tabBar, tap, weightField } from './helpers'
import { bodybuildingStory, powerliftingStory, seedStory } from './narrowChecks'

// 1.17.3 laid out some dialogs for a 320 px phone at 200 % text (160 × 284) in web/src/ui/narrowDeeper.css, each by its
// own class, and left every other dialog exactly as it was. Pictures at this width differ by a few pixels from run to
// run, so this pins the rule instead of the picture: with each dialog that must stay unchanged open from its real
// opener, no selector of narrowDeeper.css matches its scrim, its panel, anything inside it, or anything round it (an
// ancestor, which could pass on an inherited property such as font-size). The ⋮ menu has no scrim: its list, its
// items and the ⋮ button's own box (`.gr-menu`) are checked instead. Rules in other stylesheets are not checked here.

const SOURCE = resolve(process.cwd(), 'src/ui/narrowDeeper.css')

/**
 * Every selector in narrowDeeper.css, pseudo-elements stripped (`matches()` takes none). The file must be one
 * `@media (max-width: 239px)` block of plain rules; inside it, only `@media (forced-colors: active)` blocks of plain
 * rules. Anything else (another top-level rule or at-rule, a rule nested in a rule, `&`) throws: the parse below would
 * no longer be the whole of what the file does.
 */
function narrowSelectors(): string[] {
  const root = postcss.parse(readFileSync(SOURCE, 'utf8'), { from: SOURCE })
  const top = root.nodes.filter((n) => n.type !== 'comment')
  const block = top[0]
  if (top.length !== 1 || block?.type !== 'atrule' || block.name !== 'media' || block.params !== '(max-width: 239px)')
    throw new Error(`narrowDeeper.css is no longer one @media (max-width: 239px) block: ${top.map((n) => n.toString().slice(0, 60)).join(' | ')}`)
  const selectors: string[] = []
  const plainRule = (rule: Rule) => {
    for (const child of rule.nodes) if (child.type !== 'decl' && child.type !== 'comment') throw new Error(`narrowDeeper.css nests inside "${rule.selector}": ${child.toString().slice(0, 60)}`)
    if (rule.selector.includes('&')) throw new Error(`narrowDeeper.css uses nesting: ${rule.selector}`)
    // Pseudo-elements, in both spellings (`::after`, and the old single-colon `:after`), which `matches()` never matches.
    for (const s of rule.selectors) selectors.push(s.replace(/::[a-z-]+(\([^)]*\))?|:(before|after|first-line|first-letter)\b/g, '').trim())
  }
  const walk = (nodes: ChildNode[], inForcedColors: boolean) => {
    for (const node of nodes) {
      if (node.type === 'comment') continue
      if (node.type === 'rule') plainRule(node)
      else if (node.type === 'atrule' && !inForcedColors && node.name === 'media' && node.params === '(forced-colors: active)') walk(node.nodes ?? [], true)
      else throw new Error(`narrowDeeper.css has something other than a plain rule inside its block: ${node.toString().slice(0, 60)}`)
    }
  }
  walk(block.nodes ?? [], false)
  if (selectors.length === 0 || selectors.some((s) => s === '')) throw new Error(`narrowDeeper.css parsed to no usable selectors: ${JSON.stringify(selectors)}`)
  return [...new Set(selectors)]
}

const SELECTORS = narrowSelectors()

/** Which of `selectors` match `scope`'s dialog (its scrim and everything in it) or ⋮ menu, or anything round it. */
const matchesIn = (scope: Locator, selectors: string[]) =>
  scope.evaluate((el, selectors) => {
    const root = el.closest('.gr-dialog-scrim') ?? el.closest('.gr-menu')
    if (!root) throw new Error('neither a dialog nor a menu')
    const inside = [root, ...root.querySelectorAll('*')]
    const around: Element[] = []
    for (let n = root.parentElement; n != null; n = n.parentElement) around.push(n)
    const name = (n: Element) => `${n.tagName.toLowerCase()}.${(n.getAttribute('class') ?? '').trim().replace(/\s+/g, '.')}`
    const found: string[] = []
    for (const s of selectors) {
      for (const n of inside) if (n.matches(s)) found.push(`${s} → ${name(n)}`)
      for (const n of around) if (n.matches(s)) found.push(`${s} → round it: ${name(n)}`)
    }
    return { narrow: matchMedia('(max-width: 239px)').matches, inside: inside.length, around: around.length, found }
  }, selectors)

/** `scope` (an open dialog, or an open ⋮ menu) is reached by no rule of narrowDeeper.css. */
async function untouched(scope: Locator, what: string) {
  await expect(scope).toBeVisible()
  const r = await matchesIn(scope, SELECTORS)
  expect(r.narrow, `${what}: the narrow rules are in force`).toBe(true)
  // Something real was looked at: a panel with its content, and the page round it up to <html>.
  expect(r.inside, `${what}: elements checked inside`).toBeGreaterThan(4)
  expect(r.around, `${what}: elements checked round it`).toBeGreaterThan(1)
  expect(r.found, `${what}: no rule of narrowDeeper.css reaches it`).toEqual([])
}

const dialog = (page: Page, name: string | RegExp) => page.getByRole('dialog', { name })

/** The RPE sheet, by its title. */
const rpeSheet = (page: Page) => dialog(page, GritDomain.getInstance().rpeGuide().title)

/** Cable chest press (catalogue) planned on Wednesdays, on the powerlifting side: its ⋮ menu offers plates and skip. */
async function seedCable(page: Page) {
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
    const request = tx.objectStore('exercises').add({ name: 'Cable chest press', category: 'ACCESSORY', parentLift: null, muscles: ['PECS'], isBuiltIn: true, archived: false })
    request.onsuccess = () => tx.objectStore('planItems').add({ weekday: 3, exerciseId: request.result, sets: 3, reps: 10, targetRpe: 8, order: 0, mode: 'POWERLIFTING' })
    await settled
    db.close()
  })
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
}

test.describe('the dialogs 1.17.3 left as they were, at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })
  test.describe.configure({ timeout: 90_000 })

  test('narrowDeeper.css parses into its selectors', () => {
    expect(SELECTORS.length, 'selectors read from narrowDeeper.css').toBeGreaterThan(0)
    // The ones the checks below lean on are among them.
    expect(SELECTORS).toContain('.plan-best-dialog-scrim')
    expect(SELECTORS).toContain(':is(.gr-dialog.lib-picker, .shell-main .pb-builder) .lib-row')
  })

  test('the check is not empty: the rules do reach Set best, a dialog they lay out', async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, powerliftingStory())
    await page.goto('/#/plan')
    await page.getByRole('button', { name: 'Set best' }).first().click()
    const r = await matchesIn(dialog(page, /^Known best: /), SELECTORS)
    expect(r.narrow).toBe(true)
    expect(r.found).toContain('.plan-best-dialog-scrim → div.gr-dialog-scrim.plan-best-dialog-scrim')
    // And inside its panel: its buttons one over the other.
    expect(r.found).toContain('.gr-dialog:is(.lib-create-dialog, .plan-item-dialog, .plan-best-dialog, .pb-confirm-dialog) > .gr-dialog-actions → div.gr-dialog-actions')
  })

  test("Today: the ⋮ menu on the start card and the open card, the plate sheet and the skip sheet", async ({ page }) => {
    await openOnWednesday(page)
    await seedCable(page)
    const menu = page.getByRole('menu', { name: 'Options for Cable chest press' })
    await page.locator('.today-plan').getByRole('button', { name: 'Options for Cable chest press' }).click()
    await untouched(menu, 'the ⋮ menu on the start card')
    await page.keyboard.press('Escape')
    await expect(menu).toHaveCount(0)

    await tap(page, 'Start session')
    const head = page.locator('.today-card-head')
    await expect(head.getByRole('heading', { name: 'Cable chest press' })).toBeVisible()
    await head.getByRole('button', { name: 'Options for Cable chest press' }).click()
    await untouched(menu, 'the ⋮ menu on the open card')
    await menu.getByRole('menuitem', { name: 'Count plates on this machine' }).click()
    const plates = dialog(page, 'Count plates on Cable chest press')
    await expect(plates.getByRole('radiogroup', { name: 'How does this machine show its weight?' })).toBeVisible()
    await untouched(plates, 'the plate sheet')
    await page.keyboard.press('Escape')
    await expect(plates).toHaveCount(0)

    await head.getByRole('button', { name: 'Options for Cable chest press' }).click()
    await menu.getByRole('menuitem', { name: 'Skip today' }).click()
    await untouched(dialog(page, 'Why skip Cable chest press?'), 'the skip sheet')
  })

  test('Today: the RPE sheet and the typo check', async ({ page }) => {
    await openOnWednesday(page)
    await seedFlatBench(page)
    await tap(page, 'Start session')
    await page.getByRole('button', { name: 'What RPE means' }).first().click()
    const rpe = rpeSheet(page)
    await untouched(rpe, 'the RPE sheet from Today')
    await page.keyboard.press('Escape')
    await expect(rpe).toHaveCount(0)

    // A big jump from the last 100 kg asks first.
    await weightField(page).fill('250')
    await weightField(page).blur()
    await tap(page, 'Log set 1')
    await untouched(dialog(page, 'Did you mean 250 kg × 5?'), 'the typo check')
  })

  test("Plan: the RPE sheet from Plan's edit form", async ({ page }) => {
    await openOnWednesday(page)
    await seedFlatBench(page)
    await page.goto('/#/plan')
    await page.getByRole('region', { name: 'Wednesday' }).getByRole('button', { name: 'More' }).click()
    await page.getByRole('menuitem', { name: 'Edit' }).click()
    const form = dialog(page, 'Edit Wednesday')
    await form.getByRole('button', { name: 'What RPE means' }).click()
    const rpe = rpeSheet(page)
    // Over Plan's form, which the rules do lay out: the sheet is drawn at the end of the page, not inside the form.
    await untouched(rpe, "the RPE sheet from Plan's form")
  })

  test("Templates: the swap sheet, Plan's ⋮ menu and \"Replace your plan?\"", async ({ page }) => {
    await openOnWednesday(page, '#/plan/templates/powerlifting')
    await page.getByRole('button', { name: '5-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Swap Lat pulldown' }).first().click()
    const sheet = dialog(page, 'Swap Lat pulldown')
    await expect(sheet.getByRole('listitem').first()).toBeVisible()
    await untouched(sheet, "the templates' swap sheet")
    await sheet.getByRole('button', { name: 'Close', exact: true }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await expect(page.getByText('Plan set: 5-day powerlifting.')).toBeVisible()

    await page.getByRole('button', { name: 'Plan options' }).click()
    const menu = page.getByRole('menu', { name: 'Plan options' })
    await untouched(menu, "Plan's ⋮ menu")
    await menu.getByRole('menuitem', { name: 'Choose a template' }).click()
    await page.getByRole('button', { name: '3-day powerlifting' }).click()
    await page.getByRole('button', { name: 'Use this plan' }).click()
    await untouched(dialog(page, 'Replace your powerlifting plan?'), '"Replace your plan?"')
  })

  test("the plan builder's swap sheet", async ({ page }) => {
    await openOnWednesday(page)
    await seedStory(page, bodybuildingStory())
    await page.goto('/#/plan/build')
    for (let i = 1; i < 4; i++) await page.getByRole('button', { name: /^(Next|Choose for me|Skip)$/ }).click()
    await expect(page.getByRole('heading', { name: 'Your draft week' })).toBeVisible()
    const swap = page.getByRole('button', { name: /^Swap / }).first()
    const name = (await swap.getAttribute('aria-label'))!
    await swap.click()
    await untouched(dialog(page, name), "the builder's swap sheet")
  })

  test("the Exercises tab: Add to plan's day chooser", async ({ page }) => {
    await openOnWednesday(page, '#/exercises')
    await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
    await dialog(page, 'Form guide for Squat').getByRole('button', { name: 'Add to plan' }).click()
    const days = dialog(page, 'Add to plan')
    await expect(days.getByText('Which day?')).toBeVisible()
    await untouched(days, 'the day chooser')
  })

  test('Settings: Replace all data', async ({ page }) => {
    await openOnWednesday(page, '#/settings')
    await page.getByLabel('Backup file').setInputFiles(resolve(process.cwd(), 'src/backup/__fixtures__/web-v1.json'))
    await untouched(dialog(page, 'Replace all data?'), 'Replace all data')
  })
})

// 1.17.3: at this width the form guide's and the Muscles sheet's head (the name and Close) scroll away with the sheet,
// so Close is out of view once the sheet is scrolled (the owner's call). Escape and a tap on the strip of page above the
// sheet still close it, at the top and scrolled, and focus goes back to what opened it, as at every other width.

const SHEETS = [
  {
    what: 'the form guide',
    open: async (page: Page) => {
      await page.getByRole('button', { name: 'Open Squat', exact: true }).click()
      return dialog(page, 'Form guide for Squat')
    },
    opener: (page: Page) => page.getByRole('button', { name: 'Open Squat', exact: true }),
  },
  {
    what: 'the Muscles sheet',
    open: async (page: Page) => {
      await page.locator('.lib-dropdown').click()
      return dialog(page, 'Muscles')
    },
    opener: (page: Page) => page.locator('.lib-dropdown'),
  },
] as const

/** Where the sheet's head and its top edge are, and the point halfway down the strip above it, with what is drawn there. */
const sheetPlace = (sheet: Locator) =>
  sheet.evaluate((el) => {
    const panel = el.getBoundingClientRect()
    const head = el.querySelector('.lib-guide-head')!.getBoundingClientRect()
    const x = panel.left + panel.width / 2
    const y = panel.top / 2
    const there = document.elementFromPoint(x, y)
    return { top: panel.top, headBottom: head.bottom, x, y, onScrim: there != null && there === el.closest('.gr-dialog-scrim') }
  })

test.describe('the form guide and the Muscles sheet close without their head, at 320 px and 200 % text', () => {
  test.use({ viewport: { width: 160, height: 284 } })

  for (const sheetKind of SHEETS)
    for (const scrolled of [false, true])
      for (const how of ['Escape', 'a tap above the sheet'] as const)
        test(`${sheetKind.what}, ${scrolled ? 'scrolled until its head is out of view' : 'at its top'}: ${how} closes it, and focus goes back to its opener`, async ({ page }) => {
          await openOnWednesday(page, '#/exercises')
          const sheet = await sheetKind.open(page)
          await expect(sheet).toBeVisible()
          await sheet.evaluate((el) => Promise.all(el.getAnimations({ subtree: true }).map((a) => a.finished)))
          if (scrolled) {
            await sheet.evaluate((el) => el.scrollTo(0, el.querySelector('.lib-guide-head')!.getBoundingClientRect().height + 40))
            await expect.poll(async () => (await sheetPlace(sheet)).headBottom).toBeLessThanOrEqual((await sheetPlace(sheet)).top)
            await expect(sheet.getByRole('button', { name: /^Close/ })).not.toBeInViewport()
          }
          if (how === 'Escape') await page.keyboard.press('Escape')
          else {
            const at = await sheetPlace(sheet)
            expect(at.top, 'there is a strip above the sheet').toBeGreaterThan(0)
            expect(at.onScrim, 'the tap lands on the page behind the sheet').toBe(true)
            await page.touchscreen.tap(at.x, at.y)
          }
          await expect(sheet).toHaveCount(0)
          await expect(sheetKind.opener(page)).toBeFocused()
        })
})
