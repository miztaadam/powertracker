import { act, cleanup, render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor, resolveCatalogue } from '../../../db/catalogue'
import { getSettings, updateSettings, type AppSettings, type GritDb } from '../../../db/db'
import type { AdviceRow, PlanItemRow, TrainingMode } from '../../../db/schema'
import { skip as storeSkip, skips as storedSkips } from '../../../db/skips'
import { grit } from '../../../domain'
import { parseHash, routeToHash } from '../../../router'
import { useCoach } from '../../../state/useCoach'
import { useTemplates } from '../../../state/useTemplates'
import { useToday } from '../../../state/useToday'
import type { PlanActions, PlanState } from '../../../state/usePlan'
import { TODAY, addDays, baseline, logSet, set, startSession, todayFixture } from '../../../test/todayFixture'
import { PlanContent, PlanScreen } from '../PlanScreen'
import { TrainingModeContext } from '../../../ui/components/ModeLabel'
import { TemplatesScreen } from './TemplatesScreen'

/** Choosing a plan template (1.14, Task 4), as Android's TemplatesViewModelTest and TemplatesScreenTest. */

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  // The powerlifting side trains Monday, Wednesday, Friday; the bodybuilding side Tuesday, Thursday, Saturday.
  await updateSettings(db, (s) => ({ ...s, trainingDays: [1, 3, 5], bodybuildingTrainingDays: [2, 4, 6] }))
})
afterEach(async () => {
  cleanup()
  vi.restoreAllMocks()
  await fx.dispose()
})

/** The 3-day powerlifting template's Wednesday (its second day, on Monday, Wednesday, Friday), strict press off. */
const WEDNESDAY_PL_3 = ['Deadlift', 'Bench', 'Lat pulldown', 'Triceps pushdown']
const P = 'com.powertracker.domain.model.AdvicePayload'

const onSide = (mode: TrainingMode, over: Partial<AppSettings> = {}) => updateSettings(db, (s) => ({ ...s, trainingMode: mode, ...over }))
const id = (name: string) => resolveCatalogue(db, name)
const item = async (mode: TrainingMode, weekday: number, name: string, order = 0, over: Partial<PlanItemRow> = {}) =>
  (await db.planItems.add({
    weekday,
    exerciseId: await id(name),
    sets: 3,
    reps: mode === 'BODYBUILDING' ? 8 : 5,
    ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}),
    targetRpe: 8,
    order,
    mode,
    ...over,
  })) as number

/** `mode`'s plan as "3 0 Bench 3x5-none @ 8", in week and plan order. */
async function described(mode: TrainingMode): Promise<string[]> {
  const names = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
  return (await planFor(db, mode))
    .sort((a, b) => a.weekday - b.weekday || a.order - b.order)
    .map((p) => `${p.weekday} ${p.order} ${names.get(p.exerciseId)} ${p.sets}x${p.reps}-${'repsHigh' in p ? p.repsHigh : 'none'} @ ${p.targetRpe}`)
}

/** The template's week as the domain lays it on `days`, described as `described` does. */
function expected(templateId: string, days: number[], strictPress = false, fixed = true): string[] {
  return grit
    .planTemplateDraft(templateId, days, strictPress)
    .sessions.flatMap((s) => s.exercises.map((e, order) => `${s.day} ${order} ${e.name} ${e.sets}x${e.repsLow}-${fixed ? 'none' : e.repsHigh} @ ${e.rpe}`))
}

function show(mode: TrainingMode) {
  const onLeave = vi.fn()
  const onSaved = vi.fn()
  render(<TemplatesScreen db={db} mode={mode} onLeave={onLeave} onSaved={onSaved} />)
  return { onLeave, onSaved }
}

const button = (name: string | RegExp) => screen.getByRole('button', { name })
const tap = (name: string | RegExp) => userEvent.click(button(name))
const chip = (day: string) => within(screen.getByRole('group', { name: 'Which days?' })).getByRole('checkbox', { name: day })
const openTemplate = async (name: string) => userEvent.click(await screen.findByRole('button', { name }))
const headings = () => screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)
const useButton = () => button('Use this plan')

/** Saves `templateId` on `mode`'s side through the hook, on its suggested days: what Use this plan (and Replace) does. */
async function saveTemplate(mode: TrainingMode, templateId: string) {
  const hook = renderHook(() => useTemplates(db, mode))
  await waitFor(() => expect(hook.result.current.loading).toBe(false))
  act(() => hook.result.current.choose(templateId))
  act(() => hook.result.current.use())
  if (hook.result.current.confirming) act(() => hook.result.current.confirmReplace())
  await waitFor(() => expect(hook.result.current.saved).toBe(true))
  hook.unmount()
}

describe('the ways into the templates', () => {
  const showPlan = (onChooseTemplate = vi.fn(), onBuildPlan = vi.fn()) => {
    render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} onBuildPlan={onBuildPlan} onChooseTemplate={onChooseTemplate} />)
    return { onChooseTemplate, onBuildPlan }
  }

  it('anEmptyPowerliftingPlan_saysSo_andOffersChooseATemplate', async () => {
    const { onChooseTemplate } = showPlan()
    expect(await screen.findByText('Your plan is empty. Start from a template or add exercises to each day.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Build my plan' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Plan options' })).toBeNull()
    await tap('Choose a template')
    expect(onChooseTemplate).toHaveBeenCalledWith('POWERLIFTING')
  })

  it('anEmptyBodybuildingPlan_hasChooseATemplateFirst_andBuildMyPlanUnderIt', async () => {
    await onSide('BODYBUILDING')
    const { onChooseTemplate, onBuildPlan } = showPlan()
    expect(await screen.findByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeInTheDocument()
    const choose = button('Choose a template')
    const build = button('Build my plan')
    expect(choose.compareDocumentPosition(build) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(choose).toHaveClass('gr-btn-primary')
    expect(build).toHaveClass('gr-btn-secondary')
    await userEvent.click(choose)
    expect(onChooseTemplate).toHaveBeenCalledWith('BODYBUILDING')
    await userEvent.click(build)
    expect(onBuildPlan).toHaveBeenCalledOnce()
    expect(screen.queryByRole('button', { name: 'Plan options' })).toBeNull()
  })

  it('thePlanMenu_onBothSides_hasChooseATemplate_afterRebuildOnBodybuilding', async () => {
    await item('POWERLIFTING', 3, 'Squat')
    const pl = showPlan()
    await screen.findByRole('listitem', { name: 'Squat 3 × 5 @ 8' })
    await tap('Plan options')
    expect(screen.getAllByRole('menuitem').map((m) => m.textContent)).toEqual(['Choose a template'])
    await userEvent.click(screen.getByRole('menuitem', { name: 'Choose a template' }))
    expect(pl.onChooseTemplate).toHaveBeenCalledWith('POWERLIFTING')
    cleanup()

    await item('BODYBUILDING', 2, 'Curl')
    await onSide('BODYBUILDING')
    const bb = showPlan()
    await screen.findByRole('listitem', { name: 'Curl 3 × 8–12 @ 8' })
    await tap('Plan options')
    expect(screen.getAllByRole('menuitem').map((m) => m.textContent)).toEqual(['Rebuild my plan', 'Choose a template'])
    await userEvent.click(screen.getByRole('menuitem', { name: 'Choose a template' }))
    expect(bb.onChooseTemplate).toHaveBeenCalledWith('BODYBUILDING')
  })

  it('beforeThePlanIsRead_planShowsNoEmptyLine_noTemplateButton_andNoMenu', () => {
    // The first frame, before the stored plan is read (Android's 9c89f4d): nothing is called empty, no menu is drawn.
    const loading: PlanState = { loading: true, sections: [], exercises: [], recentIds: [], mainExercises: [], defaultRpe: 8, trainingDays: [], side: 'POWERLIFTING' }
    for (const side of ['POWERLIFTING', 'BODYBUILDING'] as const) {
      render(<PlanContent state={{ ...loading, side }} actions={{} as PlanActions} onOpenSettings={() => {}} onBuildPlan={() => {}} onChooseTemplate={() => {}} />)
      expect(screen.queryByText(/plan is empty/)).toBeNull()
      expect(screen.queryByRole('button', { name: 'Choose a template' })).toBeNull()
      expect(screen.queryByRole('button', { name: 'Build my plan' })).toBeNull()
      expect(screen.queryByRole('button', { name: 'Plan options' })).toBeNull()
      cleanup()
    }
  })

  it('aMessage_isShownOnce_andClearedWhereItCameFrom', async () => {
    const onMessageShown = vi.fn()
    render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} message="Plan set: 3-day powerlifting." onMessageShown={onMessageShown} />)
    expect(await screen.findByText('Plan set: 3-day powerlifting.')).toBeInTheDocument()
    expect(onMessageShown).toHaveBeenCalledOnce()
  })

  it('theRoute_namesTheSide', () => {
    expect(parseHash('#/plan/templates/powerlifting')).toEqual({ page: 'templates', mode: 'POWERLIFTING' })
    expect(parseHash('#/plan/templates/bodybuilding')).toEqual({ page: 'templates', mode: 'BODYBUILDING' })
    expect(parseHash('#/plan/templates/other')).toEqual({ tab: 'today' })
    expect(routeToHash({ page: 'templates', mode: 'BODYBUILDING' })).toBe('#/plan/templates/bodybuilding')
    expect(routeToHash({ page: 'templates', mode: 'POWERLIFTING' })).toBe('#/plan/templates/powerlifting')
  })
})

describe('the templates screen', () => {
  it('list_isTheSidesTemplates_fewestDaysFirst_withTheirLines', async () => {
    show('POWERLIFTING')
    await screen.findByRole('heading', { level: 1, name: 'Choose a template' })
    const rows = within(screen.getByRole('list', { name: 'Templates' })).getAllByRole('button')
    expect(rows.map((r) => r.querySelector('.tpl-name')?.textContent)).toEqual(['3-day powerlifting', '4-day powerlifting', '5-day powerlifting'])
    expect(rows[0]).toHaveAccessibleDescription('3 days a week · New to powerlifting: 2–3 days a week Squat & bench · Deadlift & bench · Squat & deadlift')
    cleanup()
    show('BODYBUILDING')
    await screen.findByRole('heading', { level: 1, name: 'Choose a template' })
    expect(within(screen.getByRole('list', { name: 'Templates' })).getAllByRole('button').map((r) => r.querySelector('.tpl-name')?.textContent)).toEqual([
      '3-day full body',
      '4-day upper/lower',
      '5-day body-part split',
    ])
    expect(screen.getByText('Upper A · Lower A · Upper B · Lower B')).toBeInTheDocument()
  })

  it('draft_startsOnTheSidesOwnDays_whenAsMany_elseTheSpread_andBackReturnsToTheList', async () => {
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    expect(screen.getByRole('heading', { level: 1, name: '3-day powerlifting' })).toBeInTheDocument()
    expect(screen.getByText('Mon, Wed, Fri')).toBeInTheDocument()
    expect(headings()).toEqual(expect.arrayContaining(['Monday · Squat & bench', 'Wednesday · Deadlift & bench', 'Friday · Squat & deadlift']))
    expect(chip('Mon')).toBeChecked()
    expect(chip('Tue')).not.toBeChecked()
    await tap('Back')
    await openTemplate('4-day powerlifting')
    // Monday, Tuesday, Thursday, Friday: the fixed 4-day spread, as the side's days are three.
    expect(screen.getByText('Mon, Tue, Thu, Fri')).toBeInTheDocument()
    expect(useButton()).toBeEnabled()
  })

  it('browserBack_fromTheDraft_returnsToTheList_asAndroidsBack_andTheScreensBackTakesItsEntryOff', async () => {
    window.history.replaceState({ gritFrom: '#/plan' }, '')
    const { onLeave } = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    expect((window.history.state as { templatesStep?: string }).templatesStep).toBe('draft')
    window.history.back()
    expect(await screen.findByRole('heading', { level: 1, name: 'Choose a template' })).toBeInTheDocument()
    expect(onLeave).not.toHaveBeenCalled()
    expect(window.history.state).toEqual({ gritFrom: '#/plan' })
    // The draft's own Back does the same, and leaves no entry behind for the browser's Back to land on.
    await openTemplate('4-day powerlifting')
    await tap('Back')
    await waitFor(() => expect(window.history.state).toEqual({ gritFrom: '#/plan' }))
    expect(screen.getByRole('heading', { level: 1, name: 'Choose a template' })).toBeInTheDocument()
    expect(onLeave).not.toHaveBeenCalled()
  })

  it('closeFromTheDraft_andASave_leaveFromTheScreensOwnEntry', async () => {
    window.history.replaceState({ gritFrom: '#/plan' }, '')
    const first = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    await tap('Close')
    await waitFor(() => expect(first.onLeave).toHaveBeenCalled())
    expect(window.history.state).toEqual({ gritFrom: '#/plan' })
    cleanup()

    const second = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    expect((window.history.state as { templatesStep?: string }).templatesStep).toBe('draft')
    await userEvent.click(useButton())
    await waitFor(() => expect(second.onSaved).toHaveBeenCalledWith('3-day powerlifting'))
    expect(window.history.state).toEqual({ gritFrom: '#/plan' })
  })

  it('theHeadersModeLabel_isTheRoutesSide_notTheApps', async () => {
    render(
      <TrainingModeContext.Provider value="BODYBUILDING">
        <TemplatesScreen db={db} mode="POWERLIFTING" onLeave={vi.fn()} onSaved={vi.fn()} />
      </TrainingModeContext.Provider>,
    )
    expect(await screen.findByRole('heading', { level: 1, name: 'Choose a template' })).toBeInTheDocument()
    expect(screen.queryByTestId('mode-label')).toBeNull()
    cleanup()
    render(
      <TrainingModeContext.Provider value="POWERLIFTING">
        <TemplatesScreen db={db} mode="BODYBUILDING" onLeave={vi.fn()} onSaved={vi.fn()} />
      </TrainingModeContext.Provider>,
    )
    expect(await screen.findByRole('heading', { level: 1, name: 'Choose a template' })).toBeInTheDocument()
    expect(screen.getByTestId('mode-label')).toHaveTextContent('Bodybuilding mode')
  })

  it('useThisPlan_needsExactlyTheTemplatesDayCount_andTheCardsSayDayNWhileItIsOff', async () => {
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    await userEvent.click(chip('Fri'))
    expect(screen.getByText('Choose 3 days.')).toBeInTheDocument()
    expect(useButton()).toBeDisabled()
    expect(headings()).toEqual(expect.arrayContaining(['Day 1 · Squat & bench', 'Day 2 · Deadlift & bench', 'Day 3 · Squat & deadlift']))
    await userEvent.click(chip('Sat'))
    await userEvent.click(chip('Sun'))
    expect(useButton()).toBeDisabled()
    await userEvent.click(chip('Mon'))
    expect(screen.getByText('Wed, Sat, Sun')).toBeInTheDocument()
    expect(headings()).toEqual(expect.arrayContaining(['Wednesday · Squat & bench', 'Saturday · Deadlift & bench', 'Sunday · Squat & deadlift']))
    expect(useButton()).toBeEnabled()
  })

  it('swap_noneOnAMainLift_theTemplatesListOnAnAccessory_andTheSheetNamesTheCardsDay', async () => {
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    for (const lift of ['Squat', 'Bench', 'Deadlift']) expect(screen.queryByRole('button', { name: `Swap ${lift}` })).toBeNull()
    // Move the week to Tuesday, Thursday, Saturday: Wednesday's day is Thursday's now.
    for (const day of ['Mon', 'Wed', 'Fri', 'Tue', 'Thu', 'Sat']) await userEvent.click(chip(day))
    const thursday = screen.getByRole('region', { name: 'Thursday' })
    await userEvent.click(within(thursday).getByRole('button', { name: 'Swap Lat pulldown' }))
    const sheet = screen.getByRole('dialog', { name: 'Swap Lat pulldown' })
    expect(within(sheet).getByText('Thursday')).toBeInTheDocument()
    const offered = within(sheet).getAllByRole('listitem').map((li) => li.getAttribute('aria-label'))
    const draft = grit.planTemplateDraft('pl-3', [2, 4, 6], false)
    expect(offered).toEqual(grit.planTemplateAlternatives('pl-3', draft, 4, 'Lat pulldown').map((e) => e.name))
    await userEvent.click(within(within(sheet).getByRole('listitem', { name: offered[0]! })).getByRole('button'))
    expect(within(screen.getByRole('region', { name: 'Thursday' })).getByRole('listitem', { name: `${offered[0]} 3 × 10 @ 8` })).toBeInTheDocument()
    // The swap stays through a change of days, and the sheet names the day as its card does: "Day 2" while the count is off.
    await userEvent.click(chip('Sat'))
    const day2 = screen.getByRole('region', { name: 'Day 2' })
    expect(within(day2).getByRole('listitem', { name: `${offered[0]} 3 × 10 @ 8` })).toBeInTheDocument()
    await userEvent.click(within(day2).getByRole('button', { name: 'Swap Triceps pushdown' }))
    expect(within(screen.getByRole('dialog', { name: 'Swap Triceps pushdown' })).getByText('Day 2')).toBeInTheDocument()
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Swap Triceps pushdown' })).getByRole('button', { name: 'Close' }))
    // Once the count matches again the week moves onto the new days, swap kept.
    await userEvent.click(chip('Sun'))
    expect(within(screen.getByRole('region', { name: 'Thursday' })).getByRole('listitem', { name: `${offered[0]} 3 × 10 @ 8` })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Sunday' })).toBeInTheDocument()
  })

  it('swap_inTheHook_refusesAMainLift_andANameThatIsNotOffered_andTakesOneThatIs', async () => {
    // As Android's TemplatesViewModelTest: the domain's swap itself takes any name, so these are the hook's own refusals.
    const hook = renderHook(() => useTemplates(db, 'POWERLIFTING'))
    await waitFor(() => expect(hook.result.current.loading).toBe(false))
    act(() => hook.result.current.choose('pl-3'))
    const monday = () => hook.result.current.draft!.sessions.find((s) => s.day === 1)!.exercises.map((e) => e.name)
    expect(monday()).toEqual(['Squat', 'Bench', 'Seated cable row', 'Cable crunch'])
    const before = hook.result.current.draft

    // A main lift is never swapped, even if asked.
    expect(hook.result.current.alternatives(1, 'Squat')).toEqual([])
    act(() => hook.result.current.swap(1, 'Squat', 'Leg press'))
    expect(hook.result.current.draft).toBe(before)

    // A real catalogue exercise that is not among the offered alternatives is refused too.
    const offered = hook.result.current.alternatives(1, 'Seated cable row').map((e) => e.name)
    expect(offered.length).toBeGreaterThan(0)
    expect(grit.findInLibrary('Leg press')).not.toBeNull()
    expect(offered).not.toContain('Leg press')
    act(() => hook.result.current.swap(1, 'Seated cable row', 'Leg press'))
    expect(hook.result.current.draft).toBe(before)

    // One that is offered is swapped, with the same sets, reps and RPE.
    act(() => hook.result.current.swap(1, 'Seated cable row', offered[0]!))
    expect(monday()).toEqual(['Squat', 'Bench', offered[0], 'Cable crunch'])
    const swapped = hook.result.current.draft!.sessions[0]!.exercises[2]!
    expect([swapped.sets, swapped.repsLow, swapped.rpe]).toEqual([3, 10, 8])
    hook.unmount()
  })

  it('aVariation_swapsOnlyToVariationsOfTheSameLift', async () => {
    show('POWERLIFTING')
    await openTemplate('4-day powerlifting')
    // Pause squat is on the 4-day template's last day, Friday on the spread.
    await userEvent.click(within(screen.getByRole('region', { name: 'Friday' })).getByRole('button', { name: 'Swap Pause squat' }))
    const offered = within(screen.getByRole('dialog', { name: 'Swap Pause squat' }))
      .getAllByRole('listitem')
      .map((li) => li.getAttribute('aria-label')!)
    expect(offered.length).toBeGreaterThan(0)
    for (const name of offered) expect([grit.findInLibrary(name)?.category, grit.findInLibrary(name)?.parentLift]).toEqual(['VARIATION', 'SQUAT'])
  })

  it('strictPress_isInTheTemplatesOnlyWithTheSettingOn_andHasNoSwap', async () => {
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    expect(screen.queryByText('Strict press')).toBeNull()
    cleanup()
    await updateSettings(db, (s) => ({ ...s, showStrictPress: true }))
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    const wednesday = screen.getByRole('region', { name: 'Wednesday' })
    expect(within(wednesday).getByRole('listitem', { name: 'Strict press 3 × 5 @ 8' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Swap Strict press' })).toBeNull()
  })

  it('theBodybuildingDraft_showsRanges_andTheWeeklySets_thePowerliftingDraftNeither', async () => {
    show('BODYBUILDING')
    await openTemplate('4-day upper/lower')
    expect(screen.getByRole('region', { name: 'Weekly sets' })).toBeInTheDocument()
    expect(screen.getAllByRole('listitem', { name: 'DB press 4 × 8–12 @ 8' }).length).toBeGreaterThan(0)
    cleanup()
    show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    expect(screen.queryByRole('region', { name: 'Weekly sets' })).toBeNull()
    expect(screen.getAllByRole('listitem', { name: 'Squat 3 × 5 @ 8' })).toHaveLength(2)
  })
})

describe('saving a template', () => {
  it('onAnEmptyPowerliftingSide_storesItOnTheChosenDays_withFixedReps_andSetsOnlyThatSidesDays', async () => {
    const { onSaved } = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    for (const day of ['Mon', 'Tue']) await userEvent.click(chip(day))
    await tap('Use this plan')
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith('3-day powerlifting'))
    expect(await described('POWERLIFTING')).toEqual(expected('pl-3', [2, 3, 5]))
    // No top of a range at all: the key is left out, as on every fixed-reps row.
    expect((await planFor(db, 'POWERLIFTING')).every((p) => !('repsHigh' in p))).toBe(true)
    const settings = await getSettings(db)
    expect(settings.trainingDays).toEqual([2, 3, 5])
    expect(settings.bodybuildingTrainingDays).toEqual([2, 4, 6])
    expect(await planFor(db, 'BODYBUILDING')).toEqual([])
  })

  it('onAnEmptyBodybuildingSide_keepsTheRanges_andSetsOnlyItsDays', async () => {
    const { onSaved } = show('BODYBUILDING')
    await openTemplate('3-day full body')
    expect(screen.getByText('Tue, Thu, Sat')).toBeInTheDocument()
    await tap('Use this plan')
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith('3-day full body'))
    expect(await described('BODYBUILDING')).toEqual(expected('bb-3', [2, 4, 6], false, false))
    expect((await getSettings(db)).trainingDays).toEqual([1, 3, 5])
    expect((await getSettings(db)).bodybuildingTrainingDays).toEqual([2, 4, 6])
  })

  it('replacing_asksFirst_cancelStoresNothing_andReplaceTouchesOnlyThatSide', async () => {
    const squat = await id('Squat')
    const bench = await item('POWERLIFTING', 3, 'Bench')
    await item('POWERLIFTING', 3, 'Curl', 1)
    await item('BODYBUILDING', 2, 'Curl')
    await baseline(db, squat, 180, addDays(TODAY, -20))
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squat, 150))
    await db.checkIns.add({ date: addDays(TODAY, -10), lagging: ['CHEST'], growingWell: [] })
    await db.advice.add({ type: 'DELOAD', lift: null, createdAt: TODAY, reason: 'Tired.', severity: 2, status: 'OPEN', payloadJson: JSON.stringify({ type: `${P}.Deload` }) })
    await storeSkip(db, { date: addDays(TODAY, -2), mode: 'POWERLIFTING', exerciseId: await id('Bench'), planItemId: bench, reason: 'BUSY' }, () => [])
    const keep = async () => ({
      sessions: await db.sessions.toArray(),
      sets: await db.sets.toArray(),
      baselines: await db.baselines.toArray(),
      checkIns: await db.checkIns.toArray(),
      advice: await db.advice.toArray(),
      bodybuilding: await described('BODYBUILDING'),
      bodybuildingDays: (await getSettings(db)).bodybuildingTrainingDays,
    })
    const before = { ...(await keep()), powerlifting: await described('POWERLIFTING'), skips: await storedSkips(db), days: (await getSettings(db)).trainingDays }

    const { onSaved } = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    await tap('Use this plan')
    const confirm = screen.getByRole('dialog', { name: 'Replace your powerlifting plan?' })
    expect(within(confirm).getByText('Your history and bests are kept.')).toBeInTheDocument()
    await userEvent.click(within(confirm).getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect({ ...(await keep()), powerlifting: await described('POWERLIFTING'), skips: await storedSkips(db), days: (await getSettings(db)).trainingDays }).toEqual(before)
    expect(onSaved).not.toHaveBeenCalled()

    await tap('Use this plan')
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Replace your powerlifting plan?' })).getByRole('button', { name: 'Replace' }))
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith('3-day powerlifting'))
    expect(await described('POWERLIFTING')).toEqual(expected('pl-3', [1, 3, 5]))
    const { powerlifting: _pl, skips: oldSkips, days: _days, ...rest } = before
    expect(await keep()).toEqual(rest)
    // The skip keeps its record, with no plan item, as when a plan item is deleted.
    expect(await storedSkips(db)).toEqual(oldSkips.map((s) => ({ ...s, planItemId: null })))
  })

  it('theBodybuildingConfirm_namesItsSide', async () => {
    await item('BODYBUILDING', 2, 'Curl')
    show('BODYBUILDING')
    await openTemplate('3-day full body')
    await tap('Use this plan')
    expect(screen.getByRole('dialog', { name: 'Replace your bodybuilding plan?' })).toBeInTheDocument()
  })

  it('aFailedSave_saysSo_changesNothing_andCanBeTriedAgain', async () => {
    const { onSaved } = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    await userEvent.click(chip('Mon'))
    await userEvent.click(chip('Tue'))
    const add = vi.spyOn(db.planItems, 'add').mockRejectedValueOnce(new Error('disk full'))
    await tap('Use this plan')
    expect(await screen.findByRole('alert')).toHaveTextContent('The plan could not be saved. Nothing was changed.')
    // One transaction: neither the plan nor the side's days were written.
    expect(await planFor(db, 'POWERLIFTING')).toEqual([])
    expect((await getSettings(db)).trainingDays).toEqual([1, 3, 5])
    expect(onSaved).not.toHaveBeenCalled()
    add.mockRestore()
    await tap('Use this plan')
    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    expect(await described('POWERLIFTING')).toEqual(expected('pl-3', [2, 3, 5]))
    expect((await getSettings(db)).trainingDays).toEqual([2, 3, 5])
  })
})

describe('a replace whose training days cannot be set', () => {
  it('leavesTheOldPlanItsSkipAndItsDays_saysNothingWasChanged_andATryAgainSavesBoth', async () => {
    const bench = await item('POWERLIFTING', 3, 'Bench')
    await item('POWERLIFTING', 1, 'Curl')
    await updateSettings(db, (s) => ({ ...s, trainingDays: [1, 3] }))
    await storeSkip(db, { date: addDays(TODAY, -2), mode: 'POWERLIFTING', exerciseId: await id('Bench'), planItemId: bench, reason: 'BUSY' }, () => [])
    const rows = await planFor(db, 'POWERLIFTING')
    const skips = await storedSkips(db)

    const { onSaved } = show('POWERLIFTING')
    await openTemplate('3-day powerlifting')
    // The plan is replaced first; then the side's days are written, and that write fails, once.
    const put = vi.spyOn(db.settings, 'bulkPut').mockRejectedValueOnce(new Error('disk full'))
    await tap('Use this plan')
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Replace your powerlifting plan?' })).getByRole('button', { name: 'Replace' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('The plan could not be saved. Nothing was changed.')
    expect(put).toHaveBeenCalledOnce()
    // One transaction: the old plan is there with its own ids, its skip still points at its item, the days are as stored.
    expect(await planFor(db, 'POWERLIFTING')).toEqual(rows)
    expect(await storedSkips(db)).toEqual(skips)
    expect((await getSettings(db)).trainingDays).toEqual([1, 3])
    expect(onSaved).not.toHaveBeenCalled()

    put.mockRestore()
    await tap('Use this plan')
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Replace your powerlifting plan?' })).getByRole('button', { name: 'Replace' }))
    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce())
    expect(await described('POWERLIFTING')).toEqual(expected('pl-3', [1, 3, 5]))
    expect((await getSettings(db)).trainingDays).toEqual([1, 3, 5])
  })
})

describe('the coach after a template is saved', () => {
  async function todaysTarget(exerciseId: number) {
    const rows = (await db.advice.toArray()).filter((a) => a.type === 'TARGET' && a.createdAt === TODAY && a.status === 'OPEN')
    return rows.map((a) => grit.advicePayload(a.payloadJson)!).find((p) => p.exerciseId === exerciseId) ?? null
  }
  const today = () => renderHook(() => useToday(db, () => TODAY))
  const passed = async (hook: ReturnType<typeof today>) => waitFor(() => expect(hook.result.current.coachPasses).toBeGreaterThan(0))

  it('aNewLifter_getsNoTargetOnATemplatesLifts_onEitherSide', async () => {
    await saveTemplate('POWERLIFTING', 'pl-3')
    const pl = today()
    await waitFor(() => expect(pl.result.current.items.map((i) => i.exercise.name)).toEqual(WEDNESDAY_PL_3))
    await passed(pl)
    for (const name of WEDNESDAY_PL_3) expect(await todaysTarget(await id(name))).toBeNull()
    pl.unmount()

    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [1, 3, 5] })
    await saveTemplate('BODYBUILDING', 'bb-3')
    const bb = today()
    await waitFor(() => expect(bb.result.current.items.map((i) => i.exercise.name)).toContain('Face pull'))
    await passed(bb)
    for (const i of bb.result.current.items) expect(await todaysTarget(i.exercise.id)).toBeNull()
  })

  it('withHistory_aPowerliftingLiftOnTwoDays_getsTheSameTargetAsAPlainPlan', async () => {
    const bench = await id('Bench')
    await baseline(db, bench, 125, addDays(TODAY, -30))
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(bench, 100, { rpe: 7, isTopSet: true }))
    await item('POWERLIFTING', 3, 'Bench')
    const control = today()
    await waitFor(async () => expect(await todaysTarget(bench)).not.toBeNull())
    const plain = await todaysTarget(bench)
    control.unmount()
    await db.advice.clear()
    await db.planItems.clear()

    await saveTemplate('POWERLIFTING', 'pl-3')
    expect((await planFor(db, 'POWERLIFTING')).filter((p) => p.exerciseId === bench)).toHaveLength(2)
    today()
    await waitFor(async () => expect(await todaysTarget(bench)).not.toBeNull())
    const target = await todaysTarget(bench)
    expect([target?.weightKg, target?.reps]).toEqual([plain?.weightKg, plain?.reps])
    expect(target!.weightKg).toBeGreaterThan(100)
  })

  it('withHistory_aBodybuildingLiftOnTwoDays_getsTheSameTargetAsAPlainPlan', async () => {
    // Face pull is on the 3-day full body's Wednesday and Friday, 3 × 8–12 @ 8 on both.
    await onSide('BODYBUILDING', { bodybuildingTrainingDays: [1, 3, 5] })
    const facePull = await id('Face pull')
    const last = (await db.sessions.add({ date: addDays(TODAY, -7), bodyweightKg: null, readiness: null, mode: 'BODYBUILDING' })) as number
    for (let i = 0; i < 3; i++) await logSet(db, last, set(facePull, 20, { reps: 12 }))
    await item('BODYBUILDING', 3, 'Face pull')
    const control = today()
    await waitFor(async () => expect(await todaysTarget(facePull)).not.toBeNull())
    const plain = await todaysTarget(facePull)
    control.unmount()
    await db.advice.clear()
    await db.planItems.clear()

    await saveTemplate('BODYBUILDING', 'bb-3')
    expect((await planFor(db, 'BODYBUILDING')).filter((p) => p.exerciseId === facePull)).toHaveLength(2)
    today()
    await waitFor(async () => expect(await todaysTarget(facePull)).not.toBeNull())
    expect(await todaysTarget(facePull)).toEqual(plain)
  })
})

describe('replacing with state tied to the old plan', () => {
  const card = (type: 'SKIP' | 'SWAP', mode: TrainingMode, payloadJson: string): AdviceRow => ({ type, lift: null, createdAt: TODAY, reason: 'Card.', severity: 3, status: 'OPEN', payloadJson, mode })
  const status = async (adviceId: number) => (await db.advice.get(adviceId))?.status
  const namesOn = async (mode: TrainingMode, weekday?: number) => {
    const names = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    return (await planFor(db, mode)).filter((p) => weekday == null || p.weekday === weekday).map((p) => names.get(p.exerciseId))
  }

  it('anOpenSkipCard_forAnOldItem_thenApply_afterAPowerliftingReplace', async () => {
    const curl = await id('Curl')
    const pulldown = await id('Lat pulldown')
    const curlItem = await item('POWERLIFTING', 1, 'Curl')
    const pulldownItem = await item('POWERLIFTING', 3, 'Lat pulldown')
    const curlSkip = await storeSkip(db, { date: addDays(TODAY, -1), mode: 'POWERLIFTING', exerciseId: curl, planItemId: curlItem, reason: 'BUSY' }, () => [])
    const pulldownSkip = await storeSkip(db, { date: addDays(TODAY, -2), mode: 'POWERLIFTING', exerciseId: pulldown, planItemId: pulldownItem, reason: 'BUSY' }, () => [])
    const skipJson = (skipId: number, exerciseId: number, name: string, to: string) =>
      JSON.stringify({ type: `${P}.Skipped`, skipId, exerciseId, name, reason: 'BUSY', alternatives: [to] })
    const absent = (await db.advice.add(card('SKIP', 'POWERLIFTING', skipJson(curlSkip, curl, 'Curl', 'Hammer curl')))) as number
    const present = (await db.advice.add(card('SKIP', 'POWERLIFTING', skipJson(pulldownSkip, pulldown, 'Lat pulldown', 'Chin-up')))) as number
    const coach = renderHook(() => useCoach(db, () => TODAY)).result
    await waitFor(() => expect(coach.current.cards).toHaveLength(2))
    await saveTemplate('POWERLIFTING', 'pl-3')
    const replaced = await described('POWERLIFTING')

    // Curl is not in the template: nothing is swapped and the card stays, as after Rebuild my plan.
    await act(() => coach.current.apply(absent, 'Hammer curl'))
    expect(coach.current.message?.text).toBe('Your plan has changed since; nothing was swapped')
    expect(await status(absent)).toBe('OPEN')
    expect(await described('POWERLIFTING')).toEqual(replaced)

    // Lat pulldown is in the template on Wednesday: the swap lands on the new item.
    await act(() => coach.current.apply(present, 'Chin-up'))
    expect(await status(present)).toBe('APPLIED')
    const wednesday = await namesOn('POWERLIFTING', 3)
    expect(wednesday).toContain('Chin-up')
    expect(wednesday).not.toContain('Lat pulldown')
    expect(await planFor(db, 'POWERLIFTING')).toHaveLength(replaced.length)
    expect((await planFor(db, 'POWERLIFTING')).every((p) => p.repsHigh == null)).toBe(true)
  })

  it('anOpenSwapCard_forAnOldItem_thenApply_afterABodybuildingReplace', async () => {
    await onSide('BODYBUILDING')
    const pecDeck = await id('Pec deck')
    const pulldown = await id('Lat pulldown')
    await item('BODYBUILDING', 2, 'Pec deck')
    await item('BODYBUILDING', 4, 'Lat pulldown')
    const swapJson = (exerciseId: number, name: string, group: string, to: string) =>
      JSON.stringify({ type: `${P}.Swap`, exerciseId, name, group, since: addDays(TODAY, -30), alternatives: [to] })
    const absent = (await db.advice.add(card('SWAP', 'BODYBUILDING', swapJson(pecDeck, 'Pec deck', 'CHEST', 'Cable crossover')))) as number
    const present = (await db.advice.add(card('SWAP', 'BODYBUILDING', swapJson(pulldown, 'Lat pulldown', 'LATS', 'Neutral-grip pull-up')))) as number
    const coach = renderHook(() => useCoach(db, () => TODAY)).result
    await waitFor(() => expect(coach.current.cards).toHaveLength(2))
    await saveTemplate('BODYBUILDING', 'bb-3')
    const replaced = await described('BODYBUILDING')

    await act(() => coach.current.apply(absent, 'Cable crossover'))
    expect(coach.current.message?.text).toBe('Your plan has changed since; nothing was swapped')
    expect(await status(absent)).toBe('OPEN')
    expect(await described('BODYBUILDING')).toEqual(replaced)

    await act(() => coach.current.apply(present, 'Neutral-grip pull-up'))
    expect(await status(present)).toBe('APPLIED')
    const names = await namesOn('BODYBUILDING')
    expect(names).toContain('Neutral-grip pull-up')
    expect(names).not.toContain('Lat pulldown')
    expect(await planFor(db, 'BODYBUILDING')).toHaveLength(replaced.length)
  })

  it('aSkipMadeToday_keepsItsRecord_throughAReplace', async () => {
    const bench = await id('Bench')
    const curl = await id('Curl')
    const benchItem = await item('POWERLIFTING', 3, 'Bench')
    const curlItem = await item('POWERLIFTING', 3, 'Curl', 1)
    const vm = renderHook(() => useToday(db, () => TODAY)).result
    await waitFor(() => expect(vm.current.items).toHaveLength(2))
    await act(async () => {
      await vm.current.skip(bench, benchItem, 'BUSY')
    })
    // Curl, which the template does not plan, skipped today too.
    await storeSkip(db, { date: TODAY, mode: 'POWERLIFTING', exerciseId: curl, planItemId: curlItem, reason: 'TIME' }, () => [])
    const skips = await storedSkips(db)
    expect(skips).toHaveLength(2)

    await saveTemplate('POWERLIFTING', 'pl-3')

    expect(await storedSkips(db)).toEqual(skips.map((s) => ({ ...s, planItemId: null })))
    // Wednesday is the template's now: bench is still skipped today, the rest are on the list.
    await waitFor(() => expect(vm.current.items.map((i) => i.exercise.name)).toEqual(WEDNESDAY_PL_3.filter((n) => n !== 'Bench')))
    expect(vm.current.skippedToday.map((s) => s.name)).toEqual(['Bench'])
  })

  it('aSessionInProgress_keepsItsLoggedSets_throughAReplace', async () => {
    const bench = await id('Bench')
    await item('POWERLIFTING', 3, 'Bench')
    const vm = renderHook(() => useToday(db, () => TODAY)).result
    await waitFor(() => expect(vm.current.items).toHaveLength(1))
    await act(() => vm.current.startSession(85, 4))
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    await act(() => vm.current.logSet(bench, { weightKg: 100, reps: 5, rpe: 8, failed: false }, true))
    await waitFor(() => expect(vm.current.items[0]?.sets).toHaveLength(1))

    await saveTemplate('POWERLIFTING', 'pl-3')
    await waitFor(() => expect(new Set(vm.current.items.map((i) => i.exercise.name))).toEqual(new Set(WEDNESDAY_PL_3)))
    const benchRow = vm.current.items.find((i) => i.exercise.id === bench)!
    expect(benchRow.sets).toHaveLength(1)
    expect(benchRow.planned).not.toBeNull()

    // Logging goes on against the new plan.
    await act(() => vm.current.logSet(bench, { weightKg: 102.5, reps: 5, rpe: 8, failed: false }, true))
    await waitFor(() => expect(vm.current.items.find((i) => i.exercise.id === bench)?.sets).toHaveLength(2))
    expect((await db.sets.where('sessionId').equals(vm.current.sessionId!).sortBy('id')).map((s) => s.weightKg)).toEqual([100, 102.5])
  })
})
