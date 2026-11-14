import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, replacePlan } from '../../db/catalogue'
import { updateSettings, type GritDb } from '../../db/db'
import { grit } from '../../domain'
import { draftItems } from '../../state/usePlanBuilder'
import { TODAY, addDays, exerciseNamed, todayFixture } from '../../test/todayFixture'
import { CheckInScreen, GROWING_LINE, GROWING_QUESTION, LAGGING_LINE, LAGGING_QUESTION, SAVED_TITLE } from './CheckInScreen'

/** Bodybuilding mode stage 3 (plan Task 2, "What the lifter sees" 2): the check-in screen. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING' }))
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

/** The stage 2 builder's default three days (Monday, Wednesday, Friday), saved as the bodybuilding plan. */
async function defaultPlan() {
  await replacePlan(db, 'BODYBUILDING', draftItems(grit.planBuilderBuild([1, 3, 5], [], [], 0), []))
}

const MORE_WORK_OPEN = {
  type: 'MORE_WORK',
  lift: null,
  createdAt: addDays(TODAY, -28),
  reason: 'old',
  severity: 0,
  status: 'OPEN',
  payloadJson: '{"type":"com.powertracker.domain.model.AdvicePayload.MoreWork","group":"CALVES","addSets":[],"newExercise":null,"weeklyBefore":6,"weeklyAfter":6}',
  mode: 'BODYBUILDING' as const,
}

function show(props: { onLeave?: () => void; onSeeCoach?: () => void; onDone?: () => void } = {}) {
  const handlers = { onLeave: vi.fn(), onSeeCoach: vi.fn(), onDone: vi.fn(), ...props }
  render(<CheckInScreen db={db} today={() => TODAY} {...handlers} />)
  return handlers
}

const box = (name: string) => screen.getByRole('checkbox', { name })
const tap = async (name: string) => userEvent.click(await screen.findByRole('button', { name }))

describe('the check-in screen', () => {
  it('threeSteps_withTheirWording_andBack', async () => {
    await defaultPlan()
    show()
    expect(await screen.findByRole('heading', { name: LAGGING_QUESTION })).toBeInTheDocument()
    expect(LAGGING_QUESTION).toBe('Which body parts are lagging?')
    expect(LAGGING_LINE).toBe('Leave a body part unticked if it is on track.')
    expect(screen.getByText(LAGGING_LINE)).toBeInTheDocument()
    expect(screen.getByText('Step 1 of 2')).toBeInTheDocument()
    // All 18 groups, nothing ticked at first.
    const grid = screen.getByRole('group', { name: LAGGING_QUESTION })
    expect(within(grid).getAllByRole('checkbox')).toHaveLength(18)
    expect(within(grid).getAllByRole('checkbox').every((c) => !(c as HTMLInputElement).checked)).toBe(true)
    expect(within(grid).getAllByRole('checkbox').map((c) => c.getAttribute('aria-label') ?? c.parentElement?.textContent)).toEqual(grit.muscleGroups().map((g) => grit.muscleGroupLabel(g)))
    await userEvent.click(box('Chest'))
    await tap('Next')

    expect(screen.getByRole('heading', { name: GROWING_QUESTION })).toBeInTheDocument()
    expect(GROWING_QUESTION).toBe('Which are growing well?')
    expect(screen.getByText('Step 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument()
    await tap('Back')
    expect(screen.getByRole('heading', { name: LAGGING_QUESTION })).toBeInTheDocument()
    expect((box('Chest') as HTMLInputElement).checked).toBe(true)
  })

  it('aGroupTickedLagging_isShownButCannotBeTickedGrowingWell', async () => {
    await defaultPlan()
    show()
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Chest' }))
    await tap('Next')
    expect(screen.getByText(GROWING_LINE)).toBeInTheDocument()
    expect(GROWING_LINE).toBe("Body parts marked lagging can't also be growing well.")
    const chest = box('Chest') as HTMLInputElement
    expect(chest).toBeDisabled()
    expect(chest.checked).toBe(false)
    await userEvent.click(chest)
    expect(chest.checked).toBe(false)
    await userEvent.click(box('Quads'))
    expect((box('Quads') as HTMLInputElement).checked).toBe(true)
    // Ticking Quads lagging after all takes it off growing well.
    await tap('Back')
    await userEvent.click(box('Quads'))
    await tap('Next')
    expect(box('Quads')).toBeDisabled()
    await tap('Save')
    expect((await db.checkIns.toArray()).map((c) => [c.lagging, c.growingWell])).toEqual([[['CHEST', 'QUADS'], []]])
  })

  it('save_storesOneCheckIn_dismissesTheOldMoreWork_andStoresTheNew_thenSaysWhatHappened', async () => {
    await defaultPlan()
    await db.advice.add(MORE_WORK_OPEN)
    await db.checkIns.add({ date: TODAY, lagging: ['CALVES'], growingWell: [] }) // replaced: one per date
    const { onSeeCoach, onDone } = show()
    // Calves (6 sets a week), side delts (9) and chest (10) get more work; traps waits: the target groups come first.
    for (const name of ['Chest', 'Calves', 'Side delts', 'Traps']) await userEvent.click(await screen.findByRole('checkbox', { name }))
    await tap('Next')
    await userEvent.click(box('Quads'))
    await tap('Save')

    expect(await screen.findByRole('heading', { name: SAVED_TITLE })).toBeInTheDocument()
    expect(SAVED_TITLE).toBe('Check-in saved')
    expect(screen.queryByText('Step 2 of 2')).toBeNull()
    const lines = screen.getByRole('list', { name: 'Lagging body parts' })
    expect(within(lines).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'ChestMore work suggested on Coach',
      'TrapsThe coach adds to three body parts at a time. It will look at traps again at your next check-in.',
      'Side deltsMore work suggested on Coach',
      'CalvesMore work suggested on Coach',
    ])

    const checkIns = await db.checkIns.toArray()
    expect(checkIns.map((c) => [c.date, c.lagging, c.growingWell])).toEqual([[TODAY, ['CHEST', 'TRAPS', 'SIDE_DELTS', 'CALVES'], ['QUADS']]])
    const advice = await db.advice.orderBy('id').toArray()
    expect(advice.map((a) => [a.type, a.status, a.mode])).toEqual([
      ['MORE_WORK', 'DISMISSED', 'BODYBUILDING'],
      ['MORE_WORK', 'OPEN', 'BODYBUILDING'],
      ['MORE_WORK', 'OPEN', 'BODYBUILDING'],
      ['MORE_WORK', 'OPEN', 'BODYBUILDING'],
    ])
    expect(advice.slice(1).map((a) => grit.advicePayload(a.payloadJson)?.group).sort()).toEqual(['CALVES', 'CHEST', 'SIDE_DELTS'])
    // Nothing in the plan changes until the lifter taps a card on Coach.
    expect((await planFor(db, 'BODYBUILDING')).every((p) => p.sets <= 4)).toBe(true)

    await tap('See Coach')
    expect(onSeeCoach).toHaveBeenCalled()
    await tap('Done')
    expect(onDone).toHaveBeenCalled()
  })

  it('theNotes_readAsThePlanSays', async () => {
    // DB press 5 × on four days: chest is at 20 sets a week, and no adductor exercise uses dumbbells.
    const press = (await exerciseNamed(db, 'DB press')).id
    for (const weekday of [1, 2, 3, 4]) await db.planItems.add({ weekday, exerciseId: press, sets: 5, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    show()
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Chest' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Adductors' }))
    await tap('Next')
    await tap('Save')
    expect(await screen.findByText('Chest already gets 20 sets a week, the most the coach adds.')).toBeInTheDocument()
    expect(screen.getByText('No other adductors exercise uses the equipment in your plan.')).toBeInTheDocument()
    // No advice: no See Coach.
    expect(screen.queryByRole('button', { name: 'See Coach' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Done' })).toBeInTheDocument()
  })

  it('nothingMarked_isAValidAnswer_inTwoTaps', async () => {
    await defaultPlan()
    show()
    await tap('Next')
    await tap('Save')
    expect(await screen.findByRole('heading', { name: SAVED_TITLE })).toBeInTheDocument()
    expect((await db.checkIns.toArray()).map((c) => [c.lagging, c.growingWell])).toEqual([[[], []]])
  })

  it('leavingBeforeSave_storesNothing', async () => {
    await defaultPlan()
    await db.advice.add(MORE_WORK_OPEN)
    const { onLeave } = show()
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Chest' }))
    await tap('Next')
    await tap('Close')
    expect(onLeave).toHaveBeenCalled()
    expect(await db.checkIns.count()).toBe(0)
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['OPEN'])
  })

  it('onThePowerliftingSide_itLeavesAtOnce', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingMode: 'POWERLIFTING' }))
    const { onLeave } = show()
    await waitFor(() => expect(onLeave).toHaveBeenCalled())
    expect(screen.queryByRole('heading', { name: LAGGING_QUESTION })).toBeNull()
  })

  it('browserBack_onStep2_goesBackToStep1_keepingTheTicks', async () => {
    await defaultPlan()
    const { onLeave } = show()
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Chest' }))
    await tap('Next')
    await userEvent.click(box('Quads'))
    window.history.back()
    expect(await screen.findByRole('heading', { name: LAGGING_QUESTION })).toBeInTheDocument()
    expect((box('Chest') as HTMLInputElement).checked).toBe(true)
    expect(onLeave).not.toHaveBeenCalled()
    // Forward again by Next: growing well is as it was left.
    await tap('Next')
    expect((box('Quads') as HTMLInputElement).checked).toBe(true)
    // The on-screen Back does the same as the browser's.
    await tap('Back')
    expect(await screen.findByRole('heading', { name: LAGGING_QUESTION })).toBeInTheDocument()
  })

  it('afterSaving_theStepIsGoneFromHistory_soDoneLeavesFromWhereItWasOpened', async () => {
    await defaultPlan()
    window.history.replaceState({ gritFrom: '#/progress' }, '')
    const length = window.history.length
    const { onDone } = show()
    await tap('Next')
    expect((window.history.state as { checkInStep?: string }).checkInStep).toBe('growing')
    await tap('Save')
    expect(await screen.findByRole('heading', { name: SAVED_TITLE })).toBeInTheDocument()
    await waitFor(() => expect((window.history.state as { gritFrom?: string; checkInStep?: string })).toEqual({ gritFrom: '#/progress' }))
    // Still on the saved step after the step entry is gone.
    expect(screen.getByRole('heading', { name: SAVED_TITLE })).toBeInTheDocument()
    expect(window.history.length).toBeGreaterThanOrEqual(length)
    await tap('Done')
    expect(onDone).toHaveBeenCalled()
  })

  it('closeOnStep2_leavesWithoutSaving', async () => {
    await defaultPlan()
    const { onLeave } = show()
    await tap('Next')
    await tap('Close')
    await waitFor(() => expect(onLeave).toHaveBeenCalled())
    expect((window.history.state as { checkInStep?: string } | null)?.checkInStep).toBeUndefined()
    expect(await db.checkIns.count()).toBe(0)
  })

  it('save_isDisabledWhileSaving', async () => {
    await defaultPlan()
    show()
    await tap('Next')
    const transaction = vi.spyOn(db, 'transaction').mockImplementation((() => new Promise(() => {})) as never)
    await tap('Save')
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(transaction).toHaveBeenCalledTimes(1)
    transaction.mockRestore()
  })

  it('aFailedSave_changesNothing_andSaysSo', async () => {
    await defaultPlan()
    await db.advice.add(MORE_WORK_OPEN)
    show()
    await userEvent.click(await screen.findByRole('checkbox', { name: 'Chest' }))
    await tap('Next')
    const boom = () => {
      throw new Error('disk full')
    }
    db.advice.hook('creating', boom)
    await tap('Save')
    expect(await screen.findByText('The check-in could not be saved. Nothing was changed.')).toBeInTheDocument()
    db.advice.hook('creating').unsubscribe(boom)
    expect(await db.checkIns.count()).toBe(0)
    expect((await db.advice.toArray()).map((a) => a.status)).toEqual(['OPEN'])
  })
})
