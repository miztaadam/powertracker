import { act, cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Menu } from './Menu'

// 1.17.1: a menu is placed when it opens, and only then brought into view and focused. A change of the layout
// viewport's width, or turning the phone, closes it and gives the focus back to its button. A change of height only (the
// on-screen keyboard), or of the visual viewport alone (pinch zoom), keeps it open and places it again.
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

/** A stand-in visual viewport whose size and scale the test sets. */
function fakeViewport(width: number, height: number) {
  const viewport = Object.assign(new EventTarget(), { width, height, scale: 1 })
  Object.defineProperty(window, 'visualViewport', { configurable: true, value: viewport })
  return viewport
}

afterEach(() => {
  setWindow(1024, 768)
  Object.defineProperty(window, 'visualViewport', { configurable: true, value: undefined })
  vi.restoreAllMocks()
})

describe('Menu and a change of the window', () => {
  it('a change of the window width closes it and gives the focus back to its button', async () => {
    setWindow(390, 844)
    await open()
    act(() => {
      setWindow(844, 844)
      window.dispatchEvent(new Event('resize'))
    })
    expect(screen.queryByRole('menu')).toBeNull()
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Options' }))
  })

  it('turning the phone closes it and gives the focus back to its button', async () => {
    await open()
    act(() => {
      window.dispatchEvent(new Event('orientationchange'))
    })
    expect(screen.queryByRole('menu')).toBeNull()
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Options' }))
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

  it('pinch zoom (the visual viewport narrower, the window not) keeps it open', async () => {
    setWindow(390, 844)
    const viewport = fakeViewport(390, 844)
    await open()
    act(() => {
      viewport.scale = 2
      viewport.width = 195
      viewport.height = 422
      viewport.dispatchEvent(new Event('resize'))
    })
    expect(screen.getByRole('menu')).toBeInTheDocument()
  })

  it('a scroll of the visual viewport alone leaves it open', async () => {
    const viewport = fakeViewport(390, 844)
    await open()
    act(() => {
      viewport.dispatchEvent(new Event('scroll'))
    })
    expect(screen.getByRole('menu')).toBeInTheDocument()
  })

  it('every listener it adds while open is removed when it closes', async () => {
    const viewport = fakeViewport(390, 844)
    const added: [EventTarget, string, unknown][] = []
    const removed: [EventTarget, string, unknown][] = []
    for (const target of [window, document, viewport] as EventTarget[]) {
      const add = target.addEventListener.bind(target)
      const remove = target.removeEventListener.bind(target)
      vi.spyOn(target, 'addEventListener').mockImplementation((type, fn, options) => {
        added.push([target, type, fn])
        add(type, fn, options)
      })
      vi.spyOn(target, 'removeEventListener').mockImplementation((type, fn, options) => {
        removed.push([target, type, fn])
        remove(type, fn, options)
      })
    }
    await open()
    const mine = added.filter(([, type]) => ['resize', 'orientationchange', 'pointerdown'].includes(type))
    expect(mine.length).toBeGreaterThanOrEqual(4)
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).toBeNull()
    for (const [target, type, fn] of mine) expect(removed.some(([t, ty, f]) => t === target && ty === type && f === fn), `${type} removed`).toBe(true)
  })
})

describe('Menu is brought into view and focused only once it is placed', () => {
  it('when it opens above its button, the focus lands on a menu already placed above', async () => {
    setWindow(390, 400)
    // The button near the bottom of the window; the menu 100 px tall: no room under it, room above.
    const rect = (top: number, height: number) => ({ left: 300, right: 348, top, bottom: top + height, width: 48, height, x: 300, y: top, toJSON: () => ({}) }) as DOMRect
    const realRect = HTMLElement.prototype.getBoundingClientRect
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
      return this.getAttribute('aria-haspopup') === 'menu' ? rect(340, 48) : realRect.call(this)
    })
    for (const key of ['offsetHeight', 'scrollHeight'] as const)
      vi.spyOn(HTMLElement.prototype, key, 'get').mockImplementation(function (this: HTMLElement) {
        return this.getAttribute('role') === 'menu' ? 100 : 0
      })
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockImplementation(function (this: HTMLElement) {
      return this.getAttribute('role') === 'menu' ? 168 : 0
    })
    const seen: string[] = []
    const realFocus = HTMLElement.prototype.focus
    vi.spyOn(HTMLElement.prototype, 'focus').mockImplementation(function (this: HTMLElement, options?: FocusOptions) {
      if (this.getAttribute('role') === 'menuitem') seen.push(this.closest('[role="menu"]')!.className)
      realFocus.call(this, options)
    })
    await open()
    expect(seen.length).toBeGreaterThan(0)
    for (const className of seen) expect(className).toContain('gr-menu-list-up')
  })
})

// 1.23.2: something drawn late above the button (the "Install Grit" card, a moment after the plan) pushes the button down
// under an open menu. The menu is placed again for where its button is now; the lifter's own scrolling is not such a move.
describe('Menu follows its button when the page under it shifts', () => {
  const frames = (count = 2) =>
    act(async () => {
      for (let i = 0; i < count; i++) await new Promise((done) => requestAnimationFrame(done))
    })

  /** A window 400 px high, a menu 100 px tall, and a ⋮ whose top the test moves. */
  function stage(top: number) {
    setWindow(390, 400)
    const at = { top }
    const realRect = HTMLElement.prototype.getBoundingClientRect
    const reads = { button: 0 }
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
      if (this.getAttribute('aria-haspopup') !== 'menu') return realRect.call(this)
      reads.button++
      return { left: 300, right: 348, top: at.top, bottom: at.top + 48, width: 48, height: 48, x: 300, y: at.top, toJSON: () => ({}) } as DOMRect
    })
    for (const key of ['offsetHeight', 'scrollHeight'] as const)
      vi.spyOn(HTMLElement.prototype, key, 'get').mockImplementation(function (this: HTMLElement) {
        return this.getAttribute('role') === 'menu' ? 100 : 0
      })
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockImplementation(function (this: HTMLElement) {
      return this.getAttribute('role') === 'menu' ? 168 : 0
    })
    return { at, reads }
  }

  it('pushed down to where it has no room under it, it opens above instead, and the focus stays where it was', async () => {
    const { at } = stage(40)
    await open()
    expect(screen.getByRole('menu').className).not.toContain('gr-menu-list-up')
    const focused = document.activeElement
    expect(screen.getByRole('menu').contains(focused)).toBe(true)
    at.top = 340
    await frames()
    expect(screen.getByRole('menu').className).toContain('gr-menu-list-up')
    expect(document.activeElement).toBe(focused)
  })

  it('pushed back up, it opens under its button again', async () => {
    const { at } = stage(340)
    await open()
    expect(screen.getByRole('menu').className).toContain('gr-menu-list-up')
    at.top = 40
    await frames()
    expect(screen.getByRole('menu').className).not.toContain('gr-menu-list-up')
  })

  it("the lifter's own scrolling is not a move: the menu is left as it was placed", async () => {
    const { at } = stage(40)
    const main = document.createElement('main')
    main.className = 'shell-main'
    document.body.append(main)
    try {
      main.scrollTop = 300
      render(<Menu label="Options" items={[{ text: 'Skip today', onSelect: () => {} }]} />, { container: main })
      await userEvent.click(screen.getByRole('button', { name: 'Options' }))
      // Scrolled up by 300 px: the button is 300 px lower in the window, at the same place on its screen.
      main.scrollTop = 0
      at.top = 340
      await frames()
      expect(screen.getByRole('menu').className).not.toContain('gr-menu-list-up')
    } finally {
      cleanup()
      main.remove()
    }
  })

  it('stops looking at its button once it is closed', async () => {
    const { reads } = stage(40)
    await open()
    await frames()
    expect(reads.button).toBeGreaterThan(1)
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).toBeNull()
    const after = reads.button
    await frames(3)
    expect(reads.button).toBe(after)
  })
})
