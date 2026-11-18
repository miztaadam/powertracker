import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AdviceCard } from './AdviceCard'
import { PrimaryButton, QuietButton, SecondaryButton } from './Button'
import { CoachSuggestion } from './CoachSuggestion'
import { Dialog } from './Dialog'
import { LineChart } from './LineChart'
import { Menu } from './Menu'
import { PlateStrip, platesLabel } from './PlateStrip'
import { RatioBar } from './RatioBar'
import { RestFlash } from './RestFlash'
import { RestTimerBar, formatClock } from './RestTimerBar'
import { ChoiceChip, SegmentedChoice } from './SegmentedChoice'
import { SetRow } from './SetRow'
import { Snackbar } from './Snackbar'
import { formatKg } from './format'
import { StatTile } from './StatTile'
import { NumberField } from './NumberField'
import { Stepper, StepperField } from './StepperField'
import { setTapHaptics } from '../../platform/haptics'
import { Switch } from './Switch'
import { TabBar } from './TabBar'
import { TextField } from './TextField'
import { BoltIcon } from './icons'
import { BarStrip } from './BarStrip'
import { Sparkline } from './Sparkline'

describe('SetRow', () => {
  it('reads a done set as one sentence', () => {
    render(<SetRow index={1} weightKg={150} reps={5} rpe={8} state="done" />)
    expect(screen.getByText('Set 1: 150 kg × 5 at RPE 8, done')).toBeInTheDocument()
  })

  it('reads a failed set and shows the Failed pill', () => {
    const { container } = render(<SetRow index={3} weightKg={155} reps={3} rpe={10} state="failed" />)
    expect(screen.getByText('Set 3: 155 kg × 3 at RPE 10, failed')).toBeInTheDocument()
    expect(container.querySelector('[aria-hidden="true"]')).toHaveTextContent('Failed')
  })

  it('reads top set and personal record, and half-kilo weights and RPE', () => {
    render(<SetRow index={2} weightKg={152.5} reps={5} rpe={8.5} state="done" isTopSet isPr />)
    expect(screen.getByText('Set 2: 152.5 kg × 5 at RPE 8.5, top set, personal record, done')).toBeInTheDocument()
  })

  it('groups the top set and record pills so they can stack on a narrow phone', () => {
    const { container } = render(<SetRow index={2} weightKg={152.5} reps={5} rpe={8.5} state="done" isTopSet isPr />)
    expect(Array.from(container.querySelectorAll('.gr-set-pills > .gr-pill')).map((p) => p.textContent)).toEqual(['Top set', 'PR'])
    const plain = render(<SetRow index={1} weightKg={150} reps={5} rpe={8} state="done" />)
    expect(plain.container.querySelector('.gr-set-pills')).toBeNull()
  })

  it('reads a planned set without RPE', () => {
    render(<SetRow index={4} weightKg={150} reps={5} rpe={null} state="planned" />)
    expect(screen.getByText('Set 4: 150 kg × 5, planned')).toBeInTheDocument()
  })

  it('is a button named by the sentence when it has onClick', async () => {
    const onClick = vi.fn()
    render(<SetRow index={1} weightKg={150} reps={5} rpe={8} state="done" onClick={onClick} />)
    await userEvent.click(screen.getByRole('button', { name: 'Set 1: 150 kg × 5 at RPE 8, done' }))
    expect(onClick).toHaveBeenCalledOnce()
  })
})

describe('StepperField and Stepper', () => {
  it('has labelled − and + buttons that call back', async () => {
    const onDecrement = vi.fn()
    const onIncrement = vi.fn()
    render(
      <StepperField label="Weight" valueText="155" unit="kg" canDecrement canIncrement onDecrement={onDecrement} onIncrement={onIncrement} />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Weight' }))
    await userEvent.click(screen.getByRole('button', { name: 'Increase Weight' }))
    expect(onDecrement).toHaveBeenCalledOnce()
    expect(onIncrement).toHaveBeenCalledOnce()
    expect(screen.getByLabelText('Weight 155 kg')).toBeInTheDocument()
  })

  it('disables a button at its limit', () => {
    render(<StepperField label="Reps" valueText="1" canDecrement={false} canIncrement onDecrement={() => {}} onIncrement={() => {}} />)
    expect(screen.getByRole('button', { name: 'Decrease Reps' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Increase Reps' })).toBeEnabled()
  })

  it('shows a field in place of the figure when one is given', async () => {
    render(
      <StepperField
        label="Weight"
        valueText="155"
        unit="kg"
        canDecrement
        canIncrement
        onDecrement={() => {}}
        onIncrement={() => {}}
        field={<NumberField label="Weight, kilograms" valueText="155" unit="kg" inputMode="decimal" onDraft={() => {}} onCommit={() => {}} />}
      />,
    )
    expect(screen.getByRole('textbox', { name: 'Weight, kilograms' })).toHaveValue('155')
    expect(screen.getByRole('button', { name: 'Decrease Weight' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Increase Weight' })).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Weight 155 kg' })).toBeNull()
    // The caption and the unit belong to the field: a tap on either puts the cursor in it.
    await userEvent.click(screen.getByText('Weight'))
    expect(screen.getByRole('textbox')).toHaveFocus()
  })

  it('the one-line Stepper uses the same labels', () => {
    render(<Stepper label="Default RPE" valueText="8" canDecrement canIncrement onDecrement={() => {}} onIncrement={() => {}} />)
    expect(screen.getByRole('button', { name: 'Decrease Default RPE' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Increase Default RPE' })).toBeInTheDocument()
  })
})

describe('PlateStrip', () => {
  it('labels 182.5 kg as 25 + 25 + 25 + 5 + 1.25', () => {
    render(<PlateStrip perSide={[25, 25, 25, 5, 1.25]} totalKg={182.5} />)
    expect(screen.getByText('25 + 25 + 25 + 5 + 1.25')).toBeInTheDocument()
    expect(screen.getByText('per side · 20 kg bar')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Plates per side: 25 + 25 + 25 + 5 + 1.25' })).toBeInTheDocument()
  })

  it('says Empty bar for no plates', () => {
    expect(platesLabel([])).toBe('Empty bar')
    render(<PlateStrip perSide={[]} totalKg={20} />)
    expect(screen.getByText('Empty bar')).toBeInTheDocument()
  })

  it('says so in words when the weight cannot be loaded', () => {
    render(<PlateStrip perSide={null} totalKg={181} nearestKg={180} />)
    expect(screen.getByText("Can't load 181 kg exactly")).toBeInTheDocument()
    expect(screen.getByText('Nearest is 180 kg.')).toBeInTheDocument()
  })

  it('says when the weight is lighter than the bar', () => {
    render(<PlateStrip perSide={null} totalKg={15} />)
    expect(screen.getByText('Lighter than the bar')).toBeInTheDocument()
    expect(screen.getByText('The bar alone weighs 20 kg.')).toBeInTheDocument()
  })
})

describe('RatioBar', () => {
  it('says In range inside the range, bounds included', () => {
    render(<RatioBar label="Bench ÷ Squat" valuePct={64} lowPct={60} highPct={70} />)
    expect(screen.getByText('In range')).toBeInTheDocument()
    expect(screen.getByText('64%')).toBeInTheDocument()
  })

  it('says Below range and Above range outside it', () => {
    const { rerender } = render(<RatioBar label="Squat ÷ Deadlift" valuePct={76} lowPct={80} highPct={90} />)
    expect(screen.getByText('Below range')).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()
    expect(screen.getByText('90%')).toBeInTheDocument()
    rerender(<RatioBar label="Squat ÷ Deadlift" valuePct={93} lowPct={80} highPct={90} />)
    expect(screen.getByText('Above range')).toBeInTheDocument()
  })
})

describe('buttons', () => {
  it('click, and stay inert when disabled', async () => {
    const onClick = vi.fn()
    render(
      <>
        <PrimaryButton text="Log set 4" onClick={onClick} />
        <SecondaryButton text="Missed" onClick={onClick} enabled={false} />
        <QuietButton text="Dismiss" onClick={onClick} />
      </>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Log set 4' }))
    await userEvent.click(screen.getByRole('button', { name: 'Missed' }))
    await userEvent.click(screen.getByRole('button', { name: 'Dismiss' }))
    expect(onClick).toHaveBeenCalledTimes(2)
  })
})

describe('SegmentedChoice and ChoiceChip', () => {
  it('is a radio group that reports the picked index', async () => {
    const onSelect = vi.fn()
    render(<SegmentedChoice label="Where did it stall?" options={['Floor', 'Mid', 'Lockout']} selectedIndex={0} onSelect={onSelect} />)
    const group = screen.getByRole('radiogroup', { name: 'Where did it stall?' })
    expect(within(group).getByRole('radio', { name: 'Floor' })).toBeChecked()
    await userEvent.click(within(group).getByRole('radio', { name: 'Lockout' }))
    expect(onSelect).toHaveBeenCalledWith(2)
  })

  it('has nothing checked when selectedIndex is null', () => {
    render(<SegmentedChoice label="Range" options={['A', 'B']} selectedIndex={null} onSelect={() => {}} />)
    expect(screen.getAllByRole('radio').every((r) => !(r as HTMLInputElement).checked)).toBe(true)
  })

  // 1.12.3: the rest alert opts in to segments sized to their labels; every other use keeps equal segments.
  it('keeps equal segments unless sizeToLabels is set', () => {
    const { rerender } = render(<SegmentedChoice label="Range" options={['A', 'B']} selectedIndex={0} onSelect={() => {}} />)
    expect(screen.getByRole('radiogroup', { name: 'Range' })).not.toHaveClass('gr-segmented-fit')
    rerender(<SegmentedChoice label="Range" options={['A', 'B']} selectedIndex={0} onSelect={() => {}} sizeToLabels />)
    expect(screen.getByRole('radiogroup', { name: 'Range' })).toHaveClass('gr-segmented', 'gr-segmented-fit')
  })

  it('a chip is a checkbox', async () => {
    const onClick = vi.fn()
    render(<ChoiceChip text="Mon" selected onClick={onClick} />)
    const chip = screen.getByRole('checkbox', { name: 'Mon' })
    expect(chip).toBeChecked()
    await userEvent.click(chip)
    expect(onClick).toHaveBeenCalledOnce()
  })
})

describe('CoachSuggestion and AdviceCard', () => {
  it('offers Apply and Dismiss', async () => {
    const onApply = vi.fn()
    const onDismiss = vi.fn()
    render(<CoachSuggestion headline="155 × 5 @ 8" reason="Last time: 150 × 5 @ 7." onApply={onApply} onDismiss={onDismiss} />)
    expect(screen.getByText('Coach suggests')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }))
    await userEvent.click(screen.getByRole('button', { name: 'Dismiss' }))
    expect(onApply).toHaveBeenCalledOnce()
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('shows tag, title, body, detail and actions', async () => {
    const onPrimary = vi.fn()
    render(
      <AdviceCard tone="warning" icon={<BoltIcon />} tag="Stall · Bench" title="Bench has stalled" body="Flat for 3 weeks." primaryAction="Add to plan" onPrimary={onPrimary} secondaryAction="Dismiss">
        <p>detail</p>
      </AdviceCard>,
    )
    expect(screen.getByRole('heading', { name: 'Bench has stalled' })).toBeInTheDocument()
    expect(screen.getByText('Stall · Bench')).toBeInTheDocument()
    expect(screen.getByText('detail')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Add to plan' }))
    expect(onPrimary).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument()
  })
})

describe('StatTile', () => {
  it('is plain without onClick and a labelled button with it', async () => {
    const onClick = vi.fn()
    const { rerender } = render(<StatTile label="Squat" value="190.5" unit="kg" note="+5.5 kg" noteTone="success" />)
    expect(screen.queryByRole('button')).toBeNull()
    expect(screen.getByText('+5.5 kg')).toBeInTheDocument()
    rerender(<StatTile label="Squat" value="190.5" unit="kg" onClick={onClick} onClickLabel="Open squat history" />)
    await userEvent.click(screen.getByRole('button', { name: /Open squat history/ }))
    expect(onClick).toHaveBeenCalledOnce()
  })
})

describe('LineChart', () => {
  it('names the latest values and shows them in the legend, skipping missing weeks', () => {
    render(
      <LineChart
        series={[
          { name: 'Squat', color: 'var(--chart-squat)', values: [170, null, 187.5, null] },
          { name: 'Bench', color: 'var(--chart-bench)', values: [110, 112.5, 112.46, 122.5] },
        ]}
        xLabels={['Aug', '', '', 'Nov']}
      />,
    )
    expect(screen.getByRole('img', { name: 'Line chart. Latest: Squat 187.5 kg, Bench 122.5 kg' })).toBeInTheDocument()
    expect(screen.getByText('187.5')).toBeInTheDocument()
  })
})

describe('RestTimerBar', () => {
  it('formats the clock', () => {
    expect(formatClock(108)).toBe('1:48')
    expect(formatClock(5)).toBe('0:05')
  })

  it('floors a fraction of a second and never shows less than zero', () => {
    const props = { totalSec: 180, onSubtractThirty: () => {}, onAddThirty: () => {}, onSkip: () => {} }
    const { rerender, container } = render(<RestTimerBar remainingSec={108.5} {...props} />)
    expect(screen.getByRole('timer', { name: 'Rest, 1:48 left' })).toHaveTextContent('1:48')
    rerender(<RestTimerBar remainingSec={-3} {...props} />)
    expect(screen.getByRole('timer', { name: 'Rest, 0:00 left' })).toHaveTextContent('0:00')
    expect(container.querySelector<HTMLElement>('.gr-rest-fill')!.style.width).toBe('0%')
    rerender(<RestTimerBar remainingSec={Number.NaN} {...props} />)
    expect(screen.getByRole('timer', { name: 'Rest, 0:00 left' })).toBeInTheDocument()
  })

  it('shows time left and the three actions', async () => {
    const onSubtractThirty = vi.fn()
    const onAddThirty = vi.fn()
    const onSkip = vi.fn()
    render(<RestTimerBar remainingSec={108} totalSec={180} onSubtractThirty={onSubtractThirty} onAddThirty={onAddThirty} onSkip={onSkip} />)
    expect(screen.getByRole('timer', { name: 'Rest, 1:48 left' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '−30 s' }))
    await userEvent.click(screen.getByRole('button', { name: '+30 s' }))
    await userEvent.click(screen.getByRole('button', { name: 'Skip' }))
    expect(onSubtractThirty).toHaveBeenCalledOnce()
    expect(onAddThirty).toHaveBeenCalledOnce()
    expect(onSkip).toHaveBeenCalledOnce()
  })
})

describe('TabBar', () => {
  it('links the five tabs and marks the current one', () => {
    render(<TabBar current="coach" />)
    const nav = screen.getByRole('navigation', { name: 'Main' })
    expect(within(nav).getAllByRole('link').map((a) => a.getAttribute('href'))).toEqual(['#/today', '#/coach', '#/progress', '#/exercises', '#/plan'])
    expect(within(nav).getByRole('link', { name: 'Coach' })).toHaveAttribute('aria-current', 'page')
    expect(within(nav).getByRole('link', { name: 'Today' })).not.toHaveAttribute('aria-current')
  })
})

describe('Dialog', () => {
  it('renders nothing when closed', () => {
    render(<Dialog open={false} title="Replace all data?" onDismiss={() => {}} />)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('is a labelled modal that takes focus and closes on Escape', async () => {
    const onDismiss = vi.fn()
    render(
      <Dialog open title="Replace all data?" onDismiss={onDismiss} actions={<PrimaryButton text="Replace" onClick={() => {}} compact />}>
        Restoring replaces everything in Grit with the contents of this backup.
      </Dialog>,
    )
    const dialog = screen.getByRole('dialog', { name: 'Replace all data?' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(dialog.contains(document.activeElement)).toBe(true)
    await userEvent.keyboard('{Escape}')
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('keeps Tab inside the dialog', async () => {
    render(
      <>
        <button>outside</button>
        <Dialog open title="T" onDismiss={() => {}} actions={<><QuietButton text="Cancel" onClick={() => {}} /><PrimaryButton text="OK" onClick={() => {}} compact /></>} />
      </>,
    )
    await userEvent.tab()
    await userEvent.tab()
    await userEvent.tab()
    expect(screen.getByRole('dialog').contains(document.activeElement)).toBe(true)
  })
})

describe('Dialog carry-ins', () => {
  function withInput(onDismiss: () => void) {
    render(
      <Dialog open title="Weight" onDismiss={onDismiss}>
        <input aria-label="kg" defaultValue="150" />
      </Dialog>,
    )
    const scrim = screen.getByRole('dialog').parentElement!
    return { scrim, input: screen.getByLabelText('kg') }
  }

  it('closes on a tap that starts and ends on the scrim', () => {
    const onDismiss = vi.fn()
    const { scrim } = withInput(onDismiss)
    fireEvent.pointerDown(scrim)
    fireEvent.pointerUp(scrim)
    fireEvent.click(scrim)
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('stays open when a press starts inside the dialog and ends on the scrim', () => {
    const onDismiss = vi.fn()
    const { scrim, input } = withInput(onDismiss)
    // Selecting the text in the field and letting go outside the dialog: the click lands on the scrim.
    fireEvent.pointerDown(input)
    fireEvent.pointerUp(scrim)
    fireEvent.click(scrim)
    expect(onDismiss).not.toHaveBeenCalled()
    // The next real tap on the scrim still closes it.
    fireEvent.pointerDown(scrim)
    fireEvent.pointerUp(scrim)
    fireEvent.click(scrim)
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('stays open when a press starts on the scrim and ends inside the dialog', () => {
    const onDismiss = vi.fn()
    const { scrim, input } = withInput(onDismiss)
    // The browser sends the click to the nearest element both ends share: the scrim.
    fireEvent.pointerDown(scrim)
    fireEvent.pointerUp(input)
    fireEvent.click(scrim)
    expect(onDismiss).not.toHaveBeenCalled()
  })

  it('makes everything behind it inert while open, and usable again on close', () => {
    function Page({ open }: { open: boolean }) {
      return (
        <>
          <nav data-testid="tabs" />
          <main data-testid="main">
            <button>behind</button>
            <section data-testid="screen">
              <p data-testid="content" />
              <Dialog open={open} title="T" onDismiss={() => {}} />
            </section>
          </main>
        </>
      )
    }
    // Something outside the React root, as a second root or a browser extension would add.
    const outside = document.createElement('div')
    document.body.append(outside)
    try {
      const { rerender, container } = render(<Page open />)
      for (const id of ['tabs', 'content']) expect(screen.getByTestId(id)).toHaveAttribute('inert')
      expect(screen.getByRole('button', { name: 'behind', hidden: true })).toHaveAttribute('inert')
      expect(outside).toHaveAttribute('inert')
      // The dialog and the elements it sits in stay reachable.
      for (const el of [screen.getByTestId('main'), screen.getByTestId('screen'), container, screen.getByRole('dialog')]) expect(el).not.toHaveAttribute('inert')

      rerender(<Page open={false} />)
      expect(document.querySelectorAll('[inert]')).toHaveLength(0)
    } finally {
      outside.remove()
    }
  })

  it('leaves alone what was already inert', () => {
    const page = (open: boolean) => (
      <>
        <aside data-testid="aside" />
        <Dialog open={open} title="T" onDismiss={() => {}} />
      </>
    )
    const { rerender } = render(page(false))
    screen.getByTestId('aside').setAttribute('inert', '')
    rerender(page(true))
    rerender(page(false))
    expect(screen.getByTestId('aside')).toHaveAttribute('inert')
  })

  it('does not make the page inert when isolate is off (the showcase)', () => {
    render(
      <>
        <button>behind</button>
        <Dialog open title="T" onDismiss={() => {}} isolate={false} />
      </>,
    )
    expect(screen.getByRole('button', { name: 'behind' })).not.toHaveAttribute('inert')
  })

  it('leaves the focus on a field that asked for it', () => {
    render(
      <Dialog open title="Weight" onDismiss={() => {}}>
        <TextField label="Kilograms" value="150" onChange={() => {}} autoFocus />
      </Dialog>,
    )
    expect(screen.getByLabelText('Kilograms')).toHaveFocus()
  })

  it('gives the focus back to what opened it, also when a field inside took the focus', () => {
    const page = (open: boolean) => (
      <>
        <button>opener</button>
        {open && (
          <Dialog open title="Weight" onDismiss={() => {}}>
            <TextField label="Kilograms" value="150" onChange={() => {}} autoFocus />
          </Dialog>
        )}
      </>
    )
    const { rerender } = render(page(false))
    screen.getByRole('button', { name: 'opener' }).focus()
    rerender(page(true))
    expect(screen.getByLabelText('Kilograms')).toHaveFocus()
    rerender(page(false))
    expect(screen.getByRole('button', { name: 'opener' })).toHaveFocus()
  })
})

describe('formatKg', () => {
  it('hides float noise and trailing zeros', () => {
    expect(formatKg(152.50000000000003)).toBe('152.5')
    expect(formatKg(0.1 + 0.2)).toBe('0.3')
    expect(formatKg(155)).toBe('155')
    expect(formatKg(181.25)).toBe('181.25')
    expect(formatKg(60.004)).toBe('60')
  })
})

describe('SegmentedChoice reselect', () => {
  it('reports a tap on the option that is already selected, so a caller can clear it', async () => {
    const onSelect = vi.fn()
    render(<SegmentedChoice options={['Floor', 'Mid', 'Lockout']} selectedIndex={2} onSelect={onSelect} label="Where did it stall?" />)
    await userEvent.click(screen.getByRole('radio', { name: 'Lockout' }))
    expect(onSelect).toHaveBeenCalledExactlyOnceWith(2)
    await userEvent.click(screen.getByRole('radio', { name: 'Mid' }))
    expect(onSelect).toHaveBeenCalledTimes(2)
    expect(onSelect).toHaveBeenLastCalledWith(1)
  })
})

describe('Snackbar', () => {
  it('restarts its timer when the same message is shown again', () => {
    vi.useFakeTimers()
    try {
      const onDismiss = vi.fn()
      const { rerender } = render(<Snackbar message="Set deleted." showId={1} onDismiss={onDismiss} durationMs={4000} />)
      const firstNode = screen.getByText('Set deleted.').parentElement
      vi.advanceTimersByTime(3000)
      rerender(<Snackbar message="Set deleted." showId={2} onDismiss={onDismiss} durationMs={4000} />)
      // A new element, so the entry animation plays again.
      expect(screen.getByText('Set deleted.').parentElement).not.toBe(firstNode)
      vi.advanceTimersByTime(3999)
      expect(onDismiss).not.toHaveBeenCalled()
      vi.advanceTimersByTime(1)
      expect(onDismiss).toHaveBeenCalledOnce()
    } finally {
      vi.useRealTimers()
    }
  })

  it('announces the message politely and offers its action', async () => {
    const onAction = vi.fn()
    render(<Snackbar message="Update ready — tap to refresh" actionLabel="Refresh" onAction={onAction} />)
    expect(screen.getByRole('status')).toHaveTextContent('Update ready — tap to refresh')
    await userEvent.click(screen.getByRole('button', { name: 'Refresh' }))
    expect(onAction).toHaveBeenCalledOnce()
  })

  it('dismisses itself after its duration and shows nothing for null', () => {
    vi.useFakeTimers()
    try {
      const onDismiss = vi.fn()
      const { rerender } = render(<Snackbar message="Backup saved." onDismiss={onDismiss} durationMs={4000} />)
      vi.advanceTimersByTime(3999)
      expect(onDismiss).not.toHaveBeenCalled()
      vi.advanceTimersByTime(1)
      expect(onDismiss).toHaveBeenCalledOnce()
      rerender(<Snackbar message={null} />)
      expect(screen.getByRole('status')).toBeEmptyDOMElement()
    } finally {
      vi.useRealTimers()
    }
  })
})

describe('Switch and TextField', () => {
  it('a switch reports the new value', async () => {
    const onChange = vi.fn()
    render(<Switch label="Change plates" checked={false} onChange={onChange} />)
    await userEvent.click(screen.getByRole('switch', { name: 'Change plates' }))
    expect(onChange).toHaveBeenCalledWith(true)
  })

  it('a numeric field asks for the decimal keypad', async () => {
    const onChange = vi.fn()
    render(<TextField label="Weight" value="" onChange={onChange} numeric unit="kg" />)
    const input = screen.getByLabelText('Weight')
    expect(input).toHaveAttribute('inputmode', 'decimal')
    await userEvent.type(input, '1')
    expect(onChange).toHaveBeenCalledWith('1')
  })
})

describe('Menu', () => {
  const show = (onEdit = vi.fn(), onDelete = vi.fn()) => {
    render(
      <>
        <Menu
          label="More"
          items={[
            { text: 'Edit', onSelect: onEdit },
            { text: 'Delete', danger: true, onSelect: onDelete },
          ]}
        />
        <p>Elsewhere</p>
      </>,
    )
    return { onEdit, onDelete, trigger: screen.getByRole('button', { name: 'More' }) }
  }

  it('opens under its button, and choosing an action closes it', async () => {
    const { onEdit, onDelete, trigger } = show()
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByRole('menu')).toBeNull()

    await userEvent.click(trigger)
    expect(trigger).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual(['Edit', 'Delete'])
    expect(screen.getByRole('menuitem', { name: 'Edit' })).toHaveFocus()

    await userEvent.click(screen.getByRole('menuitem', { name: 'Delete' }))
    expect(onDelete).toHaveBeenCalledOnce()
    expect(onEdit).not.toHaveBeenCalled()
    expect(screen.queryByRole('menu')).toBeNull()
    expect(trigger).toHaveFocus()
  })

  it('closes on Escape and on a tap outside, without choosing', async () => {
    const { onEdit, onDelete, trigger } = show()
    await userEvent.click(trigger)
    await userEvent.keyboard('{ArrowDown}')
    expect(screen.getByRole('menuitem', { name: 'Delete' })).toHaveFocus()
    await userEvent.keyboard('{ArrowDown}')
    expect(screen.getByRole('menuitem', { name: 'Edit' })).toHaveFocus()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).toBeNull()
    expect(trigger).toHaveFocus()

    await userEvent.click(trigger)
    await userEvent.click(screen.getByText('Elsewhere'))
    expect(screen.queryByRole('menu')).toBeNull()
    expect(onEdit).not.toHaveBeenCalled()
    expect(onDelete).not.toHaveBeenCalled()
  })
})

describe('RestFlash', () => {
  it('shows nothing until a rest ends, then one overlay that removes itself', () => {
    vi.useFakeTimers()
    try {
      const { rerender } = render(<RestFlash showId={0} />)
      expect(screen.queryByTestId('rest-flash')).toBeNull()
      rerender(<RestFlash showId={1} />)
      expect(screen.getByTestId('rest-flash')).toHaveAttribute('aria-hidden', 'true')
      act(() => void vi.advanceTimersByTime(1_500))
      expect(screen.queryByTestId('rest-flash')).toBeNull()
      // The next rest flashes again.
      rerender(<RestFlash showId={2} />)
      expect(screen.getByTestId('rest-flash')).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it('is a 300 ms accent overlay, and a fade where motion is reduced', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/ui/components/RestFlash.css'), 'utf8')
    const [normal, reduced] = css.split('@media (prefers-reduced-motion: reduce)') as [string, string]
    expect(normal).toMatch(/\.gr-rest-flash\s*\{[^}]*background:\s*var\(--accent\)/)
    expect(normal).toMatch(/animation:\s*gr-rest-flash 300ms/)
    expect(normal).toMatch(/pointer-events:\s*none/)
    // The app-wide reduced-motion rule cuts every animation to nothing; the fade has to outrank it.
    expect(reduced).toMatch(/animation:\s*gr-rest-fade [^;]*!important/)
  })
})

describe('NumberField', () => {
  function Typed({ onCommit = () => {}, error }: { onCommit?: (text: string) => void; error?: string }) {
    const [text, setText] = useState('155')
    return <NumberField label="Weight, kilograms" valueText={text} unit="kg" placeholder="kg" inputMode="decimal" error={error} onDraft={setText} onCommit={onCommit} />
  }

  it('is a labelled input with the keypad it asks for', () => {
    render(<NumberField label="Reps" valueText="5" inputMode="numeric" onDraft={() => {}} onCommit={() => {}} />)
    const input = screen.getByRole('textbox', { name: 'Reps' })
    expect(input).toHaveValue('5')
    expect(input).toHaveAttribute('inputmode', 'numeric')
    expect(input).toHaveAttribute('type', 'text')
    expect(input).toBeValid()
  })

  it('selects the value on focus, reports each keystroke, and commits on Enter and on blur', async () => {
    const onCommit = vi.fn()
    render(<Typed onCommit={onCommit} />)
    const input = screen.getByRole<HTMLInputElement>('textbox')
    await userEvent.click(input)
    expect([input.selectionStart, input.selectionEnd]).toEqual([0, 3])
    await userEvent.keyboard('16')
    expect(input).toHaveValue('16')
    expect(onCommit).not.toHaveBeenCalled()

    // Enter leaves the field, which commits once.
    await userEvent.keyboard('{Enter}')
    expect(input).not.toHaveFocus()
    expect(onCommit).toHaveBeenCalledExactlyOnceWith('16')

    await userEvent.click(input)
    await userEvent.keyboard('170')
    await userEvent.tab()
    expect(onCommit).toHaveBeenLastCalledWith('170')
    expect(onCommit).toHaveBeenCalledTimes(2)
  })

  it('shows the placeholder instead of the unit while empty', () => {
    const { rerender } = render(<NumberField label="Weight, kilograms" valueText="" unit="kg" placeholder="kg" inputMode="decimal" onDraft={() => {}} onCommit={() => {}} />)
    expect(screen.getByRole('textbox')).toHaveAttribute('placeholder', 'kg')
    expect(screen.queryByText('kg')).toBeNull()
    rerender(<NumberField label="Weight, kilograms" valueText="60" unit="kg" placeholder="kg" inputMode="decimal" onDraft={() => {}} onCommit={() => {}} />)
    expect(screen.getByText('kg')).toBeInTheDocument()
  })

  it('reads the error with the field', () => {
    render(<Typed error="0 to 1000 kg" />)
    expect(screen.getByRole('textbox')).toBeInvalid()
    expect(screen.getByRole('textbox')).toHaveAccessibleDescription('0 to 1000 kg')
    expect(screen.getByRole('alert')).toHaveTextContent('0 to 1000 kg')
  })
})

// ---- Stage 2: only the set entry steppers tick (owner decision 2: weight, reps and RPE on Today; nothing else buzzes) ----
describe('Stepper haptics', () => {
  let vibrate: ReturnType<typeof vi.fn>
  beforeEach(() => {
    vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
  })
  afterEach(() => {
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('a StepperField with tick: each step ticks once, a disabled one never', async () => {
    const onIncrement = vi.fn()
    render(<StepperField label="Reps" valueText="1" canDecrement={false} canIncrement onDecrement={() => {}} onIncrement={onIncrement} tick />)
    await userEvent.click(screen.getByRole('button', { name: 'Increase Reps' }))
    expect(onIncrement).toHaveBeenCalledOnce()
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Reps' }))
    expect(vibrate).toHaveBeenCalledOnce()
  })

  it('a StepperField does not tick by default, and the one-line Stepper never does', async () => {
    const onIncrement = vi.fn()
    render(
      <>
        <StepperField label="Reps" valueText="5" canDecrement canIncrement onDecrement={() => {}} onIncrement={onIncrement} />
        <Stepper label="Rest" valueText="3:00" canDecrement canIncrement onDecrement={() => {}} onIncrement={onIncrement} />
      </>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Increase Reps' }))
    await userEvent.click(screen.getByRole('button', { name: 'Increase Rest' }))
    expect(onIncrement).toHaveBeenCalledTimes(2)
    expect(vibrate).not.toHaveBeenCalled()
  })
})

// Stage 4: the charts on Progress fill in when they mount. The animation is CSS only (charts.css, RatioBar.css), so the
// DOM is the finished chart from the first frame: every point, bar height, marker and label is already in place.
describe('charts that fill in', () => {
  const series = [
    { name: 'Squat', color: 'var(--chart-squat)', values: [170, null, 187.5] },
    { name: 'Bench', color: 'var(--chart-bench)', values: [110, 112.5, 122.5] },
  ]

  it('a line chart is complete in the DOM, with its name and values', () => {
    const { container } = render(<LineChart series={series} xLabels={['Aug', '', 'Nov']} fillIn />)
    expect(container.querySelector('.gr-linechart')).toHaveClass('gr-fill-in')
    expect(screen.getByRole('img', { name: 'Line chart. Latest: Squat 187.5 kg, Bench 122.5 kg' })).toBeInTheDocument()
    // The same lines as the chart that does not fill in: every point is there from the start.
    const still = render(<LineChart series={series} xLabels={['Aug', '', 'Nov']} />).container
    const points = (root: Element) => Array.from(root.querySelectorAll('polyline')).map((p) => p.getAttribute('points'))
    expect(points(container)).toHaveLength(2)
    expect(points(container)).toEqual(points(still))
    expect(container.querySelectorAll('.gr-plot-marker')).toHaveLength(2)
    expect(within(container as HTMLElement).getByText('187.5')).toBeInTheDocument()
  })

  it('a sparkline is complete in the DOM', () => {
    const { container } = render(<Sparkline values={[1, 3, 2]} color="red" fillIn />)
    expect(container.querySelector('.gr-spark')).toHaveClass('gr-fill-in')
    expect(container.querySelector('polyline')!.getAttribute('points')).toBe('0,100 50,0 100,50')
    expect(container.querySelector('.gr-spark-fill')).not.toBeNull()
    expect(container.querySelector('.gr-plot-marker')).not.toBeNull()
  })

  it('bars are at their full heights, each with its place in the stagger', () => {
    const { container } = render(<BarStrip values={[50, 0, 100]} color="red" fillIn />)
    expect(container.querySelector('.gr-bars')).toHaveClass('gr-fill-in')
    const bars = Array.from(container.querySelectorAll<HTMLElement>('.gr-bar'))
    expect(bars.map((b) => b.style.height)).toEqual(['50%', '', '100%'])
    expect(bars.map((b) => b.style.getPropertyValue('--i'))).toEqual(['0', '1', '2'])
  })

  it('the ratio marker sits at its value, with the figures written out', () => {
    const { container } = render(<RatioBar label="Bench ÷ Squat" valuePct={65} lowPct={60} highPct={70} fillIn />)
    expect(container.querySelector('.gr-ratio')).toHaveClass('gr-fill-in')
    const marker = container.querySelector<HTMLElement>('.gr-ratio-marker')!
    expect(marker.style.getPropertyValue('--x')).toBe('50%')
    expect(marker.style.getPropertyValue('--xq')).toBe('50')
    expect(screen.getByText('65%')).toBeInTheDocument()
    expect(screen.getByText('In range')).toBeInTheDocument()
  })

  it('stays still where it is not asked for (Coach, the showcase)', () => {
    const { container } = render(
      <>
        <LineChart series={series} xLabels={['Aug', '', 'Nov']} />
        <Sparkline values={[1, 3, 2]} color="red" />
        <BarStrip values={[50, 100]} color="red" />
        <RatioBar label="Bench ÷ Squat" valuePct={65} lowPct={60} highPct={70} />
      </>,
    )
    expect(container.querySelector('.gr-fill-in')).toBeNull()
    expect(container.querySelector<HTMLElement>('.gr-bar')!.style.getPropertyValue('--i')).toBe('')
    expect(container.querySelector<HTMLElement>('.gr-ratio-marker')!.style.getPropertyValue('--xq')).toBe('')
  })

  it('new data on the same chart keeps its nodes, so it does not draw again', () => {
    const { container, rerender } = render(<LineChart series={series} xLabels={['Aug', '', 'Nov']} fillIn />)
    const svg = container.querySelector('svg')
    const bars = render(<BarStrip values={[0, 100]} color="red" fillIn />)
    const stub = bars.container.querySelector('.gr-bar-stub')
    rerender(<LineChart series={[{ ...series[0]!, values: [170, 180, 190] }, series[1]!]} xLabels={['Aug', '', 'Nov']} fillIn />)
    bars.rerender(<BarStrip values={[40, 100]} color="red" fillIn />)
    expect(container.querySelector('svg')).toBe(svg)
    // An empty week that fills is the same bar, so it does not grow again either.
    expect(bars.container.querySelector('.gr-bar')).toBe(stub)
  })

  it('animates only with motion on', () => {
    for (const file of ['charts.css', 'RatioBar.css']) {
      const css = readFileSync(resolve(process.cwd(), 'src/ui/components', file), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')
      const rules = Array.from(css.matchAll(/([^{}]+)\{([^{}]*)\}/g)).filter(([, , body]) => /\banimation|container-type/.test(body!))
      expect(rules.length).toBeGreaterThan(0)
      for (const [, selector] of rules) {
        for (const one of selector!.split(',')) expect(one.trim()).toMatch(/^:root:not\(\[data-motion='off'\]\) /)
      }
    }
  })
})
