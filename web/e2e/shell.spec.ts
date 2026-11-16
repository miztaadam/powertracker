import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, tabBar } from './helpers'

// The bottom of the iPhone Home Screen app: the tab bar over the home indicator, and the shell down to the
// screen's edge. Chromium can stand in for an iPhone's safe area (the DevTools protocol's override), so the
// real env(safe-area-inset-*) values are measured, not a faked token.

async function safeArea(page: Page, bottom: number) {
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('Emulation.setSafeAreaInsetsOverride' as never, { insets: { top: 47, bottom, left: 0, right: 0 } } as never)
}

const px = (value: string) => parseFloat(value)

for (const [inset, expected] of [
  [34, 20],
  [20, 6],
  [14, 0],
  [0, 0],
] as const) {
  test(`with a ${inset} px home-bar inset the tab bar pads ${expected} px below its labels, and the snackbar sits above it`, async ({ page }) => {
    await safeArea(page, inset)
    await openOnWednesday(page)
    const bar = tabBar(page)
    expect(px(await bar.evaluate((el) => getComputedStyle(el).paddingBottom))).toBe(expected)
    // The snackbar is placed from the same inset: tab bar height + rest dock + gap + inset.
    const bottom = await page.locator('.shell').evaluate((el) => {
      const probe = document.createElement('div')
      probe.style.position = 'fixed'
      probe.style.bottom = 'var(--snackbar-bottom)'
      el.append(probe)
      const value = getComputedStyle(probe).bottom
      probe.remove()
      return value
    })
    // Tabs of --tabbar-height, a --space-4 gap, and the same trimmed inset as the tab bar's padding.
    const [tabs, gap] = await page.evaluate(() => {
      const root = getComputedStyle(document.documentElement)
      return [root.getPropertyValue('--tabbar-height'), root.getPropertyValue('--space-4')]
    })
    expect(px(bottom)).toBe(px(tabs) + px(gap) + expected)
  })
}

test('the shell fills the whole screen, even when the page is shorter than it (the iOS Home Screen app)', async ({ page }) => {
  await safeArea(page, 34)
  await openOnWednesday(page)
  const covers = () =>
    page.evaluate(() => {
      const box = document.querySelector('.shell')!.getBoundingClientRect()
      const bar = document.querySelector('.gr-tabbar')!.getBoundingClientRect()
      return { top: box.top, bottom: Math.round(box.bottom), barBottom: Math.round(bar.bottom), height: window.innerHeight }
    })
  expect(await covers()).toEqual({ top: 0, bottom: (await covers()).height, barBottom: (await covers()).height, height: (await covers()).height })
  // iOS sizes html, body and #root at 100% a little short of the screen in the Home Screen app: the shell must not follow.
  await page.evaluate(() => {
    for (const el of [document.documentElement, document.body, document.getElementById('root')!]) el.style.height = '80%'
  })
  const short = await covers()
  expect(short.bottom).toBe(short.height)
  expect(short.barBottom).toBe(short.height)
  // Nothing scrolls the page itself: only the screen inside the shell scrolls.
  expect(await page.evaluate(() => document.scrollingElement!.scrollHeight <= window.innerHeight)).toBe(true)
})

test('the page background is painted on html too, so overscroll and the system bars match it', async ({ page }) => {
  await openOnWednesday(page)
  const [html, body] = await page.evaluate(() => [getComputedStyle(document.documentElement).backgroundColor, getComputedStyle(document.body).backgroundColor])
  expect(html).toBe(body)
  expect(html).not.toBe('rgba(0, 0, 0, 0)')
})

// iOS 26 Home Screen app (WebKit bug 301108): with viewport-fit=cover and a black-translucent status bar, the
// viewport is short by the status bar's height, so the shell reaches down by that inset too, capped at the large
// viewport so it can never run past the screen. Chromium cannot pretend to be in standalone display mode, so the
// rule is reached through the class the head script sets there (`ios-standalone`).
test.describe('the iOS Home Screen app', () => {
  async function measure(page: Page) {
    return page.evaluate(() => {
      const probe = document.createElement('div')
      probe.style.cssText = 'position: fixed; top: 0; height: 100lvh; visibility: hidden'
      document.body.append(probe)
      const lvh = probe.getBoundingClientRect().height
      probe.remove()
      const shell = document.querySelector('.shell')!
      return {
        lvh,
        viewport: window.innerHeight,
        shellTop: shell.getBoundingClientRect().top,
        shellHeight: shell.getBoundingClientRect().height,
        // The minimum as the box shows it: html squeezed to 10 px is held up by its min-height, if it has one.
        htmlMinHeight: (() => {
          const html = document.documentElement
          html.style.height = '10px'
          const held = html.getBoundingClientRect().height
          html.style.height = ''
          return held === 10 ? 0 : held
        })(),
        pageScrolls: document.scrollingElement!.scrollHeight > window.innerHeight,
      }
    })
  }

  test('reaches down by the status-bar inset, capped at the large viewport', async ({ page }) => {
    const cdp = await page.context().newCDPSession(page)
    await cdp.send('Emulation.setSafeAreaInsetsOverride' as never, { insets: { top: 59, bottom: 34, left: 0, right: 0 } } as never)
    await openOnWednesday(page)
    const before = await measure(page)
    // Outside the Home Screen app nothing changes: the shell is the viewport, html has no minimum.
    expect(before.shellHeight).toBe(before.viewport)
    expect(before.htmlMinHeight).toBe(0)

    await page.evaluate(() => document.documentElement.classList.add('ios-standalone'))
    const after = await measure(page)
    const expected = Math.min(after.lvh, after.viewport + 59)
    expect(after.shellTop).toBe(0)
    expect(after.shellHeight).toBe(expected)
    // The page behind can never show a strip either.
    expect(after.htmlMinHeight).toBe(expected)
    // The 1.5rem strip and the trimmed tab inset are as before.
    expect(px(await page.locator('.shell-main').evaluate((el) => getComputedStyle(el).paddingTop))).toBe(59 + 24)
    expect(px(await tabBar(page).evaluate((el) => getComputedStyle(el).paddingBottom))).toBe(20)
  })
})

// A centred dialog keeps clear of the status bar and the home bar; on a phone without them it keeps its 24 px.
for (const [top, bottom, expected] of [
  [47, 34, [47, 34]],
  [0, 0, [24, 24]],
] as const) {
  test(`a centred dialog's backdrop pads ${expected[0]} px at the top and ${expected[1]} px at the bottom with a ${top}/${bottom} px safe area`, async ({ page }) => {
    const cdp = await page.context().newCDPSession(page)
    await cdp.send('Emulation.setSafeAreaInsetsOverride' as never, { insets: { top, bottom, left: 0, right: 0 } } as never)
    await openOnWednesday(page)
    const padding = await page.locator('.shell').evaluate((el) => {
      const scrim = document.createElement('div')
      scrim.className = 'gr-dialog-scrim'
      el.append(scrim)
      const style = getComputedStyle(scrim)
      const values = [style.paddingTop, style.paddingBottom]
      scrim.remove()
      return values
    })
    expect(padding.map(px)).toEqual([...expected])
  })
}
