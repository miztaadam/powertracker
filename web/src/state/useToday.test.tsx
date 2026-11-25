import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../db/db'
import { DELOAD_ADVICE, TODAY, addDays, baseline, exerciseNamed, logSet, plan, set, setsOn, startSession, targets, todayFixture } from '../test/todayFixture'
import { useToday, type SetInput } from './useToday'

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squatId: number
let benchId: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squatId = (await exerciseNamed(db, 'Squat')).id
  benchId = (await exerciseNamed(db, 'Bench')).id
})
// Unmount before the database closes: Vitest runs this before the cleanup registered in the test setup.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const input = (weightKg: number, over: Partial<SetInput> = {}): SetInput => ({ weightKg, reps: 5, rpe: 8, failed: false, ...over })

function show(database: GritDb = db, today: () => string = () => TODAY, onSetLogged?: (main: boolean) => void) {
  return renderHook(() => useToday(database, today, onSetLogged)).result
}

async function planSquatWithHistory() {
  await baseline(db, squatId, 190, addDays(TODAY, -30))
  await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150, { rpe: 7 }))
  await plan(db, squatId)
}

/** Starts an action the way a tap does: without waiting for it. The screen is awaited instead. */
function fire(action: () => unknown) {
  act(() => {
    void action()
  })
}

/** Gives a write that should not happen every chance to happen. */
const settle = () => act(() => new Promise<void>((r) => setTimeout(r, 200)))

const targetExercise = (a: { payloadJson: string }) => (JSON.parse(a.payloadJson) as { exerciseId: number }).exerciseId

describe('useToday', () => {
  it('finishSession_insertsCoachAdvice', async () => {
    // Back after a 30-day gap: finishing today's session should store a re-entry note.
    await logSet(db, await startSession(db, addDays(TODAY, -30)), set(squatId, 150))
    await logSet(db, await startSession(db, TODAY), set(squatId, 140))
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))

    await act(() => vm.current.finishSession())

    await waitFor(() => expect(vm.current.summary).not.toBeNull())
    expect((await db.advice.toArray()).map((a) => a.type)).toEqual(['RE_ENTRY'])
    expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 1 })
  })

  it('finishedSession_staysFinished_afterAReload', async () => {
    await logSet(db, await startSession(db, TODAY), set(squatId, 140))
    const first = renderHook(() => useToday(db, () => TODAY))
    await waitFor(() => expect(first.result.current.loading).toBe(false))
    await act(() => first.result.current.finishSession())
    await waitFor(() => expect(first.result.current.summary).not.toBeNull())
    const summary = first.result.current.summary
    first.unmount()

    // Closed and opened again: still finished, with the numbers Finish showed, from the moment the screen has its data.
    const second = renderHook(() => useToday(db, () => TODAY))
    await waitFor(() => expect(second.result.current.loading).toBe(false))
    expect(second.result.current.summary).toEqual(summary)
    expect(second.result.current.sessionId).not.toBeNull()
    // And nothing is added to it from the Exercises tab, as before the reload.
    expect(await second.result.current.addToOpenSession('Hammer curl', null)).toBe('REFUSED')

    // Keep logging opens the session again, and it is still open after the next reload.
    act(() => second.result.current.dismissSummary())
    await waitFor(async () => expect(await db.meta.get('today:finished')).toBeUndefined())
    second.unmount()
    const third = show()
    await waitFor(() => expect(third.current.loading).toBe(false))
    expect(third.current.summary).toBeNull()
    expect(third.current.sessionId).not.toBeNull()
  })

  it('finishedYesterday_reloadedToday_opensOnTheNewDay', async () => {
    await logSet(db, await startSession(db, TODAY), set(squatId, 140))
    const first = renderHook(() => useToday(db, () => TODAY))
    await waitFor(() => expect(first.result.current.loading).toBe(false))
    await act(() => first.result.current.finishSession())
    await waitFor(() => expect(first.result.current.summary).not.toBeNull())
    first.unmount()

    const vm = show(db, () => addDays(TODAY, 1))
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.date).toBe(addDays(TODAY, 1))
    expect(vm.current.sessionId).toBeNull()
    expect(vm.current.summary).toBeNull()
  })

  it('finishedNote_ofASessionThatIsGone_isIgnored', async () => {
    // The note names a session by id and day: another session on that day (the log was replaced) is not finished by it.
    const sessionId = await startSession(db, TODAY)
    await db.meta.put({ key: 'today:finished', value: { sessionId: sessionId + 1, date: TODAY, summary: { setCount: 3, exerciseCount: 1, newNotes: 0 } } })
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.sessionId).toBe(sessionId)
    expect(vm.current.summary).toBeNull()
  })

  it('finishedNote_thatIsDamaged_isIgnored', async () => {
    const sessionId = await startSession(db, TODAY)
    await db.meta.put({ key: 'today:finished', value: { sessionId, date: TODAY, summary: { setCount: 'three' } } })
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.summary).toBeNull()
  })

  it('finishTappedTwice_keepsTheFirstSummary', async () => {
    await logSet(db, await startSession(db, addDays(TODAY, -30)), set(squatId, 150))
    await logSet(db, await startSession(db, TODAY), set(squatId, 140))
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))

    // A fast double tap: the second run would find the note already stored and report none.
    await act(async () => {
      await Promise.all([vm.current.finishSession(), vm.current.finishSession()])
    })
    expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 1 })

    // Nor does a tap while the summary is up replace it.
    await act(() => vm.current.finishSession())
    expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 1 })
    expect((await db.advice.toArray()).map((a) => a.type)).toEqual(['RE_ENTRY'])
  })

  it('targets_areStoredOncePerDay', async () => {
    await planSquatWithHistory()
    const first = show()
    await waitFor(() => expect(first.current.items[0]?.suggestion).toBeTruthy())
    const suggestion = first.current.items[0]!.suggestion!

    // Dismissed stays dismissed: reopening the screen must not bring the chip back.
    await act(() => first.current.dismissTarget(suggestion.id))
    await waitFor(() => expect(first.current.items[0]!.suggestion).toBeNull())
    const second = show()
    // Only after the second screen's own coach pass can a duplicate exist.
    await waitFor(() => expect(second.current.coachPasses).toBeGreaterThanOrEqual(1))
    await waitFor(() => expect(second.current.loading).toBe(false))

    expect(await targets(db)).toHaveLength(1)
    expect(second.current.items[0]!.suggestion).toBeNull()
  })

  it('addedExercise_comesAfterThePlan_andStaysOnceLogged', async () => {
    const curl = (await db.exercises.toArray()).find((e) => e.category === 'ACCESSORY')!
    await plan(db, squatId)
    await startSession(db, TODAY)
    const logged: boolean[] = []
    const vm = show(db, () => TODAY, (main) => logged.push(main))
    await waitFor(() => expect(vm.current.loading).toBe(false))

    act(() => vm.current.addExercise(curl.id!))
    expect(vm.current.items.map((i) => i.exercise.id)).toEqual([squatId, curl.id])
    expect(vm.current.items[1]!.planned).toBeNull()

    fire(() => vm.current.logSet(curl.id!, input(20)))
    await waitFor(() => expect(vm.current.items[1]!.sets).toHaveLength(1))
    // The rest timer hears about it: an accessory is not a main lift or variation.
    expect(logged).toEqual([false])

    // A fresh screen (reload) still shows it, because a set was logged.
    const restarted = show()
    await waitFor(() => expect(restarted.current.items).toHaveLength(2))
    expect(restarted.current.items[1]!.exercise.id).toBe(curl.id)
  })

  it('catalogueExercise_withoutARow_isShownUnderAStandIn_untilItsFirstSet', async () => {
    const curl = await exerciseNamed(db, 'Curl')
    await db.exercises.delete(curl.id)
    const before = await db.exercises.toArray()
    await plan(db, squatId)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.exercises.map((e) => e.id)).toEqual(before.map((e) => e.id))

    // A name the catalogue does not know adds nothing.
    let id: number | null = 0
    act(() => void (id = vm.current.addFromCatalogue('Tyre flip')))
    expect(id).toBeNull()
    expect(vm.current.items).toHaveLength(1)

    // By alias, in any case; twice is still one card.
    act(() => void (id = vm.current.addFromCatalogue('BICEPS CURL')))
    act(() => void vm.current.addFromCatalogue('Curl'))
    const standIn = id as unknown as number
    expect(standIn).toBeLessThan(0)
    expect(vm.current.items.map((i) => [i.exercise.id, i.exercise.name, i.exercise.category])).toEqual([
      [squatId, 'Squat', 'SQUAT'],
      [standIn, 'Curl', 'ACCESSORY'],
    ])
    expect(vm.current.resolved).toEqual({})

    // Its numbers are kept on screen, not in the database.
    act(() => vm.current.edit(standIn, { weightKg: 20, reps: 12, rpe: 7 }))
    expect(vm.current.items[1]!.editor).toEqual({ weightKg: 20, reps: 12, rpe: 7 })
    await settle()
    expect(await db.exercises.toArray()).toEqual(before)
    expect(await db.meta.where('key').startsWith('editor:').count()).toBe(0)

    // The first set saves the row with it; the card moves to the row's id and keeps its numbers.
    fire(() => vm.current.logSet(standIn, input(20, { reps: 12, rpe: 7 })))
    await waitFor(() => expect(vm.current.items[1]!.sets).toHaveLength(1))
    const row = await exerciseNamed(db, 'Curl')
    expect(row).toMatchObject({ isBuiltIn: true, archived: false, category: 'ACCESSORY' })
    expect(vm.current.resolved).toEqual({ [standIn]: row.id })
    expect(vm.current.items[1]!.exercise.id).toBe(row.id)
    expect(vm.current.items[1]!.editor).toEqual({ weightKg: 20, reps: 12, rpe: 7 })
    expect((await setsOn(db, TODAY)).map((s) => s.exerciseId)).toEqual([row.id])

    // A second set under the old key lands on the same row.
    fire(() => vm.current.logSet(standIn, input(20, { reps: 12, rpe: 7 })))
    await waitFor(() => expect(vm.current.items[1]!.sets).toHaveLength(2))
    expect((await setsOn(db, TODAY)).map((s) => s.exerciseId)).toEqual([row.id, row.id])
    expect((await db.exercises.toArray()).filter((e) => e.name === 'Curl')).toHaveLength(1)
  })

  it('catalogueExercise_withARow_isAddedUnderItsRow', async () => {
    const curl = await exerciseNamed(db, 'Curl')
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))

    let id: number | null = null
    act(() => void (id = vm.current.addFromCatalogue('Curl')))
    expect(id).toBe(curl.id)
    expect(vm.current.items.map((i) => i.exercise.id)).toEqual([curl.id])
    act(() => vm.current.removeExercise(curl.id))
    expect(vm.current.items).toEqual([])
  })

  it('exercisePlannedTwice_isOneCard_withOneTarget', async () => {
    await planSquatWithHistory()
    await plan(db, squatId, { sets: 2, reps: 8, targetRpe: 7, order: 1 })
    const vm = show()
    await waitFor(() => expect(vm.current.items[0]?.suggestion).toBeTruthy())

    expect(vm.current.items.map((i) => i.exercise.id)).toEqual([squatId])
    expect(vm.current.items[0]!.targetSets).toBe(5)
    expect(await targets(db)).toHaveLength(1)
  })

  it('appliedTarget_withFewerSets_changesTheSetCount', async () => {
    await planSquatWithHistory()
    // An open deload makes the coach cut the 5 planned sets to 3.
    await db.advice.add(DELOAD_ADVICE)
    const vm = show()
    await waitFor(() => expect(vm.current.items[0]?.suggestion).toBeTruthy())
    const item = vm.current.items[0]!
    const target = item.suggestion!.target
    expect(target.sets).toBe(3)
    expect(item.targetSets, 'nothing changes before Apply').toBe(5)
    expect(item.editor.weightKg).toBe(150)

    await act(() => vm.current.applyTarget(item.suggestion!.id))

    await waitFor(() => expect(vm.current.items[0]!.suggestion).toBeNull())
    expect(vm.current.items[0]!.editor.weightKg).toBe(target.weightKg)
    expect(vm.current.items[0]!.targetSets).toBe(3)
    expect((await targets(db)).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('doubleTap_savesOneSet', async () => {
    await plan(db, squatId)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))

    fire(() => vm.current.logSet(squatId, input(100)))
    fire(() => vm.current.logSet(squatId, input(100)))

    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
    await settle()
    expect(await setsOn(db, TODAY)).toHaveLength(1)
    expect((await setsOn(db, TODAY))[0]).toMatchObject({ exerciseId: squatId, weightKg: 100, isTopSet: false })

    // Once the first set is on screen the button works again.
    fire(() => vm.current.logSet(squatId, input(100)))
    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(2))
  })

  it('doubleTapOnConfirm_savesOneSet', async () => {
    await plan(db, squatId)
    await startSession(db, TODAY)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    await act(() => vm.current.logSet(squatId, input(100, { reps: 13 })))
    expect(vm.current.pending?.warnings).toEqual(['HIGH_REPS'])
    expect(await setsOn(db, TODAY)).toHaveLength(0)

    fire(() => vm.current.confirmPending())
    fire(() => vm.current.confirmPending())

    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
    await settle()
    expect(await setsOn(db, TODAY)).toHaveLength(1)
    expect(vm.current.pending).toBeNull()
  })

  it('appLeftOpenPastMidnight_doesNotStoreTargetsUnderYesterday', async () => {
    await planSquatWithHistory()
    await baseline(db, benchId, 120, addDays(TODAY, -30))
    await logSet(db, await startSession(db, addDays(TODAY, -6)), set(benchId, 100, { rpe: 7 }))
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.coachPasses).toBeGreaterThanOrEqual(1))
    expect((await targets(db)).map(targetExercise)).toEqual([squatId])

    // Midnight passes with yesterday's screen still open; then the plan for its weekday changes.
    clock = addDays(TODAY, 1)
    const passes = vm.current.coachPasses
    await act(() => plan(db, benchId, { sets: 3, order: 1 }))
    // The screen does see the new plan line; give its coach pass every chance to run.
    await waitFor(() => expect(vm.current.items.map((i) => i.exercise.id)).toEqual([squatId, benchId]))
    await settle()

    expect(vm.current.coachPasses).toBe(passes)
    const yesterdays = (await targets(db)).filter((a) => a.createdAt === TODAY)
    expect(yesterdays.map(targetExercise)).toEqual([squatId])
    expect(await targets(db)).toHaveLength(1)
  })

  it('startTappedAfterMidnight_startsTodaysSession', async () => {
    // Left in the foreground on the start card from 23:55; Start is tapped at 00:02.
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.loading).toBe(false))
    clock = addDays(TODAY, 1)

    fire(() => vm.current.startSession(82.5, null))

    // One tap: the screen moves to the new day and the session is dated that day, never the day before.
    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    expect((await db.sessions.toArray()).map((s) => [s.date, s.bodyweightKg])).toEqual([[addDays(TODAY, 1), 82.5]])
  })

  it('setAfterMidnight_inASessionStartedBefore_isSavedToThatSession', async () => {
    // Training 23:30–00:30: the session belongs to the day it was started.
    await logSet(db, await startSession(db, addDays(TODAY, -30)), set(squatId, 150))
    await plan(db, squatId)
    const sessionId = await startSession(db, TODAY)
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.sessionId).toBe(sessionId))
    clock = addDays(TODAY, 1)

    fire(() => vm.current.logSet(squatId, input(140)))

    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
    expect(vm.current.date).toBe(TODAY)
    expect((await setsOn(db, TODAY)).map((s) => [s.sessionId, s.weightKg])).toEqual([[sessionId, 140]])
    expect(await db.sessions.count()).toBe(2)

    // Finishing it works as usual; what the coach stores is dated by the real clock, never the day before.
    fire(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).toEqual({ setCount: 1, exerciseCount: 1, newNotes: 1 }))
    expect(vm.current.date).toBe(TODAY)
    expect((await db.advice.toArray()).map((a) => [a.type, a.createdAt])).toEqual([['RE_ENTRY', addDays(TODAY, 1)]])

    // Leaving the summary moves the screen to the new day.
    act(() => vm.current.dismissSummary())
    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.sessionId).toBeNull()
    expect(vm.current.summary).toBeNull()
  })

  const comeBackToTheFront = () =>
    act(() => {
      document.dispatchEvent(new Event('visibilitychange'))
      window.dispatchEvent(new Event('pageshow'))
    })

  it('refreshDay_movesToTheClocksDay_unlessASummaryIsUp', async () => {
    // What coming back to the Today tab does: the same as coming back to the front.
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.loading).toBe(false))
    clock = addDays(TODAY, 1)
    act(() => vm.current.refreshDay())
    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
    await waitFor(() => expect(vm.current.loading).toBe(false))

    // A finish summary up keeps the screen where it is.
    await logSet(db, await startSession(db, addDays(TODAY, 1)), set(squatId, 150))
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    fire(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).not.toBeNull())
    clock = addDays(TODAY, 2)
    act(() => vm.current.refreshDay())
    await settle()
    expect(vm.current.date).toBe(addDays(TODAY, 1))
    expect(vm.current.summary).not.toBeNull()
  })

  it('lockedAndUnlockedAfterMidnight_staysOnTheOpenSession', async () => {
    // Started at 23:50; the phone locks between sets and is unlocked at 00:05.
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150))
    await plan(db, squatId)
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.loading).toBe(false))
    fire(() => vm.current.startSession(null, null))
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    const sessionId = vm.current.sessionId
    clock = addDays(TODAY, 1)

    comeBackToTheFront()
    await settle()

    expect(vm.current.date).toBe(TODAY)
    expect(vm.current.sessionId).toBe(sessionId)
    // And it can be logged to.
    fire(() => vm.current.logSet(squatId, input(150)))
    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
    comeBackToTheFront()
    await settle()
    expect(vm.current.date).toBe(TODAY)

    // Finished and the summary left: now the screen moves on.
    fire(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).not.toBeNull())
    act(() => vm.current.dismissSummary())
    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
  })

  it('summaryAfterMidnight_survivesLockAndUnlock', async () => {
    await plan(db, squatId)
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.loading).toBe(false))
    fire(() => vm.current.startSession(null, null))
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    fire(() => vm.current.logSet(squatId, input(100)))
    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
    clock = addDays(TODAY, 1)
    fire(() => vm.current.finishSession())
    await waitFor(() => expect(vm.current.summary).not.toBeNull())

    // The phone locks while the summary is up.
    comeBackToTheFront()
    await settle()

    expect(vm.current.date).toBe(TODAY)
    expect(vm.current.summary).toMatchObject({ setCount: 1, exerciseCount: 1 })

    act(() => vm.current.dismissSummary())
    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
  })

  it('reloadAfterMidnight_comesBackToTheOpenSession', async () => {
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150))
    await plan(db, squatId)
    let clock = TODAY
    const first = renderHook(() => useToday(db, () => clock))
    await waitFor(() => expect(first.result.current.loading).toBe(false))
    fire(() => first.result.current.startSession(null, null))
    await waitFor(() => expect(first.result.current.sessionId).not.toBeNull())
    fire(() => first.result.current.logSet(squatId, input(150)))
    await waitFor(() => expect(first.result.current.items[0]!.sets).toHaveLength(1))
    act(() => first.result.current.edit(squatId, { weightKg: 152.5, reps: 3, rpe: 9 }))
    await waitFor(async () => expect(await db.meta.get(`editor:${TODAY}:${squatId}`)).toBeTruthy())
    first.unmount()

    // Reloaded at 00:05.
    clock = addDays(TODAY, 1)
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.loading).toBe(false))

    expect(vm.current.date).toBe(TODAY)
    expect(vm.current.sessionId).not.toBeNull()
    expect(vm.current.items[0]!.sets).toHaveLength(1)
    expect(vm.current.items[0]!.editor).toEqual({ weightKg: 152.5, reps: 3, rpe: 9 })
    // The new day's coach pass has not run behind its back.
    expect((await targets(db)).filter((a) => a.createdAt === addDays(TODAY, 1))).toEqual([])
  })

  it('sessionLeftOpenForHours_doesNotHoldTheNextDay', async () => {
    // Yesterday's session was never finished; its last activity was seven hours ago.
    await startSession(db, TODAY)
    await db.meta.put({ key: 'today:active', value: { date: TODAY, at: Date.now() - 7 * 3600_000 } })
    const vm = show(db, () => addDays(TODAY, 1))
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.date).toBe(addDays(TODAY, 1))
    expect(vm.current.sessionId).toBeNull()
  })

  it('logSet_whileTheScreenChangesDay_stillSaves', async () => {
    // The tap lands while the screen is loading, and the day is re-read before that load ends.
    await plan(db, squatId)
    await plan(db, squatId, { weekday: 4 })
    await startSession(db, TODAY)
    await startSession(db, addDays(TODAY, 1))
    let clock = TODAY
    const vm = show(db, () => clock)
    fire(() => vm.current.logSet(squatId, input(100)))
    clock = addDays(TODAY, 1)
    comeBackToTheFront()

    await waitFor(() => expect(vm.current.items[0]?.sets).toHaveLength(1))
    expect(await db.sets.count()).toBe(1)
  })

  it('logSet_withNoSession_isRefusedOutLoud', async () => {
    await plan(db, squatId)
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    await expect(vm.current.logSet(squatId, input(100))).rejects.toThrow('There is no session to log to.')
    // The guard is released: once a session exists the next tap saves.
    await startSession(db, TODAY)
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    fire(() => vm.current.logSet(squatId, input(100)))
    await waitFor(() => expect(vm.current.items[0]!.sets).toHaveLength(1))
  })

  it('logSet_beforeTheScreenHasLoaded_waitsAndSaves', async () => {
    await plan(db, squatId)
    await startSession(db, TODAY)
    const vm = show()
    expect(vm.current.loading).toBe(true)

    fire(() => vm.current.logSet(squatId, input(100)))

    await waitFor(() => expect(vm.current.items[0]?.sets).toHaveLength(1))
    expect(await setsOn(db, TODAY)).toHaveLength(1)
  })

  it('comingBackTheNextDay_showsTheNewDay', async () => {
    await startSession(db, TODAY)
    let clock = TODAY
    const vm = show(db, () => clock)
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())

    clock = addDays(TODAY, 1)
    act(() => {
      document.dispatchEvent(new Event('visibilitychange'))
    })

    await waitFor(() => expect(vm.current.date).toBe(addDays(TODAY, 1)))
    await waitFor(() => expect(vm.current.loading).toBe(false))
    expect(vm.current.sessionId).toBeNull()
  })

  it('reloadMidSession_restoresSetsAndEditorValues', async () => {
    await planSquatWithHistory()
    const first = renderHook(() => useToday(db, () => TODAY))
    await waitFor(() => expect(first.result.current.loading).toBe(false))
    await act(() => first.result.current.startSession(82.5, 4))
    await waitFor(() => expect(first.result.current.sessionId).not.toBeNull())
    fire(() => first.result.current.logSet(squatId, input(150)))
    await waitFor(() => expect(first.result.current.items[0]!.sets).toHaveLength(1))
    // The numbers for the next set are changed but not logged when the app is killed.
    act(() => first.result.current.edit(squatId, { weightKg: 152.5, reps: 3, rpe: 9 }))
    await waitFor(async () => expect(await db.meta.get(`editor:${TODAY}:${squatId}`)).toBeTruthy())
    first.unmount()
    db.close()

    const reopened = await fx.open()
    const vm = show(reopened)
    await waitFor(() => expect(vm.current.loading).toBe(false))

    expect(vm.current.sessionId).not.toBeNull()
    expect(vm.current.items[0]!.sets.map((s) => [s.weightKg, s.reps, s.rpe])).toEqual([[150, 5, 8]])
    expect(vm.current.items[0]!.editor).toEqual({ weightKg: 152.5, reps: 3, rpe: 9 })
    expect(await reopened.sessions.where('date').equals(TODAY).first()).toMatchObject({ bodyweightKg: 82.5, readiness: 4 })
  })

  it('editorValues_fromEarlierDays_areCleanedUp', async () => {
    await db.meta.put({ key: `editor:${addDays(TODAY, -1)}:${squatId}`, value: { weightKg: 100, reps: 5, rpe: 8 } })
    await db.meta.put({ key: 'other', value: 1 })
    const vm = show()
    await waitFor(() => expect(vm.current.coachPasses).toBeGreaterThanOrEqual(1))
    expect((await db.meta.toCollection().primaryKeys()).sort()).toEqual(['other'])
  })

  it('startSession_twice_makesOneSession', async () => {
    const vm = show()
    await waitFor(() => expect(vm.current.loading).toBe(false))
    await act(async () => {
      await Promise.all([vm.current.startSession(null, null), vm.current.startSession(null, null)])
    })
    await waitFor(() => expect(vm.current.sessionId).not.toBeNull())
    expect(await db.sessions.count()).toBe(1)
  })
})
