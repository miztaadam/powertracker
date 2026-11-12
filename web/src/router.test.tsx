import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { App } from './App'
import { navigate } from './router'
import { todayFixture } from './test/todayFixture'

function setHash(hash: string) {
  act(() => {
    window.location.hash = hash
    window.dispatchEvent(new HashChangeEvent('hashchange'))
  })
}

let fx: ReturnType<typeof todayFixture>
const show = () => render(<App dbName={fx.name} />)
/** The app is behind its database and lock: the tab bar shows once it is open. */
const opened = () => screen.findByRole('navigation', { name: 'Main' })

describe('app shell routing', () => {
  beforeEach(() => {
    window.location.hash = ''
    fx = todayFixture()
  })
  // Unmount before the database closes.
  afterEach(async () => {
    cleanup()
    await fx.dispose()
  })

  it('starts on Today with the tab bar', async () => {
    show()
    await opened()
    expect(screen.getByRole('heading', { level: 1, name: 'Today' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Today' })).toHaveAttribute('aria-current', 'page')
  })

  it('switches tabs from the tab bar', async () => {
    show()
    await opened()
    await userEvent.click(screen.getByRole('link', { name: 'Coach' }))
    expect(window.location.hash).toBe('#/coach')
    expect(screen.getByRole('heading', { level: 1, name: 'Coach' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Plan' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Plan' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Plan' })).toHaveAttribute('aria-current', 'page')
  })

  it('renders the lift page for #/lift/BENCH', async () => {
    window.location.hash = '#/lift/BENCH'
    show()
    await opened()
    expect(screen.getByRole('heading', { level: 1, name: 'Bench history' })).toBeInTheDocument()
  })

  it('renders the lift page for #/lift/STRICT_PRESS', async () => {
    window.location.hash = '#/lift/STRICT_PRESS'
    show()
    await opened()
    expect(screen.getByRole('heading', { level: 1, name: 'Strict press history' })).toBeInTheDocument()
    expect(await screen.findByText('No strict press sessions yet')).toBeInTheDocument()
  })

  it('renders settings and reacts to hash changes', async () => {
    show()
    await opened()
    setHash('#/settings')
    expect(screen.getByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
    setHash('#/progress')
    expect(screen.getByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
  })

  it('falls back to Today for unknown routes', async () => {
    window.location.hash = '#/lift/CURL'
    show()
    await opened()
    expect(screen.getByRole('heading', { level: 1, name: 'Today' })).toBeInTheDocument()
  })

  it('navigate() updates the hash and the screen', async () => {
    show()
    await opened()
    act(() => navigate({ page: 'lift', lift: 'DEADLIFT' }))
    expect(window.location.hash).toBe('#/lift/DEADLIFT')
    expect(await screen.findByRole('heading', { level: 1, name: 'Deadlift history' })).toBeInTheDocument()
  })
})
