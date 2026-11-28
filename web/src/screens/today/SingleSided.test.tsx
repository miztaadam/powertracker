import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { JsExercise } from '../../domain'
import type { TodayActions, TodayItem } from '../../state/useToday'
import { ExerciseCard } from './ExerciseCard'

function pecDeck(overrides: Partial<TodayItem> = {}): TodayItem {
  return {
    exercise: new JsExercise(42, 'Pec deck', 'ACCESSORY', null, ['Chest'], true, false, 'OFF', true),
    planned: null,
    suggestion: null,
    sets: [],
    editor: { weightKg: 40, reps: 12, rpe: 8 },
    targetSets: 3,
    isComplete: false,
    perSide: null,
    lastWeightKg: null,
    lastTime: null,
    warmUpOpen: false,
    warmUp: [],
    warmUpDone: [],
    singleSided: true,
    singleSidedOffered: true,
    ...overrides,
  }
}

function stubActions(overrides: Partial<TodayActions> = {}): TodayActions {
  return {
    startSession: vi.fn(async () => {}),
    edit: vi.fn(),
    tickWarmUp: vi.fn(),
    logSet: vi.fn(async () => {}),
    confirmPending: vi.fn(async () => {}),
    cancelPending: vi.fn(),
    setMissPoint: vi.fn(async () => {}),
    deleteSet: vi.fn(async () => {}),
    applyTarget: vi.fn(async () => {}),
    dismissTarget: vi.fn(async () => {}),
    addExercise: vi.fn(),
    addFromCatalogue: vi.fn(() => null),
    createExercise: vi.fn(async () => null),
    addToOpenSession: vi.fn(async () => null),
    removeExercise: vi.fn(),
    skip: vi.fn(async () => null),
    undoSkip: vi.fn(async () => {}),
    bringBack: vi.fn(async () => {}),
    finishSession: vi.fn(async () => {}),
    dismissSummary: vi.fn(),
    refreshDay: vi.fn(),
    ...overrides,
  }
}

describe('Single-sided set logging on ExerciseCard', () => {
  it('logs Right then Left as one set using the weaker side for Coach & Progress', async () => {
    const user = userEvent.setup()
    const logSet = vi.fn(async () => {})
    const edit = vi.fn()
    const actions = stubActions({ logSet, edit })

    const item = pecDeck()
    const { rerender } = render(
      <ExerciseCard
        item={item}
        position={1}
        count={1}
        weightStepKg={2.5}
        changePlates={false}
        next={{ label: 'Finish session', onClick: vi.fn() }}
        actions={actions}
      />,
    )

    // Starts on Right (1/2)
    await user.click(screen.getByRole('button', { name: 'Log Right (1/2)' }))
    expect(logSet).not.toHaveBeenCalled()
    expect(screen.getByTestId('single-sided-pending')).toHaveTextContent('Right done · now log Left')
    expect(screen.getByRole('button', { name: 'Log Left · Set 1' })).toBeInTheDocument()

    // Adjust reps down to 10 for Left
    rerender(
      <ExerciseCard
        item={pecDeck({ editor: { weightKg: 40, reps: 10, rpe: 8.5 } })}
        position={1}
        count={1}
        weightStepKg={2.5}
        changePlates={false}
        next={{ label: 'Finish session', onClick: vi.fn() }}
        actions={actions}
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Log Left · Set 1' }))
    expect(logSet).toHaveBeenCalledTimes(1)
    expect(logSet).toHaveBeenCalledWith(42, {
      weightKg: 40,
      reps: 10,
      rpe: 8.5,
      failed: false,
      splitSides: {
        right: { weightKg: 40, reps: 12, rpe: 8, failed: false },
        left: { weightKg: 40, reps: 10, rpe: 8.5, failed: false },
      },
    })
  })

  it('allows undoing the first side or starting with Left', async () => {
    const user = userEvent.setup()
    const actions = stubActions()
    render(
      <ExerciseCard
        item={pecDeck()}
        position={1}
        count={1}
        weightStepKg={2.5}
        changePlates={false}
        next={{ label: 'Finish session', onClick: vi.fn() }}
        actions={actions}
      />,
    )

    await user.click(screen.getByRole('radio', { name: 'Left' }))
    await user.click(screen.getByRole('button', { name: 'Log Left (1/2)' }))
    expect(screen.getByTestId('single-sided-pending')).toHaveTextContent('Left done · now log Right')

    await user.click(screen.getByRole('button', { name: 'Undo' }))
    expect(screen.queryByTestId('single-sided-pending')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Log Left (1/2)' })).toBeInTheDocument()
  })

  it('displays the R · L summary pill on logged single-sided sets', () => {
    const actions = stubActions()
    const item = pecDeck({
      sets: [
        {
          id: 101,
          sessionId: 1,
          exerciseId: 42,
          weightKg: 40,
          reps: 10,
          rpe: 8.5,
          failed: false,
          missPoint: null,
          isTopSet: false,
          splitSides: {
            right: { weightKg: 40, reps: 12, rpe: 8, failed: false },
            left: { weightKg: 40, reps: 10, rpe: 8.5, failed: false },
          },
        },
      ],
    })
    render(
      <ExerciseCard
        item={item}
        position={1}
        count={1}
        weightStepKg={2.5}
        changePlates={false}
        next={{ label: 'Finish session', onClick: vi.fn() }}
        actions={actions}
      />,
    )
    expect(screen.getByText('R 12 · L 10')).toBeInTheDocument()
  })
})
