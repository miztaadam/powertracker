import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode, useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Dialog } from './Dialog'
import { Menu } from './Menu'

// jsdom has no CloseWatcher, so every other Dialog and Menu test runs the Escape keydown path. These stub one in, as
// Chromium has it (1.17.4): a close request fires `close` on the newest live watcher, which is then spent.

class FakeWatcher {
  static made: FakeWatcher[] = []
  destroyed = false
  spent = false
  private listeners: (() => void)[] = []
  constructor() {
    FakeWatcher.made.push(this)
  }
  addEventListener(type: string, listener: () => void) {
    if (type === 'close') this.listeners.push(listener)
  }
  destroy() {
    this.destroyed = true
  }
  /** What the browser does on a close request (Back, Escape) for this watcher. */
  close() {
    if (this.destroyed || this.spent) return
    this.spent = true
    this.listeners.forEach((l) => l())
  }
  static live() {
    return FakeWatcher.made.filter((w) => !w.destroyed && !w.spent)
  }
}

/** A close request, as Back on a phone sends it: to the newest live watcher. */
function backPress() {
  act(() => FakeWatcher.live().at(-1)?.close())
}

let addSpy: { mock: { calls: unknown[][] }; mockRestore(): void }

beforeEach(() => {
  FakeWatcher.made = []
  vi.stubGlobal('CloseWatcher', FakeWatcher)
  addSpy = vi.spyOn(document, 'addEventListener')
})

afterEach(() => {
  vi.unstubAllGlobals()
  addSpy.mockRestore()
})

const keydownListeners = () => addSpy.mock.calls.filter((call) => call[0] === 'keydown').length

describe('Dialog with CloseWatcher', () => {
  it('makes one watcher on open and destroys it on close', () => {
    const { rerender } = render(<Dialog open title="Q" onDismiss={() => {}} />)
    expect(FakeWatcher.made).toHaveLength(1)
    expect(FakeWatcher.live()).toHaveLength(1)
    rerender(<Dialog open={false} title="Q" onDismiss={() => {}} />)
    expect(FakeWatcher.made[0]?.destroyed).toBe(true)
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('destroys its watcher on unmount', () => {
    const { unmount } = render(<Dialog open title="Q" onDismiss={() => {}} />)
    unmount()
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('dismisses once on a close request, with no Escape keydown listener as well', async () => {
    const onDismiss = vi.fn()
    render(<Dialog open title="Q" onDismiss={onDismiss} />)
    expect(keydownListeners()).toBe(0)
    backPress()
    expect(onDismiss).toHaveBeenCalledTimes(1)
    // Escape alone does nothing here: in the browser it is itself a close request.
    await userEvent.keyboard('{Escape}')
    expect(onDismiss).toHaveBeenCalledTimes(1)
  })

  it('gets a fresh watcher when a dismiss leaves it open', () => {
    const onDismiss = vi.fn()
    render(<Dialog open title="Q" onDismiss={onDismiss} />)
    backPress()
    expect(onDismiss).toHaveBeenCalledTimes(1)
    expect(FakeWatcher.made).toHaveLength(2)
    expect(FakeWatcher.live()).toHaveLength(1)
    backPress()
    expect(onDismiss).toHaveBeenCalledTimes(2)
    expect(FakeWatcher.live()).toHaveLength(1)
  })

  it('with two open, dismisses only the newer, and the next press the older; one live watcher throughout', () => {
    function Two() {
      const [outer, setOuter] = useState(true)
      const [inner, setInner] = useState(true)
      return (
        <>
          <Dialog open={outer} title="Outer" onDismiss={() => setOuter(false)} />
          <Dialog open={inner} title="Inner" onDismiss={() => setInner(false)} portal />
        </>
      )
    }
    render(<Two />)
    expect(FakeWatcher.live()).toHaveLength(1)
    backPress()
    expect(screen.queryByRole('dialog', { name: 'Inner' })).toBeNull()
    expect(screen.getByRole('dialog', { name: 'Outer' })).toBeTruthy()
    expect(FakeWatcher.live()).toHaveLength(1)
    backPress()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('a dismiss that steps back inside the dialog keeps it open and the next press closes it', () => {
    function Stepping() {
      const [open, setOpen] = useState(true)
      const [step, setStep] = useState(1)
      return <Dialog open={open} title={`Step ${step}`} onDismiss={() => (step > 0 ? setStep(step - 1) : setOpen(false))} />
    }
    render(<Stepping />)
    backPress()
    expect(screen.getByRole('dialog', { name: 'Step 0' })).toBeTruthy()
    backPress()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('leaks no watcher under StrictMode', () => {
    const { rerender } = render(
      <StrictMode>
        <Dialog open title="Q" onDismiss={() => {}} />
      </StrictMode>,
    )
    expect(FakeWatcher.live()).toHaveLength(1)
    rerender(
      <StrictMode>
        <Dialog open={false} title="Q" onDismiss={() => {}} />
      </StrictMode>,
    )
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('gives the focus back to its opener after a close request', () => {
    function Opener() {
      const [open, setOpen] = useState(false)
      return (
        <>
          <button onClick={() => setOpen(true)}>Open</button>
          <Dialog open={open} title="Q" onDismiss={() => setOpen(false)} />
        </>
      )
    }
    render(<Opener />)
    const button = screen.getByRole('button', { name: 'Open' })
    button.focus()
    fireEvent.click(button)
    backPress()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(document.activeElement).toBe(button)
  })
})

describe('Menu with CloseWatcher', () => {
  const items = [{ text: 'Rename', onSelect: () => {} }]

  it('a close request closes the open menu and gives the focus back to its button', async () => {
    render(<Menu label="More" items={items} />)
    expect(FakeWatcher.made).toHaveLength(0)
    await userEvent.click(screen.getByRole('button', { name: 'More' }))
    expect(screen.getByRole('menu')).toBeTruthy()
    expect(FakeWatcher.live()).toHaveLength(1)
    backPress()
    expect(screen.queryByRole('menu')).toBeNull()
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'More' }))
    expect(FakeWatcher.live()).toHaveLength(0)
  })

  it('does not handle Escape itself as well (the browser sends it as a close request)', async () => {
    render(<Menu label="More" items={items} />)
    await userEvent.click(screen.getByRole('button', { name: 'More' }))
    await userEvent.keyboard('{Escape}')
    expect(screen.getByRole('menu')).toBeTruthy()
  })

  it('a menu item that opens a dialog: the menu lets go, the dialog takes the close request', async () => {
    function WithDialog() {
      const [open, setOpen] = useState(false)
      return (
        <>
          <Menu label="More" items={[{ text: 'Rename', onSelect: () => setOpen(true) }]} />
          <Dialog open={open} title="Rename" onDismiss={() => setOpen(false)} />
        </>
      )
    }
    render(<WithDialog />)
    await userEvent.click(screen.getByRole('button', { name: 'More' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Rename' }))
    expect(screen.queryByRole('menu')).toBeNull()
    expect(screen.getByRole('dialog', { name: 'Rename' })).toBeTruthy()
    expect(FakeWatcher.live()).toHaveLength(1)
    backPress()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(FakeWatcher.live()).toHaveLength(0)
  })
})

describe('on an iPhone or iPad, even with CloseWatcher', () => {
  it('is not used: Dialog keeps its Escape keydown, and no watcher is made (Back goes through historyCloses.ts there)', async () => {
    const ua = vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148')
    const onDismiss = vi.fn()
    render(<Dialog open title="Q" onDismiss={onDismiss} />)
    expect(FakeWatcher.made).toHaveLength(0)
    expect(keydownListeners()).toBe(1)
    await userEvent.keyboard('{Escape}')
    expect(onDismiss).toHaveBeenCalledTimes(1)
    ua.mockRestore()
  })
})
