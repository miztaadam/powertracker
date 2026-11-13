import { readFileSync } from 'node:fs'
import { expect, test } from '@playwright/test'
import { openOnWednesday, readAllTables, seedFlatBench, tap } from './helpers'

test('backupRoundTrip: export, clear site data, import, same data', async ({ page, context }) => {
  await openOnWednesday(page)
  await seedFlatBench(page)
  await tap(page, 'Start session')
  await tap(page, 'Log set 1')
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
  // A changed setting, so the round trip shows settings come back too.
  await page.evaluate(
    () =>
      new Promise<void>((done, fail) => {
        const open = indexedDB.open('grit')
        open.onerror = () => fail(open.error)
        open.onsuccess = () => {
          const tx = open.result.transaction('settings', 'readwrite')
          tx.objectStore('settings').put({ key: 'defaultRpe', value: 7.5 })
          tx.oncomplete = () => {
            open.result.close()
            done()
          }
        }
      }),
  )
  const before = await readAllTables(page)
  expect(before.settings).toContainEqual({ key: 'defaultRpe', value: 7.5 })
  expect(before.sets).toHaveLength(7)

  // Export: the file is named for the day and holds the log.
  await page.goto('/#/settings')
  const download = page.waitForEvent('download')
  await tap(page, 'Export backup')
  const file = await download
  expect(file.suggestedFilename()).toBe('grit-backup-2026-11-11.json')
  await expect(page.getByText('Backup saved.')).toBeVisible()
  const path = await file.path()
  expect(JSON.parse(readFileSync(path, 'utf8')).version).toBe(3)

  // Site data cleared, as Safari does after weeks unused: the log is gone.
  const cdp = await context.newCDPSession(page)
  await cdp.send('Storage.clearDataForOrigin', { origin: new URL(page.url()).origin, storageTypes: 'all' })
  await page.goto('/#/today')
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Ready to train?' })).toBeVisible()
  expect((await readAllTables(page)).sets).toHaveLength(0)

  // Import: asks first, then everything is back.
  await page.goto('/#/settings')
  await page.getByLabel('Backup file').setInputFiles(path)
  await expect(page.getByRole('dialog', { name: 'Replace all data?' })).toBeVisible()
  await page.getByRole('dialog').getByRole('button', { name: 'Replace' }).click()
  await expect(page.getByText('Backup restored.')).toBeVisible()
  // A fresh install has no setting rows, a restore writes them all: the changed one must be among them.
  const after = await readAllTables(page)
  expect({ ...after, settings: [] }).toEqual({ ...before, settings: [] })
  expect(after.settings).toEqual(expect.arrayContaining(before.settings!))

  await page.getByRole('link', { name: 'Today' }).click()
  await expect(page.getByRole('button', { name: 'Set 1: 100 kg × 5 at RPE 8, done' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Log set 2' })).toBeVisible()
})
