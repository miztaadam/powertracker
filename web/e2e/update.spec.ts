import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import { openOnWednesday, seedFlatBench, tap } from './helpers'

// This spec has its own copy of the build (dist-update, served on 4175; see playwright.config.ts),
// because it rewrites sw.js to fake a new deploy and specs in other workers must not see that.
const SW_PATH = resolve(process.cwd(), 'dist-update/sw.js')
test.use({ baseURL: 'http://localhost:4175' })
test.describe.configure({ mode: 'serial' })

test('updateDoesNotReloadDuringSession', async ({ page }) => {
  const original = readFileSync(SW_PATH, 'utf8')
  try {
    await runSession(page, original)
  } finally {
    writeFileSync(SW_PATH, original)
  }
})

async function runSession(page: Page, original: string) {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready
  })
  await page.reload()
  await expect.poll(() => page.evaluate(() => navigator.serviceWorker.controller != null)).toBe(true)

  // Mid-session: a marker in the page stands for "nothing reloaded".
  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  await page.evaluate(() => {
    ;(window as unknown as { notReloaded: boolean }).notReloaded = true
  })

  // A second deploy: the script changes by a comment, so the browser sees a new version, installs it and leaves it waiting.
  writeFileSync(SW_PATH, `${original}\n// next version`)
  await page.evaluate(async () => {
    await (await navigator.serviceWorker.getRegistration())!.update()
  })
  await expect.poll(() => page.evaluate(async () => (await navigator.serviceWorker.getRegistration())!.waiting != null)).toBe(true)

  // The new version waits; the session is not touched and no prompt interrupts it.
  // The page hears about the waiting version a moment after the browser has it: let that settle. The
  // prompt appearing after Finish below proves it was heard.
  await page.evaluate(() => new Promise((done) => requestAnimationFrame(() => requestAnimationFrame(done))))
  await expect(page.getByText('Update ready — tap to refresh')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBe(true)

  // Finished: Finish clears the session's note, so now the prompt shows. Still nothing reloads until it is tapped.
  await tap(page, 'Finish')
  await expect(page.getByRole('heading', { name: 'Session saved' })).toBeVisible()
  await expect(page.getByText('Update ready — tap to refresh')).toBeVisible()
  await expect(page.getByText('Update ready — tap to refresh')).toBeVisible()
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBe(true)

  const reloaded = page.waitForEvent('load')
  await page.getByText('Update ready — tap to refresh').click()
  await reloaded
  expect(await page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)).toBeUndefined()
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
}

// 1.17: when a new version of the service worker takes over, it reloads every window the previous version controlled
// (public/sw-reload-clients.js), so a window that never asked for updates (here one showing "Grit is open in another
// tab") does not stay on old code. The new sw.js below also answers "build?" with "next", so each page can ask the
// worker that controls it which version that is.
// It also notes, for the test's record, how many windows it found when it took over and how each reload went.
const NEXT_BUILD = `
// next version
const seen = { windows: -1, navigations: [] }
const navigate = WindowClient.prototype.navigate
WindowClient.prototype.navigate = function (url) {
  const result = navigate.call(this, url)
  result.then(() => seen.navigations.push('resolved'), (e) => seen.navigations.push('rejected: ' + e.name))
  return result
}
self.addEventListener('activate', () => {
  self.clients.matchAll({ type: 'window' }).then((list) => { seen.windows = list.length })
})
self.addEventListener('message', (event) => {
  if (event.data === 'build?') event.ports[0].postMessage('next')
  if (event.data === 'seen?') event.ports[0].postMessage(JSON.stringify(seen))
})`

/** Asks the worker controlling `page`; null when it does not answer (the old build does not). */
const ask = (page: Page, question: string) =>
  page.evaluate(
    (question) =>
      new Promise<string | null>((done) => {
        const controller = navigator.serviceWorker.controller
        if (controller == null) return done(null)
        const channel = new MessageChannel()
        channel.port1.onmessage = (event) => done(String(event.data))
        controller.postMessage(question, [channel.port2])
        setTimeout(() => done(null), 2000)
      }),
    question,
  )

/** The build the worker controlling `page` says it is: 'next' for the new one; 'old' when it does not answer. */
const buildOf = async (page: Page) => (await ask(page, 'build?')) ?? 'old'

const markNotReloaded = (page: Page) =>
  page.evaluate(() => {
    ;(window as unknown as { notReloaded: boolean }).notReloaded = true
  })
const notReloaded = (page: Page) => page.evaluate(() => (window as unknown as { notReloaded?: boolean }).notReloaded)

/** Counts the times `page`'s document loads, from now on. */
function countLoads(page: Page) {
  let loads = 0
  page.on('load', () => loads++)
  return () => loads
}

test('a window that never registered for updates reloads when another one updates', async ({ page, context }) => {
  const original = readFileSync(SW_PATH, 'utf8')
  try {
    await page.goto('/#/today')
    await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
    await page.evaluate(async () => {
      await navigator.serviceWorker.ready
    })
    await page.reload()
    await expect.poll(() => page.evaluate(() => navigator.serviceWorker.controller != null)).toBe(true)

    // The second window stands aside and never registers for updates itself.
    const second = await context.newPage()
    await second.goto('/#/today')
    await expect(second.getByRole('heading', { name: 'Grit is open in another tab' })).toBeVisible()
    await expect.poll(() => second.evaluate(() => navigator.serviceWorker.controller != null)).toBe(true)
    expect(await buildOf(page)).toBe('old')
    expect(await buildOf(second)).toBe('old')
    await markNotReloaded(page)
    await markNotReloaded(second)
    const firstLoads = countLoads(page)
    const secondLoads = countLoads(second)

    // A new deploy, found by the first window; the second one hears nothing of it.
    writeFileSync(SW_PATH, `${original}${NEXT_BUILD}`)
    await page.evaluate(async () => {
      await (await navigator.serviceWorker.getRegistration())!.update()
    })
    await expect(page.getByText('Update ready — tap to refresh')).toBeVisible()
    expect(await notReloaded(second)).toBe(true)

    await page.getByText('Update ready — tap to refresh').click()
    await expect.poll(secondLoads).toBeGreaterThanOrEqual(1)
    await expect.poll(firstLoads).toBeGreaterThanOrEqual(1)
    expect(await notReloaded(second)).toBeUndefined()
    expect(await notReloaded(page)).toBeUndefined()
    await expect(page.getByRole('navigation', { name: 'Main' }).or(page.getByRole('heading', { name: 'Grit is open in another tab' }))).toBeVisible()
    await expect(second.getByRole('navigation', { name: 'Main' }).or(second.getByRole('heading', { name: 'Grit is open in another tab' }))).toBeVisible()
    expect(await buildOf(page)).toBe('next')
    expect(await buildOf(second)).toBe('next')

    // Once: nothing reloads again.
    const settled = [firstLoads(), secondLoads()]
    const seen = JSON.parse((await ask(page, 'seen?'))!) as { windows: number; navigations: string[] }
    console.log(`after the update: first window loaded ${settled[0]} time(s), second window ${settled[1]}; at activate the worker found ${seen.windows} window(s); navigations: ${seen.navigations.join(', ')}`)
    expect(seen.windows).toBe(2)
    await page.waitForTimeout(2000)
    expect([firstLoads(), secondLoads()]).toEqual(settled)
    expect(settled[1]).toBe(1)
  } finally {
    writeFileSync(SW_PATH, original)
  }
})

test('a first install reloads nothing', async ({ page }) => {
  const loads = countLoads(page)
  let navigations = 0
  page.on('framenavigated', (frame) => {
    if (frame === page.mainFrame()) navigations++
  })
  await page.goto('/#/today')
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
  // The first worker installs and activates; no window is controlled by an earlier one, so none is reloaded.
  await expect.poll(() => page.evaluate(async () => (await navigator.serviceWorker.getRegistration())?.active?.state)).toBe('activated')
  await page.waitForTimeout(1500)
  expect(loads()).toBe(1)
  expect(navigations).toBe(1)
  expect(await page.evaluate(() => navigator.serviceWorker.controller)).toBeNull()
  await expect(page.getByRole('navigation', { name: 'Main' })).toBeVisible()
})
