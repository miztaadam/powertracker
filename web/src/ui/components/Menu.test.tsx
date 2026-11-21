import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { Menu } from './Menu'

// 1.17.1: a menu is placed once, when it opens. A change of the window's size (a rotation, the on-screen keyboard)
// closes it, so it can never be left where it no longer fits.
afterEach(cleanup)

const open = async () => {
  render(<Menu label="Options" items={[{ text: 'Skip today', onSelect: () => {} }]} />)
  await userEvent.click(screen.getByRole('button', { name: 'Options' }))
  expect(screen.getByRole('menu')).toBeInTheDocument()
}

describe('Menu closes when the window changes size', () => {
  for (const event of ['resize', 'orientationchange'] as const) {
    it(`on ${event}`, async () => {
      await open()
      act(() => {
        window.dispatchEvent(new Event(event))
      })
      expect(screen.queryByRole('menu')).toBeNull()
    })
  }

  it('on a resize of the visual viewport (the on-screen keyboard)', async () => {
    const viewport = new EventTarget()
    Object.defineProperty(window, 'visualViewport', { configurable: true, value: viewport })
    try {
      await open()
      act(() => {
        viewport.dispatchEvent(new Event('resize'))
      })
      expect(screen.queryByRole('menu')).toBeNull()
    } finally {
      Object.defineProperty(window, 'visualViewport', { configurable: true, value: undefined })
    }
  })

  it('a scroll of the visual viewport alone leaves it open', async () => {
    const viewport = new EventTarget()
    Object.defineProperty(window, 'visualViewport', { configurable: true, value: viewport })
    try {
      await open()
      act(() => {
        viewport.dispatchEvent(new Event('scroll'))
      })
      expect(screen.getByRole('menu')).toBeInTheDocument()
    } finally {
      Object.defineProperty(window, 'visualViewport', { configurable: true, value: undefined })
    }
  })
})
