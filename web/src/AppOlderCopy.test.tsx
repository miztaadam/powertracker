import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import Dexie from 'dexie'
import { App } from './App'
import { ensureSeeded, openGritDb } from './db/db'

// 1.17: the app, opened on a log a newer Grit wrote, shows the older-copy screen and nothing of the log, and never
// reaches the recovery screen (which would read the database and offer an export that drops the plate markers).

const name = 'grit-app-older-copy'

afterEach(async () => {
  vi.restoreAllMocks()
  await Dexie.delete(name)
})

async function newerLog() {
  const db = await openGritDb(name)
  await ensureSeeded(db)
  db.close()
  await new Promise<void>((done, fail) => {
    // Ten above this copy's own native version (declared 7, native 70, since 1.24): a newer Grit's.
    const req = indexedDB.open(name, 80)
    req.onupgradeneeded = () => {}
    req.onsuccess = () => {
      req.result.close()
      done()
    }
    req.onerror = () => fail(req.error)
  })
}

describe('App on a log a newer Grit wrote', () => {
  it('shows only the older-copy screen: no tabs, no recovery, no export', async () => {
    await newerLog()
    // React reports the render error the screen is chosen from.
    vi.spyOn(console, 'error').mockImplementation(() => {})
    const swallow = (event: ErrorEvent) => event.preventDefault()
    window.addEventListener('error', swallow)
    try {
      render(<App dbName={name} />)
      expect(await screen.findByRole('heading', { name: 'Update Grit to carry on' })).toBeInTheDocument()
      expect(screen.getAllByRole('button').map((b) => b.textContent)).toEqual(['Update and reload'])
      expect(screen.queryByRole('navigation', { name: 'Main' })).toBeNull()
      expect(screen.queryByRole('heading', { name: /^Grit couldn't/ })).toBeNull()
      expect(screen.queryByRole('button', { name: /export/i })).toBeNull()
    } finally {
      window.removeEventListener('error', swallow)
    }
  })
})
