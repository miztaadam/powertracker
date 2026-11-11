import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from './App'
import { navigate } from './router'

function setHash(hash: string) {
  act(() => {
    window.location.hash = hash
    window.dispatchEvent(new HashChangeEvent('hashchange'))
  })
}

describe('app shell routing', () => {
  beforeEach(() => {
    window.location.hash = ''
  })

  it('starts on Today with the tab bar', () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'Today' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Today' })).toHaveAttribute('aria-current', 'page')
  })

  it('switches tabs from the tab bar', async () => {
    render(<App />)
    await userEvent.click(screen.getByRole('link', { name: 'Coach' }))
    expect(window.location.hash).toBe('#/coach')
    expect(screen.getByRole('heading', { level: 1, name: 'Coach' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Plan' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Plan' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Plan' })).toHaveAttribute('aria-current', 'page')
  })

  it('renders the lift page for #/lift/BENCH', () => {
    window.location.hash = '#/lift/BENCH'
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'Bench history' })).toBeInTheDocument()
  })

  it('renders settings and reacts to hash changes', () => {
    render(<App />)
    setHash('#/settings')
    expect(screen.getByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
    setHash('#/progress')
    expect(screen.getByRole('heading', { level: 1, name: 'Progress' })).toBeInTheDocument()
  })

  it('falls back to Today for unknown routes', () => {
    window.location.hash = '#/lift/CURL'
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'Today' })).toBeInTheDocument()
  })

  it('navigate() updates the hash and the screen', async () => {
    render(<App />)
    act(() => navigate({ page: 'lift', lift: 'DEADLIFT' }))
    expect(window.location.hash).toBe('#/lift/DEADLIFT')
    expect(await screen.findByRole('heading', { level: 1, name: 'Deadlift history' })).toBeInTheDocument()
  })
})
