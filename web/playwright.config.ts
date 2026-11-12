import { existsSync } from 'node:fs'
import { defineConfig, devices } from '@playwright/test'

// The container only has a pre-installed Chromium (no WebKit), so e2e runs in Chromium
// with the iPhone 13 viewport and touch emulation.
const CHROMIUM = '/opt/pw-browsers/chromium'
const iphone = { ...devices['iPhone 13'], browserName: 'chromium' as const }

// The timing checks (tagged @speed in e2e/library.spec.ts and e2e/pictures.spec.ts) hold the picker and the
// Exercises tab to 100 ms on the page's own clock. Beside other workers they measure the machine, not the
// app, so they are their own project and run once everything else has finished, one after another: one
// worker, so the two files do not run side by side either.
const SPEED = /@speed/

export default defineConfig({
  testDir: './e2e',
  reporter: 'list',
  projects: [
    { name: 'e2e', grepInvert: SPEED },
    { name: 'speed', grep: SPEED, dependencies: ['e2e'], workers: 1 },
  ],
  use: {
    baseURL: 'http://localhost:4173',
    ...iphone,
    launchOptions: existsSync(CHROMIUM) ? { executablePath: CHROMIUM } : {},
  },
  webServer: [
    {
      command: 'npm run build && npm run preview -- --port 4173 --strictPort',
      url: 'http://localhost:4173',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      // A private copy of the build for e2e/update.spec.ts, which rewrites its sw.js to fake a new
      // deploy; other spec files keep running against the untouched dist on 4173. Starts after the
      // first server, so dist is already built.
      command: "node -e \"require('node:fs').cpSync('dist','dist-update',{recursive:true})\" && npm run preview -- --outDir dist-update --port 4175 --strictPort",
      url: 'http://localhost:4175',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      // The dev server, for the dev-only design-system showcase (e2e/showcase.spec.ts).
      command: 'npm run dev -- --port 4174 --strictPort',
      url: 'http://localhost:4174',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
})
