import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, resolveCatalogue } from '../../../db/catalogue'
import { getSettings, updateSettings, type AppSettings, type GritDb } from '../../../db/db'
import type { PlanItemRow, TrainingMode } from '../../../db/schema'
import { grit } from '../../../domain'
import { TODAY, addDays, exerciseNamed, logSet, set, todayFixture } from '../../../test/todayFixture'
import { CoachScreen } from '../../coach/CoachScreen'
import { TodayScreen } from '../../today/TodayScreen'
import { PlanScreen } from '../PlanScreen'
import { PlanBuilderScreen } from './PlanBuilderScreen'

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

async function onSide(mode: TrainingMode, over: Partial<AppSettings> = {}) {
  await updateSettings(db, (s) => ({ ...s, trainingMode: mode, trainingDays: [1, 3, 5], bodybuildingTrainingDays: [1, 3, 5], ...over }))
}

const planOn = async (mode: TrainingMode, name: string, over: Partial<PlanItemRow> = {}) => {
  const exerciseId = await resolveCatalogue(db, name)
  return db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: mode === 'BODYBUILDING' ? 8 : 5, ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}), targetRpe: 8, order: 0, mode, ...over })
}

const logged = async (date: string, name: string) => {
  const id = (await db.sessions.add({ date, bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })) as number
  await logSet(db, id, set(await resolveCatalogue(db, name), 20, { reps: 10 }))
}

/** Everything the builder could change: both sides' plans and the settings. */
const stored = async () => ({ plan: await db.planItems.toArray(), settings: await getSettings(db) })

function show() {
  const onLeave = vi.fn()
  const onDone = vi.fn()
  render(<PlanBuilderScreen db={db} today={() => TODAY} onLeave={onLeave} onDone={onDone} />)
  return { onLeave, onDone }
}

const button = (name: string | RegExp) => screen.getByRole('button', { name })
const tap = (name: string | RegExp) => userEvent.click(button(name))
const step = (n: number) => screen.findByText(`Step ${n} of 4`)
const exerciseRow = (name: string) => within(screen.getByRole('listitem', { name })).getByRole('checkbox')
const sessionNames = (day: string) =>
  within(screen.getByRole('region', { name: day }))
    .getAllByRole('listitem')
    .map((li) => li.getAttribute('aria-label'))

/** From the days step to the draft, with the days as they start and nothing changed on the way. */
async function toDraft() {
  await step(1)
  await tap('Next')
  await step(2)
  await tap(/^(Next|Choose for me)$/)
  await step(3)
  await tap(/^(Next|Skip)$/)
  await step(4)
}

describe('the plan builder', () => {
  it('days_startFromTheSidesDays_andNextNeedsExactlyThatMany', async () => {
    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [1, 3, 5] })
    show()
    await step(1)
    expect(screen.getByRole('heading', { level: 1, name: 'Build my plan' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'How many days a week?' })).toBeInTheDocument()
    const count = screen.getByRole('radiogroup', { name: 'How many days a week?' })
    expect(within(count).getByRole('radio', { name: '3 days' })).toBeChecked()
    const days = within(screen.getByRole('group', { name: 'Which days?' }))
    expect(days.getAllByRole('checkbox').filter((c) => (c as HTMLInputElement).checked).map((c) => c.closest('label')!.textContent)).toEqual(['Mon', 'Wed', 'Fri'])
    expect(button('Next')).toBeEnabled()

    await userEvent.click(days.getByRole('checkbox', { name: 'Sat' }))
    expect(button('Next')).toBeDisabled()
    expect(screen.getByText('Choose 3 days.')).toBeInTheDocument()
    await userEvent.click(within(count).getByRole('radio', { name: '4 days' }))
    expect(button('Next')).toBeEnabled()
    await userEvent.click(days.getByRole('checkbox', { name: 'Mon' }))
    expect(button('Next')).toBeDisabled()
  })

  it('days_withNeither3Nor4Stored_startWithNothingChosen', async () => {
    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [2, 4] })
    show()
    await step(1)
    expect(screen.getByRole('radio', { name: '3 days' })).not.toBeChecked()
    expect(screen.getByRole('radio', { name: '4 days' })).not.toBeChecked()
    expect(within(screen.getByRole('group', { name: 'Which days?' })).getAllByRole('checkbox').filter((c) => (c as HTMLInputElement).checked)).toEqual([])
    expect(button('Next')).toBeDisabled()
  })

  it('exercises_arePreTickedFromBothPlansAndTheLastMonth_andNoneGivesChooseForMe', async () => {
    await planOn('POWERLIFTING', 'Squat')
    await planOn('BODYBUILDING', 'Curl')
    await logged(addDays(TODAY, -10), 'Leg press')
    await logged(addDays(TODAY, -29), 'Leg extension')
    await onSide('BODYBUILDING')
    show()
    await step(1)
    await tap('Next')
    await step(2)
    expect(screen.getByRole('heading', { name: 'Which exercises do you do now?' })).toBeInTheDocument()
    expect(screen.getByText('3 chosen')).toBeInTheDocument()
    for (const name of ['Squat', 'Curl', 'Leg press']) expect(exerciseRow(name)).toHaveAttribute('aria-checked', 'true')
    // Logged 29 days ago: not pre-ticked.
    await userEvent.type(screen.getByRole('textbox', { name: 'Search exercises' }), 'Leg extension')
    expect(exerciseRow('Leg extension')).toHaveAttribute('aria-checked', 'false')
    await userEvent.clear(screen.getByRole('textbox', { name: 'Search exercises' }))

    for (const name of ['Squat', 'Curl', 'Leg press']) await userEvent.click(exerciseRow(name))
    expect(screen.getByRole('heading', { name: 'Which exercises would you like to do?' })).toBeInTheDocument()
    expect(button('Choose for me')).toBeEnabled()
    expect(screen.queryByRole('button', { name: 'Next' })).toBeNull()
    await userEvent.click(exerciseRow('Curl'))
    expect(screen.getByText('1 chosen')).toBeInTheDocument()
    expect(button('Next')).toBeEnabled()
  })

  it('chooseForMe_buildsAFullPlanFromTheCatalogue', async () => {
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const expected = grit.planBuilderBuild([1, 3, 5], [], [], 0)
    for (const session of expected.sessions) {
      const day = ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][session.day]!
      expect(sessionNames(day)).toEqual(session.exercises.map((e) => `${e.name}  ${e.sets} × 8–12 @ 8`))
      expect(within(screen.getByRole('region', { name: day })).getByRole('heading', { name: `${day} · Full body` })).toBeInTheDocument()
      expect(within(screen.getByRole('region', { name: day })).getByText(new RegExp(`^${session.exercises.length} exercises · `))).toBeInTheDocument()
    }
    expect(sessionNames('Monday')[0]).toBe('Leg press  4 × 8–12 @ 8')
  })

  it('bodyParts_offerTheTwelve_refuseAFourth_andSkipIsAllowed', async () => {
    await onSide('BODYBUILDING')
    show()
    await step(1)
    await tap('Next')
    await tap('Choose for me')
    await step(3)
    expect(screen.getByRole('heading', { name: 'Which body parts do you most want to improve?' })).toBeInTheDocument()
    const groups = within(screen.getByRole('group', { name: 'Which body parts do you most want to improve?' }))
    expect(groups.getAllByRole('checkbox').map((c) => c.closest('label')!.textContent)).toEqual(grit.planBuilderTargetGroups().map((g) => grit.muscleGroupLabel(g)))
    expect(button('Skip')).toBeEnabled()

    for (const name of ['Chest', 'Biceps', 'Calves']) await userEvent.click(groups.getByRole('checkbox', { name }))
    expect(button('Next')).toBeEnabled()
    await userEvent.click(groups.getByRole('checkbox', { name: 'Abs' }))
    expect(groups.getByRole('checkbox', { name: 'Abs' })).not.toBeChecked()
    expect(screen.getByText("That's 3 already. Untick one to choose another.")).toBeInTheDocument()
    await userEvent.click(groups.getByRole('checkbox', { name: 'Calves' }))
    expect(screen.queryByText("That's 3 already. Untick one to choose another.")).toBeNull()
    await userEvent.click(groups.getByRole('checkbox', { name: 'Abs' }))
    expect(groups.getByRole('checkbox', { name: 'Abs' })).toBeChecked()

    await tap('Next')
    await step(4)
    const expected = grit.planBuilderBuild([1, 3, 5], [], ['CHEST', 'BICEPS', 'ABS'], 0)
    expect(sessionNames('Monday')).toEqual(expected.sessions[0]!.exercises.map((e) => `${e.name}  ${e.sets} × 8–12 @ 8`))
  })

  it('draft_showsWeeklySets_theShortMarks_andTheGuideline', async () => {
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const expected = grit.planBuilderBuild([1, 3, 5], [], [], 0)
    const weekly = within(screen.getByRole('region', { name: 'Weekly sets' }))
    expect(weekly.getAllByRole('listitem').map((li) => li.getAttribute('aria-label'))).toEqual(
      expected.volume.map((v) => `${grit.muscleGroupLabel(v.group)}, ${v.weeklySets} sets${v.short ? ', under 10' : ''}`),
    )
    expect(expected.volume.length).toBe(12)
    expect(weekly.getAllByText('Under 10').length).toBe(expected.volume.filter((v) => v.short).length)
    expect(weekly.getByText('10 or more a week is the guideline for growth. Make a body part a priority to give it more.')).toBeInTheDocument()
    // No picks, nothing left out.
    expect(screen.queryByRole('region', { name: 'Left out' })).toBeNull()
  })

  it('draft_namesTheLeftOutPicksWithTheirReasons', async () => {
    // Many picks for three days: more than the sessions hold.
    const names = grit.library().filter((e) => e.kind === 'ACCESSORY' && e.muscles.length > 0).slice(0, 40).map((e) => e.name)
    for (const [i, name] of names.entries()) await planOn('BODYBUILDING', name, { order: i })
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const picks = names.map((name) => {
      const e = grit.findInLibrary(name)!
      return { name: e.name, muscles: e.muscles, compound: false, equipment: e.equipment, inCatalogue: true }
    })
    const leftOut = within(screen.getByRole('region', { name: 'Left out' }))
    const rows = leftOut.getAllByRole('listitem')
    expect(rows.length).toBeGreaterThan(0)
    expect(rows.length).toBeLessThan(picks.length)
    for (const row of rows) expect(row).toHaveTextContent('No room: every session it fits is full.')
  })

  it('swap_replacesInPlace_keepingTheSets', async () => {
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const before = sessionNames('Monday')
    const lead = within(screen.getByRole('region', { name: 'Monday' })).getAllByRole('listitem')[0]!
    await userEvent.click(within(lead).getByRole('button', { name: 'Swap' }))
    const sheet = within(await screen.findByRole('dialog', { name: 'Swap Leg press' }))
    const options = sheet.getAllByRole('listitem').map((li) => li.getAttribute('aria-label'))
    expect(options).not.toContain('Leg press')
    expect(options[0]).toBe('Hack squat')
    await userEvent.click(within(sheet.getByRole('listitem', { name: 'Hack squat' })).getByRole('button'))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(sessionNames('Monday')).toEqual(['Hack squat  4 × 8–12 @ 8', ...before.slice(1)])
  })

  it('anotherDraft_changesOnlyTheDefaults', async () => {
    await planOn('BODYBUILDING', 'Curl')
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const first = ['Monday', 'Wednesday', 'Friday'].map(sessionNames)
    await tap('Another draft')
    const second = ['Monday', 'Wednesday', 'Friday'].map(sessionNames)
    expect(second).not.toEqual(first)
    expect(second.flat().filter((n) => n!.startsWith('Curl '))).toEqual(first.flat().filter((n) => n!.startsWith('Curl ')))
    const pick = grit.findInLibrary('Curl')!
    const expected = grit.planBuilderBuild([1, 3, 5], [{ name: 'Curl', muscles: pick.muscles, compound: pick.kind === 'COMPOUND', equipment: pick.equipment, inCatalogue: true } as never], [], 1)
    expect(second[0]).toEqual(expected.sessions[0]!.exercises.map((e) => `${e.name}  ${e.sets} × 8–12 @ 8`))
  })

  it('useThisPlan_withNoPlan_savesTheDraftAndTheDays_andLeavesThePowerliftingPlan', async () => {
    await planOn('POWERLIFTING', 'Squat')
    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [2, 4, 6, 7] })
    const powerlifting = await planFor(db, 'POWERLIFTING')
    const { onDone } = show()
    await step(1)
    // Sat and Sun off, Mon on: Mon, Tue, Thu.
    for (const day of ['Sat', 'Sun']) await userEvent.click(screen.getByRole('checkbox', { name: day }))
    await userEvent.click(screen.getByRole('radio', { name: '3 days' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Mon' }))
    await tap('Next')
    await step(2)
    // Squat is pre-ticked from the powerlifting plan; untick it.
    await userEvent.click(exerciseRow('Squat'))
    await tap('Choose for me')
    await tap('Skip')
    await step(4)
    const draft = grit.planBuilderBuild([1, 2, 4], [], [], 0)
    await tap('Use this plan')
    await waitFor(() => expect(onDone).toHaveBeenCalledOnce())
    expect(screen.queryByRole('dialog')).toBeNull()

    const byId = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    const saved = (await planFor(db, 'BODYBUILDING')).sort((a, b) => a.id - b.id)
    expect(saved.map((p) => [p.weekday, byId.get(p.exerciseId), p.sets, p.reps, p.repsHigh, p.targetRpe, p.mode])).toEqual(
      draft.sessions.flatMap((s) => s.exercises.map((e) => [s.day, e.name, e.sets, 8, 12, 8, 'BODYBUILDING'])),
    )
    expect(saved.filter((p) => p.weekday === 1).map((p) => p.order)).toEqual(draft.sessions[0]!.exercises.map((_, i) => i))
    const settings = await getSettings(db)
    expect(settings.bodybuildingTrainingDays).toEqual([1, 2, 4])
    expect(settings.trainingDays).toEqual([1, 3, 5])
    expect(await planFor(db, 'POWERLIFTING')).toEqual(powerlifting)
  })

  it('useThisPlan_overAPlan_confirmsFirst_andCancelKeepsIt', async () => {
    await planOn('BODYBUILDING', 'Curl', { weekday: 5 })
    await planOn('POWERLIFTING', 'Squat')
    await onSide('BODYBUILDING')
    const before = await stored()
    const { onDone } = show()
    await toDraft()
    await tap('Use this plan')
    const confirm = within(await screen.findByRole('dialog', { name: 'Use this plan?' }))
    expect(confirm.getByText('This replaces your bodybuilding plan. Your log is kept.')).toBeInTheDocument()
    await userEvent.click(confirm.getByRole('button', { name: 'Cancel' }))
    expect(await stored()).toEqual(before)
    expect(onDone).not.toHaveBeenCalled()

    await tap('Use this plan')
    await userEvent.click(within(await screen.findByRole('dialog', { name: 'Use this plan?' })).getByRole('button', { name: 'Replace' }))
    await waitFor(() => expect(onDone).toHaveBeenCalledOnce())
    const byId = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    const saved = await planFor(db, 'BODYBUILDING')
    // The old plan is gone; the curl is in the new one as a pick, twice.
    const oldIds = before.plan.filter((p) => p.mode === 'BODYBUILDING').map((p) => p.id)
    expect(saved.filter((p) => oldIds.includes(p.id))).toEqual([])
    expect(saved.map((p) => byId.get(p.exerciseId)).filter((n) => n === 'Curl').length).toBe(2)
    expect(await planFor(db, 'POWERLIFTING')).toEqual(before.plan.filter((p) => p.mode === 'POWERLIFTING'))
  })

  it('anOwnExercise_isSavedByItsRow', async () => {
    const own = (await db.exercises.add({ name: 'Band pull thing', category: 'ACCESSORY', parentLift: null, muscles: [], isBuiltIn: false, archived: false })) as number
    await db.planItems.add({ weekday: 1, exerciseId: own, sets: 3, reps: 8, repsHigh: 12, targetRpe: 8, order: 0, mode: 'BODYBUILDING' })
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    expect(screen.getByRole('listitem', { name: 'Band pull thing 3 × 8–12 @ 8' })).toBeInTheDocument()
    await tap('Use this plan')
    await userEvent.click(within(await screen.findByRole('dialog', { name: 'Use this plan?' })).getByRole('button', { name: 'Replace' }))
    await waitFor(async () => expect((await planFor(db, 'BODYBUILDING')).filter((p) => p.exerciseId === own)).toHaveLength(1))
    expect((await db.exercises.toArray()).filter((e) => e.name === 'Band pull thing')).toHaveLength(1)
  })

  it('leavingAtAnyStep_changesNothing', async () => {
    await planOn('BODYBUILDING', 'Curl')
    await planOn('POWERLIFTING', 'Squat')
    await onSide('BODYBUILDING')
    const before = await stored()
    for (const at of [1, 2, 3, 4]) {
      const { onLeave } = show()
      await step(1)
      for (let i = 1; i < at; i++) await tap(/^(Next|Choose for me|Skip)$/)
      await step(at)
      await tap('Close')
      expect(onLeave).toHaveBeenCalledOnce()
      cleanup()
    }
    // Back on the first step leaves too; Back elsewhere is one step back.
    const { onLeave } = show()
    await step(1)
    await tap('Next')
    await step(2)
    await tap('Back')
    await step(1)
    expect(onLeave).not.toHaveBeenCalled()
    await tap('Back')
    expect(onLeave).toHaveBeenCalledOnce()
    expect(await stored()).toEqual(before)
  })

  it('isNotThereOnThePowerliftingSide', async () => {
    await onSide('POWERLIFTING')
    const { onLeave } = show()
    await waitFor(() => expect(onLeave).toHaveBeenCalled())
    expect(screen.queryByRole('heading', { name: 'Build my plan' })).toBeNull()
  })
})

describe('the ways into the builder', () => {
  const showPlan = (onBuildPlan = vi.fn()) => {
    render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} onBuildPlan={onBuildPlan} />)
    return onBuildPlan
  }

  it('anEmptyBodybuildingPlan_offersBuildMyPlan', async () => {
    await onSide('BODYBUILDING')
    const onBuildPlan = showPlan()
    expect(await screen.findByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeInTheDocument()
    await tap('Build my plan')
    expect(onBuildPlan).toHaveBeenCalledOnce()
    expect(screen.queryByRole('button', { name: 'Plan options' })).toBeNull()
  })

  it('aBodybuildingPlan_hasRebuildMyPlanInItsMenu', async () => {
    await planOn('BODYBUILDING', 'Curl')
    await onSide('BODYBUILDING')
    const onBuildPlan = showPlan()
    await screen.findByRole('listitem', { name: 'Curl 3 × 8–12 @ 8' })
    await tap('Plan options')
    await userEvent.click(screen.getByRole('menuitem', { name: 'Rebuild my plan' }))
    expect(onBuildPlan).toHaveBeenCalledOnce()
  })

  it('thePowerliftingPlan_hasNoBuilder', async () => {
    await planOn('POWERLIFTING', 'Squat')
    await onSide('POWERLIFTING')
    showPlan()
    await screen.findByRole('listitem', { name: 'Squat 3 × 5 @ 8' })
    expect(screen.queryByRole('button', { name: 'Plan options' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Build my plan' })).toBeNull()
    cleanup()
    await db.planItems.clear()
    showPlan()
    await screen.findByRole('heading', { name: 'Known bests' })
    expect(screen.queryByRole('button', { name: 'Build my plan' })).toBeNull()
  })

  it('myGoalsHaveChanged_opensTheBuilderOnTheBodybuildingSide_andPlanOnThePowerliftingSide', async () => {
    await planOn('BODYBUILDING', 'Curl')
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -40) })
    const onOpenPlan = vi.fn()
    const onOpenBuilder = vi.fn()
    render(<CoachScreen db={db} today={() => TODAY} onOpenPlan={onOpenPlan} onOpenBuilder={onOpenBuilder} />)
    await userEvent.click(await screen.findByRole('button', { name: 'My goals have changed' }))
    expect(onOpenBuilder).toHaveBeenCalledOnce()
    expect(onOpenPlan).not.toHaveBeenCalled()
    cleanup()

    await planOn('POWERLIFTING', 'Squat')
    await onSide('POWERLIFTING', { powerliftingLeftOn: addDays(TODAY, -40) })
    render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} onOpenPlan={onOpenPlan} onOpenBuilder={onOpenBuilder} />)
    await userEvent.click(await screen.findByRole('button', { name: 'My goals have changed' }))
    expect(onOpenPlan).toHaveBeenCalledOnce()
    expect(onOpenBuilder).toHaveBeenCalledOnce()
  })
})
