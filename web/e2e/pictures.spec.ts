import { expect, test, type Locator, type Page } from '@playwright/test'
import { GritDomain } from '../src/domain/generated/powertracker-domain.mjs'
import { AREAS } from '../src/screens/library/labels'
import { openOnWednesday, tabBar } from './helpers'

// The exercise pictures on the narrowest iPhone, where they are really laid out and really coloured: the two-pose
// picture in the guide sheet and the thumbnails on the Exercises tab. The same names as src/screens/library/
// ExercisePicture.test.tsx and Android's ExercisePictureTest. "Bench" is the exercise with a figure.

const grit = GritDomain.getInstance()
const bench = grit.figureInfo('Bench')!
const LABEL = `Picture: ${bench.startCaption}, then ${bench.endCaption}.`

/** How long the Exercises tab may take to settle after it opens, after a keystroke and after a filter change. */
const SETTLE_MS = 100
/**
 * How many rows the tab draws at once (FIRST_ROWS in src/screens/library/ExerciseBrowser.tsx). The rest follow
 * straight after, in one draw, so at any moment the list is either its first rows or all of it.
 */
const FIRST_ROWS = 24

test.use({ viewport: { width: 375, height: 812 } })

const guide = (page: Page, name: string) => page.getByRole('dialog', { name: `Form guide for ${name}` })
const row = (page: Page, name: string) => page.locator('.shell-main').getByRole('listitem', { name, exact: true })

async function openTab(page: Page) {
  await openOnWednesday(page, '#/exercises')
  await expect(row(page, 'Bench')).toBeVisible()
}

async function openGuide(page: Page, name = 'Bench') {
  await openTab(page)
  await page.getByRole('button', { name: `Open ${name}`, exact: true }).click()
  await expect(guide(page, name).getByRole('heading', { name: 'How to do it' })).toBeVisible()
  // The sheet has risen: boxes are read where they come to rest.
  await guide(page, name).evaluate((el) => Promise.all(el.getAnimations().map((a) => a.finished)))
}

/** Adds one of the lifter's own exercises straight in IndexedDB, then reloads. */
async function addOwnExercise(page: Page, ...names: string[]) {
  await page.evaluate(async (names) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction('exercises', 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    for (const name of names) tx.objectStore('exercises').add({ name, category: 'ACCESSORY', parentLift: null, muscles: ['Quads', 'Glutes'], isBuiltIn: false, archived: false })
    await settled
    db.close()
  }, names)
  await page.reload()
}

/** Things reaching past either edge of the phone, and text that does not fit its own box. The chip rows scroll sideways on purpose. */
const overflowing = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    if (document.documentElement.scrollWidth > window.innerWidth) found.push('page')
    for (const el of document.querySelectorAll<Element>('.shell-main *, .gr-dialog, .gr-dialog *')) {
      if (el.closest('.sr-only') || (el.closest('.lib-chips') && !el.classList.contains('lib-chips'))) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`${el.tagName.toLowerCase()}.${el.getAttribute('class')}`)
      if (el.matches('li, p, h1, h2, h3, .lib-row-name, .lib-row-muscles, .lib-picture-caption') && el.scrollWidth > el.clientWidth + 1) found.push(`clipped ${el.tagName.toLowerCase()}.${el.getAttribute('class')}: ${el.textContent}`)
    }
    return found
  })

type Box = { left: number; right: number; top: number; bottom: number; width: number; height: number }
const box = (locator: Locator) =>
  locator.evaluate((el): Box => {
    const { left, right, top, bottom, width, height } = el.getBoundingClientRect()
    return { left, right, top, bottom, width, height }
  })
const boxes = (locator: Locator) =>
  locator.evaluateAll((els) =>
    els.map((el): Box => {
      const { left, right, top, bottom, width, height } = el.getBoundingClientRect()
      return { left, right, top, bottom, width, height }
    }),
  )

/** A caption as it is laid out: its lines, and whether anything cuts it. */
const captions = (sheet: Locator) =>
  sheet.locator('.lib-picture-caption').evaluateAll((els) =>
    els.map((el) => {
      const text = document.createRange()
      text.selectNodeContents(el)
      const style = getComputedStyle(el)
      const { left, right, top, bottom } = el.getBoundingClientRect()
      return {
        text: el.textContent,
        lines: new Set(Array.from(text.getClientRects(), (r) => Math.round(r.top))).size,
        cut: el.scrollWidth > el.clientWidth + 1 || el.scrollHeight > el.clientHeight + 1 || style.textOverflow === 'ellipsis' || style.whiteSpace === 'nowrap',
        left,
        right,
        top,
        bottom,
      }
    }),
  )

/** The colour a role is painted in right now, and the surface behind the picture. */
const colours = (root: Locator) =>
  root.evaluate((el) => {
    const paint = (role: string) => {
      const shape = el.querySelector(`.fig-${role}`)
      if (shape == null) return null
      const style = getComputedStyle(shape)
      return shape.tagName === 'line' ? style.stroke : style.fill
    }
    let behind: Element | null = el
    while (behind != null && getComputedStyle(behind).backgroundColor === 'rgba(0, 0, 0, 0)') behind = behind.parentElement
    return {
      body: paint('body'),
      bodyFar: paint('body-far'),
      equipment: paint('equipment'),
      accent: paint('accent'),
      gap: paint('gap'),
      plateOpacity: getComputedStyle(el.querySelector('.fig-accent-fill')!).fillOpacity,
      surface: behind == null ? null : getComputedStyle(behind).backgroundColor,
    }
  })

/** Design tokens as the page resolves them now. */
const tokens = (page: Page) =>
  page.evaluate(() => {
    const probe = document.createElement('span')
    document.body.append(probe)
    const resolved: Record<string, string> = {}
    for (const token of ['--text', '--text-muted', '--text-secondary', '--accent', '--surface', '--container-high']) {
      probe.style.color = `var(${token})`
      resolved[token] = getComputedStyle(probe).color
    }
    probe.remove()
    return resolved
  })

function contrast(a: string, b: string): number {
  const luminance = (colour: string) => {
    // `rgb(243, 244, 239)`, or `color(srgb 0.5 0.5 0.5)` for a mixed colour.
    const scale = colour.startsWith('color(') ? 1 : 255
    const [r, g, b] = colour.match(/\d*\.?\d+/g)!.slice(0, 3).map((c) => Number(c) / scale).map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)) as [number, number, number]
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  }
  return (Math.max(luminance(a), luminance(b)) + 0.05) / (Math.min(luminance(a), luminance(b)) + 0.05)
}

test('guideSheet_showsTwoPosePicture_withCaptions', async ({ page }) => {
  await openGuide(page)
  const sheet = guide(page, 'Bench')
  const picture = sheet.locator('.lib-picture')
  await expect(picture).toBeVisible()

  // Two panels side by side: level, the same size, 4:3, start on the left.
  const [start, end] = (await boxes(picture.locator('svg.fig'))) as [Box, Box]
  expect(await picture.locator('svg.fig').count()).toBe(2)
  expect(start.top).toBeCloseTo(end.top, 0)
  expect(start.width).toBeCloseTo(end.width, 0)
  expect(start.right).toBeLessThanOrEqual(end.left)
  for (const panel of [start, end]) {
    expect(panel.width).toBeGreaterThan(140)
    expect(panel.width / panel.height).toBeCloseTo(4 / 3, 1)
  }
  // Something is drawn in each, across most of it.
  for (const svg of await picture.locator('svg.fig').all()) {
    const drawn = await svg.evaluate((el) => {
      const all = Array.from(el.children, (c) => c.getBoundingClientRect())
      return { count: all.length, width: Math.max(...all.map((r) => r.right)) - Math.min(...all.map((r) => r.left)), box: el.getBoundingClientRect().width }
    })
    expect(drawn.count).toBeGreaterThan(10)
    expect(drawn.width).toBeGreaterThan(drawn.box * 0.6)
  }

  // Each caption sits under its own panel, whole and in the muted text colour.
  const [first, second] = (await captions(sheet)) as [Awaited<ReturnType<typeof captions>>[number], Awaited<ReturnType<typeof captions>>[number]]
  expect([first.text, second.text]).toEqual([bench.startCaption, bench.endCaption])
  for (const [caption, panel] of [[first, start], [second, end]] as const) {
    expect(caption.top).toBeGreaterThanOrEqual(panel.bottom)
    expect(caption.left).toBeGreaterThanOrEqual(panel.left - 0.5)
    expect(caption.right).toBeLessThanOrEqual(panel.right + 0.5)
    expect(caption.cut).toBe(false)
  }
  expect(await sheet.locator('.lib-picture-caption').first().evaluate((el) => getComputedStyle(el).color)).toBe((await tokens(page))['--text-secondary'])

  // Under the name and the area and equipment line, above the purpose.
  const where = await box(sheet.getByText('Main lifts · Barbell'))
  const purpose = await box(sheet.getByText(grit.findInLibrary('Bench')!.guide.purpose))
  expect(start.top).toBeGreaterThanOrEqual(where.bottom)
  expect(Math.max(first.bottom, second.bottom)).toBeLessThanOrEqual(purpose.top)
})

test('guideSheet_pictureLabel_readsBothCaptions', async ({ page }) => {
  await openGuide(page)
  // One image, named by both captions; nothing else in the sheet is an image.
  await expect(guide(page, 'Bench').getByRole('img')).toHaveCount(1)
  await expect(guide(page, 'Bench').getByRole('img', { name: LABEL, exact: true })).toBeVisible()
  // The whole picture is that one line to a screen reader: no caption is read a second time.
  const spoken = (await guide(page, 'Bench').locator('.lib-picture').ariaSnapshot()).split('\n')
  expect(spoken).toHaveLength(1)
  expect(spoken[0]).toContain(`img "${LABEL}"`)
})

test('guideSheet_noFigure_showsNoPictureArea', async ({ page }) => {
  // The lifter's own exercise: its one line, and no room kept for a picture.
  await openOnWednesday(page, '#/exercises')
  await addOwnExercise(page, 'Tyre flip')
  await page.getByRole('button', { name: 'Open Tyre flip', exact: true }).click()
  const sheet = guide(page, 'Tyre flip')
  await expect(sheet.getByText('No form guide for your own exercises.')).toBeVisible()
  await expect(sheet.locator('.lib-picture')).toHaveCount(0)
  await expect(sheet.locator('svg.fig')).toHaveCount(0)
  await expect(sheet.getByRole('img')).toHaveCount(0)
})

test('exercisesTab_rowsShowThumbnail', async ({ page }) => {
  await openTab(page)
  // The first screenful comes first and the rest straight after: every row, before they are counted.
  await expect.poll(() => page.locator('.shell-main .lib-row').count()).toBeGreaterThanOrEqual(grit.library().length)
  const names = await page.locator('.shell-main .lib-row').count()
  expect(names).toBeGreaterThanOrEqual(grit.library().length)
  await expect(page.locator('.shell-main .lib-thumb')).toHaveCount(names)

  // About 56 × 40, on the left of the name, with the row still a full-size target.
  const benchRow = row(page, 'Bench')
  const thumb = await box(benchRow.locator('.lib-thumb'))
  expect(thumb.width).toBeCloseTo(56, 0)
  expect(thumb.height).toBeCloseTo(40, 0)
  const name = await box(benchRow.locator('.lib-row-name'))
  expect(thumb.right).toBeLessThanOrEqual(name.left)
  expect(name.left - thumb.right).toBeCloseTo(12, 0)
  expect((await box(benchRow.getByRole('button', { name: 'Open Bench' }))).height).toBeGreaterThanOrEqual(48)
  expect((await box(benchRow.getByRole('button', { name: 'Form guide for Bench' }))).height).toBeGreaterThanOrEqual(48)

  // Bench's tile holds its chosen pose, whole, 4:3 and centred.
  const figure = benchRow.locator('.lib-thumb svg.fig')
  await expect(figure).toHaveCount(1)
  const drawn = await box(figure)
  expect(drawn.height).toBeCloseTo(40, 0)
  expect(drawn.width / drawn.height).toBeCloseTo(4 / 3, 1)
  expect(drawn.left - thumb.left).toBeCloseTo(thumb.right - drawn.right, 0)
  expect(await figure.evaluate((el) => el.childElementCount)).toBe(grit.figureShapes('Bench', bench.thumbnail)!.length)
  // Hidden from screen readers: the row is its two buttons and nothing more.
  await expect(benchRow.getByRole('img')).toHaveCount(0)
  await expect(benchRow.locator('.lib-thumb')).toHaveAttribute('aria-hidden', 'true')
  // The tile is the muted surface with small corners, and the figure's gaps are that tile's colour.
  const tile = await benchRow.locator('.lib-thumb').evaluate((el) => ({ background: getComputedStyle(el).backgroundColor, radius: getComputedStyle(el).borderTopLeftRadius }))
  expect(tile).toEqual({ background: (await tokens(page))['--container-high'], radius: '6px' })
  expect((await colours(benchRow.locator('.lib-thumb'))).gap).toBe(tile.background)

  // A search keeps the thumbnails.
  await page.getByRole('textbox', { name: 'Search exercises' }).fill('bench')
  await expect(row(page, 'Bench').locator('.lib-thumb svg.fig')).toHaveCount(1)
  expect(await overflowing(page)).toEqual([])
})

test('exercisesTab_thumbnailFarOffScreen_isDrawnWhenItsRowComesNear', async ({ page }) => {
  // Forty of the lifter's own exercises lead the list, so Bench starts far below it: more than the 600 px a
  // thumbnail is drawn ahead by.
  await openOnWednesday(page, '#/exercises')
  await addOwnExercise(page, ...Array.from({ length: 40 }, (_, i) => `Own exercise ${String(i + 1).padStart(2, '0')}`))
  const bench = row(page, 'Bench')
  // The catalogue is listed before the lifter's own exercises have been read, with Bench near the top and drawn.
  // A search that leaves it out and is cleared again lists it afresh, where it now belongs.
  await expect(row(page, 'Own exercise 40')).toBeAttached()
  await page.getByRole('textbox', { name: 'Search exercises' }).fill('own exercise')
  await expect(bench).toHaveCount(0)
  await page.getByRole('textbox', { name: 'Search exercises' }).fill('')
  await expect(bench).toBeAttached()
  const list = page.locator('.shell-main .lib-scroll')
  /** How far below the list's bottom edge Bench's row starts, and whether its thumbnail is drawn. */
  const state = () =>
    list.evaluate(async (scroller) => {
      await new Promise<void>((done) => requestAnimationFrame(() => requestAnimationFrame(() => done())))
      const row = scroller.querySelector('.lib-row[aria-label="Bench"]')!
      return { below: Math.round(row.getBoundingClientRect().top - scroller.getBoundingClientRect().bottom), drawn: row.querySelector('.lib-thumb svg.fig') != null }
    })
  const far = await state()
  expect(far.below).toBeGreaterThan(1200)
  expect(far.drawn).toBe(false)

  /** Scrolls until Bench's row starts `px` below the list. Rows take their real height as they come near, so it settles in steps. */
  const bringTo = async (px: number) => {
    for (let step = 0; step < 6; step++) {
      const { below } = await state()
      if (Math.abs(below - px) <= 2) break
      await list.evaluate((scroller, by) => scroller.scrollBy(0, by), below - px)
    }
    return state()
  }

  // Still 900 px below the list: too far, nothing drawn yet.
  const coming = await bringTo(900)
  expect(coming.below).toBeCloseTo(900, -1)
  expect(coming.drawn).toBe(false)

  // 400 px below the list, still out of sight: the thumbnail is already drawn, before the row scrolls in.
  const near = await bringTo(400)
  expect(near.below).toBeCloseTo(400, -1)
  expect(near.drawn).toBe(true)
  await expect(bench).not.toBeInViewport()

  // In sight it is the drawing made ahead, and it stays when the row is scrolled far away again.
  await bench.scrollIntoViewIfNeeded()
  await expect(bench.locator('.lib-thumb svg.fig')).toBeVisible()
  await list.evaluate((scroller) => scroller.scrollTo(0, 0))
  expect((await state()).drawn).toBe(true)
})

test('exercisesTab_customExercise_showsEmptyTile', async ({ page }) => {
  await openOnWednesday(page, '#/exercises')
  await addOwnExercise(page, 'Tyre flip')
  const own = row(page, 'Tyre flip')
  await expect(own).toBeVisible()
  // An empty tile of the same size in the same place, so the names line up.
  await expect(own.locator('.lib-thumb')).toHaveCount(1)
  await expect(own.locator('.lib-thumb > *')).toHaveCount(0)
  const empty = await box(own.locator('.lib-thumb'))
  const drawn = await box(row(page, 'Bench').locator('.lib-thumb'))
  expect([empty.width, empty.height, empty.left]).toEqual([drawn.width, drawn.height, drawn.left])
  expect((await box(own.locator('.lib-row-name'))).left).toBe((await box(row(page, 'Bench').locator('.lib-row-name'))).left)
  expect(await own.locator('.lib-thumb').evaluate((el) => getComputedStyle(el).backgroundColor)).toBe((await tokens(page))['--container-high'])
})

/**
 * The catalogue names on the tab that break inside a word: a word whose letters are laid out on more than one line.
 * A hyphen is a place a line may end, so "Safety-bar" over two lines at its hyphen is two whole words.
 * Every row is laid out for this, rows off screen too (they skip layout through `content-visibility` otherwise).
 */
const namesBrokenInsideAWord = (page: Page) =>
  page.locator('.shell-main .lib-scroll').evaluate((list) => {
    const laidOut = document.createElement('style')
    laidOut.textContent = '.lib-row { content-visibility: visible !important; }'
    document.head.append(laidOut)
    const broken: string[] = []
    for (const name of list.querySelectorAll('.lib-row-name')) {
      const text = name.firstChild
      if (text == null || text.nodeType !== Node.TEXT_NODE) continue
      for (const word of text.textContent!.matchAll(/[^\s-]+/g)) {
        const range = document.createRange()
        range.setStart(text, word.index)
        range.setEnd(text, word.index + word[0].length)
        if (new Set(Array.from(range.getClientRects(), (r) => Math.round(r.top))).size > 1) {
          broken.push(name.textContent!)
          break
        }
      }
    }
    laidOut.remove()
    return broken
  })

test('exercisesTab_namesWholeAt288', async ({ page }) => {
  // A 375 px phone at 130% zoom: 288 CSS px of room. Under 320 a row has no tile at all, so every name stays whole.
  await page.setViewportSize({ width: 288, height: 625 })
  await openTab(page)
  await expect.poll(() => page.locator('.shell-main .lib-row').count()).toBeGreaterThanOrEqual(grit.library().length)
  expect(await namesBrokenInsideAWord(page)).toEqual([])
  for (const tile of await boxes(page.locator('.shell-main .lib-thumb'))) expect([tile.width, tile.height]).toEqual([0, 0])
  // The name starts where the tile would have been, and nothing reaches past the phone.
  const benchRow = await box(row(page, 'Bench').getByRole('button', { name: 'Open Bench' }))
  expect((await box(row(page, 'Bench').locator('.lib-row-name'))).left - benchRow.left).toBeLessThan(56)
  expect(await overflowing(page)).toEqual([])

  // From 320 px the tiles are back, and the names are still whole.
  await page.setViewportSize({ width: 320, height: 693 })
  await expect.poll(async () => (await box(row(page, 'Bench').locator('.lib-thumb'))).width).toBeCloseTo(56, 0)
  await expect(row(page, 'Bench').locator('.lib-thumb svg.fig')).toHaveCount(1)
  expect(await namesBrokenInsideAWord(page)).toEqual([])
})

test('picker_hasNoThumbnails', async ({ page }) => {
  await openOnWednesday(page, '#/plan')
  await page.getByRole('region', { name: 'Monday' }).getByRole('button', { name: 'Add' }).click()
  const picker = page.getByRole('dialog', { name: /^Add to / })
  await expect(picker.getByRole('listitem', { name: 'Bench', exact: true })).toBeVisible()
  // Every row drawn, so none of them has one.
  await expect.poll(() => picker.locator('.lib-row').count()).toBeGreaterThanOrEqual(grit.library().length)
  await expect(page.locator('.lib-thumb')).toHaveCount(0)
  await expect(page.locator('svg.fig')).toHaveCount(0)
})

test('picture_noOverflow_atPhoneWidth', async ({ page }) => {
  // The tab with its thumbnails, then the sheet with its picture: nothing past either edge of 375 px, nothing clipped.
  await openGuide(page)
  expect(await page.evaluate(() => window.innerWidth)).toBe(375)
  expect(await overflowing(page)).toEqual([])
  const sheet = await box(guide(page, 'Bench'))
  const picture = await box(guide(page, 'Bench').locator('.lib-picture'))
  expect(picture.left).toBeGreaterThanOrEqual(sheet.left)
  expect(picture.right).toBeLessThanOrEqual(sheet.right)
  // The captions are whole, each on one line at this width.
  for (const caption of await captions(guide(page, 'Bench'))) {
    expect(caption.cut, caption.text!).toBe(false)
    expect(caption.lines, caption.text!).toBe(1)
  }
  await page.keyboard.press('Escape')
  await expect(guide(page, 'Bench')).toHaveCount(0)
  expect(await overflowing(page)).toEqual([])
})

test('picture_contrast_darkAndLight', async ({ page }) => {
  await openGuide(page)
  for (const scheme of ['dark', 'light'] as const) {
    await page.emulateMedia({ colorScheme: scheme })
    const token = await tokens(page)
    // On the sheet, and on the thumbnail's tile behind it.
    for (const [where, surface] of [[guide(page, 'Bench').locator('.lib-picture'), '--surface'], [row(page, 'Bench').locator('.lib-thumb'), '--container-high']] as const) {
      const shown = await colours(where)
      expect(shown.surface, `${surface}, ${scheme}`).toBe(token[surface])
      expect(shown.gap).toBe(token[surface])
      expect([shown.body, shown.equipment, shown.accent]).toEqual([token['--text'], token['--text-muted'], token['--accent']])
      expect(shown.plateOpacity).toBe('0.1')
      for (const role of ['body', 'equipment', 'accent'] as const) expect(contrast(shown[role]!, shown.surface!), `${role} on ${surface}, ${scheme}`).toBeGreaterThanOrEqual(3)
      // The far limbs are the body's colour mixed toward the surface: between the two, and still told from both.
      expect(shown.bodyFar).not.toBe(shown.body)
      expect(contrast(shown.bodyFar!, shown.surface!)).toBeGreaterThan(1.5)
      expect(contrast(shown.bodyFar!, shown.surface!)).toBeLessThan(contrast(shown.body!, shown.surface!))
    }
  }
})

test('picture_largeText_captionsWrap', async ({ page }) => {
  // What 200% text does to a 375 × 812 phone: everything is laid out in half the width and height.
  await page.setViewportSize({ width: Math.floor(375 / 2), height: Math.floor(812 / 2) })
  await openGuide(page)
  const sheet = guide(page, 'Bench')
  const shown = await captions(sheet)
  expect(shown.map((c) => c.text)).toEqual([bench.startCaption, bench.endCaption])
  // Too long for half a narrow sheet: they wrap, and every line is there.
  for (const caption of shown) {
    expect(caption.lines, caption.text!).toBeGreaterThanOrEqual(2)
    expect(caption.cut, caption.text!).toBe(false)
  }
  // The panels are still two, side by side, 4:3 and inside the sheet.
  const within = await box(sheet)
  const [start, end] = (await boxes(sheet.locator('.lib-picture svg.fig'))) as [Box, Box]
  expect(start.right).toBeLessThanOrEqual(end.left)
  for (const panel of [start, end]) {
    expect(panel.left).toBeGreaterThanOrEqual(within.left)
    expect(panel.right).toBeLessThanOrEqual(within.right)
    expect(panel.width / panel.height).toBeCloseTo(4 / 3, 1)
  }
  // The purpose starts under the taller caption.
  expect((await box(sheet.getByText(grit.findInLibrary('Bench')!.guide.purpose))).top).toBeGreaterThanOrEqual(Math.max(...shown.map((c) => c.bottom)))
  // Nothing of the picture reaches past the sheet. (The rest of the app is not made for a phone half this wide.)
  for (const part of await boxes(sheet.locator('.lib-picture, .lib-picture *'))) {
    expect(part.left).toBeGreaterThanOrEqual(within.left - 0.5)
    expect(part.right).toBeLessThanOrEqual(within.right + 0.5)
  }
})

test('picture_followsThemeChange', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'dark' })
  await openGuide(page)
  const picture = guide(page, 'Bench').locator('.lib-picture')
  const thumb = row(page, 'Bench').locator('.lib-thumb')
  const dark = { token: await tokens(page), picture: await colours(picture), thumb: await colours(thumb) }
  expect(dark.picture.body).toBe(dark.token['--text'])

  // The theme changes under the open sheet: the same picture and thumbnail, recoloured, without being opened again.
  const sameDrawing = await picture.locator('svg.fig').first().elementHandle()
  await page.emulateMedia({ colorScheme: 'light' })
  await expect(guide(page, 'Bench')).toBeVisible()
  const light = { token: await tokens(page), picture: await colours(picture), thumb: await colours(thumb) }
  expect(await sameDrawing!.evaluate((el) => el.isConnected)).toBe(true)
  expect(light.token['--text']).not.toBe(dark.token['--text'])
  for (const role of ['body', 'bodyFar', 'equipment', 'accent', 'gap', 'surface'] as const) {
    expect(light.picture[role], role).not.toBe(dark.picture[role])
    expect(light.thumb[role], role).not.toBe(dark.thumb[role])
  }
  expect([light.picture.body, light.picture.equipment, light.picture.accent, light.picture.gap]).toEqual([light.token['--text'], light.token['--text-muted'], light.token['--accent'], light.token['--surface']])
  expect(light.thumb.gap).toBe(light.token['--container-high'])
})

// Timed after every other spec has finished, one after another (the "speed" project in playwright.config.ts).
test.describe('Exercises tab speed', { tag: '@speed' }, () => {
  test.describe.configure({ mode: 'serial' })

  /**
   * Does one thing to the Exercises tab inside the page and measures, on the page's own clock, how long it takes
   * until the result has been drawn: the change, its render, and two frames. Returns the time, the rows then on
   * screen and how many of them have a tile and a drawn thumbnail. What is timed is the first screenful
   * (FIRST_ROWS), with the thumbnails on it: the rest of the list may or may not have followed by then, and the
   * test waits for it after.
   */
  const settle = (page: Page, action: { open: true } | { type: string } | { chip: string }) =>
    page.evaluate(async (action) => {
      const frame = () => new Promise<void>((done) => requestAnimationFrame(() => done()))
      const started = performance.now()
      if ('open' in action) {
        document.querySelector<HTMLElement>('nav[aria-label="Main"] a[href="#/exercises"]')!.click()
      } else if ('type' in action) {
        const input = document.querySelector<HTMLInputElement>('.shell-main input[type="text"]')!
        // What a keystroke does: the field's value changes and it fires `input`.
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(input, action.type)
        input.dispatchEvent(new Event('input', { bubbles: true }))
      } else {
        const chip = Array.from(document.querySelectorAll<HTMLElement>('.shell-main .lib-chips .gr-chip')).find((c) => c.textContent === action.chip)!
        chip.querySelector('input')!.click()
      }
      await frame()
      await frame()
      const ms = performance.now() - started
      return {
        ms,
        rows: Array.from(document.querySelectorAll('.shell-main .lib-row'), (row) => row.getAttribute('aria-label')!),
        tiles: document.querySelectorAll('.shell-main .lib-row .lib-thumb').length,
        drawn: Array.from(document.querySelectorAll('.shell-main .lib-row:has(.lib-thumb svg.fig)'), (row) => row.getAttribute('aria-label')!),
      }
    }, action)

  const sorted = (names: readonly string[]) => [...names].sort()
  const library = (query = '', areas: string[] = [], equipment: string[] = []) => grit.searchLibrary(query, areas, equipment).map((e) => e.name)
  /**
   * The list as the tab shows it: under body areas while nothing is searched, ranked while something is. With one
   * area chosen, everything is under that area in the domain's order, the main lifts it also lists included.
   */
  const inOrderShown = (query = '', areas: string[] = [], equipment: string[] = []) => {
    const found = grit.searchLibrary(query, areas, equipment)
    if (query !== '' || areas.length === 1) return found.map((e) => e.name)
    return AREAS.flatMap((a) => found.filter((e) => e.area === a.id).map((e) => e.name))
  }
  const shownNames = (page: Page) => page.evaluate(() => Array.from(document.querySelectorAll('.shell-main .lib-row'), (row) => row.getAttribute('aria-label')!))
  /** The rows drawn at settle are the list's first screenful or more: the start of `expected`, in its order. */
  const expectFirstRows = (rows: string[], expected: readonly string[], what: string) => {
    expect(rows.length, `${what}: ${rows.length} rows`).toBeGreaterThanOrEqual(Math.min(FIRST_ROWS, expected.length))
    expect(rows, what).toEqual(expected.slice(0, rows.length))
  }

  test('the Exercises tab settles within 100 ms with its thumbnails when it opens, on every keystroke and on every filter change', async ({ page }) => {
    await openOnWednesday(page, '#/today')
    // The app has finished starting (its first screen is up, its files are cached for offline use), so what is
    // timed is the tab and not the launch going on behind it.
    await expect(page.getByRole('heading', { level: 1, name: 'Today' })).toBeVisible()
    await page.evaluate(() => navigator.serviceWorker.ready.then(() => undefined))
    await page.waitForLoadState('networkidle')
    const withFigure = (names: readonly string[]) => names.filter((name) => grit.figureInfo(name) != null)
    expect(withFigure(library())).toContain('Bench')

    // Opening it lists the whole catalogue, a tile on every row: its first screenful in the time, with the
    // thumbnails on screen drawn, and the rest straight after.
    const opened = await settle(page, { open: true })
    expectFirstRows(opened.rows, inOrderShown(), 'opening')
    expect(opened.tiles).toBe(opened.rows.length)
    expect(opened.drawn).toContain('Bench')
    expect(opened.ms, `opening took ${opened.ms} ms`).toBeLessThan(SETTLE_MS)
    await expect.poll(() => shownNames(page)).toEqual(inOrderShown())
    expect(sorted(await shownNames(page))).toEqual(sorted(library()))

    // Typing, one key at a time; then deleting it again.
    for (const typed of ['b', 'be', 'ben', 'benc', 'bench', 'benc', '', 'pull up', 'RDL', 'romanian', '']) {
      const result = await settle(page, { type: typed })
      const names = typed === '' ? sorted(library()) : library(typed)
      expectFirstRows(result.rows, inOrderShown(typed), `"${typed}"`)
      expect(result.tiles, `"${typed}"`).toBe(result.rows.length)
      // A search starts from the top, so every match with a figure is near enough to be drawn at once.
      if (typed !== '' && names.length < 8) expect(sorted(result.drawn), `"${typed}"`).toEqual(sorted(withFigure(names)))
      expect(result.ms, `"${typed}" took ${result.ms} ms`).toBeLessThan(SETTLE_MS)
      // The whole list before the next key, so its drawing is not timed with the next one.
      await expect.poll(() => shownNames(page), `"${typed}"`).toEqual(inOrderShown(typed))
      expect(typed === '' ? sorted(await shownNames(page)) : await shownNames(page), `"${typed}"`).toEqual(names)
    }

    // Filters: one body area at a time, any of the chosen equipment.
    const steps: [chip: string, areas: string[], equipment: string[]][] = [
      ['Main lifts', ['MAIN_LIFTS'], []],
      ['Barbell', ['MAIN_LIFTS'], ['BARBELL']],
      ['Chest', ['CHEST'], ['BARBELL']],
      ['Bodyweight', ['CHEST'], ['BARBELL', 'BODYWEIGHT']],
      ['All', [], ['BARBELL', 'BODYWEIGHT']],
      ['Bodyweight', [], ['BARBELL']],
      ['Barbell', [], []],
    ]
    for (const [chip, areas, equipment] of steps) {
      const result = await settle(page, { chip })
      expectFirstRows(result.rows, inOrderShown('', areas, equipment), chip)
      expect(result.tiles, chip).toBe(result.rows.length)
      if (areas.includes('MAIN_LIFTS')) expect(result.drawn, chip).toContain('Bench')
      expect(result.ms, `${chip} took ${result.ms} ms`).toBeLessThan(SETTLE_MS)
      await expect.poll(() => shownNames(page), chip).toEqual(inOrderShown('', areas, equipment))
      expect(sorted(await shownNames(page)), chip).toEqual(sorted(library('', areas, equipment)))
    }
  })
})
