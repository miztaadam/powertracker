import { afterEach, describe, expect, it, vi } from 'vitest'
import { NAMING_CLASS, SLIDE_WAIT_MS, afterTransition, slideNextChange, withTransition } from './viewTransition'

type Started = { update: () => void; skipTransition: ReturnType<typeof vi.fn> }

/** A stand-in for document.startViewTransition that holds each update until the test runs it, as a browser holds it for a frame. */
function fakeTransitions() {
  const started: Started[] = []
  const start = vi.fn((update: () => void) => {
    const entry: Started = { update, skipTransition: vi.fn() }
    started.push(entry)
    const settled = Promise.resolve()
    return { skipTransition: entry.skipTransition, ready: settled, finished: settled, updateCallbackDone: settled }
  })
  Object.defineProperty(document, 'startViewTransition', { value: start, configurable: true, writable: true })
  return { start, started }
}

afterEach(() => {
  delete (document as { startViewTransition?: unknown }).startViewTransition
  document.documentElement.removeAttribute('data-motion')
  document.documentElement.classList.remove(NAMING_CLASS)
})

describe('withTransition', () => {
  it('runs the update at once where the browser has no view transitions', () => {
    expect('startViewTransition' in document).toBe(false)
    const update = vi.fn()
    withTransition(update)
    expect(update).toHaveBeenCalledTimes(1)
  })

  it('runs the update at once, with no transition, while motion is off', () => {
    const { start } = fakeTransitions()
    document.documentElement.setAttribute('data-motion', 'off')
    const update = vi.fn()
    withTransition(update)
    expect(update).toHaveBeenCalledTimes(1)
    expect(start).not.toHaveBeenCalled()
  })

  it('runs the update inside a view transition while motion is on', () => {
    const { start, started } = fakeTransitions()
    const update = vi.fn()
    withTransition(update)
    expect(start).toHaveBeenCalledTimes(1)
    expect(update).not.toHaveBeenCalled()
    started[0]!.update()
    expect(update).toHaveBeenCalledTimes(1)
  })

  it('skips a transition still running before it starts the next, and loses no update', () => {
    const { start, started } = fakeTransitions()
    const first = vi.fn()
    const second = vi.fn()
    withTransition(first)
    withTransition(second)
    expect(start).toHaveBeenCalledTimes(2)
    expect(started[0]!.skipTransition).toHaveBeenCalledTimes(1)
    expect(started[1]!.skipTransition).not.toHaveBeenCalled()
    started[0]!.update()
    started[1]!.update()
    expect(first).toHaveBeenCalledTimes(1)
    expect(second).toHaveBeenCalledTimes(1)
  })

  it('skips nothing once the last transition has finished', async () => {
    const { started } = fakeTransitions()
    withTransition(vi.fn())
    await Promise.resolve()
    await Promise.resolve()
    withTransition(vi.fn())
    expect(started[0]!.skipTransition).not.toHaveBeenCalled()
  })

  it('still runs the update when the browser refuses to start a transition', () => {
    Object.defineProperty(document, 'startViewTransition', {
      value: () => {
        throw new Error('refused')
      },
      configurable: true,
      writable: true,
    })
    const update = vi.fn()
    withTransition(update)
    expect(update).toHaveBeenCalledTimes(1)
  })
})

describe('slideNextChange', () => {
  const ids = (value: number[]) => value.join()

  it('commits every value at once while nothing is armed', () => {
    const { start } = fakeTransitions()
    const commit = vi.fn()
    const slide = slideNextChange(commit, ids)
    slide.next([1])
    slide.next([1, 2])
    expect(commit.mock.calls).toEqual([[[1]], [[1, 2]]])
    expect(start).not.toHaveBeenCalled()
  })

  it('slides the first change of shape after arm(), once, and commits the newest value', () => {
    const { start, started } = fakeTransitions()
    const commit = vi.fn()
    const slide = slideNextChange(commit, ids)
    slide.next([1])
    slide.arm()
    slide.next([1]) // the same ids: lands at once, still armed
    expect(start).not.toHaveBeenCalled()
    slide.next([1, 2])
    expect(start).toHaveBeenCalledTimes(1)
    slide.next([1, 2, 3]) // arrives before the transition commits: lands at once
    expect(start).toHaveBeenCalledTimes(1)
    started[0]!.update()
    expect(commit.mock.calls.map(([v]) => ids(v as number[]))).toEqual(['1', '1', '1,2,3', '1,2,3'])
  })

  it('slides nothing after disarm(), or once the wait has lapsed', () => {
    const { start } = fakeTransitions()
    const slide = slideNextChange(vi.fn(), ids)
    slide.next([1])
    slide.arm()
    slide.disarm()
    slide.next([1, 2])
    const now = vi.spyOn(performance, 'now')
    slide.arm()
    now.mockReturnValue(performance.now() + SLIDE_WAIT_MS + 1)
    slide.next([1, 2, 3])
    now.mockRestore()
    expect(start).not.toHaveBeenCalled()
  })
})

describe('a press during a transition', () => {
  it('skips the transition, so the tap reaches the page, and stops listening once it has finished', async () => {
    const { started } = fakeTransitions()
    withTransition(vi.fn())
    document.dispatchEvent(new Event('pointerdown'))
    expect(started[0]!.skipTransition).toHaveBeenCalledTimes(1)
    await Promise.resolve()
    await Promise.resolve()
    await Promise.resolve()
    document.dispatchEvent(new Event('pointerdown'))
    expect(started[0]!.skipTransition).toHaveBeenCalledTimes(1)
  })
})

describe('afterTransition', () => {
  it('runs at once while no transition runs', () => {
    const work = vi.fn()
    afterTransition(work)
    expect(work).toHaveBeenCalledTimes(1)
  })

  it('waits for the transition running now to finish', async () => {
    let finish!: () => void
    const finished = new Promise<void>((done) => (finish = done))
    Object.defineProperty(document, 'startViewTransition', {
      value: () => ({ skipTransition: vi.fn(), ready: Promise.resolve(), updateCallbackDone: Promise.resolve(), finished }),
      configurable: true,
      writable: true,
    })
    withTransition(vi.fn())
    const work = vi.fn()
    afterTransition(work)
    await Promise.resolve()
    expect(work).not.toHaveBeenCalled()
    finish()
    await new Promise((done) => setTimeout(done, 0))
    expect(work).toHaveBeenCalledTimes(1)
  })
})

describe('the names', () => {
  it('are on only while a transition runs, and never with motion off', async () => {
    const root = document.documentElement
    let finish!: () => void
    const finished = new Promise<void>((done) => (finish = done))
    Object.defineProperty(document, 'startViewTransition', {
      value: () => ({ skipTransition: vi.fn(), ready: Promise.resolve(), updateCallbackDone: Promise.resolve(), finished }),
      configurable: true,
      writable: true,
    })
    root.setAttribute('data-motion', 'off')
    withTransition(vi.fn())
    expect(root.classList.contains(NAMING_CLASS)).toBe(false)
    root.removeAttribute('data-motion')
    withTransition(vi.fn())
    expect(root.classList.contains(NAMING_CLASS)).toBe(true)
    finish()
    await new Promise((done) => setTimeout(done, 0))
    expect(root.classList.contains(NAMING_CLASS)).toBe(false)
  })
})
