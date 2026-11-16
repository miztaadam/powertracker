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
