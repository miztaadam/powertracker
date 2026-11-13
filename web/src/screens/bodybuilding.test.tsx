import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { planFor } from '../db/catalogue'
import { getSettings, updateSettings, type GritDb } from '../db/db'
import type { AdviceRow, PlanItemRow, TrainingMode } from '../db/schema'
import { adviceForSide } from '../state/sides'
import { TODAY, addDays, exerciseNamed, logSet, set, setsOn, targets, todayFixture } from '../test/todayFixture'
import { CoachScreen } from './coach/CoachScreen'
import { PlanScreen } from './plan/PlanScreen'
import { ExerciseHistoryScreen } from './progress/ExerciseHistoryScreen'
import { ProgressScreen } from './progress/ProgressScreen'
import { SettingsScreen } from './settings/SettingsScreen'
import { TodayScreen } from './today/TodayScreen'

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squatId: number
let dbPressId: number
let curlId: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squatId = (await exerciseNamed(db, 'Squat')).id
  dbPressId = (await exerciseNamed(db, 'DB press')).id
  curlId = (await exerciseNamed(db, 'Curl')).id
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

/** Today (TODAY) is a Wednesday, weekday 3. */
const planOn = (mode: TrainingMode, exerciseId: number, over: Partial<PlanItemRow> = {}) =>
  db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: mode === 'BODYBUILDING' ? 8 : 5, ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}), targetRpe: 8, order: 0, mode, ...over })

const session = async (date: string, mode: TrainingMode) => (await db.sessions.add({ date, bodyweightKg: null, readiness: null, mode })) as number

/** Puts the app on `mode`; the side it leaves was left on `leftOn` days ago (the other side's left-on as given). */
async function onSide(mode: TrainingMode, over: Partial<Awaited<ReturnType<typeof getSettings>>> = {}) {
  await updateSettings(db, (s) => ({ ...s, trainingMode: mode, trainingDays: [1, 3, 5], bodybuildingTrainingDays: [2, 3, 6], ...over }))
}

/** Last week's DB press on the bodybuilding side: 26 × 11, 10, 9 @ 8. */
async function lastWeeksPress() {
  const id = await session(addDays(TODAY, -7), 'BODYBUILDING')
  for (const reps of [11, 10, 9]) await logSet(db, id, set(dbPressId, 26, { reps, rpe: 8 }))
}

const showToday = (props: { onSetLogged?: (main: boolean) => void; onOpenPlan?: () => void } = {}) =>
  render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} onSetLogged={props.onSetLogged} onOpenPlan={props.onOpenPlan} />)
const showPlan = () => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} />)
const showCoach = (onOpenPlan: () => void = () => {}) => render(<CoachScreen db={db} today={() => TODAY} onOpenPlan={onOpenPlan} />)

const tapWhenShown = async (name: string | RegExp) => userEvent.click(await screen.findByRole('button', { name }))

describe('Plan on the bodybuilding side', () => {
  it('showsOnlyThatSidesPlanAndDays_withRanges_andSwitchingBackShowsThePowerliftingPlan', async () => {
    await planOn('POWERLIFTING', squatId, { weekday: 1 })
    await planOn('BODYBUILDING', dbPressId, { weekday: 2 })
    await onSide('BODYBUILDING')
    showPlan()

    const press = await screen.findByRole('listitem', { name: 'DB press 3 × 8–12 @ 8' })
    expect(within(press).getByText('3 × 8–12', { exact: false })).toBeInTheDocument()
    expect(screen.queryByRole('listitem', { name: /^Squat/ })).toBeNull()
    // The bodybuilding side's training days: Tuesday, Wednesday, Saturday.
    expect(screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)).toEqual(expect.arrayContaining(['Tuesday', 'Wednesday', 'Saturday']))
    expect(screen.queryByRole('heading', { name: 'Monday' })).toBeNull()

    await onSide('POWERLIFTING')
    expect(await screen.findByRole('listitem', { name: 'Squat 3 × 5 @ 8' })).toBeInTheDocument()
    expect(screen.queryByRole('listitem', { name: /^DB press/ })).toBeNull()
    expect(screen.getByRole('heading', { name: 'Monday' })).toBeInTheDocument()
  })

  it('anEmptyBodybuildingPlan_saysSo_andTheDaysInviteExercises', async () => {
    await planOn('POWERLIFTING', squatId)
    await onSide('BODYBUILDING')
    showPlan()
    expect(await screen.findByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Add' }).length).toBe(3)

    // The powerlifting side never shows the line.
    cleanup()
    await onSide('POWERLIFTING')
    showPlan()
    await screen.findByRole('listitem', { name: 'Squat 3 × 5 @ 8' })
    expect(screen.queryByText('Your bodybuilding plan is empty. Add exercises to each training day.')).toBeNull()
  })

  it('addingAnExercise_gives3SetsOf8To12AtRpe8_onThatSideOnly', async () => {
    await updateSettings(db, (s) => ({ ...s, defaultRpe: 7 }))
    await planOn('POWERLIFTING', squatId)
    await onSide('BODYBUILDING', { defaultRpe: 7 })
    showPlan()
    const wednesday = within(await screen.findByRole('region', { name: 'Wednesday' }))
    await userEvent.click(wednesday.getByRole('button', { name: 'Add' }))
    const picker = within(await screen.findByRole('dialog', { name: 'Add to Wednesday' }))
    await userEvent.type(picker.getByRole('textbox', { name: 'Search exercises' }), 'Curl')
    await userEvent.click(within(await picker.findByRole('listitem', { name: 'Curl' })).getAllByRole('button')[0]!)

    expect(await screen.findByRole('listitem', { name: 'Curl 3 × 8–12 @ 8' })).toBeInTheDocument()
    const bb = await planFor(db, 'BODYBUILDING')
    expect(bb.map((p) => [p.exerciseId, p.sets, p.reps, p.repsHigh, p.targetRpe, p.mode])).toEqual([[curlId, 3, 8, 12, 8, 'BODYBUILDING']])
    expect((await planFor(db, 'POWERLIFTING')).map((p) => [p.exerciseId, p.sets, p.reps, p.repsHigh])).toEqual([[squatId, 3, 5, undefined]])
  })

  it('theEntryEditor_hasTwoRepsSteppers_andTheTopNeverGoesBelowTheBottom', async () => {
    await planOn('BODYBUILDING', curlId, { reps: 10, repsHigh: 11 })
    await onSide('BODYBUILDING')
    showPlan()
    const row = await screen.findByRole('listitem', { name: 'Curl 3 × 10–11 @ 8' })
    await userEvent.click(within(row).getByRole('button', { name: 'More' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Edit' }))
    const dialog = within(await screen.findByRole('dialog', { name: 'Edit Wednesday' }))
    expect(dialog.queryByRole('group', { name: 'Reps' })).toBeNull()

    const from = dialog.getByRole('group', { name: 'Reps from' })
    const to = dialog.getByRole('group', { name: 'Reps to' })
    // 10 to 11: the bottom goes up once to 11, then stops at the top.
    await userEvent.click(within(from).getByRole('button', { name: /increase/i }))
    expect(within(from).getByText('11')).toBeInTheDocument()
    expect(within(from).getByRole('button', { name: /increase/i })).toBeDisabled()
    expect(within(to).getByRole('button', { name: /decrease/i })).toBeDisabled()
    await userEvent.click(within(to).getByRole('button', { name: /increase/i }))
    await userEvent.click(within(to).getByRole('button', { name: /increase/i }))
    await userEvent.click(dialog.getByRole('button', { name: 'Save' }))

    await waitFor(async () => expect((await planFor(db, 'BODYBUILDING')).map((p) => [p.reps, p.repsHigh])).toEqual([[11, 13]]))
    expect(await screen.findByRole('listitem', { name: 'Curl 3 × 11–13 @ 8' })).toBeInTheDocument()
  })
})

describe('Today on the bodybuilding side', () => {
  it('showsTheRange_theCoachCard_andApplyFillsTheWeightAndTheRepsToBeat', async () => {
    await planOn('BODYBUILDING', dbPressId)
    await lastWeeksPress()
    await onSide('BODYBUILDING')
    await session(TODAY, 'BODYBUILDING')
    showToday()

    expect(await screen.findByText('26 kg × 8–12 @ 8')).toBeInTheDocument()
    expect(screen.getByText('Last time: 26 × 11, 10, 9. Beat it.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }))
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Weight, kilograms' })).toHaveValue('26'))
    // Set 1 did 11 last time: 12 to beat it.
    expect(screen.getByRole('textbox', { name: 'Reps' })).toHaveValue('12')
    expect(screen.getByRole('textbox', { name: 'RPE' })).toHaveValue('8')
    const stored = await targets(db)
    expect(stored.map((t) => [t.mode, t.status])).toEqual([['BODYBUILDING', 'APPLIED']])
  })

  it('theStartCard_showsTheRange_andANewSessionIsOnTheActiveSide', async () => {
    await planOn('BODYBUILDING', dbPressId)
    await planOn('POWERLIFTING', squatId)
    await onSide('BODYBUILDING')
    showToday()
    const card = await screen.findByText('Ready to train?')
    expect(card).toBeInTheDocument()
    expect(screen.getByText('3 × 8–12 @ 8')).toBeInTheDocument()
    expect(screen.queryByText('Squat')).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: 'Start session' }))
    await waitFor(async () => expect((await db.sessions.where('date').equals(TODAY).first())?.mode).toBe('BODYBUILDING'))
  })

  it('hidesTheTopSetChipAndTheStallQuestion_missedStillWorks_andRestsLikeAnAccessory', async () => {
    await planOn('BODYBUILDING', squatId, { reps: 8, repsHigh: 12 })
    await onSide('BODYBUILDING')
    const id = await session(TODAY, 'BODYBUILDING')
    await logSet(db, id, set(squatId, 100, { reps: 10 }))
    await logSet(db, id, set(squatId, 90, { reps: 12 }))
    const onSetLogged = vi.fn()
    showToday({ onSetLogged })

    await screen.findByText('Set 3 of 3')
    expect(screen.queryByText('Top set')).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: 'Missed' }))
    await waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(3))
    expect((await setsOn(db, TODAY))[2]?.failed).toBe(true)
    expect(screen.queryByText('Where did it stall?')).toBeNull()
    // A squat on this side rests as an accessory does.
    expect(onSetLogged).toHaveBeenCalledWith(false)
  })

  it('anExerciseWithNoHistory_hasNoCoachCard_andCanBeLogged', async () => {
    await planOn('BODYBUILDING', curlId)
    await onSide('BODYBUILDING')
    await session(TODAY, 'BODYBUILDING')
    showToday()
    await screen.findByText('Set 1 of 3')
    // The coach pass has run and found nothing to suggest.
    await waitFor(async () => expect(await targets(db)).toEqual([]))
    expect(screen.queryByRole('button', { name: 'Apply' })).toBeNull()
    await userEvent.type(screen.getByRole('textbox', { name: 'Weight, kilograms' }), '12{Enter}')
    await userEvent.click(screen.getByRole('button', { name: /Log set 1/ }))
    await waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(1))
  })

  it('switchingOnADayAlreadyTrained_keepsTodayOnThatSession_withTheLine_andOneSession', async () => {
    await planOn('POWERLIFTING', squatId, { sets: 5 })
    await planOn('BODYBUILDING', curlId)
    const id = await session(TODAY, 'POWERLIFTING')
    await logSet(db, id, set(squatId, 140))
    await onSide('POWERLIFTING')
    showToday()
    await screen.findByText('Set 2 of 5')
    expect(screen.queryByText(/Today's session is on/)).toBeNull()

    await onSide('BODYBUILDING')
    expect(await screen.findByText("Today's session is on the powerlifting side. Bodybuilding starts with your next session.")).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Squat' })).toBeInTheDocument()
    expect(screen.queryByText('Curl')).toBeNull()
    expect(await db.sessions.where('date').equals(TODAY).count()).toBe(1)
    // Logging goes on into the same session.
    await userEvent.click(screen.getByRole('button', { name: /Log set 2/ }))
    await waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(2))
    expect(await db.sessions.count()).toBe(1)
  })

  it('theReverseLine_whenTheSessionIsOnTheBodybuildingSide', async () => {
    await planOn('BODYBUILDING', curlId)
    await session(TODAY, 'BODYBUILDING')
    await onSide('POWERLIFTING')
    showToday()
    expect(await screen.findByText("Today's session is on the bodybuilding side. Powerlifting starts with your next session.")).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Curl' })).toBeInTheDocument()
  })
})

describe('welcome back', () => {
  it('after31DaysAway_showsOnCoachAndToday_andResumeClearsIt', async () => {
    await planOn('BODYBUILDING', curlId)
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -31) })
    showCoach()
    expect(await screen.findByText('Welcome back to the better side.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'My goals have changed' })).toBeInTheDocument()
    cleanup()
    showToday()
    expect(await screen.findByText('Welcome back to the better side.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Resume my plan' }))
    await waitFor(async () => expect((await getSettings(db)).bodybuildingLeftOn).toBeNull())
    await waitFor(() => expect(screen.queryByText('Welcome back to the better side.')).toBeNull())
    // Once: it does not come back.
    cleanup()
    showCoach()
    await screen.findByText('Coach')
    await screen.findByText('Nothing open')
    expect(screen.queryByText('Welcome back to the better side.')).toBeNull()
  })

  it('myGoalsHaveChanged_clearsItAndOpensPlan', async () => {
    await planOn('POWERLIFTING', squatId)
    await onSide('POWERLIFTING', { powerliftingLeftOn: addDays(TODAY, -40), bodybuildingLeftOn: null })
    const onOpenPlan = vi.fn()
    showCoach(onOpenPlan)
    await tapWhenShown('My goals have changed')
    await waitFor(() => expect(onOpenPlan).toHaveBeenCalledOnce())
    await waitFor(async () => expect((await getSettings(db)).powerliftingLeftOn).toBeNull())
  })

  it('noCard_onFirstEntry_after30Days_orWithNoPlan', async () => {
    await planOn('BODYBUILDING', curlId)
    // First entry: never left.
    await onSide('BODYBUILDING', { bodybuildingLeftOn: null })
    showCoach()
    await screen.findByText('Nothing open')
    expect(screen.queryByText('Welcome back to the better side.')).toBeNull()
    cleanup()
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -30) })
    showCoach()
    await screen.findByText('Nothing open')
    expect(screen.queryByText('Welcome back to the better side.')).toBeNull()
    cleanup()
    await db.planItems.clear()
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -60) })
    showToday()
    await screen.findByText('Ready to train?')
    expect(screen.queryByText('Welcome back to the better side.')).toBeNull()
  })
})

const advice = (type: string, mode: TrainingMode, payload: string, reason: string): AdviceRow => ({ type, lift: type === 'STALL' ? 'BENCH' : null, createdAt: TODAY, reason, severity: 2, status: 'OPEN', payloadJson: payload, mode })
const DELOAD = '{"type":"com.powertracker.domain.model.AdvicePayload.Deload"}'
const RE_ENTRY = '{"type":"com.powertracker.domain.model.AdvicePayload.ReEntry"}'
const STALL = '{"type":"com.powertracker.domain.model.AdvicePayload.Accessories","items":[{"exerciseName":"Close-grip bench","sets":3,"repsLow":6,"repsHigh":8}]}'

describe('Coach on the bodybuilding side', () => {
  it('forSide_keepsEveryDeloadAndReEntry_andTheRestOfThatSideOnly', () => {
    const rows = [advice('DELOAD', 'POWERLIFTING', DELOAD, 'd'), advice('RE_ENTRY', 'POWERLIFTING', RE_ENTRY, 'r'), advice('STALL', 'POWERLIFTING', STALL, 's'), advice('TARGET', 'BODYBUILDING', '{}', 't'), { ...advice('STALL', 'POWERLIFTING', STALL, 'old'), mode: undefined }]
    expect(adviceForSide(rows, 'BODYBUILDING').map((a) => a.reason)).toEqual(['d', 'r', 't'])
    expect(adviceForSide(rows, 'POWERLIFTING').map((a) => a.reason)).toEqual(['d', 'r', 's', 'old'])
  })

  it('showsDeloadsAndEasingBackInWhateverTheirSide_butNotThePowerliftingStall', async () => {
    await db.advice.bulkAdd([advice('DELOAD', 'POWERLIFTING', DELOAD, 'Take a deload week.'), advice('RE_ENTRY', 'BODYBUILDING', RE_ENTRY, 'Ease back in.'), advice('STALL', 'POWERLIFTING', STALL, 'Bench has stalled.')])
    await onSide('BODYBUILDING')
    showCoach()
    expect(await screen.findByText('Take a deload week.')).toBeInTheDocument()
    expect(screen.getByText('Ease back in.')).toBeInTheDocument()
    expect(screen.queryByText('Bench has stalled.')).toBeNull()
    expect(screen.getByText('2 open suggestions')).toBeInTheDocument()
    cleanup()
    await onSide('POWERLIFTING')
    showCoach()
    expect(await screen.findByText('Bench has stalled.')).toBeInTheDocument()
    expect(screen.getByText('3 open suggestions')).toBeInTheDocument()
  })

  it('finishingABodybuildingSession_storesItsAdviceOnThatSide', async () => {
    await planOn('BODYBUILDING', dbPressId)
    await lastWeeksPress()
    await onSide('BODYBUILDING')
    const id = await session(TODAY, 'BODYBUILDING')
    // Readiness 1 on every session of the last two weeks: low readiness alone deloads on this side.
    await db.sessions.toCollection().modify({ readiness: 1 })
    await logSet(db, id, set(dbPressId, 26, { reps: 12 }))
    showToday()
    await tapWhenShown('Finish')
    await screen.findByText('Session saved')
    const stored = (await db.advice.toArray()).filter((a) => a.type !== 'TARGET')
    expect(stored.map((a) => [a.type, a.mode])).toEqual([['DELOAD', 'BODYBUILDING']])
  })
})

describe('Progress on the bodybuilding side', () => {
  it('listsThePlansExercises_withBestSets_andWhetherTheyGoUp', async () => {
    await planOn('BODYBUILDING', dbPressId)
    await planOn('BODYBUILDING', curlId, { order: 1 })
    await planOn('POWERLIFTING', squatId)
    for (const [days, reps] of [[-14, [10, 9, 8]], [-7, [11, 10, 9]]] as const) {
      const id = await session(addDays(TODAY, days), 'BODYBUILDING')
      for (const r of reps) await logSet(db, id, set(dbPressId, 26, { reps: r }))
    }
    // A powerlifting squat session: the charts belong to the other side and are not shown here.
    await logSet(db, await session(addDays(TODAY, -3), 'POWERLIFTING'), set(squatId, 140))
    await onSide('BODYBUILDING')
    const onOpenExercise = vi.fn()
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={() => {}} onOpenExercise={onOpenExercise} />)

    const press = await screen.findByRole('button', { name: /^DB press/ })
    expect(within(press).getByText('26 × 10')).toBeInTheDocument()
    expect(within(press).getByText('26 × 11')).toBeInTheDocument()
    expect(within(press).getByRole('img', { name: 'Going up' })).toBeInTheDocument()
    const curl = screen.getByRole('button', { name: /^Curl/ })
    expect(within(curl).getByText('Not logged yet')).toBeInTheDocument()
    expect(within(curl).queryByRole('img')).toBeNull()
    expect(screen.queryByText('Total')).toBeNull()
    expect(screen.queryByText(/Estimated 1RM/)).toBeNull()

    await userEvent.click(press)
    expect(onOpenExercise).toHaveBeenCalledWith(dbPressId)
  })

  it('aLevelWeek_showsTheLevelMark', async () => {
    await planOn('BODYBUILDING', dbPressId)
    for (const [days, reps] of [[-14, [11, 10, 9]], [-7, [10, 9, 9]]] as const) {
      const id = await session(addDays(TODAY, days), 'BODYBUILDING')
      for (const r of reps) await logSet(db, id, set(dbPressId, 26, { reps: r }))
    }
    await onSide('BODYBUILDING')
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={() => {}} onOpenExercise={() => {}} />)
    const press = await screen.findByRole('button', { name: /^DB press/ })
    expect(within(press).getByRole('img', { name: 'Not going up' })).toBeInTheDocument()
  })

  it('aMainLiftsRow_opensTheExerciseHistory_notTheLiftHistory', async () => {
    // Fix round ruling B: every row on this side opens the exercise history, squat included.
    await planOn('BODYBUILDING', squatId)
    await onSide('BODYBUILDING')
    const onOpenLift = vi.fn()
    const onOpenExercise = vi.fn()
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={onOpenLift} onOpenExercise={onOpenExercise} />)
    await userEvent.click(await screen.findByRole('button', { name: /^Squat/ }))
    expect(onOpenExercise).toHaveBeenCalledWith(squatId)
    expect(onOpenLift).not.toHaveBeenCalled()
  })

  it('theBestSets_areSeparatedByAMiddleDot_andTheRowSaysWhatItOpens', async () => {
    // Fix round ruling C.
    await planOn('BODYBUILDING', dbPressId)
    await planOn('BODYBUILDING', curlId, { order: 1 })
    for (const [days, reps] of [[-14, [10, 9, 8]], [-7, [11, 10, 9]]] as const) {
      const id = await session(addDays(TODAY, days), 'BODYBUILDING')
      for (const r of reps) await logSet(db, id, set(dbPressId, 26, { reps: r }))
    }
    await onSide('BODYBUILDING')
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={() => {}} onOpenExercise={() => {}} />)

    const press = await screen.findByRole('button', { name: 'DB press. 26 × 10, 26 × 11. Going up. Open history' })
    expect(press.querySelector('.bb-row-sets')?.textContent).toBe('26 × 10 · 26 × 11')
    expect(screen.getByRole('button', { name: 'Curl. Not logged yet. Open history' })).toBeInTheDocument()
  })

  it('thePowerliftingSidesRecords_ignoreBodybuildingSessions', async () => {
    const benchId = (await exerciseNamed(db, 'Bench')).id
    await logSet(db, await session(addDays(TODAY, -7), 'BODYBUILDING'), set(benchId, 80, { reps: 10 }))
    await onSide('POWERLIFTING')
    render(<ProgressScreen db={db} today={() => TODAY} onOpenLift={() => {}} onOpenExercise={() => {}} />)
    expect(await screen.findByText('No lifts logged yet')).toBeInTheDocument()
  })
})

describe('exercise history', () => {
  it('ofAMainLift_showsItsSetsFromBothSides', async () => {
    // Fix round ruling B: what a squat row on the bodybuilding Progress opens.
    await logSet(db, await session(addDays(TODAY, -14), 'POWERLIFTING'), set(squatId, 140, { reps: 5 }))
    await logSet(db, await session(addDays(TODAY, -7), 'BODYBUILDING'), set(squatId, 100, { reps: 10 }))
    await onSide('BODYBUILDING')
    render(<ExerciseHistoryScreen db={db} exerciseId={squatId} onBack={() => {}} />)
    expect(await screen.findByRole('heading', { name: 'Squat history' })).toBeInTheDocument()
    expect(screen.getByText('2 sessions')).toBeInTheDocument()
    expect(screen.getByText(/140 kg × 5/)).toBeInTheDocument()
    expect(screen.getByText(/100 kg × 10/)).toBeInTheDocument()
  })

  it('showsSetsFromBothSides', async () => {
    await logSet(db, await session(addDays(TODAY, -14), 'POWERLIFTING'), set(curlId, 15, { reps: 8 }))
    await logSet(db, await session(addDays(TODAY, -7), 'BODYBUILDING'), set(curlId, 14, { reps: 11 }))
    await onSide('POWERLIFTING')
    render(<ExerciseHistoryScreen db={db} exerciseId={curlId} onBack={() => {}} />)
    expect(await screen.findByRole('heading', { name: 'Curl history' })).toBeInTheDocument()
    expect(screen.getByText('2 sessions')).toBeInTheDocument()
    expect(screen.getByText(/14 kg × 11/)).toBeInTheDocument()
    expect(screen.getByText(/15 kg × 8/)).toBeInTheDocument()
  })
})

describe('the plan editor on the bodybuilding side (fix round ruling D)', () => {
  const openEditor = async (row: string, day = 'Wednesday') => {
    await userEvent.click(within(await screen.findByRole('listitem', { name: row })).getByRole('button', { name: 'More' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Edit' }))
    return within(await screen.findByRole('dialog', { name: `Edit ${day}` }))
  }

  it('anItemWithNoTop_opensAsARangeFromItsRepsToItsReps_andTheTopGoesUpTo30', async () => {
    await planOn('BODYBUILDING', curlId, { reps: 10, repsHigh: undefined })
    await onSide('BODYBUILDING')
    showPlan()
    const dialog = await openEditor('Curl 3 × 10 @ 8')
    expect(dialog.queryByRole('group', { name: 'Reps' })).toBeNull()
    const from = dialog.getByRole('group', { name: 'Reps from' })
    const to = dialog.getByRole('group', { name: 'Reps to' })
    expect(within(from).getByText('10')).toBeInTheDocument()
    expect(within(to).getByText('10')).toBeInTheDocument()
    for (let i = 0; i < 20; i++) await userEvent.click(within(to).getByRole('button', { name: /increase/i }))
    expect(within(to).getByText('30')).toBeInTheDocument()
    expect(within(to).getByRole('button', { name: /increase/i })).toBeDisabled()
    await userEvent.click(dialog.getByRole('button', { name: 'Save' }))
    await waitFor(async () => expect((await planFor(db, 'BODYBUILDING')).map((p) => [p.reps, p.repsHigh])).toEqual([[10, 30]]))
  })

  it('thePowerliftingEditor_keepsOneRepsStepper_upTo20', async () => {
    await planOn('POWERLIFTING', squatId, { reps: 20 })
    await onSide('POWERLIFTING')
    showPlan()
    const dialog = await openEditor('Squat 3 × 20 @ 8')
    expect(dialog.queryByRole('group', { name: 'Reps from' })).toBeNull()
    expect(within(dialog.getByRole('group', { name: 'Reps' })).getByRole('button', { name: /increase/i })).toBeDisabled()
  })
})

describe('no powerlifting wording on the bodybuilding side (fix round ruling E)', () => {
  const deload = (mode: TrainingMode): AdviceRow => ({ ...advice('DELOAD', mode, DELOAD, 'Take a deload week.') })

  it('theDeloadCard_saysAllExercisesAndLastWeights_andThePowerliftingCardIsUnchanged', async () => {
    await db.advice.add(deload('POWERLIFTING'))
    await onSide('BODYBUILDING')
    showCoach()
    expect(await screen.findByText('Deload · All exercises')).toBeInTheDocument()
    expect(screen.getByText('of your last weights')).toBeInTheDocument()
    expect(screen.queryByText(/All lifts/)).toBeNull()
    expect(screen.queryByText(/top sets/)).toBeNull()
    cleanup()
    await onSide('POWERLIFTING')
    showCoach()
    expect(await screen.findByText('Deload · All lifts')).toBeInTheDocument()
    expect(screen.getByText('of your last top sets')).toBeInTheDocument()
  })

  it('theEasingBackInCard_saysAllExercisesAndLastWeights_andThePowerliftingCardIsUnchanged', async () => {
    await db.advice.add(advice('RE_ENTRY', 'POWERLIFTING', RE_ENTRY, 'Ease back in.'))
    await onSide('BODYBUILDING')
    showCoach()
    expect(await screen.findByText('Re-entry · All exercises')).toBeInTheDocument()
    expect(screen.getByText('of your last weights, for 7 days')).toBeInTheDocument()
    expect(screen.queryByText(/All lifts/)).toBeNull()
    expect(screen.queryByText(/top sets/)).toBeNull()
    cleanup()
    await onSide('POWERLIFTING')
    showCoach()
    expect(await screen.findByText('Re-entry · All lifts')).toBeInTheDocument()
    expect(screen.getByText('of your last top sets, for 7 days')).toBeInTheDocument()
  })

  it('settings_hidesDefaultRpeStrictPressAndMainLiftRest_andNamesTheOneRest', async () => {
    await onSide('BODYBUILDING')
    render(<SettingsScreen db={db} onBack={() => {}} today={() => TODAY} />)
    await screen.findByRole('switch', { name: 'Bodybuilding mode' })
    expect(screen.queryByText('Default RPE')).toBeNull()
    expect(screen.queryByText('Planned effort for main lifts')).toBeNull()
    expect(screen.queryByRole('heading', { name: 'Progress' })).toBeNull()
    expect(screen.queryByRole('switch', { name: 'Strict press' })).toBeNull()
    expect(screen.queryByText('Main lift rest')).toBeNull()
    expect(screen.queryByText('Accessory rest')).toBeNull()
    expect(screen.getByText('Every exercise')).toBeInTheDocument()
    const rest = within(screen.getByRole('group', { name: 'Rest' }))
    const before = (await getSettings(db)).restAccessorySec
    await userEvent.click(rest.getByRole('button', { name: /increase/i }))
    await waitFor(async () => expect((await getSettings(db)).restAccessorySec).toBe(before + 15))
    const stored = await getSettings(db)
    cleanup()

    // Back on the powerlifting side every row is there again, the stored values untouched.
    await onSide('POWERLIFTING')
    render(<SettingsScreen db={db} onBack={() => {}} today={() => TODAY} />)
    await screen.findByRole('switch', { name: 'Strict press' })
    expect(screen.getByText('Default RPE')).toBeInTheDocument()
    expect(screen.getByText('Planned effort for main lifts')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Progress' })).toBeInTheDocument()
    expect(screen.getByText('Main lift rest')).toBeInTheDocument()
    expect(screen.getByText('Accessory rest')).toBeInTheDocument()
    expect(screen.getByText('Everything else')).toBeInTheDocument()
    expect(screen.queryByText('Every exercise')).toBeNull()
    const after = await getSettings(db)
    expect([after.defaultRpe, after.showStrictPress, after.restMainSec, after.restAccessorySec]).toEqual([stored.defaultRpe, stored.showStrictPress, stored.restMainSec, stored.restAccessorySec])
  })

  it('plan_hidesKnownBests_onTheBodybuildingSideOnly', async () => {
    await planOn('BODYBUILDING', curlId)
    await onSide('BODYBUILDING')
    showPlan()
    await screen.findByRole('listitem', { name: 'Curl 3 × 8–12 @ 8' })
    expect(screen.queryByRole('heading', { name: 'Known bests' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Set best' })).toBeNull()
    cleanup()
    await onSide('POWERLIFTING')
    showPlan()
    expect(await screen.findByRole('heading', { name: 'Known bests' })).toBeInTheDocument()
  })
})

describe('the welcome-back card clears everywhere at once (fix round ruling F)', () => {
  const both = () => {
    const today = showToday()
    const coach = showCoach()
    return { today: within(today.container), coach: within(coach.container) }
  }

  it('answeringItOnToday_removesItFromCoach', async () => {
    await planOn('BODYBUILDING', curlId)
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -31) })
    const { today, coach } = both()
    await coach.findByText('Welcome back to the better side.')
    await userEvent.click(await today.findByRole('button', { name: 'Resume my plan' }))
    await waitFor(() => expect(coach.queryByText('Welcome back to the better side.')).toBeNull())
    expect(today.queryByText('Welcome back to the better side.')).toBeNull()
  })

  it('answeringItOnCoach_removesItFromToday', async () => {
    await planOn('BODYBUILDING', curlId)
    await onSide('BODYBUILDING', { bodybuildingLeftOn: addDays(TODAY, -31) })
    const { today, coach } = both()
    await today.findByText('Welcome back to the better side.')
    await userEvent.click(await coach.findByRole('button', { name: 'My goals have changed' }))
    await waitFor(() => expect(today.queryByText('Welcome back to the better side.')).toBeNull())
    expect(coach.queryByText('Welcome back to the better side.')).toBeNull()
  })
})
