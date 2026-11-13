import '@testing-library/jest-dom/vitest'
import 'fake-indexeddb/auto'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
  // The remembered side (ui/modeHeadStart.ts) is a head start for the next page load: no test inherits another's.
  try {
    localStorage.removeItem('grit:trainingMode')
  } catch {
    // Nothing to clear.
  }
})
