import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { ModeLabel, TrainingModeContext } from './ModeLabel'
import { ScreenHeader } from './ScreenHeader'

afterEach(cleanup)

describe('ModeLabel', () => {
  it('powerlifting_showsNothing', () => {
    const { container } = render(<ScreenHeader title="Today" eyebrow="Wednesday 11 November" label={<ModeLabel />} />)
    expect(screen.queryByTestId('mode-label')).toBeNull()
    expect(container.textContent).toBe('Wednesday 11 NovemberToday')
  })

  it('bodybuilding_showsThePill_underTheEyebrow_readOnceAsBodybuildingMode', () => {
    render(
      <TrainingModeContext.Provider value="BODYBUILDING">
        <ScreenHeader title="Today" eyebrow="Wednesday 11 November" label={<ModeLabel />} />
      </TrainingModeContext.Provider>,
    )
    const label = screen.getByTestId('mode-label')
    expect(label).toHaveTextContent('BODYBUILDING')
    // Seen as BODYBUILDING, heard once as "Bodybuilding mode"; never a tab stop.
    expect(screen.getByText('BODYBUILDING')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.getByText('Bodybuilding mode')).toHaveClass('sr-only')
    expect(label.querySelector('[tabindex], a, button')).toBeNull()
    // Between the eyebrow and the title.
    const text = label.parentElement!
    expect(Array.from(text.children).map((c) => c.tagName)).toEqual(['P', 'P', 'H1'])
  })
})
