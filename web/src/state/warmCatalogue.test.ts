import { afterEach, describe, expect, it, vi } from 'vitest'

// The catalogue warm-up main.tsx starts: once, right after the first paint, the catalogue only.

/** A fresh domain and a fresh catalogue cache, as at start-up, with the next frame handed out by the test. */
async function startUp() {
  vi.resetModules()
  const grit = (await import('../domain')).grit
  const spies = {
    library: vi.spyOn(grit, 'library'),
    search: vi.spyOn(grit, 'searchLibrary'),
    figure: [vi.spyOn(grit, 'figureInfo'), vi.spyOn(grit, 'figureShapes'), vi.spyOn(grit, 'hasFigure')],
  }
  const frames: FrameRequestCallback[] = []
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => frames.push(callback))
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  const picker = await import('./exercisePicker')
  return { grit, spies, frames, picker }
}

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('catalogue warm-up', () => {
  it('warmCatalogue_runsOnceRightAfterTheFirstPaint_andBuildsNoFigure', async () => {
    const { spies, frames, picker } = await startUp()
    picker.warmCatalogue()
    // Nothing before the first frame is painted ...
    expect(spies.library).not.toHaveBeenCalled()
    expect(frames).toHaveLength(1)
    frames.shift()!(0)
    // ... nor in the frame itself: it waits for the task after it, so the paint is not held up.
    expect(spies.library).not.toHaveBeenCalled()
    await vi.runAllTimersAsync()
    // Then the catalogue, and the full list the picker and the Exercises tab open on, once each.
    expect(spies.library).toHaveBeenCalledTimes(1)
    expect(spies.search).toHaveBeenCalledTimes(1)
    expect(spies.search).toHaveBeenCalledWith('', [], [])
    // No more frames or tasks: it runs once.
    expect(frames).toHaveLength(0)
    expect(vi.getTimerCount()).toBe(0)
    // The catalogue only: no figure is looked up, built or drawn.
    for (const spy of spies.figure) expect(spy).not.toHaveBeenCalled()
    // The picker's cache is what was warmed: asking again does not ask the domain.
    expect(picker.library()).toHaveLength(225)
    expect(spies.library).toHaveBeenCalledTimes(1)
  })

  it('warmCatalogue_aFailure_isSwallowed_andThePickerStillWorksItOutLater', async () => {
    const { spies, frames, picker } = await startUp()
    spies.library.mockImplementationOnce(() => {
      throw new Error('broken catalogue')
    })
    picker.warmCatalogue()
    frames.shift()!(0)
    await expect(vi.runAllTimersAsync()).resolves.not.toThrow()
    // It was only a head start: the picker asks again on open, and gets the catalogue.
    expect(picker.library()).toHaveLength(225)
    expect(spies.library).toHaveBeenCalledTimes(2)
  })
})
