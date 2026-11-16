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

// The build under test gets a fixed commit id, so the version line at the bottom of Settings, and its
// screenshots, never change with the commit (plugins/buildInfo.ts). e2e/plan.spec.ts checks the line.
export const E2E_BUILD_ID = 'a1b2c3d'
const env = { GRIT_BUILD_ID: E2E_BUILD_ID }

// Motion off on every e2e page (stage 2): public/mode-head-start.js reads this key and puts data-motion="off" on
// <html>, so screenshots and position checks run still, as Robolectric does on Android. Set through the browser
// storage every test context starts with, so no spec has to remember a fixture; a motion-on test clears it with
// test.use({ storageState: { cookies: [], origins: [] } }). The key is MOTION_KEY in src/ui/modeHeadStart.ts.
const MOTION_OFF = [4173, 4174, 4175].map((port) => ({
  origin: `http://localhost:${port}`,
  localStorage: [{ name: 'grit:motion', value: 'off' }],
}))

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
    storageState: { cookies: [], origins: MOTION_OFF },
    launchOptions: existsSync(CHROMIUM) ? { executablePath: CHROMIUM } : {},
  },
  webServer: [
    {
      command: 'npm run build && npm run preview -- --port 4173 --strictPort',
      env,
      url: 'http://localhost:4173',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      // A private copy of the build for e2e/update.spec.ts, which rewrites its sw.js to fake a new
      // deploy; other spec files keep running against the untouched dist on 4173. Starts after the
      // first server, so dist is already built.
      command: "node -e \"require('node:fs').cpSync('dist','dist-update',{recursive:true})\" && npm run preview -- --outDir dist-update --port 4175 --strictPort",
      env,
      url: 'http://localhost:4175',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
    {
      // The dev server, for the dev-only design-system showcase (e2e/showcase.spec.ts).
      command: 'npm run dev -- --port 4174 --strictPort',
      env,
      url: 'http://localhost:4174',
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
})
