import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { TrainingMode } from '../../db/schema'
import { grit } from '../../domain'
import { TrainingModeContext } from '../../ui/components/ModeLabel'
import { NO_GUIDE, checkInfoButtonOpensGuide, guideIsOpen } from '../../test/guideChecks'
import { FormGuideSheet } from './FormGuideSheet'
import { InfoButton } from './InfoButton'

// Ported from Android's FormGuideSheetTest. Scrolling at 130% zoom is checked in e2e/library.spec.ts.

const SAFETY = 'Stop if you feel sharp pain. For an injury or a condition, check with a doctor or physiotherapist.'

const sheet = (name: string) => screen.getByRole('dialog', { name: `Form guide for ${name}` })

/** The sheet's lines of text, top to bottom: every element that holds text of its own. */
function linesOf(root: HTMLElement): string[] {
  return Array.from(root.querySelectorAll<HTMLElement>('h2, h3, p, li')).map((el) => el.textContent ?? '')
}

describe('FormGuideSheet', () => {
  it('showsAllSections_andSafetyLineLast', () => {
    const entry = grit.findInLibrary('Squat')!
    const guide = entry.guide
    render(<FormGuideSheet name="Squat" onClose={() => {}} />)

    expect(grit.safetyLine()).toBe(SAFETY)
    // Name; area and equipment; purpose; Setup; How to do it; Cues; Common mistakes; the safety line, and nothing after it.
    expect(linesOf(sheet('Squat'))).toEqual([
      'Squat',
      'Main lifts · Barbell',
      guide.purpose,
      'Setup',
      ...guide.setup,
      'How to do it',
      ...guide.steps,
      'Cues',
      ...guide.cues,
      'Common mistakes',
      ...guide.mistakes,
      SAFETY,
    ])
    const inSheet = within(sheet('Squat'))
    for (const title of ['Setup', 'How to do it', 'Cues', 'Common mistakes']) expect(inSheet.getByRole('heading', { level: 3, name: title })).toBeInTheDocument()
    // The steps are numbered; the other sections are bulleted.
    const lists = inSheet.getAllByRole('list')
    expect(lists.map((l) => l.tagName)).toEqual(['UL', 'OL', 'UL', 'UL'])
    expect(within(lists[1]!).getAllByRole('listitem').map((li) => li.textContent)).toEqual([...guide.steps])
    // Read between sets: 16 px body text for the guide's lines and the safety line.
    for (const line of [guide.purpose, guide.setup[0]!, guide.steps[0]!, guide.cues[0]!, guide.mistakes[0]!, SAFETY]) expect(inSheet.getByText(line).closest('.body-large')).not.toBeNull()
  })

  it('areaAndEquipment_areNamedInWords', () => {
    render(<FormGuideSheet name="Bulgarian split squat" onClose={() => {}} />)
    expect(within(sheet('Bulgarian split squat')).getByText('Quads · Dumbbell or barbell')).toBeInTheDocument()
  })

  // One case per body area, and one sheet re-rendered per name, so each case stays fast as the catalogue grows.
  const areas = [...new Set(grit.library().map((e) => e.area))]
  it.each(areas)('everyCatalogueName_andAlias_showsItsGuide (%s)', (area) => {
    const entries = grit.library().filter((e) => e.area === area)
    const { rerender, unmount } = render(<FormGuideSheet name={entries[0]!.name} onClose={() => {}} />)
    for (const entry of entries) {
      for (const name of [entry.name, ...entry.aliases]) {
        rerender(<FormGuideSheet name={name} onClose={() => {}} />)
        const lines = linesOf(sheet(name))
        expect(lines[0]).toBe(name)
        expect(lines).toContain(entry.guide.purpose)
        expect(lines.at(-1)).toBe(SAFETY)
      }
    }
    unmount()
  })

  it('customExercise_showsNoGuideText', () => {
    render(<FormGuideSheet name="Tyre flip" onClose={() => {}} />)

    expect(NO_GUIDE).toBe('No form guide for your own exercises.')
    // The sheet still ends with the safety line.
    expect(linesOf(sheet('Tyre flip'))).toEqual(['Tyre flip', NO_GUIDE, SAFETY])
    expect(within(sheet('Tyre flip')).queryByText('Setup')).toBeNull()
    expect(within(sheet('Tyre flip')).queryByText('Common mistakes')).toBeNull()
  })

  it('closes_fromItsButton_andOnEscape', async () => {
    const onClose = vi.fn()
    render(<FormGuideSheet name="Squat" onClose={onClose} />)
    await userEvent.click(within(sheet('Squat')).getByRole('button', { name: 'Close guide' }))
    await userEvent.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(2)
  })
})

describe('InfoButton', () => {
  it('opensTheGuide_andOnlyThat', async () => {
    const onRow = vi.fn()
    render(
      // A row that is itself tapped, as in the picker and on Today.
      <div onClick={onRow}>
        <InfoButton name="Squat" />
        <InfoButton name="Tyre flip" />
      </div>,
    )
    expect(guideIsOpen()).toBe(false)
    await checkInfoButtonOpensGuide('Squat')
    // Custom exercises keep the button; the sheet says there is no guide.
    await checkInfoButtonOpensGuide('Tyre flip')
    expect(onRow).not.toHaveBeenCalled()
  })

  it('escape_closesTheGuide_notTheDialogUnderIt', async () => {
    const { Dialog } = await import('../../ui/components/Dialog')
    const onDismiss = vi.fn()
    render(
      <Dialog open title="Under" onDismiss={onDismiss}>
        <InfoButton name="Squat" />
      </Dialog>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Form guide for Squat' }))
    expect(guideIsOpen()).toBe(true)
    await userEvent.keyboard('{Escape}')
    expect(guideIsOpen()).toBe(false)
    expect(onDismiss).not.toHaveBeenCalled()
    await userEvent.keyboard('{Escape}')
    expect(onDismiss).toHaveBeenCalledOnce()
  })
})

// Stage 4: the "Stretch and squeeze" section, after Cues, only while bodybuilding mode is on (owner decision 1).
describe('FormGuideSheet stretch and squeeze', () => {
  const NO_FOCUS_LINE = 'No stretch or squeeze to aim for in this one.'
  const onSide = (mode: TrainingMode, name: string) => (
    <TrainingModeContext.Provider value={mode}>
      <FormGuideSheet name={name} onClose={() => {}} />
    </TrainingModeContext.Provider>
  )

  it('guideSheet_bodybuilding_hasStretchAndSqueeze', () => {
    const guide = grit.findInLibrary('Squat')!.guide
    const focus = grit.muscleFocus('Squat')!
    const { rerender } = render(onSide('BODYBUILDING', 'Squat'))
    const inSheet = within(sheet('Squat'))
    expect(inSheet.getByRole('heading', { level: 3, name: 'Stretch and squeeze' })).toBeInTheDocument()
    // Right after Cues: its two lines, stretch first, as the guide's other lines are.
    const lines = linesOf(sheet('Squat'))
    const at = lines.indexOf('Stretch and squeeze')
    expect(lines.slice(at - guide.cues.length - 1, at)).toEqual(['Cues', ...guide.cues])
    expect(lines.slice(at, at + 4)).toEqual(['Stretch and squeeze', focus.stretch, focus.squeeze, 'Common mistakes'])
    expect(inSheet.getByText(focus.stretch!).closest('.body-large')).not.toBeNull()

    // An exercise with nothing to aim for says so in one line.
    rerender(onSide('BODYBUILDING', 'Front plank'))
    const plank = linesOf(sheet('Front plank'))
    const plankAt = plank.indexOf('Stretch and squeeze')
    expect(plankAt).toBeGreaterThan(plank.indexOf('Cues'))
    expect(plank.slice(plankAt, plankAt + 3)).toEqual(['Stretch and squeeze', NO_FOCUS_LINE, 'Common mistakes'])

    // Bodybuilding mode off: no section, and the sheet is exactly as before. Switching shows or hides it at once.
    rerender(onSide('POWERLIFTING', 'Squat'))
    expect(within(sheet('Squat')).queryByRole('heading', { name: 'Stretch and squeeze' })).toBeNull()
    expect(within(sheet('Squat')).queryByText(focus.stretch!)).toBeNull()
    rerender(onSide('POWERLIFTING', 'Front plank'))
    expect(within(sheet('Front plank')).queryByText(NO_FOCUS_LINE)).toBeNull()
    rerender(onSide('BODYBUILDING', 'Squat'))
    expect(within(sheet('Squat')).getByText(focus.squeeze!)).toBeInTheDocument()

    // The lifter's own exercise has no guide, so no section either.
    rerender(onSide('BODYBUILDING', 'Tyre flip'))
    expect(linesOf(sheet('Tyre flip'))).toEqual(['Tyre flip', NO_GUIDE, SAFETY])
  })

  it('guideSheet_bodybuilding_notWrittenYet_hasNoSection', () => {
    // Every catalogue exercise has lines now, so this fakes the lookup giving null: the sheet is as on the other side.
    const lookup = vi.spyOn(grit, 'muscleFocus').mockImplementation(() => null)
    try {
      const guide = grit.findInLibrary('Squat')!.guide
      render(onSide('BODYBUILDING', 'Squat'))
      expect(lookup).toHaveBeenCalledWith('Squat')
      expect(within(sheet('Squat')).queryByRole('heading', { name: 'Stretch and squeeze' })).toBeNull()
      const lines = linesOf(sheet('Squat'))
      const at = lines.indexOf('Common mistakes')
      expect(lines.slice(at - guide.cues.length - 1, at)).toEqual(['Cues', ...guide.cues])
    } finally {
      lookup.mockRestore()
    }
  })

  it('guideSheet_restoredRowUnderCatalogueName_showsLines', () => {
    const focus = grit.muscleFocus('Squat')!
    render(onSide('BODYBUILDING', 'squat'))
    const inSheet = within(sheet('squat'))
    expect(inSheet.getByRole('heading', { level: 3, name: 'Stretch and squeeze' })).toBeInTheDocument()
    expect(inSheet.getByText(focus.stretch!)).toBeInTheDocument()
    expect(inSheet.getByText(focus.squeeze!)).toBeInTheDocument()
  })
})
