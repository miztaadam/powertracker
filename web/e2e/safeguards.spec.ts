import { expect, test } from '@playwright/test'
import { openOnWednesday, seedFlatBench, tabBar } from './helpers'

// App lock, the install card and the one-tab guard in a real browser. Chromium stands in for
// iPhone Safari (see playwright.config.ts): Face ID is a virtual authenticator here, so the real
// passkey sheet, the Home Screen install and the storage prompt stay manual iPhone checks.

test('install card shows in a browser tab until it is put away', async ({ page }) => {
  await page.goto('/#/today')
  await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeVisible()
  await expect(page.getByText('Add Grit to your Home Screen: tap Share, then Add to Home Screen. Installed, your training log is protected from being cleared.')).toBeVisible()

  await page.getByRole('button', { name: 'Not now' }).click()
  await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeHidden()
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeHidden()
})

test('install card says to export first when there is already a log in the tab', async ({ page }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeVisible()
  await expect(page.getByText(/tap Export backup before you add Grit to the Home Screen, then restore that file in the installed app/)).toBeVisible()
  // Chromium has no share sheet for files here: the backup comes as a download.
  const download = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Export backup' }).click()
  expect((await download).suggestedFilename()).toBe('grit-backup-2026-11-11.json')
})

test('the app runs under its Content-Security-Policy', async ({ page }) => {
  const violations: string[] = []
  page.on('console', (message) => {
    if (message.text().includes('Content Security Policy')) violations.push(message.text())
  })
  const response = await page.goto('/#/today')
  const policy = response?.headers()['content-security-policy'] ?? ''
  for (const directive of ["default-src 'self'", "frame-ancestors 'none'", "base-uri 'self'", "form-action 'self'"]) expect(policy).toContain(directive)
  await expect(tabBar(page)).toBeVisible()
  for (const tab of ['Coach', 'Progress', 'Plan', 'Today']) {
    await tabBar(page).getByRole('link', { name: tab }).click()
    await expect(tabBar(page).getByRole('link', { name: tab })).toHaveAttribute('aria-current', 'page')
  }
  expect(violations).toEqual([])
})

test('install card stays away in an installed app', async ({ page }) => {
  // What iPhone Safari reports for an app opened from the Home Screen.
  await page.addInitScript(() => Object.defineProperty(navigator, 'standalone', { value: true }))
  await page.goto('/#/today')
  await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Install Grit' })).toBeHidden()
})

test('a second tab stands aside, and can take over', async ({ page, context }) => {
  await page.goto('/#/today')
  await expect(tabBar(page)).toBeVisible()

  const second = await context.newPage()
  await second.goto('/#/today')
  await expect(second.getByRole('heading', { name: 'Grit is open in another tab' })).toBeVisible()
  await expect(tabBar(second)).toBeHidden()

  await second.getByRole('button', { name: 'Use Grit here' }).click()
  await expect(tabBar(second)).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Grit is open in another tab' })).toBeVisible()
  await expect(tabBar(page)).toBeHidden()

  // Closing the tab that runs Grit hands it back.
  await second.close()
  await expect(tabBar(page)).toBeVisible()
})

test('app lock: a passkey locks the app on launch and nothing shows behind it', async ({ page }) => {
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('WebAuthn.enable')
  const { authenticatorId } = await cdp.send('WebAuthn.addVirtualAuthenticator', {
    options: { protocol: 'ctap2', transport: 'internal', hasResidentKey: true, hasUserVerification: true, isUserVerified: true, automaticPresenceSimulation: true },
  })

  await page.goto('/#/settings')
  const toggle = page.getByRole('switch', { name: 'App lock' })
  await toggle.click()
  await expect(toggle).toHaveAttribute('aria-checked', 'true')
  // Switching it on does not lock the visit it was switched on in.
  await expect(tabBar(page)).toBeVisible()

  // The next launch, with a face the phone does not know.
  await cdp.send('WebAuthn.setUserVerified', { authenticatorId, isUserVerified: false })
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Grit is locked' })).toBeVisible()
  await expect(tabBar(page)).toHaveCount(0)
  await expect(page.locator('.shell')).toHaveCount(0)

  await cdp.send('WebAuthn.setUserVerified', { authenticatorId, isUserVerified: true })
  await page.getByRole('button', { name: 'Unlock', exact: true }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Settings' })).toBeVisible()
  await expect(tabBar(page)).toBeVisible()

  // The passkey is deleted from the phone: Unlock can only fail, "Can't unlock?" makes a new one.
  await cdp.send('WebAuthn.clearCredentials', { authenticatorId })
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Grit is locked' })).toBeVisible()
  await page.getByRole('button', { name: 'Unlock', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Grit is locked' })).toBeVisible()
  await page.getByRole('button', { name: "Can't unlock?" }).click()
  await expect(tabBar(page)).toBeVisible()
  const { credentials } = await cdp.send('WebAuthn.getCredentials', { authenticatorId })
  expect(credentials).toHaveLength(1)

  // And the new passkey is the one that unlocks from now on.
  await page.reload()
  await expect(tabBar(page)).toBeVisible()
})
