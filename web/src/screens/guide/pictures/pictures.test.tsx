import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { TrainingMode } from '../../../db/schema'
import { grit } from '../../../domain'
import { GuideScreen, heldGuideClock, pictureDescription, type GuideClock } from '../GuideScreen'
import { guideHoldsBackState, guideMonthsBodybuildingState, guideTargetsState, guideTemplatesState } from '../guideSampleState'

/** The other seven pictures (1.16, Task 4), drawn on a held clock. Every value comes from the domain. */

const SIDES: TrainingMode[] = ['POWERLIFTING', 'BODYBUILDING']
const SEVEN = grit.guideScreens().filter((id) => id !== 'TODAY')

afterEach(() => {
  vi.restoreAllMocks()
  cleanup()
})

function show(id: string, mode: TrainingMode, clock: GuideClock, motionOn = true) {
  const ui = (c: GuideClock) => <GuideScreen mode={mode} firstLaunch={false} hasPlan={false} motionOn={motionOn} onPickSide={() => {}} onClose={() => {}} clock={c} startAt={id} />
  const { rerender, unmount } = render(ui(clock))
  return { rerender: (c: GuideClock) => rerender(ui(c)), unmount }
}

const picture = () => screen.getByTestId('guide-picture')

describe('each picture, first and last step on a held clock', () => {
  for (const mode of SIDES) {
    for (const id of SEVEN) {
      it(`guide_${id.toLowerCase()}_firstAndLastStep (${mode})`, () => {
        const steps = grit.guideSteps(id, mode)
        const shown = show(id, mode, heldGuideClock(0))
        expect(picture()).toHaveAttribute('data-step', steps[0])
        expect(picture()).toHaveAttribute('role', 'img')
        expect(picture()).toHaveAccessibleName(pictureDescription(id))
        shown.rerender(heldGuideClock(grit.guideDoneMs(steps.length)))
        expect(picture()).toHaveAttribute('data-step', steps.at(-1))
        expect(picture()).toHaveAttribute('data-progress', '1')
        // WELCOME and SIDE are art: no phone, no outline (spec point 11); the others a drawn phone.
        expect(picture()).toHaveAttribute('data-outlined', id === 'WELCOME' || id === 'SIDE' ? 'false' : 'true')
      })
    }
  }
})

describe('what the pictures draw', () => {
  it("screen 3's templates: the list, the sample template's draft, then that draft with the one swap made", () => {
    for (const mode of SIDES) {
      const swap = grit.guideSampleSwap(mode)
      expect(guideTemplatesState(mode, 'PLAN_TEMPLATES')).toMatchObject({ step: 'list', chosen: null, draft: null })
      const before = guideTemplatesState(mode, 'PLAN_DRAFT')
      const after = guideTemplatesState(mode, 'PLAN_SWAP')
      expect(before.chosen?.id).toBe(grit.guideSampleTemplate(mode).id)
      const names = (s: typeof before) => s.draft!.sessions.flatMap((d) => d.exercises.map((e) => e.name))
      expect(names(before)).toContain(swap.from)
      expect(names(after)).toContain(swap.to)
      expect(names(after)).not.toContain(swap.from)
      expect(after.canUse && after.daysMatch).toBe(true)
    }
  })

  it("screen 5: each session's Today shows the coach's target, its reason, and the fields as following it puts them in", () => {
    for (const mode of SIDES) {
      const advice = grit.guideSampleTargetAdvice(mode)
      advice.forEach((a, i) => {
        const e = grit.guideSampleTargetEditor(mode, i, 1)
        const state = guideTargetsState(mode, i, { weightKg: e.weightKg ?? null, reps: e.reps, rpe: e.rpe })
        expect(state.date).toBe(a.createdAt)
        expect(state.items).toHaveLength(1)
        expect(state.items[0]!.suggestion?.reason).toBe(a.reason)
        expect(state.items[0]!.editor).toEqual({ weightKg: e.weightKg, reps: e.reps, rpe: e.rpe })
      })
    }
  })

  it("screen 6: one card a step, in the rule table's order; the stall card's figures are the domain's", () => {
    for (const mode of SIDES) {
      const advice = grit.guideSampleAdvice(mode)
      advice.forEach((a, i) => {
        const cards = guideHoldsBackState(mode, i).cards
        expect(cards).toHaveLength(1)
        expect(cards[0]!.advice.reason).toBe(a.reason)
        const detail = cards[0]!.detail
        if (detail.kind === 'stall') {
          expect(mode).toBe('POWERLIFTING')
          expect(detail.trend).toEqual(grit.guideSampleStallTrend(mode).slice(-6).map((p) => p.value))
          expect(detail.e1rmKg).toBe(grit.guideSampleStallE1rm(mode))
        }
      })
    }
  })

  it('screen 7: the check-in arrives only at its end, on bodybuilding, marking the lagging muscle', () => {
    const mode = 'BODYBUILDING'
    const steps = grit.guideSteps('MONTHS_LATER', mode)
    const end = grit.guideSampleMonthsDate(mode, steps.at(-1)!, 1)
    const checkIn = grit.guideSampleMonthsCheckIn(mode, steps.at(-1)!, 1)!
    const state = guideMonthsBodybuildingState(mode, end, checkIn)
    const lagging = grit.guideSampleProgress(mode).laggingMuscle!
    expect(state.bodyParts.find((b) => b.group === lagging)?.ratings.at(-1)?.rating).toBe('LAGGING')
    expect(state.checkedIn).toBe(true)
    expect(grit.guideSampleMonthsCheckIn(mode, steps.at(-1)!, 0.5) ?? null).toBeNull()
    expect(grit.guideSampleMonthsCheckIn('POWERLIFTING', steps.at(-1)!, 1) ?? null).toBeNull()
  })

  it("WELCOME: its side colours are plain panels named by their side, not buttons: nothing in it can be pressed or focused", () => {
    show('WELCOME', 'POWERLIFTING', heldGuideClock(grit.guideDoneMs(2)))
    const panels = within(picture()).getAllByTestId('guide-side-panel')
    expect(panels.map((p) => p.textContent)).toEqual(['Powerlifting', 'Bodybuilding'])
    expect(picture().querySelectorAll('button, a, input, [tabindex], [role="button"]')).toHaveLength(0)
  })
})

describe('screen 2', () => {
  const art = () => [...picture().querySelectorAll('svg *')].map((n) => [...n.attributes].map((a) => `${a.name}=${a.value}`).join(' ')).join('|')

  it('on a held clock it draws that moment of the loop and runs no loop of its own', () => {
    const frames = vi.spyOn(window, 'requestAnimationFrame')
    const shown = show('SIDE', 'POWERLIFTING', heldGuideClock(300))
    expect(picture().querySelectorAll('svg')).toHaveLength(2)
    const at300 = art()
    shown.rerender(heldGuideClock(900))
    expect(art()).not.toBe(at300)
    expect(frames).not.toHaveBeenCalled()
  })

  it('with motion off it draws the still frame, the same whatever the clock; nothing is left once it is gone', () => {
    const shown = show('SIDE', 'BODYBUILDING', heldGuideClock(300), false)
    const still = art()
    shown.rerender(heldGuideClock(1200))
    expect(art()).toBe(still)
    const host = picture()
    shown.unmount()
    expect(host.querySelectorAll('svg')).toHaveLength(0)
  })

  it('on the real clock it runs a frame loop while shown, and cancels it when it goes', () => {
    const cancel = vi.spyOn(window, 'cancelAnimationFrame')
    const shown = render(<GuideScreen mode="POWERLIFTING" firstLaunch={false} hasPlan={false} motionOn onPickSide={() => {}} onClose={() => {}} startAt="SIDE" />)
    shown.unmount()
    expect(cancel).toHaveBeenCalled()
  })
})

describe("the two sides' colours (WELCOME and SIDE draw both, whichever side the guide is in)", () => {
  const read = (path: string) => readFileSync(resolve(process.cwd(), path), 'utf8')
  const tokens = read('src/ui/tokens.css')
  const guide = read('src/screens/guide/GuideScreen.css')
  /** The `--name: #hex` declarations of the first block after `selector` at or after `from` in `source`. */
  const vars = (source: string, selector: string, from = 0) => {
    const at = source.indexOf(selector, from)
    const body = source.slice(source.indexOf('{', at) + 1, source.indexOf('}', at))
    return Object.fromEntries([...body.matchAll(/(--[a-z-]+)\s*:\s*(#[0-9a-fA-F]{6})/g)].map((m) => [m[1], m[2]!.toLowerCase()]))
  }
  const light = (source: string) => source.indexOf('@media (prefers-color-scheme: light)')
  const lightAfter = (source: string, from: number) => source.indexOf('@media (prefers-color-scheme: light)', from)

  it("are the tokens' own, in dark and light", () => {
    const pickTokens = (t: Record<string, string>) => ({ '--side-bg': t['--bg'], '--side-surface': t['--surface'], '--side-hairline': t['--hairline'], '--side-accent': t['--accent'] })
    const plDark = vars(tokens, ':root {')
    const plLight = vars(tokens, ':root {', light(tokens))
    const bbAt = tokens.indexOf(":root[data-mode='bodybuilding'] {")
    const bbDark = vars(tokens, ":root[data-mode='bodybuilding'] {")
    const bbLight = vars(tokens, ":root[data-mode='bodybuilding'] {", lightAfter(tokens, bbAt))
    const sideAt = guide.indexOf('.guide-side-powerlifting {')
    const sideLight = lightAfter(guide, sideAt)
    expect(vars(guide, '.guide-side-powerlifting {')).toEqual(pickTokens(plDark))
    expect(vars(guide, '.guide-side-bodybuilding {')).toEqual(pickTokens(bbDark))
    expect(vars(guide, '.guide-side-powerlifting {', sideLight)).toEqual(pickTokens(plLight))
    expect(vars(guide, '.guide-side-bodybuilding {', sideLight)).toEqual(pickTokens(bbLight))
  })
})
