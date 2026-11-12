import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { grit } from '../../domain'
import { checkRpeInfoOpensSheet, rpeSheetIsOpen } from '../../test/guideChecks'
import { Dialog } from '../../ui/components/Dialog'
import { RpeInfoButton, RpeSheet } from './RpeSheet'

// Ported from Android's RpeSheetTest. Scrolling at 130% zoom is checked in e2e/rpe.spec.ts.

const guide = grit.rpeGuide()
const sheet = () => screen.getByRole('dialog', { name: guide.title })

/** The sheet's lines of text, top to bottom: every element that holds text of its own. */
function linesOf(root: HTMLElement): string[] {
  return Array.from(root.querySelectorAll<HTMLElement>('h2, h3, p, li')).map((el) => el.textContent ?? '')
}

describe('RpeSheet', () => {
  it('showsTheWholeExplainerInOrder_andNoSafetyLine', async () => {
    const onClose = vi.fn()
    render(<RpeSheet onClose={onClose} />)

    // It is not an exercise: nothing but the explainer, and no safety line under it.
    expect(linesOf(sheet())).toEqual([guide.title, ...guide.lines])
    expect(within(sheet()).getByRole('heading', { level: 2, name: guide.title })).toBeInTheDocument()
    expect(screen.queryByText(grit.safetyLine())).toBeNull()
    expect(sheet().querySelector('.lib-guide-safety')).toBeNull()
    // The scale itself (the lines that start with an RPE value) is one list; what frames it is plain text.
    const scale = guide.lines.filter((line) => /^\d/.test(line))
    expect(scale.length).toBeGreaterThanOrEqual(4)
    expect(within(within(sheet()).getByRole('list')).getAllByRole('listitem').map((li) => li.textContent)).toEqual(scale)
    // Read between sets: 16 px body text.
    for (const line of guide.lines) expect(within(sheet()).getByText(line).closest('.body-large')).not.toBeNull()

    await userEvent.click(within(sheet()).getByRole('button', { name: 'Close' }))
    await userEvent.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(2)
  })
})

describe('RpeInfoButton', () => {
  it('opensTheSheet_andOnlyThat', async () => {
    const onRow = vi.fn()
    render(
      // A row that is itself tapped.
      <div onClick={onRow}>
        <RpeInfoButton />
        <div data-testid="inline">
          <RpeInfoButton inline />
        </div>
      </div>,
    )
    expect(rpeSheetIsOpen()).toBe(false)
    expect(screen.getAllByRole('button', { name: 'What RPE means' })).toHaveLength(2)
    await checkRpeInfoOpensSheet(screen.getByTestId('inline'))
    expect(onRow).not.toHaveBeenCalled()
  })

  it('escape_closesTheSheet_notTheDialogUnderIt', async () => {
    const onDismiss = vi.fn()
    render(
      <Dialog open title="Under" onDismiss={onDismiss}>
        <RpeInfoButton />
      </Dialog>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'What RPE means' }))
    expect(rpeSheetIsOpen()).toBe(true)
    await userEvent.keyboard('{Escape}')
    expect(rpeSheetIsOpen()).toBe(false)
    expect(onDismiss).not.toHaveBeenCalled()
  })
})
