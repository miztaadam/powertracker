import { Profiler } from 'react'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { TrainingMode } from '../../db/schema'
import { grit } from '../../domain'
import { formatKg, formatRpe } from '../../ui/components/format'
import { setRowLabel } from '../../ui/components/SetRow'
import { SKIPPED_TODAY } from '../today/SkipToday'
import { BACK, EXAMPLE, GUIDE_LABEL, GuideScreen, NEXT, dotsLabel, heldGuideClock, heldGuideClockAtStep, pictureDescription, tickingGuideClock, type GuideClock, type GuideExit } from './GuideScreen'
import { guideTodayState } from './guideSampleState'

/** The guide's shell and its Today picture (1.16, Task 2), as Android's `GuideScreenTest`. Copy comes from the domain. */

const SIDES: TrainingMode[] = ['POWERLIFTING', 'BODYBUILDING']
const SCREENS = grit.guideScreens()

afterEach(() => {
  vi.useRealTimers()
  cleanup()
})

type Shown = { exits: GuideExit[]; rerender: (clock: GuideClock) => void; unmount: () => void }

function show({ mode = 'POWERLIFTING' as TrainingMode, motionOn = true, clock = heldGuideClock(0) as GuideClock, startAt = 'WELCOME' } = {}): Shown {
  const exits: GuideExit[] = []
  const ui = (c: GuideClock) => (
    <GuideScreen mode={mode} firstLaunch={false} hasPlan={false} motionOn={motionOn} onPickSide={() => {}} onClose={(exit) => exits.push(exit)} clock={c} startAt={startAt} />
  )
  const { rerender, unmount } = render(ui(clock))
  return { exits, rerender: (c) => rerender(ui(c)), unmount }
}

const title = (id: string, mode: TrainingMode = 'POWERLIFTING') => grit.guideScreen(id, mode).title
const picture = () => screen.getByTestId('guide-picture')

function expectOn(id: string, mode: TrainingMode = 'POWERLIFTING') {
  expect(screen.getByTestId('guide-title')).toHaveTextContent(title(id, mode))
  expect(screen.getByTestId('guide-dots')).toHaveAccessibleName(dotsLabel(SCREENS.indexOf(id), SCREENS.length))
}

const next = () => userEvent.click(screen.getByTestId('guide-next'))

/** The plan item of the lift screen 4 logs, on the day it draws. */
function liftItem(mode: TrainingMode) {
  const sample = grit.guideSampleTodaySet(mode)
  const lift = grit.guideSampleExercises(mode).find((e) => e.name === sample.exerciseId)!
  return grit.guideSampleTodayItems(mode).find((p) => p.exerciseId === lift.id)!
}

/** The lift's editor as the picture draws it: its first card's weight, reps and RPE fields. */
function drawnEditor() {
  const inPicture = within(picture())
  const value = (name: string) => (inPicture.getAllByRole('textbox', { name, hidden: true })[0] as HTMLInputElement).value
  return { weight: value('Weight, kilograms'), reps: value('Reps'), rpe: value('RPE') }
}

describe('the guide', () => {
  it('guide_showsEightDots_andMovesWithNextBackAndSwipe', async () => {
    show()
    expect(SCREENS).toHaveLength(8)
    expect(screen.getByTestId('guide-dots').querySelectorAll('.guide-dot')).toHaveLength(8)
    expectOn('WELCOME')
    await next()
    expectOn('SIDE')
    await next()
    expectOn('PLAN')
    await userEvent.click(screen.getByTestId('guide-back'))
    expectOn('SIDE')
    // A swipe that starts on the picture still turns the page.
    const pages = screen.getByTestId('guide-pages')
    fireEvent.pointerDown(picture(), { clientX: 300, clientY: 300, pointerId: 1, isPrimary: true })
    fireEvent.pointerUp(pages, { clientX: 100, clientY: 310, pointerId: 1, isPrimary: true })
    expectOn('PLAN')
    fireEvent.pointerDown(pages, { clientX: 100, clientY: 300, pointerId: 1, isPrimary: true })
    fireEvent.pointerUp(pages, { clientX: 300, clientY: 300, pointerId: 1, isPrimary: true })
    expectOn('SIDE')
  })

  it('guide_skipIsOnEveryScreen_andClosesWithSkipped', async () => {
    const { exits } = show()
    for (const [i, id] of SCREENS.entries()) {
      expectOn(id)
      expect(screen.getByTestId('guide-skip')).toBeVisible()
      if (i < SCREENS.length - 1) await next()
    }
    expect(exits).toEqual([])
    await userEvent.click(screen.getByTestId('guide-skip'))
    expect(exits).toEqual(['skipped'])
  })

  it('guide_systemBack_closesWithSkipped', async () => {
    const { exits } = show({ startAt: 'TODAY' })
    await userEvent.keyboard('{Escape}')
    expect(exits).toEqual(['skipped'])
  })

  it('guide_screen4_stepsThroughLoggingASet_onAHeldClock', () => {
    for (const mode of SIDES) {
      const { rerender } = show({ mode, startAt: 'TODAY' })
      const steps = grit.guideSteps('TODAY', mode)
      const sample = grit.guideSampleTodaySet(mode)
      const item = liftItem(mode)
      const done = setRowLabel({ index: 1, weightKg: sample.doneWeightKg, reps: sample.doneReps, rpe: sample.doneRpe, state: 'done' })
      for (const [i, step] of steps.entries()) {
        rerender(heldGuideClockAtStep(i))
        expect(picture()).toHaveAttribute('data-step', step)
        // What is drawn at this step: the lift's editor fills field by field, then the set is ticked, then Skip today.
        const at = (s: string) => i >= steps.indexOf(s)
        expect(drawnEditor(), `${mode} ${step}`).toEqual({
          weight: at('TODAY_WEIGHT') ? formatKg(sample.doneWeightKg) : '',
          reps: String(at('TODAY_REPS') ? sample.doneReps : item.reps),
          rpe: formatRpe(at('TODAY_RPE') ? sample.doneRpe : item.targetRpe),
        })
        const inPicture = within(picture())
        expect(inPicture.queryByText(done) != null, `${mode} ${step}: the ticked set`).toBe(at('TODAY_TICKED'))
        expect(inPicture.queryByText(SKIPPED_TODAY) != null, `${mode} ${step}: Skipped today`).toBe(at('TODAY_SKIP'))
      }
      cleanup()
    }
  })

  it('guide_screen4_theWeightRepsAndRpeSteps_eachChangeWhatIsDrawn', () => {
    for (const mode of SIDES) {
      const { rerender } = show({ mode, startAt: 'TODAY' })
      const steps = grit.guideSteps('TODAY', mode)
      const drawnAt = (step: string) => {
        rerender(heldGuideClockAtStep(steps.indexOf(step)))
        return drawnEditor()
      }
      const empty = drawnAt('TODAY_EMPTY')
      const weight = drawnAt('TODAY_WEIGHT')
      const reps = drawnAt('TODAY_REPS')
      const rpe = drawnAt('TODAY_RPE')
      expect(weight.weight, `${mode}: the weight step`).not.toBe(empty.weight)
      expect(reps.reps, `${mode}: the reps step`).not.toBe(weight.reps)
      expect(rpe.rpe, `${mode}: the RPE step`).not.toBe(reps.rpe)
      cleanup()
    }
  })

  it('the sample states follow the domain timeline', () => {
    for (const mode of SIDES) {
      const steps = grit.guideSteps('TODAY', mode)
      const sample = grit.guideSampleTodaySet(mode)
      const first = guideTodayState(mode, steps[0]!)
      expect(first.items[0]!.exercise.name).toBe(sample.exerciseId)
      expect(first.items[0]!.sets).toEqual([])
      expect(first.items[0]!.editor.weightKg).toBeNull()
      const ticked = guideTodayState(mode, 'TODAY_TICKED').items[0]!
      expect(ticked.editor).toEqual({ weightKg: sample.doneWeightKg, reps: sample.doneReps, rpe: sample.doneRpe })
      expect(ticked.sets.map((s) => [s.weightKg, s.reps, s.rpe])).toEqual([[sample.doneWeightKg, sample.doneReps, sample.doneRpe]])
      const last = guideTodayState(mode, steps[steps.length - 1]!)
      expect(last.skippedToday).toHaveLength(1)
      expect(last.items.some((i) => i.exercise.id === last.skippedToday[0]!.exerciseId)).toBe(false)
    }
  })

  it('guide_reduceMotion_showsTheLastStepStill', () => {
    vi.useFakeTimers()
    for (const mode of SIDES) {
      show({ mode, motionOn: false, clock: tickingGuideClock, startAt: 'TODAY' })
      const steps = grit.guideSteps('TODAY', mode)
      expect(picture()).toHaveAttribute('data-step', steps[steps.length - 1])
      act(() => vi.advanceTimersByTime(grit.guideStepMs() * 10))
      expect(picture()).toHaveAttribute('data-step', steps[steps.length - 1])
      cleanup()
    }
  })

  it('the real clock moves each step, then holds it, and holds the last for good', () => {
    vi.useFakeTimers()
    show({ clock: tickingGuideClock, startAt: 'TODAY' })
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    expect(picture()).toHaveAttribute('data-step', steps[0])
    act(() => vi.advanceTimersByTime(grit.guideStepStartMs(1) + grit.guideMoveMs() / 2))
    expect(picture()).toHaveAttribute('data-step', steps[1])
    const progress = Number(picture().getAttribute('data-progress'))
    expect(progress).toBeGreaterThan(0)
    expect(progress).toBeLessThan(1)
    act(() => vi.advanceTimersByTime(grit.guideMoveMs()))
    expect(picture()).toHaveAttribute('data-progress', '1')
    act(() => vi.advanceTimersByTime(grit.guideDoneMs(steps.length) * 2))
    expect(picture()).toHaveAttribute('data-step', steps[steps.length - 1])
    expect(picture()).toHaveAttribute('data-progress', '1')
  })

  it('guide_picturesShowingNumbers_areLabelledExample', async () => {
    for (const mode of SIDES) {
      show({ mode })
      for (const [i, id] of SCREENS.entries()) {
        expectOn(id, mode)
        const label = screen.queryByTestId('guide-example')
        expect(label != null).toBe(grit.guideScreen(id, mode).example)
        if (label) expect(label).toHaveTextContent(EXAMPLE)
        if (i < SCREENS.length - 1) await next()
      }
      cleanup()
    }
  })

  it('guide_pictureIsNotInteractive', async () => {
    show({ startAt: 'TODAY' })
    const frame = picture()
    // One description; what is drawn inside is out of the accessibility tree and inert.
    expect(screen.getByRole('img', { name: pictureDescription('TODAY') })).toBeInTheDocument()
    const drawn = frame.querySelector('.guide-picture-screen')!
    expect(drawn).toHaveAttribute('aria-hidden', 'true')
    expect(drawn).toHaveAttribute('inert')
    // So is the guide's own layer over it, where a gone row's copy (with its buttons), the tick and the ring are drawn.
    const ghosts = frame.querySelector('.guide-flip-ghosts')!
    expect(ghosts).toHaveAttribute('aria-hidden', 'true')
    expect(ghosts).toHaveAttribute('inert')
    expect(within(screen.getByTestId('guide')).queryByRole('button', { name: /Log set/ })).toBeNull()
    expect(within(screen.getByTestId('guide')).queryByRole('textbox')).toBeNull()
    await userEvent.click(frame)
    expect(frame).toHaveAttribute('data-step', 'TODAY_EMPTY')
    expectOn('TODAY')
  })

  it('the Today picture draws the domain\'s day for screen 4 only', () => {
    for (const mode of SIDES) {
      const state = guideTodayState(mode, 'TODAY_EMPTY')
      expect(state.items.map((i) => i.planned!.id)).toEqual(grit.guideSampleTodayItems(mode).map((p) => p.id))
    }
  })

  it('no picture timer outlives the guide, or the page it belongs to', () => {
    // The clock's own timers (timeouts and frames): React's own scheduling (setImmediate) is not the guide's. Counted
    // against what was pending before.
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame', 'performance'] })
    const idle = vi.getTimerCount()
    const clockTimers = () => vi.getTimerCount() - idle
    const { unmount } = show({ clock: tickingGuideClock, startAt: 'TODAY' })
    expect(clockTimers()).toBe(1)
    // The next screen's picture starts its own clock; the one before has stopped.
    fireEvent.click(screen.getByTestId('guide-next'))
    expect(clockTimers()).toBe(1)
    // It holds on its last step with nothing pending.
    act(() => vi.advanceTimersByTime(grit.guideStepMs() * 20))
    expect(clockTimers()).toBe(0)
    fireEvent.click(screen.getByTestId('guide-back'))
    expect(clockTimers()).toBe(1)
    // Mid-move, one frame pending; gone with the guide.
    act(() => vi.advanceTimersByTime(grit.guideStepStartMs(1) + grit.guideMoveMs() / 2))
    expect(clockTimers()).toBe(1)
    unmount()
    expect(clockTimers()).toBe(0)
  })

  it('guide_whileAStepHolds_nothingRenders', () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame', 'performance'] })
    let renders = 0
    render(
      <Profiler id="guide" onRender={() => (renders += 1)}>
        <GuideScreen mode="POWERLIFTING" firstLaunch={false} hasPlan={false} motionOn onPickSide={() => {}} onClose={() => {}} clock={tickingGuideClock} startAt="TODAY" />
      </Profiler>,
    )
    const slack = 50
    act(() => vi.advanceTimersByTime(grit.guideSettledMs(1) + slack))
    expect(picture()).toHaveAttribute('data-step', grit.guideSteps('TODAY', 'POWERLIFTING')[1])
    expect(picture()).toHaveAttribute('data-progress', '1')
    // A frame's time at a time, each in its own act, so each would be its own render.
    const run = (ms: number) => {
      for (let t = 0; t < ms; t += 16) act(() => vi.advanceTimersByTime(Math.min(16, ms - t)))
    }
    const held = renders
    run(grit.guideStepStartMs(2) - grit.guideSettledMs(1) - 2 * slack)
    expect(renders, 'rendered while the step held').toBe(held)
    // A move renders, a frame at a time.
    run(2 * slack + grit.guideMoveMs() / 2)
    expect(renders).toBeGreaterThan(held + 3)
  })

  it('guide_aTapRing_playsInTheLastOfTheHoldBeforeItsStep: frames for the ring, none before it', () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame', 'performance'] })
    let renders = 0
    render(
      <Profiler id="guide" onRender={() => (renders += 1)}>
        <GuideScreen mode="POWERLIFTING" firstLaunch={false} hasPlan={false} motionOn onPickSide={() => {}} onClose={() => {}} clock={tickingGuideClock} startAt="TODAY" />
      </Profiler>,
    )
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    const ticked = steps.indexOf('TODAY_TICKED')
    const ringFrom = grit.guideMotionStartMs(steps, ticked)
    expect(ringFrom).toBe(grit.guideStepStartMs(ticked) - grit.guideTapMs())
    const run = (ms: number) => {
      for (let t = 0; t < ms; t += 16) act(() => vi.advanceTimersByTime(Math.min(16, ms - t)))
    }
    const slack = 50
    run(grit.guideSettledMs(ticked - 1) + slack)
    expect(picture()).toHaveAttribute('data-step', steps[ticked - 1])
    // The hold, up to the ring: nothing renders.
    const held = renders
    run(ringFrom - grit.guideSettledMs(ticked - 1) - 2 * slack)
    expect(renders, 'rendered while the step held').toBe(held)
    // The ring: still on the step before, a frame at a time.
    run(slack + grit.guideTapMs() / 2)
    expect(picture()).toHaveAttribute('data-step', steps[ticked - 1])
    expect(renders).toBeGreaterThan(held + 3)
    // And on into the logged set's move without a pause.
    run(grit.guideTapMs())
    expect(picture()).toHaveAttribute('data-step', 'TODAY_TICKED')
  })

  it('guide_clockStops_whenHiddenOrUnmounted: a hidden page stands the clock still; shown again, it goes on', () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame', 'performance'] })
    const idle = vi.getTimerCount()
    let hidden = false
    const spy = vi.spyOn(document, 'hidden', 'get').mockImplementation(() => hidden)
    show({ clock: tickingGuideClock, startAt: 'TODAY' })
    act(() => vi.advanceTimersByTime(grit.guideSettledMs(1) + 50))
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    expect(picture()).toHaveAttribute('data-step', steps[1])
    hidden = true
    act(() => {
      document.dispatchEvent(new Event('visibilitychange'))
    })
    expect(vi.getTimerCount() - idle).toBe(0)
    act(() => vi.advanceTimersByTime(grit.guideStepMs() * 20))
    expect(picture()).toHaveAttribute('data-step', steps[1])
    hidden = false
    act(() => {
      document.dispatchEvent(new Event('visibilitychange'))
    })
    act(() => vi.advanceTimersByTime(grit.guideStepStartMs(2) - grit.guideSettledMs(1) + grit.guideMoveMs()))
    expect(picture()).toHaveAttribute('data-step', steps[2])
    spy.mockRestore()
  })

  it('guide_today_numbersRollThroughInBetweenValues: mid-move the weight is between empty and the set\'s', () => {
    for (const mode of SIDES) {
      const steps = grit.guideSteps('TODAY', mode)
      const sample = grit.guideSampleTodaySet(mode)
      const weightStep = steps.indexOf('TODAY_WEIGHT')
      const seen = new Set<string>()
      // The move's curve is fast at first: an eighth, a quarter and a third of its time are well inside it.
      for (const into of [grit.guideMoveMs() / 8, grit.guideMoveMs() / 4, grit.guideMoveMs() / 3]) {
        show({ mode, clock: heldGuideClock(grit.guideStepStartMs(weightStep) + into), startAt: 'TODAY' })
        expect(picture()).toHaveAttribute('data-step', 'TODAY_WEIGHT')
        const frame = grit.guideFrame('TODAY', mode, grit.guideStepStartMs(weightStep) + into)
        const kg = grit.guideSampleTodayEditor(mode, 'TODAY_WEIGHT', frame.progress).weightKg!
        expect(kg).toBeGreaterThan(0)
        expect(kg).toBeLessThan(sample.doneWeightKg)
        expect(drawnEditor().weight, `${mode} ${into} ms into the move`).toBe(formatKg(kg))
        seen.add(drawnEditor().weight)
        cleanup()
      }
      expect(seen.size, `${mode}: the weight rolls`).toBe(3)
    }
  })

  it('opening the guide works out screen 1 only: no other screen\'s steps, and none of the sample', () => {
    const names = Object.getOwnPropertyNames(Object.getPrototypeOf(grit)).filter((n) => n.startsWith('guideSample'))
    expect(names.length).toBeGreaterThan(0)
    const samples = names.map((n) => vi.spyOn(grit as unknown as Record<string, () => unknown>, n))
    const steps = vi.spyOn(grit, 'guideSteps')
    show()
    expect(steps.mock.calls.map(([id]) => id)).not.toHaveLength(0)
    expect(new Set(steps.mock.calls.map(([id]) => id))).toEqual(new Set(['WELCOME']))
    for (const spy of samples) expect(spy).not.toHaveBeenCalled()
    vi.restoreAllMocks()
  })

  it('the guide is a named region, and its title takes the focus when it opens', () => {
    show()
    expect(screen.getByRole('region', { name: GUIDE_LABEL })).toBe(screen.getByTestId('guide'))
    expect(document.activeElement).toBe(screen.getByTestId('guide-title'))
  })

  it('a page change is announced: the title is in a polite live region outside the page', async () => {
    show()
    const live = screen.getByTestId('guide-announce')
    expect(live).toHaveAttribute('aria-live', 'polite')
    expect(screen.getByTestId('guide-pages')).not.toContainElement(live)
    expect(live).toHaveTextContent(title('WELCOME'))
    await next()
    expect(screen.getByTestId('guide-announce')).toBe(live)
    expect(live).toHaveTextContent(title('SIDE'))
  })

  it('Back on screen 1 is aria-disabled, not disabled, so a focused Back keeps the focus', async () => {
    show({ startAt: 'SIDE' })
    const back = screen.getByTestId('guide-back')
    expect(back).toHaveAccessibleName(BACK)
    expect(back).toHaveAttribute('aria-disabled', 'false')
    back.focus()
    await userEvent.keyboard('{Enter}')
    expectOn('WELCOME')
    expect(back).not.toBeDisabled()
    expect(back).toHaveAttribute('aria-disabled', 'true')
    expect(document.activeElement).toBe(back)
    // Pressed again, it does nothing.
    await userEvent.keyboard('{Enter}')
    expectOn('WELCOME')
    expect(screen.getByTestId('guide-next')).toHaveAccessibleName(NEXT)
  })
})

describe('screens 2 and 8 (Task 3)', () => {
  function showAt(startAt: string, { mode = 'POWERLIFTING' as TrainingMode, firstLaunch = true, hasPlan = false } = {}) {
    const picks: TrainingMode[] = []
    const exits: GuideExit[] = []
    render(
      <GuideScreen mode={mode} firstLaunch={firstLaunch} hasPlan={hasPlan} motionOn={false} onPickSide={(m) => picks.push(m)} onClose={(e) => exits.push(e)} clock={heldGuideClock(0)} startAt={startAt} />,
    )
    return { picks, exits }
  }

  it('screen2_onFirstLaunch_offersBothSides_andPicksTheOtherOne_only', () => {
    const { picks } = showAt('SIDE')
    expect(screen.getByTestId('guide-side-powerlifting')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByTestId('guide-side-bodybuilding')).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(screen.getByTestId('guide-side-powerlifting'))
    expect(picks, 'the side already on: nothing').toEqual([])
    fireEvent.click(screen.getByTestId('guide-side-bodybuilding'))
    expect(picks).toEqual(['BODYBUILDING'])
  })

  it('screen2_onAReplay_showsNoButtons', () => {
    showAt('SIDE', { firstLaunch: false })
    expect(screen.queryByTestId('guide-side-powerlifting')).toBeNull()
    expect(screen.queryByTestId('guide-side-bodybuilding')).toBeNull()
  })

  it('screen 2 shows no side buttons on any other screen', () => {
    for (const id of SCREENS.filter((s) => s !== 'SIDE')) {
      cleanup()
      showAt(id)
      expect(screen.queryByTestId('guide-side-powerlifting'), id).toBeNull()
    }
  })

  it('screen8: Choose a template, with Build my plan on bodybuilding; Done and See my plan with a plan', () => {
    const pl = showAt('YOUR_TURN')
    expect(screen.getByTestId('guide-next')).toHaveTextContent('Choose a template')
    expect(screen.queryByTestId('guide-last-secondary')).toBeNull()
    fireEvent.click(screen.getByTestId('guide-next'))
    expect(pl.exits).toEqual(['choose-template'])
    cleanup()
    const bb = showAt('YOUR_TURN', { mode: 'BODYBUILDING' })
    fireEvent.click(screen.getByTestId('guide-last-secondary'))
    expect(screen.getByTestId('guide-last-secondary')).toHaveTextContent('Build my plan')
    expect(bb.exits).toEqual(['build-plan'])
    cleanup()
    const has = showAt('YOUR_TURN', { hasPlan: true, firstLaunch: false })
    expect(screen.getByTestId('guide-next')).toHaveTextContent('Done')
    expect(screen.getByTestId('guide-last-secondary')).toHaveTextContent('See my plan')
    fireEvent.click(screen.getByTestId('guide-last-secondary'))
    fireEvent.click(screen.getByTestId('guide-next'))
    expect(has.exits).toEqual(['see-plan', 'done'])
  })
})
