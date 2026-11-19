import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { TrainingMode } from '../../db/schema'
import { grit } from '../../domain'
import { setRowLabel } from '../../ui/components/SetRow'
import { SKIPPED_TODAY } from '../today/SkipToday'
import { EXAMPLE, GuideScreen, dotsLabel, heldGuideClock, pictureDescription, tickingGuideClock, type GuideClock, type GuideExit } from './GuideScreen'
import { guideTodayState } from './guideSampleState'

/** The guide's shell and its Today picture (1.16, Task 2), as Android's `GuideScreenTest`. Copy comes from the domain. */

const SIDES: TrainingMode[] = ['POWERLIFTING', 'BODYBUILDING']
const SCREENS = grit.guideScreens()

afterEach(() => {
  vi.useRealTimers()
  cleanup()
})

type Shown = { exits: GuideExit[]; rerender: (clock: GuideClock) => void }

function show({ mode = 'POWERLIFTING' as TrainingMode, motionOn = true, clock = heldGuideClock(0) as GuideClock, startAt = 'WELCOME' } = {}): Shown {
  const exits: GuideExit[] = []
  const ui = (c: GuideClock) => (
    <GuideScreen mode={mode} firstLaunch={false} hasPlan={false} motionOn={motionOn} onPickSide={() => {}} onClose={(exit) => exits.push(exit)} clock={c} startAt={startAt} />
  )
  const { rerender } = render(ui(clock))
  return { exits, rerender: (c) => rerender(ui(c)) }
}

const title = (id: string, mode: TrainingMode = 'POWERLIFTING') => grit.guideScreen(id, mode).title
const picture = () => screen.getByTestId('guide-picture')

function expectOn(id: string, mode: TrainingMode = 'POWERLIFTING') {
  expect(screen.getByTestId('guide-title')).toHaveTextContent(title(id, mode))
  expect(screen.getByTestId('guide-dots')).toHaveAccessibleName(dotsLabel(SCREENS.indexOf(id), SCREENS.length))
}

const next = () => userEvent.click(screen.getByTestId('guide-next'))

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
      const inPicture = within(picture())
      const weight = () => inPicture.getByRole('textbox', { name: 'Weight, kilograms', hidden: true })
      const done = setRowLabel({ index: 1, weightKg: sample.doneWeightKg, reps: sample.doneReps, rpe: sample.doneRpe, state: 'done' })
      // Step 0: the set row is empty.
      expect(picture()).toHaveAttribute('data-step', steps[0])
      expect(weight()).toHaveValue('')
      expect(inPicture.queryByText(done)).toBeNull()
      for (const [i, step] of steps.entries()) {
        rerender(heldGuideClock(i))
        expect(picture()).toHaveAttribute('data-step', step)
      }
      // By the last step: the weight, reps and RPE in, the set ticked, then Skip today.
      expect(inPicture.getByText(done)).toBeInTheDocument()
      expect(inPicture.getByRole('textbox', { name: 'Reps', hidden: true })).toHaveValue(String(sample.doneReps))
      expect(inPicture.getByRole('textbox', { name: 'RPE', hidden: true })).toHaveValue(String(sample.doneRpe))
      expect(inPicture.getByText(SKIPPED_TODAY)).toBeInTheDocument()
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

  it('the real clock moves one step every step length, and holds on the last', () => {
    vi.useFakeTimers()
    show({ clock: tickingGuideClock, startAt: 'TODAY' })
    const steps = grit.guideSteps('TODAY', 'POWERLIFTING')
    expect(picture()).toHaveAttribute('data-step', steps[0])
    act(() => vi.advanceTimersByTime(grit.guideStepMs()))
    expect(picture()).toHaveAttribute('data-step', steps[1])
    act(() => vi.advanceTimersByTime(grit.guideStepMs() * 10))
    expect(picture()).toHaveAttribute('data-step', steps[steps.length - 1])
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
    expect(within(screen.getByTestId('guide')).queryByRole('button', { name: /Log set/ })).toBeNull()
    expect(within(screen.getByTestId('guide')).queryByRole('textbox')).toBeNull()
    await userEvent.click(frame)
    expect(frame).toHaveAttribute('data-step', 'TODAY_EMPTY')
    expectOn('TODAY')
  })
})
