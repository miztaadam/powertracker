import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, resolveCatalogue } from '../../../db/catalogue'
import { getSettings, updateSettings, type AppSettings, type GritDb } from '../../../db/db'
import type { PlanItemRow, TrainingMode } from '../../../db/schema'
import { grit } from '../../../domain'
import { TODAY, addDays, exerciseNamed, logSet, set, todayFixture } from '../../../test/todayFixture'
import { wholeList } from '../../../test/wholeList'
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
const chosen = () => screen.queryByRole('region', { name: 'Chosen' })
/**
 * The exercise's tick box in the browser's list, under "Chosen" (which repeats the ticked ones). Each lookup walks the
 * whole page, 225 rows and more, so a caller checking many rows can look "Chosen" up once and pass it as `region`.
 */
const exerciseRow = (name: string, region = chosen()) => {
  const rows = screen.getAllByRole('listitem', { name }).filter((li) => !region?.contains(li))
  expect(rows).toHaveLength(1)
  return within(rows[0]!).getByRole('checkbox')
}
const chosenRow = (name: string, region = chosen()!) => within(within(region).getByRole('listitem', { name })).getByRole('checkbox')
const chosenNames = () => {
  const region = chosen()
  return region ? within(region).getAllByRole('listitem').map((li) => li.getAttribute('aria-label')) : []
}
const follows = (first: Node, second: Node) => (first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0
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
    // Once the count is met the line names the chosen days, in week order.
    expect(screen.getByText('Mon, Wed, Fri')).toBeInTheDocument()

    await userEvent.click(days.getByRole('checkbox', { name: 'Sat' }))
    expect(button('Next')).toBeDisabled()
    expect(screen.getByText('Choose 3 days.')).toBeInTheDocument()
    expect(screen.queryByText(/^Mon, /)).toBeNull()
    await userEvent.click(within(count).getByRole('radio', { name: '4 days' }))
    expect(button('Next')).toBeEnabled()
    expect(screen.getByText('Mon, Wed, Fri, Sat')).toBeInTheDocument()
    expect(screen.queryByText('Choose 4 days.')).toBeNull()
    await userEvent.click(days.getByRole('checkbox', { name: 'Mon' }))
    expect(button('Next')).toBeDisabled()
  })

  it('days_withNeither3Nor4Stored_startOn3DaysWithNoneTicked', async () => {
    for (const stored of [[2, 4], [1, 2, 3, 4, 5]]) {
      await onSide('BODYBUILDING', { bodybuildingTrainingDays: stored })
      show()
      await step(1)
      expect(screen.getByRole('radio', { name: '3 days' })).toBeChecked()
      expect(screen.getByRole('radio', { name: '4 days' })).not.toBeChecked()
      const days = within(screen.getByRole('group', { name: 'Which days?' }))
      expect(days.getAllByRole('checkbox').filter((c) => (c as HTMLInputElement).checked)).toEqual([])
      expect(screen.getByText('Choose 3 days.')).toBeInTheDocument()
      expect(button('Next')).toBeDisabled()
      for (const day of ['Sat', 'Tue', 'Thu']) await userEvent.click(days.getByRole('checkbox', { name: day }))
      expect(button('Next')).toBeEnabled()
      expect(screen.getByText('Tue, Thu, Sat')).toBeInTheDocument()
      cleanup()
    }
  })

  it('everyStep_hasClose_andBackFromTheSecondStepOn', async () => {
    await onSide('BODYBUILDING')
    show()
    await step(1)
    expect(button('Close')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull()
    for (const [n, go] of [[2, 'Next'], [3, 'Choose for me'], [4, 'Skip']] as const) {
      await tap(go)
      await step(n)
      expect(button('Close')).toBeInTheDocument()
      expect(button('Back')).toBeInTheDocument()
    }
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
    await wholeList()
    expect(screen.getByRole('heading', { name: 'Which exercises do you do now?' })).toBeInTheDocument()
    expect(screen.getByText('3 chosen')).toBeInTheDocument()
    for (const name of ['Squat', 'Curl', 'Leg press']) expect(exerciseRow(name)).toHaveAttribute('aria-checked', 'true')
    // Logged 29 days ago: not pre-ticked.
    await userEvent.type(screen.getByRole('textbox', { name: 'Search exercises' }), 'Leg extension')
    expect(exerciseRow('Leg extension')).toHaveAttribute('aria-checked', 'false')
    await userEvent.clear(screen.getByRole('textbox', { name: 'Search exercises' }))
    await wholeList()

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
    await userEvent.click(within(lead).getByRole('button', { name: 'Swap Leg press' }))
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

  // Final review, minor 2: "Rebuild my plan" replaces the plan only; every logged session and set is kept.
  it('rebuildingOverABodybuildingPlanWithLoggedSessions_keepsEverySessionAndSet_andThePowerliftingPlan', async () => {
    await planOn('BODYBUILDING', 'Curl', { weekday: 1 })
    await planOn('POWERLIFTING', 'Squat')
    await onSide('BODYBUILDING')
    const first = (await db.sessions.add({ date: addDays(TODAY, -9), bodyweightKg: 80, readiness: 4, mode: 'BODYBUILDING' })) as number
    const curl = await resolveCatalogue(db, 'Curl')
    await logSet(db, first, set(curl, 20, { reps: 12 }))
    await logSet(db, first, set(curl, 20, { reps: 10, rpe: 9 }))
    await logged(addDays(TODAY, -2), 'Curl')
    const heavy = (await db.sessions.add({ date: addDays(TODAY, -5), bodyweightKg: null, readiness: null, mode: 'POWERLIFTING' })) as number
    await logSet(db, heavy, set(await resolveCatalogue(db, 'Squat'), 140, { reps: 5 }))
    const sessions = await db.sessions.toArray()
    const sets = await db.sets.toArray()
    expect(sessions).toHaveLength(3)
    expect(sets).toHaveLength(4)
    const before = await stored()

    const { onDone } = show()
    await toDraft()
    await tap('Use this plan')
    await userEvent.click(within(await screen.findByRole('dialog', { name: 'Use this plan?' })).getByRole('button', { name: 'Replace' }))
    await waitFor(() => expect(onDone).toHaveBeenCalledOnce())

    expect(await planFor(db, 'BODYBUILDING')).not.toEqual(before.plan.filter((p) => p.mode === 'BODYBUILDING'))
    expect(await db.sessions.toArray()).toEqual(sessions)
    expect(await db.sets.toArray()).toEqual(sets)
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
    // Back is one step back, and the first step has none: Close leaves.
    const { onLeave } = show()
    await step(1)
    await tap('Next')
    await step(2)
    await tap('Back')
    await step(1)
    expect(onLeave).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Back' })).toBeNull()
    expect(await stored()).toEqual(before)
  })

  it('exercises_listEveryTickUnderChosenAtTheTop_andUntickingEitherUnticksBoth', async () => {
    // More pre-ticks than the browser's "Your exercises" shows.
    const names = grit.library().filter((e) => e.kind === 'ACCESSORY' && e.muscles.length > 0).slice(0, 9).map((e) => e.name)
    for (const [i, name] of names.entries()) await planOn('BODYBUILDING', name, { order: i })
    await onSide('BODYBUILDING')
    show()
    await step(1)
    await tap('Next')
    await step(2)
    await wholeList()
    expect([...chosenNames()].sort()).toEqual([...names].sort())
    // One walk of the page per row, not six.
    const region = chosen()!
    for (const name of names) {
      const inList = exerciseRow(name, region)
      expect(chosenRow(name, region)).toHaveAttribute('aria-checked', 'true')
      expect(inList).toHaveAttribute('aria-checked', 'true')
      expect(follows(region, inList)).toBe(true)
    }
    expect(within(chosen()!).getByRole('heading', { name: 'Chosen' })).toBeInTheDocument()

    // Unticked under Chosen: gone from it, and unticked in the list.
    const [first, second] = names as [string, string]
    await userEvent.click(chosenRow(first))
    expect(chosenNames()).not.toContain(first)
    expect(exerciseRow(first)).toHaveAttribute('aria-checked', 'false')
    expect(screen.getByText('8 chosen')).toBeInTheDocument()
    // Unticked in the list: gone from Chosen too.
    await userEvent.click(exerciseRow(second))
    expect(chosenNames()).not.toContain(second)
    // Ticked in the list: last under Chosen, even while a search shows only it.
    await userEvent.type(screen.getByRole('textbox', { name: 'Search exercises' }), 'Hack squat')
    await userEvent.click(exerciseRow('Hack squat'))
    expect(chosenNames().at(-1)).toBe('Hack squat')
    expect(chosenNames()).toHaveLength(8)
    await userEvent.clear(screen.getByRole('textbox', { name: 'Search exercises' }))
    await wholeList()

    for (const name of chosenNames()) await userEvent.click(chosenRow(name!))
    expect(chosen()).toBeNull()
    expect(button('Choose for me')).toBeEnabled()
  })

  it('exercises_ticksLastThroughASearch_aFilter_andLeavingTheStep', async () => {
    await planOn('BODYBUILDING', 'Curl')
    await onSide('BODYBUILDING')
    show()
    await step(1)
    await tap('Next')
    await step(2)
    await wholeList()
    const search = () => screen.getByRole('textbox', { name: 'Search exercises' })
    await userEvent.type(search(), 'Leg press')
    await userEvent.click(exerciseRow('Leg press'))
    await userEvent.clear(search())
    await wholeList()
    expect(exerciseRow('Leg press')).toHaveAttribute('aria-checked', 'true')
    expect(exerciseRow('Curl')).toHaveAttribute('aria-checked', 'true')

    // A body area, then equipment: the ticks are kept, shown or not.
    const area = within(screen.getByRole('group', { name: 'Body area' }))
    await userEvent.click(area.getByRole('checkbox', { name: 'Quads' }))
    await wholeList()
    expect(exerciseRow('Leg press')).toHaveAttribute('aria-checked', 'true')
    await userEvent.click(within(screen.getByRole('group', { name: 'Equipment' })).getByRole('checkbox', { name: 'Machine' }))
    await wholeList()
    expect(exerciseRow('Leg press')).toHaveAttribute('aria-checked', 'true')
    expect(exerciseRow('Leg extension')).toHaveAttribute('aria-checked', 'false')
    await userEvent.click(within(screen.getByRole('group', { name: 'Equipment' })).getByRole('checkbox', { name: 'Machine' }))
    await wholeList()
    await userEvent.click(area.getByRole('checkbox', { name: 'All' }))
    await wholeList()
    expect(exerciseRow('Curl')).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByText('2 chosen')).toBeInTheDocument()

    // Back to the days and on again; on to the body parts and back.
    await tap('Back')
    await step(1)
    await tap('Next')
    await step(2)
    await wholeList()
    expect(screen.getByText('2 chosen')).toBeInTheDocument()
    for (const name of ['Leg press', 'Curl']) expect(exerciseRow(name)).toHaveAttribute('aria-checked', 'true')
    await tap('Next')
    await step(3)
    await tap('Back')
    await step(2)
    await wholeList()
    expect([...chosenNames()].sort()).toEqual(['Curl', 'Leg press'])
    for (const name of ['Leg press', 'Curl']) expect(exerciseRow(name)).toHaveAttribute('aria-checked', 'true')
  })

  it('draft_barHoldsBackAndUseThisPlan_andAnotherDraftSitsUnderTheIntro', async () => {
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const bar = document.querySelector('.pb-actions') as HTMLElement
    expect(within(bar).getAllByRole('button').map((b) => b.textContent)).toEqual(['Back', 'Use this plan'])
    const intro = screen.getByText('Swap any exercise, or ask for another draft. Nothing is saved until you use this plan.')
    expect(intro.nextElementSibling).toBe(button('Another draft'))
    expect(follows(button('Another draft'), screen.getByRole('region', { name: 'Monday' }))).toBe(true)
  })

  it('draft_namesEverySwapByItsExercise_asACompactTonalButton', async () => {
    await onSide('BODYBUILDING')
    show()
    await toDraft()
    const expected = grit.planBuilderBuild([1, 3, 5], [], [], 0)
    for (const session of expected.sessions) {
      const day = ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][session.day]!
      const region = within(screen.getByRole('region', { name: day }))
      // The session's title is a heading.
      expect(region.getByRole('heading', { name: `${day} · ${session.title}` })).toBeInTheDocument()
      const swaps = region.getAllByRole('button').filter((b) => b.textContent === 'Swap')
      expect(swaps.map((b) => b.getAttribute('aria-label'))).toEqual(session.exercises.map((e) => `Swap ${e.name}`))
      for (const swap of swaps) expect(swap).toHaveClass('gr-btn-secondary', 'gr-btn-compact')
    }
  })

  it('aFailedSave_showsAPlainLineAtTheTop_keepsTheBuilderOpen_andCanBeTriedAgain', async () => {
    await planOn('POWERLIFTING', 'Squat')
    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [2, 4, 6] })
    const before = await stored()
    const { onDone } = show()
    await step(1)
    await userEvent.click(screen.getByRole('checkbox', { name: 'Sat' }))
    await userEvent.click(screen.getByRole('checkbox', { name: 'Mon' }))
    await tap('Next')
    await step(2)
    await userEvent.click(exerciseRow('Squat'))
    await tap('Choose for me')
    await tap('Skip')
    await step(4)
    // Setting the days fails: the plan goes back with it.
    const put = vi.spyOn(db.settings, 'bulkPut').mockRejectedValueOnce(new Error('No room left'))
    await tap('Use this plan')
    const line = await screen.findByRole('alert')
    expect(line).toHaveTextContent('The plan could not be saved. Nothing was changed.')
    expect(follows(line, screen.getByRole('heading', { name: 'Your draft week' }))).toBe(true)
    expect(onDone).not.toHaveBeenCalled()
    expect(await stored()).toEqual(before)
    await step(4)
    // It stays until the next try.
    await tap('Another draft')
    expect(screen.getByRole('alert')).toHaveTextContent('The plan could not be saved. Nothing was changed.')

    await tap('Use this plan')
    await waitFor(() => expect(onDone).toHaveBeenCalledOnce())
    expect(screen.queryByRole('alert')).toBeNull()
    expect(put).toHaveBeenCalledTimes(2)
    expect((await getSettings(db)).bodybuildingTrainingDays).toEqual([1, 2, 4])
    expect((await planFor(db, 'BODYBUILDING')).length).toBe(grit.planBuilderBuild([1, 2, 4], [], [], 1).sessions.flatMap((s) => s.exercises).length)
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
