import '@testing-library/jest-dom/vitest'
import 'fake-indexeddb/auto'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach } from 'vitest'
import { GUIDE_KEY } from '../screens/guide/guideSeen'

// The guide's first-launch gate (1.16) shut, as on every e2e page: each test's empty database is a new user who has not
// seen the guide. Set before each test (a test file's own afterEach may clear localStorage); the guide's first-launch
// tests remove it.
beforeEach(() => {
  try {
    localStorage.setItem(GUIDE_KEY, 'off')
  } catch {
    // No storage: nothing to set.
  }
})

afterEach(() => {
  cleanup()
  // The remembered side (ui/modeHeadStart.ts) is a head start for the next page load: no test inherits another's.
  try {
    localStorage.removeItem('grit:trainingMode')
  } catch {
    // Nothing to clear.
  }
})
