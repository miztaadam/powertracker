import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { grit } from '../../domain'
import { SettingsContent } from '../../screens/settings/SettingsScreen'
import { DEFAULT_SETTINGS } from '../../db/settings'
import { ModeSwitchOverlay } from './ModeSwitchOverlay'
import { createModeSwitch, ModeSwitchContext, SKIP_TICK_MS, usePrefetchModeSwitchSounds, type ModeSwitchController, type ModeSwitchFeedback, type ModeSwitchPainter } from './useModeSwitch'
import { buzz, dropBuzzWithoutMotion, pumpBuzzWithoutMotion } from './modeSwitchFeedback'

const DROP = grit.modeSwitchDropTimeline()
const PUMP = grit.modeSwitchPumpTimeline()
const VIBES = grit.modeSwitchVibration()

/** A frame clock the test turns by hand: each `frames(ms)` runs one animation frame every 16 ms. */
function fakeClock() {
  let now = 1000
  let queue: { id: number; cb: FrameRequestCallback }[] = []
  let next = 1
  return {
    now: () => now,
    raf: (cb: FrameRequestCallback) => {
      queue.push({ id: next, cb })
      return next++
    },
    caf: (id: number) => {
      queue = queue.filter((f) => f.id !== id)
    },
    /** Runs frames until `ms` have passed. */
    frames(ms: number) {
      const until = now + ms
      while (now + 16 <= until) {
        now += 16
        const run = queue
        queue = []
        for (const f of run) f.cb(now)
      }
      now = until
    },
    /** Time passes with no frame (a stalled main thread). */
    stall(ms: number) {
      now += ms
    },
    pending: () => queue.length,
  }
}

function fakeFeedback(): ModeSwitchFeedback & { calls: string[] } {
  const calls: string[] = []
  return {
    calls,
    buzz: (p) => calls.push(`buzz ${JSON.stringify(p)}`),
    unlock: () => calls.push('unlock'),
    play: (kind, tied) => calls.push(`play ${kind} ${tied ? 'tied' : 'free'}`),
    follow: () => {},
    stop: () => calls.push('stop'),
  }
}

let clock: ReturnType<typeof fakeClock>
let feedback: ReturnType<typeof fakeFeedback>
let motionOff = false
let controller: ModeSwitchController

function setup() {
  clock = fakeClock()
  feedback = fakeFeedback()
  controller = createModeSwitch({
    now: clock.now,
    raf: clock.raf,
    caf: clock.caf,
    motionOff: () => motionOff,
    feedback,
  })
  return controller
}

/** The switch the animation starts from, at the top left as in Settings. */
function switchElement() {
  const button = document.createElement('button')
  button.getBoundingClientRect = () =>
    ({
      left: 300,
      top: 120,
      right: 352,
      bottom: 152,
      width: 52,
      height: 32,
      x: 300,
      y: 120,
      toJSON: () => ({}),
    }) as DOMRect
  document.body.append(button)
  return button
}

beforeEach(() => {
  motionOff = false
  setup()
})
afterEach(() => {
  controller.finish('leave')
  cleanup()
  document.body.innerHTML = ''
  delete document.documentElement.dataset.mode
  vi.restoreAllMocks()
})

describe('the shown side', () => {
  it('drop_theShownSideLagsUntilTheImpact', () => {
    controller.flip('drop', switchElement())
    expect(controller.state().kind).toBe('drop')
    // The old side shows from the tap…
    expect(controller.state().shown).toBe('BODYBUILDING')
    clock.frames(DROP.impact - 10)
    expect(controller.state().shown).toBe('BODYBUILDING')
    // …and the new one from the impact.
    clock.frames(20)
    expect(controller.state().shown).toBe('POWERLIFTING')
    expect(document.documentElement.dataset.mode).toBeUndefined()
    clock.frames(DROP.end)
    expect(controller.state().kind).toBeNull()
    expect(clock.pending()).toBe(0)
  })

  it('pump_theShownSideLagsUntilTheThirdBeat', () => {
    controller.flip('pump', switchElement())
    expect(controller.state().shown).toBe('POWERLIFTING')
    clock.frames(PUMP.beats[2]! - 10)
    expect(controller.state().shown).toBe('POWERLIFTING')
    clock.frames(20)
    expect(controller.state().shown).toBe('BODYBUILDING')
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
  })

  it('afterTheEnd_theShownSideFollowsTheStoredSide', () => {
    controller.noteStored('POWERLIFTING')
    controller.flip('drop', switchElement())
    clock.frames(DROP.end + 50)
    expect(controller.state()).toEqual({ kind: null, shown: null })
  })

  it('aWriteStillOnItsWay_holdsTheNewSideUntilItLands', () => {
    controller.noteStored('BODYBUILDING')
    controller.flip('drop', switchElement())
    clock.frames(DROP.end + 50)
    expect(controller.state().shown).toBe('POWERLIFTING')
    controller.noteStored('POWERLIFTING')
    expect(controller.state().shown).toBeNull()
  })

  it('aWriteThatFailsMidAnimation_letsTheStoredSideBackInAtTheEnd', () => {
    controller.noteStored('BODYBUILDING')
    controller.flip('drop', switchElement())
    clock.frames(20)
    controller.release()
    clock.frames(DROP.end + 50)
    expect(controller.state()).toEqual({ kind: null, shown: null })
  })

  it('aFailedWrite_letsTheStoredSideBackIn', () => {
    controller.noteStored('BODYBUILDING')
    controller.flip('drop', switchElement())
    clock.frames(DROP.end + 50)
    controller.release()
    expect(controller.state().shown).toBeNull()
  })
})

describe('the clock', () => {
  /** A painter that notes the time of each frame it is asked to draw. */
  function recordingPainter() {
    const painted: number[] = []
    const painter: ModeSwitchPainter = {
      begin() {},
      paint: (t) => painted.push(t),
      reveal: (swap) => void swap(),
      end() {},
    }
    controller.setPainter(painter)
    return painted
  }

  it('theFirstFrame_isTheTimeSinceTheTap', () => {
    const painted = recordingPainter()
    controller.flip('pump', switchElement())
    // The first frame comes late: 40 ms of nothing, then the frame 16 ms on. The picture is where the sound and the
    // vibration are, which started at the tap.
    clock.stall(40)
    clock.frames(16)
    expect(painted).toEqual([56])
  })

  it('aLateFrame_jumpsAhead_withNoCap', () => {
    const painted = recordingPainter()
    controller.flip('drop', switchElement())
    clock.frames(32)
    clock.stall(300)
    clock.frames(16)
    expect(painted).toEqual([16, 32, 348])
    // The impact passed in that jump: the new side, and its buzz, are there.
    expect(controller.state().shown).toBe('POWERLIFTING')
    expect(feedback.calls).toContain(`buzz ${JSON.stringify(VIBES.dropPattern)}`)
  })
})

describe('vibration and sound', () => {
  it('drop_vibratesAtTheImpact_andPlaysItsSoundFromTheTap', () => {
    controller.flip('drop', switchElement())
    expect(feedback.calls).toEqual(['unlock', 'play drop tied'])
    clock.frames(DROP.impact - 10)
    expect(feedback.calls.some((c) => c.startsWith('buzz'))).toBe(false)
    clock.frames(20)
    expect(feedback.calls).toContain(`buzz ${JSON.stringify(VIBES.dropPattern)}`)
  })

  it('pump_givesOneBeatAtEachOnset', () => {
    controller.flip('pump', switchElement())
    clock.frames(PUMP.end + 50)
    expect(feedback.calls.filter((c) => c.startsWith('buzz'))).toEqual(VIBES.pumpPulses.map((p) => `buzz ${p}`))
  })

  it('reducedMotion_noAnimation_butTheVibrationAndTheSound', () => {
    motionOff = true
    controller.flip('drop', switchElement())
    expect(controller.state()).toEqual({ kind: null, shown: null })
    expect(clock.pending()).toBe(0)
    expect(feedback.calls).toEqual(['unlock', 'play drop free', `buzz ${JSON.stringify(dropBuzzWithoutMotion())}`])
    feedback.calls.length = 0
    controller.flip('pump', switchElement())
    expect(feedback.calls).toEqual(['unlock', 'play pump free', `buzz ${JSON.stringify(pumpBuzzWithoutMotion())}`])
  })

  it('reducedMotion_leavingStopsTheSoundAndTheBuzz', () => {
    motionOff = true
    controller.flip('pump', switchElement())
    feedback.calls.length = 0
    controller.finish('leave')
    expect(feedback.calls).toEqual(['stop', 'buzz 0'])
    feedback.calls.length = 0
    controller.finish('leave')
    expect(feedback.calls).toEqual([])
  })

  it('reducedMotion_aHiddenPageStopsTheSoundAndTheBuzz', () => {
    motionOff = true
    controller.flip('drop', switchElement())
    feedback.calls.length = 0
    controller.finish('hidden')
    expect(feedback.calls).toEqual(['stop', 'buzz 0'])
  })

  it('settingsPrefetchesTheNextSwitchsSound_once_whenTheStoredSideIsKnown', () => {
    const prefetch = vi.fn()
    function Probe({ stored }: { stored: 'POWERLIFTING' | 'BODYBUILDING' | null }) {
      usePrefetchModeSwitchSounds(stored, prefetch)
      return null
    }
    const { rerender } = render(<Probe stored={null} />)
    expect(prefetch).not.toHaveBeenCalled()
    rerender(<Probe stored="BODYBUILDING" />)
    expect(prefetch).toHaveBeenCalledWith('drop')
    rerender(<Probe stored="POWERLIFTING" />)
    expect(prefetch).toHaveBeenCalledTimes(1)
    cleanup()
    render(<Probe stored="POWERLIFTING" />)
    expect(prefetch).toHaveBeenLastCalledWith('pump')
  })

  it('withoutMotion_theBuzzesLandWhereTheyWouldHave', () => {
    // The drop: still until the impact, then its pattern.
    expect(dropBuzzWithoutMotion()).toEqual([0, DROP.impact, ...VIBES.dropPattern])
    // The pump: each beat at its onset, as one pattern.
    const [b0, b1, b2] = PUMP.beats as [number, number, number]
    const [p0, p1, p2] = VIBES.pumpPulses as [number, number, number]
    expect(pumpBuzzWithoutMotion()).toEqual([0, b0, p0, b1 - b0 - p0, p1, b2 - b1 - p1, p2])
  })

  it('vibrationIgnoresVibrateOnTaps_andNeverThrows', () => {
    // "Vibrate on taps" is never read: the buzz goes straight to the browser.
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', {
      value: vibrate,
      configurable: true,
    })
    buzz([1, 2, 3])
    expect(vibrate).toHaveBeenCalledWith([1, 2, 3])
    Object.defineProperty(navigator, 'vibrate', {
      value: () => {
        throw new Error('not allowed')
      },
      configurable: true,
    })
    expect(() => buzz(40)).not.toThrow()
    Object.defineProperty(navigator, 'vibrate', {
      value: undefined,
      configurable: true,
    })
    expect(() => buzz(40)).not.toThrow()
  })
})

describe('ending early', () => {
  it('aTapSkipsToTheEnd_andTheTapDoesNothingElse', () => {
    const elsewhere = vi.fn()
    const other = document.createElement('button')
    other.addEventListener('click', elsewhere)
    document.body.append(other)
    controller.flip('drop', switchElement())
    clock.frames(100)
    other.dispatchEvent(
      new PointerEvent('pointerdown', {
        bubbles: true,
        clientX: 10,
        clientY: 10,
      }),
    )
    other.click()
    expect(elsewhere).not.toHaveBeenCalled()
    expect(controller.state().kind).toBeNull()
    expect(document.documentElement.dataset.mode).toBeUndefined()
    // The sound fades, the running vibration is cancelled, and one short tick answers the tap.
    expect(feedback.calls.slice(-3)).toEqual(['stop', 'buzz 0', `buzz ${SKIP_TICK_MS}`])
    expect(SKIP_TICK_MS).toBe(40)
    // The next tap is the lifter's again.
    other.click()
    expect(elsewhere).toHaveBeenCalledTimes(1)
  })

  it('aTapOnThePump_endsOnTheBlueSide', () => {
    controller.flip('pump', switchElement())
    clock.frames(200)
    document.body.dispatchEvent(
      new PointerEvent('pointerdown', {
        bubbles: true,
        clientX: 10,
        clientY: 10,
      }),
    )
    expect(controller.state().kind).toBeNull()
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
    // The same on the pump: cancelled, then the tick.
    expect(feedback.calls.slice(-3)).toEqual(['stop', 'buzz 0', `buzz ${SKIP_TICK_MS}`])
  })

  it('flippingAgain_skipsTheRunningOne_andPlaysTheNewOne', () => {
    const button = switchElement()
    controller.flip('drop', button)
    clock.frames(100)
    // A press on the switch is a flip, not a skip.
    button.dispatchEvent(
      new PointerEvent('pointerdown', {
        bubbles: true,
        clientX: 320,
        clientY: 130,
      }),
    )
    expect(controller.state().kind).toBe('drop')
    controller.flip('pump', button)
    expect(controller.state()).toEqual({ kind: 'pump', shown: 'POWERLIFTING' })
    expect(feedback.calls.filter((c) => c.startsWith('play'))).toEqual(['play drop tied', 'play pump tied'])
    expect(feedback.calls).toContain('buzz 0')
    expect(clock.pending()).toBe(1)
    clock.frames(PUMP.end + 50)
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
  })

  it('threeFlipsInASecond_endOnTheLastOne', () => {
    const button = switchElement()
    controller.flip('drop', button)
    clock.frames(150)
    controller.flip('pump', button)
    clock.frames(150)
    controller.flip('drop', button)
    clock.frames(DROP.end + 50)
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(feedback.calls.filter((c) => c.startsWith('play'))).toHaveLength(3)
    expect(clock.pending()).toBe(0)
  })

  it('leaving_endsCleanly_cancelsTheBuzz_andStopsTheSound', () => {
    controller.flip('drop', switchElement())
    clock.frames(100)
    controller.finish('leave')
    expect(controller.state().kind).toBeNull()
    expect(document.documentElement.dataset.mode).toBeUndefined()
    expect(feedback.calls.slice(-2)).toEqual(['stop', 'buzz 0'])
    expect(clock.pending()).toBe(0)
  })

  it('aHiddenPage_endsIt', () => {
    controller.flip('pump', switchElement())
    clock.frames(100)
    Object.defineProperty(document, 'visibilityState', {
      value: 'hidden',
      configurable: true,
    })
    document.dispatchEvent(new Event('visibilitychange'))
    Object.defineProperty(document, 'visibilityState', {
      value: 'visible',
      configurable: true,
    })
    expect(controller.state().kind).toBeNull()
    expect(document.documentElement.dataset.mode).toBe('bodybuilding')
    expect(feedback.calls).toContain('stop')
  })
})

describe('the overlay', () => {
  const show = () =>
    render(
      <ModeSwitchContext.Provider value={controller}>
        <div className="shell">
          <main className="shell-main">
            <section className="screen">
              <header className="gr-screen-header">
                <div className="gr-screen-header-text">
                  <h1 className="gr-screen-title">Settings</h1>
                </div>
              </header>
              <div className="screen-body" />
            </section>
          </main>
        </div>
        <ModeSwitchOverlay />
      </ModeSwitchContext.Provider>,
    )

  it('atRest_drawsNothing_andWorksNothingOut', () => {
    const cracks = vi.spyOn(grit, 'modeSwitchCracks')
    const veins = vi.spyOn(grit, 'modeSwitchVeins')
    const { container } = show()
    const overlay = container.querySelector('.ms-overlay')!
    expect(overlay).toHaveAttribute('aria-hidden', 'true')
    expect(overlay.childElementCount).toBe(0)
    expect(cracks).not.toHaveBeenCalled()
    expect(veins).not.toHaveBeenCalled()
  })

  it('drop_drawsTheHammer_hiddenFromAccessibility_andClearsAtTheEnd', () => {
    const { container } = show()
    act(() => controller.flip('drop', switchElement()))
    const overlay = container.querySelector('.ms-overlay')!
    // Built in the first frame after the tap, not in the tap.
    expect(overlay.childElementCount).toBe(0)
    act(() => clock.frames(16))
    expect(overlay).toHaveAttribute('aria-hidden', 'true')
    expect(overlay.querySelector('svg')).not.toBeNull()
    expect(overlay.querySelectorAll('.ms-crack').length).toBeGreaterThan(0)
    act(() => clock.frames(DROP.impact + 20))
    expect(Number(overlay.querySelector('.ms-flash')!.getAttribute('opacity'))).toBeGreaterThan(0.8)
    // The POWERLIFTING stamp appears at the impact, in the header below the title, silent to a screen reader.
    const stamp = container.querySelector('.ms-stamp')
    expect(stamp).toHaveTextContent('POWERLIFTING')
    expect(stamp).toHaveAttribute('aria-hidden', 'true')
    expect(stamp!.parentElement).toBe(container.querySelector('.gr-screen-header'))
    act(() => clock.frames(DROP.end))
    expect(overlay.childElementCount).toBe(0)
    expect(container.querySelector('.ms-stamp')).toBeNull()
    expect((container.querySelector('.shell') as HTMLElement).style.transform).toBe('')
  })

  it('pump_drawsTheVeinsFromTheSwitch', () => {
    const veins = vi.spyOn(grit, 'modeSwitchVeins')
    const { container } = show()
    act(() => controller.flip('pump', switchElement()))
    act(() => clock.frames(16))
    // The switch's centre, where the veins start.
    expect(veins).toHaveBeenCalledWith(expect.any(Number), expect.any(Number), 326, 136)
    act(() => clock.frames(PUMP.beats[2]! + 120))
    expect(container.querySelectorAll('.ms-overlay .ms-vein').length).toBeGreaterThan(0)
  })

  it('light_theFlashIsADarkPunchAtHalfTheOpacity', () => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query.includes('light'),
      addEventListener() {},
      removeEventListener() {},
    }))
    const { container } = show()
    act(() => controller.flip('drop', switchElement()))
    act(() => clock.frames(DROP.impact + 20))
    const flash = container.querySelector('.ms-flash')!
    expect(flash.getAttribute('fill')).toBe(grit.modeSwitchColors().flashLight)
    // The last frame ran at 16 ms steps: the impact plus 6 ms.
    const dark = grit.modeSwitchDropFrame(DROP.impact + 6, 390, 844).flashOpacity
    expect(Number(flash.getAttribute('opacity'))).toBeCloseTo(dark / 2, 3)
    vi.unstubAllGlobals()
  })

  it('reducedMotion_drawsNothing', () => {
    motionOff = true
    const { container } = show()
    act(() => controller.flip('drop', switchElement()))
    expect(container.querySelector('.ms-overlay')!.childElementCount).toBe(0)
  })
})

describe('Settings during an animation', () => {
  it('theSwitchShowsTheStoredSide_theRowsTheShownSide', () => {
    // Stored: bodybuilding (the tap has just turned it on); shown: still powerlifting.
    render(<SettingsContent settings={{ ...DEFAULT_SETTINGS, trainingMode: 'BODYBUILDING' }} shownMode="POWERLIFTING" onUpdate={() => {}} onAppLock={() => {}} onBodybuilding={() => {}} />)
    expect(screen.getByRole('switch', { name: 'Bodybuilding mode' })).toHaveAttribute('aria-checked', 'true')
    // The powerlifting rows are still there until the third beat.
    expect(screen.getByRole('switch', { name: 'Strict press' })).toBeInTheDocument()
  })
})
