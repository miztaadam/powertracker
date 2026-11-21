import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { Menu } from './Menu'

// 1.17.1: a menu is placed when it opens. A change of the window's width, or turning the phone, closes it. A change of
// height only (the on-screen keyboard going away as the menu takes focus) keeps it open and places it again.
afterEach(cleanup)

const open = async () => {
  render(<Menu label="Options" items={[{ text: 'Skip today', onSelect: () => {} }, { text: 'Count plates on this machine', onSelect: () => {} }]} />)
  await userEvent.click(screen.getByRole('button', { name: 'Options' }))
  expect(screen.getByRole('menu')).toBeInTheDocument()
}

const setWindow = (width: number, height: number) => {
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: width })
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: height })
}

/** A stand-in visual viewport whose size the test sets. */
function fakeViewport(width: number, height: number) {
  const viewport = Object.assign(new EventTarget(), { width, height })
  Object.defineProperty(window, 'visualViewport', { configurable: true, value: viewport })
  return viewport
}

afterEach(() => {
  setWindow(1024, 768)
  Object.defineProperty(window, 'visualViewport', { configurable: true, value: undefined })
})

describe('Menu and a change of the window', () => {
  it('a change of the window width closes it', async () => {
    setWindow(390, 844)
    await open()
    act(() => {
      setWindow(844, 844)
      window.dispatchEvent(new Event('resize'))
    })
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('turning the phone closes it', async () => {
    await open()
    act(() => {
      window.dispatchEvent(new Event('orientationchange'))
    })
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('a change of the window height only keeps it open, with focus in it', async () => {
    setWindow(390, 500)
    await open()
    const focused = document.activeElement
    act(() => {
      setWindow(390, 844)
      window.dispatchEvent(new Event('resize'))
    })
    expect(screen.getByRole('menu')).toBeInTheDocument()
    expect(document.activeElement).toBe(focused)
    expect(screen.getByRole('menu').contains(document.activeElement)).toBe(true)
  })

  it('a change of the visual viewport height only (the keyboard) keeps it open', async () => {
    const viewport = fakeViewport(390, 500)
    await open()
    act(() => {
      viewport.height = 844
      viewport.dispatchEvent(new Event('resize'))
    })
    expect(screen.getByRole('menu')).toBeInTheDocument()
    expect(screen.getByRole('menu').contains(document.activeElement)).toBe(true)
  })

  it('a change of the visual viewport width closes it', async () => {
    const viewport = fakeViewport(390, 844)
    await open()
    act(() => {
      viewport.width = 300
      viewport.dispatchEvent(new Event('resize'))
    })
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('a scroll of the visual viewport alone leaves it open', async () => {
    const viewport = fakeViewport(390, 844)
    await open()
    act(() => {
      viewport.dispatchEvent(new Event('scroll'))
    })
    expect(screen.getByRole('menu')).toBeInTheDocument()
  })
})
