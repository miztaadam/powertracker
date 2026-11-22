import { expect, type Locator, type Page } from '@playwright/test'
import { tabBar } from './helpers'

// 1.17.3: the checks for a 320 px phone at 200 % text (laid out at 160 × 284) on the screens one step deeper and in
// the dialogs and sheets, where `narrow.spec.ts`'s checks look at the app's screen only. One check, `wrongHere`, looks
// at whatever is on top: the open dialog or sheet (the last one opened) when there is one, else the app's screen, the
// tab bar and a message. Nothing is left out by name.

/** A long name that wraps at its spaces, and a 42-letter one-word name that can only break inside itself. */
export const LONG_NAME = 'Single-arm cable lateral raise'
export const ONE_WORD = 'Pneumonoultramicroscopicsilicovolcanoconio'

/**
 * Everything wrong at this size, on top:
 * - the page, or a box in it (other than a chip row) that scrolls sideways; the open dialog scrolling sideways;
 * - anything reaching past the screen's edges, or past the open dialog's;
 * - text that does not fit its box, spills out of it, or is squeezed to nothing;
 * - a button, link, field or chip overlapping another or text, or text overlapping text (more than 1 px each way; one
 *   inside the other does not count). A bar that stays put while the rest scrolls (a sticky or fixed box: a sheet's
 *   head, a bottom action bar), an open menu and a message are each a layer drawn over the rest, so each is held to
 *   this within itself; `clearOfBars` checks that nothing has to stay under such a bar. A stepper's caption action
 *   (the RPE info button), a 48 px target laid over its line by design, is not held to it;
 * - a control reaching past the card it sits in;
 * - a word broken across lines (a run of four or more characters with no space, hyphen or dash in it on two lines),
 *   unless it is wider than the whole of its card, list row, dialog or screen.
 */
export const wrongHere = (page: Page) =>
  page.evaluate(() => {
    const found: string[] = []
    const name = (el: Element) => `${el.tagName.toLowerCase()}.${el.getAttribute('class') ?? ''}`
    const short = (el: Element) => `"${(el.textContent ?? '').trim().slice(0, 24)}"`
    if (document.documentElement.scrollWidth > window.innerWidth) found.push(`page ${document.documentElement.scrollWidth} wide`)
    const dialogs = [...document.querySelectorAll<HTMLElement>('.gr-dialog')].filter((d) => d.closest('[inert]') == null)
    const dialog = dialogs.at(-1) ?? null
    const roots: HTMLElement[] = dialog ? [dialog] : [...document.querySelectorAll<HTMLElement>('.shell-main, .shell-rest, .gr-tabbar, .gr-snackbar-host')]
    const everything = roots.flatMap((r) => [r, ...r.querySelectorAll<HTMLElement>('*')])
    const hidden = (el: Element) => el.closest('.sr-only, .gr-tab-label, [hidden]') != null
    const shown = (el: Element) => {
      if (hidden(el)) return false
      const style = getComputedStyle(el)
      const box = el.getBoundingClientRect()
      return style.visibility !== 'hidden' && style.opacity !== '0' && box.width > 0 && box.height > 0
    }
    const dialogBox = dialog?.getBoundingClientRect()
    const fixedLayer = (el: Element) => {
      for (let n: Element | null = el; n != null && n !== dialog; n = n.parentElement) if (getComputedStyle(n).position === 'fixed') return true
      return false
    }
    const holdsText = (el: Element) => [...el.childNodes].some((n) => n.nodeType === Node.TEXT_NODE && n.textContent!.trim() !== '')
    for (const el of everything) {
      if (hidden(el) || getComputedStyle(el).display === 'none') continue
      const style = getComputedStyle(el)
      const scroller = ['auto', 'scroll'].includes(style.overflowX)
      if (scroller && !el.matches('.lib-chips') && el.scrollWidth > el.clientWidth + 1) found.push(`scrolls sideways ${name(el)}: ${el.scrollWidth} > ${el.clientWidth}`)
      if (el.parentElement?.closest('.lib-chips')) continue
      const box = el.getBoundingClientRect()
      if (box.width > 0 && (box.right > window.innerWidth + 0.5 || box.left < -0.5)) found.push(`outside the screen ${name(el)} ${short(el)}`)
      // Inside the dialog's edges, but for a box placed against the screen (a message over it).
      else if (dialogBox && el !== dialog && box.width > 0 && !fixedLayer(el) && (box.right > dialogBox.right + 0.5 || box.left < dialogBox.left - 0.5)) found.push(`outside the dialog ${name(el)} ${short(el)}`)
      if (holdsText(el) && (box.width < 1 || box.height < 1)) found.push(`squeezed ${name(el)}: ${el.textContent}`)
    }
    const all = everything.filter(shown)
    const texts = all.filter(holdsText)
    const controls = all.filter((el) => el.matches('button, a, input:not([type=checkbox], [type=radio]), label.gr-chip, label.gr-segment, label.lib-option, [role="menuitem"]'))
    for (const el of texts) {
      const style = getComputedStyle(el)
      if (style.display === 'inline' || ['auto', 'scroll'].includes(style.overflowX)) continue
      if (el.scrollWidth > el.clientWidth + 1) found.push(`spills ${name(el)}: ${el.textContent}`)
    }
    // The layer an element is drawn in: the nearest box round it that stays put while the rest scrolls, an open menu or
    // a message; else what is on top.
    const layer = (el: Element) => {
      for (let n: Element | null = el; n != null; n = n.parentElement) {
        if (roots.includes(n as HTMLElement)) return n
        if (n.matches('.gr-menu-list, .gr-snackbar-host')) return n
        if (['sticky', 'fixed'].includes(getComputedStyle(n).position)) return n
      }
      return null
    }
    const designedOver = (a: Element, b: Element) => a.closest('.gr-stepper-action, .lib-rpe-inline') != null || b.closest('.gr-stepper-action, .lib-rpe-inline') != null
    // What of a box is drawn: its box cut to every box round it that clips (a list scrolled under its header is not
    // drawn over the header).
    const drawn = (el: Element) => {
      const r = el.getBoundingClientRect()
      let { left, right, top, bottom } = r
      for (let n = el.parentElement; n != null; n = n.parentElement) {
        const s = getComputedStyle(n)
        if (s.position === 'fixed') break
        if (s.overflowX === 'visible' && s.overflowY === 'visible') continue
        const c = n.getBoundingClientRect()
        left = Math.max(left, c.left)
        right = Math.min(right, c.right)
        top = Math.max(top, c.top)
        bottom = Math.min(bottom, c.bottom)
      }
      return { left, right, top, bottom }
    }
    const overlaps = (a: Element, b: Element) => {
      const ra = drawn(a)
      const rb = drawn(b)
      const x = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left)
      const y = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top)
      return x > 1 && y > 1
    }
    for (const [i, a] of texts.entries())
      for (const b of texts.slice(i + 1)) {
        if (a.contains(b) || b.contains(a) || layer(a) !== layer(b) || designedOver(a, b)) continue
        if (overlaps(a, b)) found.push(`overlap ${name(a)} ${short(a)} × ${name(b)} ${short(b)}`)
      }
    for (const a of controls) {
      const ra = a.getBoundingClientRect()
      const card = a.parentElement?.closest('.gr-card, .gr-rest')?.getBoundingClientRect()
      if (card && (ra.left < card.left - 0.5 || ra.right > card.right + 0.5)) found.push(`outside its card ${name(a)} ${short(a)}`)
      for (const b of [...controls, ...texts]) {
        if (a === b || a.contains(b) || b.contains(a) || layer(a) !== layer(b) || designedOver(a, b)) continue
        if (overlaps(a, b)) found.push(`overlap ${name(a)} ${short(a)} × ${name(b)} ${short(b)}`)
      }
    }
    // A word wider than the whole of its card (or list row, dialog or screen) cannot stay on one line: it may break.
    const room = (el: Element) => {
      const box = el.closest('.gr-card, .lib-row, .gr-dialog, .shell-main, .shell-rest') as HTMLElement | null
      if (!box) return Infinity
      const style = getComputedStyle(box)
      return box.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight)
    }
    for (const el of texts)
      for (const node of el.childNodes) {
        if (node.nodeType !== Node.TEXT_NODE) continue
        for (const m of node.textContent!.matchAll(/[^\s\-‐–—]{4,}/g)) {
          const range = document.createRange()
          range.setStart(node, m.index)
          range.setEnd(node, m.index + m[0].length)
          const rects = [...range.getClientRects()].filter((r) => r.width > 0)
          const lines = new Set(rects.map((r) => Math.round(r.top)))
          const width = rects.reduce((sum, r) => sum + r.width, 0)
          if (lines.size > 1 && width <= room(el)) found.push(`broken word ${name(el)}: ${m[0]}`)
        }
      }
    return [...new Set(found)]
  })

/**
 * Nothing has to stay under a bar that stays put: in every box that scrolls up and down on top (the open dialog, a
 * list in it, or the app's screen), scrolled to its end no text or control is under a bar held at the bottom, and
 * scrolled to its top none is under a bar held at the top. Returns what is covered; leaves each box at its top.
 */
export const clearOfBars = (page: Page) =>
  page.evaluate(async () => {
    const found: string[] = []
    const dialogs = [...document.querySelectorAll<HTMLElement>('.gr-dialog')].filter((d) => d.closest('[inert]') == null)
    const root = dialogs.at(-1) ?? document.querySelector<HTMLElement>('.shell-main')!
    const scrollers = [root, ...root.querySelectorAll<HTMLElement>('*')].filter((el) => ['auto', 'scroll'].includes(getComputedStyle(el).overflowY) && el.scrollHeight > el.clientHeight + 1)
    const frame = () => new Promise((done) => requestAnimationFrame(() => requestAnimationFrame(done)))
    const stuck = (el: Element, within: Element) => {
      for (let n: Element | null = el; n != null && n !== within.parentElement; n = n.parentElement) if (getComputedStyle(n).position === 'sticky') return n
      return null
    }
    for (const scroller of scrollers) {
      const items = [...scroller.querySelectorAll<HTMLElement>('*')].filter(
        (el) =>
          el.closest('.sr-only, [hidden]') == null &&
          (el.matches('button, a, input:not([type=checkbox], [type=radio]), label') || [...el.childNodes].some((n) => n.nodeType === Node.TEXT_NODE && n.textContent!.trim() !== '')),
      )
      // A bar held in place as the box scrolls (sticky). A fixed box (a message) is drawn over the page for a moment, not
      // held in the scroll: it is not one of these.
      const bars = [...scroller.querySelectorAll<HTMLElement>('*')].filter((el) => getComputedStyle(el).position === 'sticky')
      for (const [end, where] of [
        [true, 'bottom'],
        [false, 'top'],
      ] as const) {
        scroller.scrollTop = end ? scroller.scrollHeight : 0
        await frame()
        for (const bar of bars) {
          const style = getComputedStyle(bar)
          if ((where === 'bottom' ? style.bottom : style.top) === 'auto') continue
          const rb = bar.getBoundingClientRect()
          for (const el of items) {
            if (bar.contains(el) || el.contains(bar) || stuck(el, scroller) != null) continue
            const r = el.getBoundingClientRect()
            if (r.width === 0 || r.height === 0) continue
            const y = Math.min(r.bottom, rb.bottom) - Math.max(r.top, rb.top)
            const x = Math.min(r.right, rb.right) - Math.max(r.left, rb.left)
            if (x > 1 && y > 1) found.push(`under the ${where} bar ${bar.className}: ${el.tagName.toLowerCase()}.${el.className} "${(el.textContent ?? '').trim().slice(0, 24)}"`)
          }
        }
      }
    }
    return [...new Set(found)]
  })

/**
 * `control` can be scrolled to and tapped: once scrolled into view it is wholly on screen, and what is drawn at its
 * middle is the control itself (nothing, such as a bar that stays put, is over it). A control that is off for now
 * (Create before a name) is held to the same, so it can be tapped once it is on.
 */
export async function reachable(control: Locator, what: string) {
  await control.scrollIntoViewIfNeeded()
  await expect.soft(control, `${what} is on screen`).toBeInViewport({ ratio: 1 })
  const hit = await control.evaluate((el) => {
    const box = el.getBoundingClientRect()
    const top = document.elementFromPoint(box.left + box.width / 2, box.top + box.height / 2)
    return top != null && (el === top || el.contains(top)) ? 'itself' : `${top?.tagName.toLowerCase()}.${top?.className}`
  })
  expect.soft(hit, `${what} is not covered where a tap lands`).toBe('itself')
}

/** Draws every list row as it is on screen (rows off screen are not drawn and are guessed at 65 px each). */
export const drawAllRows = (page: Page) =>
  page.evaluate(() => {
    if (document.getElementById('all-rows-drawn')) return
    const style = document.createElement('style')
    style.id = 'all-rows-drawn'
    style.textContent = '.lib-row { content-visibility: visible !important; }'
    document.head.append(style)
  })

/** Checks what is on top, softly, so one test reports every state that is wrong. */
export async function fitsHere(page: Page, state: string) {
  await drawAllRows(page)
  await page.evaluate(() => document.fonts.ready)
  expect.soft(await wrongHere(page), state).toEqual([])
}

/**
 * The size of every button, link, field, chip, segment and tick row on top (the open dialog, else the app's screen),
 * by its kind and name.
 */
export const targetSizes = (page: Page) =>
  page.evaluate(() => {
    const dialogs = [...document.querySelectorAll<HTMLElement>('.gr-dialog')].filter((d) => d.closest('[inert]') == null)
    const root = dialogs.at(-1) ?? document.querySelector<HTMLElement>('.shell-main')!
    const out: Record<string, { width: number; height: number }> = {}
    for (const el of root.querySelectorAll<HTMLElement>(':is(button, a[href], input:not([type=radio], [type=checkbox]), label.gr-chip, label.gr-segment, label.settings-row, label.lib-option)')) {
      if (!el.checkVisibility()) continue
      const name = (el.getAttribute('aria-label') ?? el.textContent ?? '').trim()
      let key = `${el.tagName.toLowerCase()} ${name}`
      for (let n = 2; out[key] != null; n++) key = `${el.tagName.toLowerCase()} ${name} #${n}`
      const { width, height } = el.getBoundingClientRect()
      out[key] = { width, height }
    }
    return out
  })

/**
 * Every target on top is at 160 × 284 at least as large as at 390 × 844, up to 48 px each way. `open` brings the state
 * to measure on screen at the current size; it is called once at each size.
 */
export async function keepsTargets(page: Page, what: string, open: () => Promise<void>) {
  await page.setViewportSize({ width: 390, height: 844 })
  await open()
  await drawAllRows(page)
  await page.waitForTimeout(200)
  const wide = await targetSizes(page)
  await page.setViewportSize({ width: 160, height: 284 })
  await page.waitForTimeout(200)
  const narrow = await targetSizes(page)
  expect.soft(Object.keys(narrow).sort(), `${what}: the same targets`).toEqual(Object.keys(wide).sort())
  const smaller = Object.entries(narrow)
    .filter(([key, box]) => wide[key] != null && (box.width < Math.min(48, wide[key]!.width) - 0.5 || box.height < Math.min(48, wide[key]!.height) - 0.5))
    .map(([key, box]) => `${key}: ${Math.round(box.width)} × ${Math.round(box.height)}, at 390 px ${Math.round(wide[key]!.width)} × ${Math.round(wide[key]!.height)}`)
  expect.soft(smaller, `${what}: targets smaller at 160 px`).toEqual([])
}

// ---- Seeding ----

export type StorySet = [name: string, kg: number, reps: number, rpe: number | null, extra?: { failed?: boolean; top?: boolean; plates?: boolean; missPoint?: string }]
export type Story = {
  side: 'POWERLIFTING' | 'BODYBUILDING'
  /** Exercises written first when missing: [name, category, muscles, own exercise?, plate count?]. Any other name is added as an accessory. */
  exercises?: [name: string, category: string, muscles: string[], own?: boolean, plateCount?: 'WHOLE' | 'HALF'][]
  settings?: Record<string, unknown>
  /** [weekday, name, sets, reps, rpe, repsHigh?], on the story's side. */
  plan?: [number, string, number, number, number, number?][]
  sessions?: { date: string; bodyweightKg?: number; mode?: 'POWERLIFTING' | 'BODYBUILDING'; sets: StorySet[] }[]
  baselines?: [name: string, e1rmKg: number][]
  checkIns?: { date: string; lagging: string[]; growingWell: string[] }[]
}

/** Writes a story straight into IndexedDB and reloads. Returns the ids of the exercises it names. */
export async function seedStory(page: Page, story: Story): Promise<Record<string, number>> {
  const ids = await page.evaluate(async (story) => {
    const db = await new Promise<IDBDatabase>((done, fail) => {
      const request = indexedDB.open('grit')
      request.onsuccess = () => done(request.result)
      request.onerror = () => fail(request.error)
    })
    const tx = db.transaction(['exercises', 'planItems', 'sessions', 'sets', 'baselines', 'settings', 'checkIns'], 'readwrite')
    const settled = new Promise<void>((done, fail) => {
      tx.oncomplete = () => done()
      tx.onerror = () => fail(tx.error)
    })
    const add = (store: string, row: unknown) =>
      new Promise<number>((done) => {
        const request = tx.objectStore(store).add(row)
        request.onsuccess = () => done(request.result as number)
      })
    const all = await new Promise<{ id: number; name: string }[]>((done) => {
      const request = tx.objectStore('exercises').getAll()
      request.onsuccess = () => done(request.result)
    })
    const ids = new Map(all.map((e) => [e.name, e.id]))
    for (const [name, category, muscles, own, plateCount] of story.exercises ?? [])
      if (!ids.has(name)) ids.set(name, await add('exercises', { name, category, parentLift: null, muscles, isBuiltIn: !own, archived: false, ...(plateCount ? { plateCount } : {}) }))
    const idOf = async (name: string) => {
      if (!ids.has(name)) ids.set(name, await add('exercises', { name, category: 'ACCESSORY', parentLift: null, muscles: [], isBuiltIn: true, archived: false }))
      return ids.get(name)!
    }
    tx.objectStore('settings').put({ key: 'trainingMode', value: story.side })
    for (const [key, value] of Object.entries(story.settings ?? {})) tx.objectStore('settings').put({ key, value })
    const order = new Map<number, number>()
    for (const [weekday, name, sets, reps, targetRpe, repsHigh] of story.plan ?? []) {
      const next = order.get(weekday) ?? 0
      order.set(weekday, next + 1)
      await add('planItems', { weekday, exerciseId: await idOf(name), sets, reps, targetRpe, order: next, mode: story.side, ...(repsHigh != null ? { repsHigh } : {}) })
    }
    for (const s of story.sessions ?? []) {
      const sessionId = await add('sessions', { date: s.date, bodyweightKg: s.bodyweightKg ?? null, readiness: null, mode: s.mode ?? story.side })
      for (const [name, weightKg, reps, rpe, extra = {}] of s.sets)
        await add('sets', {
          sessionId,
          exerciseId: await idOf(name),
          weightKg,
          reps,
          rpe,
          failed: extra.failed ?? false,
          missPoint: extra.missPoint ?? null,
          isTopSet: extra.top ?? false,
          ...(extra.plates ? { unit: 'PLATES' } : {}),
        })
    }
    for (const [name, e1rmKg] of story.baselines ?? []) tx.objectStore('baselines').put({ exerciseId: await idOf(name), e1rmKg, date: '2026-10-12' })
    for (const c of story.checkIns ?? []) await add('checkIns', c)
    await settled
    db.close()
    return Object.fromEntries(ids)
  }, story)
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
  return ids
}

/** Every date `days` after `from`, as `YYYY-MM-DD`. */
export const dayAfter = (from: string, days: number) => new Date(Date.parse(`${from}T00:00:00Z`) + days * 86_400_000).toISOString().slice(0, 10)

// ---- The stories the deeper screens and dialogs are checked with ----

export const OWN: NonNullable<Story['exercises']> = [
  [LONG_NAME, 'ACCESSORY', ['Side delts'], true],
  [ONE_WORD, 'ACCESSORY', ['Pecs', 'Front delts', 'Triceps'], true],
]
export const STRICT_PRESS: NonNullable<Story['exercises']>[number] = ['Strict press', 'STRICT_PRESS', ['Front delts', 'Side delts', 'Triceps']]
export const PULLDOWN: NonNullable<Story['exercises']>[number] = ['Lat pulldown', 'ACCESSORY', ['Lats', 'Biceps'], false, 'HALF']

/**
 * Twelve weeks of the four lifts, each with a top set that climbs (so records), and among them a failed top set with
 * its miss point, a set with no RPE, a top set that is also a record, plate sets and a bodyweight (0 kg) set.
 */
export function powerliftingHistory(): NonNullable<Story['sessions']> {
  const sessions: NonNullable<Story['sessions']> = [{ date: '2025-10-12', bodyweightKg: 90.4, sets: [['Deadlift', 212.5, 10, 9.5, { top: true }]] }]
  for (let week = 0; week < 12; week++) {
    const monday = dayAfter('2026-08-24', week * 7)
    const squat = 142.5 + 2.5 * week
    sessions.push({
      date: monday,
      bodyweightKg: 91.6 + week / 10,
      sets: [
        ['Squat', squat, 3, 9, { top: true }],
        ['Squat', squat + 10, 1, 10, { failed: true, missPoint: 'FLOOR' }],
        ['Squat', squat - 30, 5, null],
        ['Pause squat', squat - 27.5, 3, 7.5],
      ],
    })
    sessions.push({ date: dayAfter(monday, 1), sets: [['Bench', 107.5 + week, 3, 9, { top: true }], ['Bench', 90, 12, null], ['Bench', 0, 10, 8]] })
    sessions.push({ date: dayAfter(monday, 2), sets: [['Strict press', 62.5 + 2.5 * Math.floor(week / 2), 3, 8.5, { top: true }], ['Strict press', 70, 2, 10, { failed: true, missPoint: 'FLOOR' }]] })
    sessions.push({ date: dayAfter(monday, 4), sets: [['Deadlift', 185 + 2.5 * week, 3, 7.5, { top: true }], ['Deadlift', 160, 5, 6]] })
  }
  return sessions.filter((s) => s.date < '2026-11-11')
}

export const powerliftingStory = (): Story => ({
  side: 'POWERLIFTING',
  exercises: [STRICT_PRESS, ...OWN],
  settings: { showStrictPress: true },
  plan: [
    [1, 'Squat', 5, 5, 8],
    [1, ONE_WORD, 3, 10, 8],
    [3, 'Bench', 4, 6, 7.5],
    [3, LONG_NAME, 3, 12, 8],
  ],
  sessions: powerliftingHistory(),
  baselines: [['Squat', 190]],
})

/** Five weeks on the bodybuilding side: the plate-counted pulldown (plate 12½ × 12 among them), the one-word name, a failed set, no RPE, bodyweight. */
export function bodybuildingHistory(): NonNullable<Story['sessions']> {
  const sessions: NonNullable<Story['sessions']> = []
  for (let week = 0; week < 5; week++) {
    const monday = dayAfter('2026-10-05', week * 7)
    sessions.push({ date: monday, sets: [['Incline DB press', 26, 10 + (week % 2), 8], [LONG_NAME, 12.5, 12, 8], [ONE_WORD, 40 + week, 10, 8, { top: true }], [ONE_WORD, 0, 15, null]] })
    sessions.push({
      date: dayAfter(monday, 2),
      sets: [
        ['Lat pulldown', 12.5, 12, 8, { plates: true }],
        ['Lat pulldown', 13 + week, 10, 9, { plates: true, top: true }],
        ['Lat pulldown', 14, 6, 10, { plates: true, failed: true }],
        ['Lat pulldown', 7.5, 15, null, { plates: true }],
        ['Lat pulldown', 50, 10, 8],
      ],
    })
  }
  return sessions
}

export const bodybuildingStory = (): Story => ({
  side: 'BODYBUILDING',
  exercises: [...OWN, PULLDOWN],
  settings: { bodybuildingTrainingDays: [1, 3, 5] },
  plan: [
    [1, 'Incline DB press', 3, 8, 8, 12],
    [1, LONG_NAME, 3, 12, 8, 15],
    [1, ONE_WORD, 3, 8, 8, 12],
    [3, 'Lat pulldown', 3, 8, 8, 12],
  ],
  sessions: bodybuildingHistory(),
})
