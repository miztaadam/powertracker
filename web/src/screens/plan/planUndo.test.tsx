import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, resolveCatalogue, type PlanSnapshot } from '../../db/catalogue'
import { getSettings, updateSettings, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
import { TODAY, logSet, set, todayFixture } from '../../test/todayFixture'
import { PlanBuilderScreen } from './builder/PlanBuilderScreen'
import type { PlanActions, PlanState } from '../../state/usePlan'
import { PLAN_SAVED, PlanContent, PlanScreen, UNDO_FAILED } from './PlanScreen'
import { TemplatesScreen } from './templates/TemplatesScreen'

/** 1.14.1: Plan's snackbar after a whole-plan replace offers Undo, which puts back the plan that was there. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const planOn = async (mode: TrainingMode, name: string, weekday: number) =>
  (await db.planItems.add({ weekday, exerciseId: await resolveCatalogue(db, name), sets: 3, reps: mode === 'BODYBUILDING' ? 8 : 5, ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}), targetRpe: 8, order: 0, mode })) as number
const sorted = async (mode: TrainingMode) => (await planFor(db, mode)).sort((a, b) => a.id - b.id)
const tap = (name: string | RegExp) => userEvent.click(screen.getByRole('button', { name }))

/** Saves the 3-day powerlifting template through its screen, replacing the plan if asked; resolves to what onSaved got. */
async function useTemplate(name: string): Promise<unknown[]> {
  const onSaved = vi.fn()
  render(<TemplatesScreen db={db} mode="POWERLIFTING" onLeave={() => {}} onSaved={onSaved} />)
  await userEvent.click(await screen.findByRole('button', { name }))
  await tap('Use this plan')
  const confirm = screen.queryByRole('dialog', { name: 'Replace your powerlifting plan?' })
  if (confirm) await userEvent.click(within(confirm).getByRole('button', { name: 'Replace' }))
  await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
  cleanup()
  return onSaved.mock.calls[0]!
}

const showPlan = (message: string | { text: string; undo?: PlanSnapshot }) => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} message={message} onMessageShown={() => {}} />)

describe('Undo of a plan replace', () => {
  it('aTemplateOverAPlan_offersUndo_whichPutsTheOldPlanAndDaysBack', async () => {
    await planOn('POWERLIFTING', 'Squat', 2)
    await planOn('POWERLIFTING', 'Bench', 4)
    await updateSettings(db, (s) => ({ ...s, trainingDays: [2, 4] }))
    const before = await sorted('POWERLIFTING')

    const [name, undo] = await useTemplate('3-day powerlifting')
    expect(undo).toMatchObject({ mode: 'POWERLIFTING' })
    expect(await sorted('POWERLIFTING')).not.toEqual(before)

    showPlan({ text: `Plan set: ${name as string}.`, undo: undo as PlanSnapshot })
    expect(await screen.findByText('Plan set: 3-day powerlifting.')).toBeInTheDocument()
    await tap('Undo')
    await waitFor(async () => expect(await sorted('POWERLIFTING')).toEqual(before))
    expect((await getSettings(db)).trainingDays).toEqual([2, 4])
    expect(screen.queryByText('Plan set: 3-day powerlifting.')).toBeNull()
  })

  it('aTemplateOnAnEmptySide_hasNoUndo', async () => {
    const call = await useTemplate('3-day powerlifting')
    expect(call).toEqual(['3-day powerlifting'])
    showPlan({ text: 'Plan set: 3-day powerlifting.' })
    expect(await screen.findByText('Plan set: 3-day powerlifting.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull()
  })

  it('aStaleUndo_afterAnotherSave_doesNothing', async () => {
    await planOn('POWERLIFTING', 'Squat', 2)
    const [, older] = await useTemplate('3-day powerlifting')
    await useTemplate('4-day powerlifting')
    const newest = await sorted('POWERLIFTING')
    const days = (await getSettings(db)).trainingDays

    showPlan({ text: 'Plan set: 3-day powerlifting.', undo: older as PlanSnapshot })
    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }))
    // Give the Undo its turn, then check it changed nothing and said nothing.
    await new Promise((r) => setTimeout(r, 50))
    expect(await sorted('POWERLIFTING')).toEqual(newest)
    expect((await getSettings(db)).trainingDays).toEqual(days)
    expect(screen.queryByText(UNDO_FAILED)).toBeNull()
  })

  it('afterASessionOnTheNewPlan_keepsTheSessionAndItsSets', async () => {
    await planOn('POWERLIFTING', 'Squat', 2)
    const before = await sorted('POWERLIFTING')
    const [, undo] = await useTemplate('3-day powerlifting')
    const sessionId = (await db.sessions.add({ date: TODAY, bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })) as number
    const setId = await logSet(db, sessionId, set(await resolveCatalogue(db, 'Bench'), 80))

    showPlan({ text: 'Plan set: 3-day powerlifting.', undo: undo as PlanSnapshot })
    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }))
    await waitFor(async () => expect(await sorted('POWERLIFTING')).toEqual(before))
    expect(await db.sessions.get(sessionId)).toBeDefined()
    expect(await db.sets.get(setId)).toBeDefined()
  })

  it('aFailedUndo_keepsTheNewPlan_andSaysSo', async () => {
    await planOn('POWERLIFTING', 'Squat', 2)
    const [, undo] = await useTemplate('3-day powerlifting')
    const after = await sorted('POWERLIFTING')
    const days = (await getSettings(db)).trainingDays
    db.planItems.hook('creating', () => {
      throw new Error('forced failure')
    })

    showPlan({ text: 'Plan set: 3-day powerlifting.', undo: undo as PlanSnapshot })
    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }))
    expect(await screen.findByText("Couldn't undo. Your new plan is kept.")).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull()
    expect(await sorted('POWERLIFTING')).toEqual(after)
    expect((await getSettings(db)).trainingDays).toEqual(days)
  })

  it('theBuilder_overAPlan_handsBackItsUndo_andOnAnEmptySideNone', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingMode: 'BODYBUILDING', bodybuildingTrainingDays: [1, 3, 5] }))
    const toDraftAndUse = async () => {
      const onDone = vi.fn()
      render(<PlanBuilderScreen db={db} today={() => TODAY} onLeave={() => {}} onDone={onDone} />)
      await screen.findByText('Step 1 of 4')
      await tap('Next')
      await screen.findByText('Step 2 of 4')
      await tap(/^(Next|Choose for me)$/)
      await screen.findByText('Step 3 of 4')
      await tap(/^(Next|Skip)$/)
      await screen.findByText('Step 4 of 4')
      await tap('Use this plan')
      const confirm = await screen.findByRole('dialog', { name: 'Use this plan?' }).catch(() => null)
      if (confirm) await userEvent.click(within(confirm).getByRole('button', { name: 'Replace' }))
      await waitFor(() => expect(onDone).toHaveBeenCalledOnce())
      cleanup()
      return onDone.mock.calls[0]!
    }
    expect(await toDraftAndUse()).toEqual([])
    const first = await sorted('BODYBUILDING')
    const [undo] = await toDraftAndUse()
    expect(undo).toMatchObject({ mode: 'BODYBUILDING', items: first })

    showPlan({ text: PLAN_SAVED, undo: undo as PlanSnapshot })
    expect(await screen.findByText('Plan saved.')).toBeInTheDocument()
    await tap('Undo')
    await waitFor(async () => expect(await sorted('BODYBUILDING')).toEqual(first))
  })

  it('undo_isOnOfferForTheUsualSnackbarTime_only', () => {
    vi.useFakeTimers()
    try {
      const state: PlanState = { loading: false, sections: [], exercises: [], recentIds: [], mainExercises: [], bests: {}, defaultRpe: 8, trainingDays: [], side: 'POWERLIFTING' }
      const undo = { mode: 'POWERLIFTING', items: [], skipLinks: [], days: [], replacedBy: [] } as PlanSnapshot
      const onUndoReplace = vi.fn(async () => 'restored' as const)
      render(<PlanContent state={state} actions={{} as PlanActions} onOpenSettings={() => {}} message={{ text: 'Plan set: 3-day powerlifting.', undo }} onUndoReplace={onUndoReplace} />)
      expect(screen.getByRole('button', { name: 'Undo' })).toBeInTheDocument()
      act(() => vi.advanceTimersByTime(3999))
      expect(screen.getByRole('button', { name: 'Undo' })).toBeInTheDocument()
      act(() => vi.advanceTimersByTime(1))
      expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull()
      expect(screen.queryByText('Plan set: 3-day powerlifting.')).toBeNull()
      expect(onUndoReplace).not.toHaveBeenCalled()
    } finally {
      vi.useRealTimers()
    }
  })
})
