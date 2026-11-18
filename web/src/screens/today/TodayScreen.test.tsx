import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { resolveCatalogue } from '../../db/catalogue'
import type { GritDb } from '../../db/db'
import { OWN_NAME, expectCustomExerciseEmpty, expectCustomExerciseKept, expectSaveFailedInTheForm, fillCustomExercise, openCreateForm, tapCreate } from '../../test/customExerciseChecks'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
import { DELOAD_ADVICE, TODAY, addDays, baseline, exerciseNamed, logSet, plan, set, setsOn, startSession, targets, todayFixture } from '../../test/todayFixture'
import { setTapHaptics } from '../../platform/haptics'
import { grit } from '../../domain'
import { TodayScreen } from './TodayScreen'

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squatId: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squatId = (await exerciseNamed(db, 'Squat')).id
  // Planned today: squat 5 × 5 @ 8. Last week's top set: 150 × 5 @ 7.
  await plan(db, squatId)
  await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150, { rpe: 7 }))
})
// Unmount before the database closes: Vitest runs this before the cleanup registered in the test setup.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

function show(database: GritDb = db, props: { onOpenCoach?: () => void; onSetLogged?: (main: boolean) => void } = {}) {
  return render(<TodayScreen db={database} today={() => TODAY} onOpenCoach={props.onOpenCoach ?? (() => {})} onSetLogged={props.onSetLogged} />)
}

const button = (name: string | RegExp) => screen.getByRole('button', { name })
const tap = (name: string | RegExp) => userEvent.click(button(name))
const findButton = (name: string | RegExp) => screen.findByRole('button', { name })
const tapWhenShown = async (name: string | RegExp) => userEvent.click(await findButton(name))
const waitForSets = (count: number) => waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(count))

const exerciseRows = async (name: string) => (await db.exercises.toArray()).filter((e) => e.name.toLowerCase() === name.toLowerCase())
const picker = () => within(screen.getByRole('dialog', { name: 'Add exercise' }))
/** The part of a picker row that picks the exercise, once the row is drawn. */
const pickerRow = async (name: string) => within(await picker().findByRole('listitem', { name })).getAllByRole('button')[0]!
const openPicker = () => tapWhenShown('Add exercise')

const weightField = () => screen.getByRole('textbox', { name: 'Weight, kilograms' })
const repsField = () => screen.getByRole('textbox', { name: 'Reps' })
const rpeField = () => screen.getByRole('textbox', { name: 'RPE' })

/** Types a weight into the exercise card and leaves the field. */
async function typeWeight(text: string) {
  await userEvent.clear(weightField())
  await userEvent.type(weightField(), text)
  await userEvent.keyboard('{Enter}')
}

/** Fills in the bodyweight dialog. */
async function typeNumber(text: string) {
  const field = within(screen.getByRole('dialog')).getByRole('textbox')
  await userEvent.clear(field)
  await userEvent.type(field, text)
  await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Set' }))
}

describe('TodayScreen', () => {
  it('tapSet_logsAsPrescribed', async () => {
    const onSetLogged = vi.fn()
    show(db, { onSetLogged })
    expect(screen.getByRole('heading', { level: 1, name: 'Today' })).toBeInTheDocument()
    expect(await screen.findByText('Wednesday 11 November')).toBeInTheDocument()
    expect(await screen.findByText('Wednesday · 1 exercise')).toBeInTheDocument()
    await userEvent.click(await findButton('Start session'))

    await findButton('Log set 1')
    expect(weightField()).toHaveValue('150')
    await tap('Log set 1')

    await waitForSets(1)
    // Planned 5 reps @ 8 at last week's top-set weight.
    expect(await setsOn(db, TODAY)).toMatchObject([{ exerciseId: squatId, weightKg: 150, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: false }])
    await findButton('Log set 2')
    expect(button('Set 1: 150 kg × 5 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('Set 2 of 5')).toBeInTheDocument()
    expect(onSetLogged).toHaveBeenCalledExactlyOnceWith(true)
  })

  it('failedSet_showsMissRow', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')

    // RPE 8 -> 9: a hard set, but no question yet.
    await tap('Increase RPE')
    await tap('Increase RPE')
    await tap('Log set 1')
    await findButton('Log set 2')
    expect(screen.queryByTestId('miss-row-1')).toBeNull()

    // RPE 9.5: the grind is worth asking about.
    await tap('Increase RPE')
    await tap('Log set 2')
    await findButton('Log set 3')
    expect(within(screen.getByTestId('miss-row-2')).getByText('Where was it slow?')).toBeInTheDocument()

    // A missed rep always asks, with Floor / Mid / Lockout.
    for (let i = 0; i < 3; i++) await tap('Decrease RPE')
    await tap('Missed')
    await findButton('Log set 4')
    const row = within(screen.getByTestId('miss-row-3'))
    expect(row.getByText('Where did it stall?')).toBeInTheDocument()
    for (const point of ['Floor', 'Mid', 'Lockout']) expect(row.getByRole('radio', { name: point })).toBeInTheDocument()
    expect((await setsOn(db, TODAY)).map((s) => s.failed)).toEqual([false, false, true])

    await userEvent.click(row.getByRole('radio', { name: 'Lockout' }))
    await waitFor(async () => expect((await setsOn(db, TODAY))[2]!.missPoint).toBe('LOCKOUT'))
    await waitFor(() => expect(within(screen.getByTestId('miss-row-3')).getByRole('radio', { name: 'Lockout' })).toBeChecked())
    // Tapping the selected point clears it.
    await userEvent.click(within(screen.getByTestId('miss-row-3')).getByRole('radio', { name: 'Lockout' }))
    await waitFor(async () => expect((await setsOn(db, TODAY))[2]!.missPoint).toBeNull())
  })

  it('coachChip_changesNothingUntilApplied', async () => {
    // A known best gives the coach enough history to suggest a target: 150 @ 7 -> 155 × 5 @ 8.
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    await startSession(db, TODAY)
    show()

    expect(await screen.findByText('155 × 5 @ 8')).toBeInTheDocument()
    expect(screen.getByText('Coach suggests')).toBeInTheDocument()
    // Still the planned numbers: nothing changes until Apply.
    expect(weightField()).toHaveValue('150')

    await tap('Apply')

    await waitFor(() => expect(weightField()).toHaveValue('155'))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Apply' })).toBeNull())
    expect((await targets(db)).map((a) => a.status)).toEqual(['APPLIED'])
    expect(await setsOn(db, TODAY), 'applying a suggestion must not log anything').toEqual([])
  })

  it('typo_promptsBeforeSaving', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')

    for (let i = 0; i < 8; i++) await tap('Increase Reps') // 5 -> 13
    await tap('Log set 1')

    const dialog = await screen.findByRole('dialog', { name: 'Did you mean 150 kg × 13?' })
    expect(within(dialog).getByText('13 reps is a lot for one set.')).toBeInTheDocument()
    expect(within(dialog).getByText('Nothing is saved yet.')).toBeInTheDocument()
    expect(await setsOn(db, TODAY), 'nothing is saved before Confirm').toEqual([])

    // Edit goes back without saving.
    await tap('Edit')
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(await setsOn(db, TODAY)).toEqual([])

    await tap('Log set 1')
    await screen.findByRole('dialog')
    await tap('Confirm')

    await waitForSets(1)
    expect((await setsOn(db, TODAY))[0]!.reps).toBe(13)
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
  })

  it('bigJump_promptsWithTheLastWeight', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    await typeWeight('250')
    await tap('Log set 1')
    const dialog = await screen.findByRole('dialog', { name: 'Did you mean 250 kg × 5?' })
    expect(within(dialog).getByText('That is a big jump from 150 kg.')).toBeInTheDocument()
  })

  it('plateStrip_shows', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    expect(screen.getByText('25 + 25 + 15')).toBeInTheDocument() // 150 kg

    await typeWeight('182,5')

    expect(await screen.findByText('25 + 25 + 25 + 5 + 1.25')).toBeInTheDocument()

    // 181 kg cannot be loaded with 1.25 kg plates: say so instead of drawing a wrong bar.
    await typeWeight('181')
    expect(await screen.findByText("Can't load 181 kg exactly")).toBeInTheDocument()
    expect(screen.getByText('Nearest is 180 kg.')).toBeInTheDocument()
  })

  it('bodyweightEntry_rejectsWhatIsOutOfRange', async () => {
    show()
    await tapWhenShown('Add')
    const dialog = within(screen.getByRole('dialog'))
    expect(dialog.getByRole('button', { name: 'Set' })).toBeDisabled()
    await userEvent.type(dialog.getByRole('textbox'), '1001')
    expect(dialog.getByRole('button', { name: 'Set' })).toBeDisabled()
    expect(dialog.getByRole('textbox')).toBeInvalid()
    await userEvent.click(dialog.getByRole('button', { name: 'Cancel' }))
    expect(button('Add')).toBeInTheDocument()
  })

  it('appliedDeloadTarget_showsSetOfFewerSets', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    await db.advice.add(DELOAD_ADVICE)
    await startSession(db, TODAY)
    show()

    expect(await screen.findByText(/· 3 sets$/)).toBeInTheDocument()
    expect(screen.getByText('Set 1 of 5')).toBeInTheDocument()

    await tap('Apply')

    expect(await screen.findByText('Set 1 of 3')).toBeInTheDocument()
  })

  it('reloadMidSession_restoresSetsAndEditorValues', async () => {
    const first = show()
    await userEvent.click(await findButton('Start session'))
    await tapWhenShown('Log set 1')
    await findButton('Log set 2')
    await tap('Increase Weight') // 152.5
    await tap('Decrease Reps') // 4
    await tap('Increase RPE') // 8.5
    await waitFor(async () => expect(await db.meta.get(`editor:${TODAY}:${squatId}`)).toMatchObject({ value: { weightKg: 152.5, reps: 4, rpe: 8.5 } }))
    first.unmount()
    db.close()

    db = await fx.open()
    show()

    // Straight back in the session: no start card, the logged set and the unlogged numbers are there.
    await findButton('Log set 2')
    expect(button('Set 1: 150 kg × 5 at RPE 8, done')).toBeInTheDocument()
    expect(weightField()).toHaveValue('152.5')
    expect(repsField()).toHaveValue('4')
    expect(rpeField()).toHaveValue('8.5')
    expect(screen.queryByRole('button', { name: 'Start session' })).toBeNull()
  })

  it('deleteSet_asksFirst', async () => {
    await logSet(db, await startSession(db, TODAY), set(squatId, 150))
    show()
    await tapWhenShown('Set 1: 150 kg × 5 at RPE 8, done')
    const dialog = within(screen.getByRole('dialog', { name: 'Set 1' }))
    expect(dialog.getByText('150 × 5 @ 8')).toBeInTheDocument()
    await userEvent.click(dialog.getByRole('button', { name: 'Keep' }))
    expect(await setsOn(db, TODAY)).toHaveLength(1)

    await tap('Set 1: 150 kg × 5 at RPE 8, done')
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete set' }))
    await waitForSets(0)
    await findButton('Log set 1')
  })

  it('failedWrite_showsMessageAndKeepsTheNumbers', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    await logSet(db, await startSession(db, TODAY), set(squatId, 150))
    show()
    await findButton('Log set 2')
    const unhandled = vi.fn()
    process.on('unhandledRejection', unhandled)
    try {
      // Logging a set.
      const add = vi.spyOn(db.sets, 'add').mockRejectedValue(new Error('QuotaExceededError'))
      await tap('Log set 2')
      expect(await screen.findByRole('status')).toHaveTextContent("Couldn't save — try again.")
      expect(button('Log set 2')).toBeEnabled()
      expect(weightField()).toHaveValue('150')
      add.mockRestore()

      // Deleting a set, then applying and dismissing the coach's target.
      const failsToo = async (failing: { mockRestore: () => void }, action: () => Promise<void>) => {
        await action()
        await waitFor(() => expect(failing).toHaveBeenCalled())
        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent("Couldn't save — try again."))
        failing.mockRestore()
      }
      await failsToo(vi.spyOn(db.sets, 'delete').mockRejectedValue(new Error('QuotaExceededError')), async () => {
        await tap('Set 1: 150 kg × 5 at RPE 8, done')
        await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete set' }))
      })
      await failsToo(vi.spyOn(db.advice, 'update').mockRejectedValue(new Error('QuotaExceededError')), () => tap('Apply'))
      await failsToo(vi.spyOn(db.advice, 'update').mockRejectedValue(new Error('QuotaExceededError')), () => tap('Dismiss'))
      expect(await setsOn(db, TODAY)).toHaveLength(1)
      expect(button('Apply')).toBeInTheDocument()
      await new Promise((r) => setTimeout(r, 50))
      expect(unhandled).not.toHaveBeenCalled()
    } finally {
      process.off('unhandledRejection', unhandled)
    }
  })

  it('failedMissPointWrite_showsMessage', async () => {
    await logSet(db, await startSession(db, TODAY), { ...set(squatId, 150), failed: true })
    show()
    await findButton('Log set 2')
    const unhandled = vi.fn()
    process.on('unhandledRejection', unhandled)
    const update = vi.spyOn(db.sets, 'update').mockRejectedValue(new Error('QuotaExceededError'))
    try {
      await userEvent.click(within(screen.getByTestId('miss-row-1')).getByRole('radio', { name: 'Lockout' }))
      await waitFor(() => expect(update).toHaveBeenCalled())
      expect(await screen.findByRole('status')).toHaveTextContent("Couldn't save — try again.")
      expect((await setsOn(db, TODAY))[0]!.missPoint).toBeNull()
      await new Promise((r) => setTimeout(r, 50))
      expect(unhandled).not.toHaveBeenCalled()
    } finally {
      update.mockRestore()
      process.off('unhandledRejection', unhandled)
    }
  })

  it('finish_inTheHeaderAndOnTheLastCard_showsTheSummary', async () => {
    const onOpenCoach = vi.fn()
    await db.planItems.clear()
    await plan(db, squatId, { sets: 1 })
    const bench = await exerciseNamed(db, 'Bench')
    await plan(db, bench.id, { sets: 1, order: 1 })
    await startSession(db, TODAY)
    show(db, { onOpenCoach })

    // Squat is being logged; bench is folded to one line.
    await findButton('Log set 1')
    expect(screen.getByText('Exercise 1 of 2')).toBeInTheDocument()
    expect(button(/^Bench/)).toHaveTextContent('1 × 5 @ 8')
    await tap('Log set 1')

    // Its sets are done: the big button leads to the next exercise.
    await tapWhenShown('Next: Bench')
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(button(/^Squat/)).toHaveTextContent('Done')
    await typeWeight('100')
    await tapWhenShown('Log set 1')

    // Nothing left: the last card offers Finish, as the header does.
    await findButton('Finish session')
    expect(button('Finish')).toBeInTheDocument()
    await tap('Finish session')

    expect(await screen.findByRole('heading', { name: 'Session saved' })).toBeInTheDocument()
    expect(screen.getByText('No new coach notes')).toBeInTheDocument()
    expect(screen.getByText('Sets').closest('.gr-stat')).toHaveTextContent('2')
    expect(screen.queryByRole('button', { name: 'Finish' })).toBeNull()
    await tap('Open Coach')
    expect(onOpenCoach).toHaveBeenCalledOnce()
    await tap('Keep logging')
    await findButton('Add another set')
  })

  it('addExercise_picksFromTheLibrary_andCanBeRemoved', async () => {
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    expect(picker().getByText('No exercises match. Try fewer filters, or create your own.')).toBeInTheDocument()
    await userEvent.clear(picker().getByRole('textbox'))
    await userEvent.type(picker().getByRole('textbox'), 'dead')
    await userEvent.click(await pickerRow('Deadlift'))

    // The added exercise is the one being logged, with no weight yet.
    expect(await screen.findByRole('heading', { name: 'Deadlift' })).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.getByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(screen.getByText('No weight yet')).toBeInTheDocument()
    expect(screen.getByText('Type the weight to see the plates.')).toBeInTheDocument()
    expect(weightField()).toHaveValue('')
    expect(button('Log set 1')).toBeDisabled()
    expect(button('Missed')).toBeDisabled()
    await tap('Remove')
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Deadlift' })).toBeNull())

    // Picking one already in the session just opens its card.
    await openPicker()
    await userEvent.click(await pickerRow('Squat'))
    expect(await screen.findByText('Exercise 1 of 1')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Squat' })).toBeInTheDocument()
  })

  // ---- The exercise library: the picker and the info button ----

  it('infoButton_opensGuide_fromEveryPlace', async () => {
    // Squat is the open card; bench is a second planned exercise, folded to a row.
    await plan(db, (await exerciseNamed(db, 'Bench')).id, { sets: 3, order: 1 })
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    expect(screen.getByText('Exercise 1 of 2')).toBeInTheDocument()

    await checkInfoButtonOpensGuide('Squat')
    await checkInfoButtonOpensGuide('Bench')

    // Reading the row's guide did not open its card, and nothing was logged.
    expect(screen.getByText('Exercise 1 of 2')).toBeInTheDocument()
    expect(button('Log set 1')).toBeInTheDocument()
    expect(await setsOn(db, TODAY)).toEqual([])

    // In the picker too, without adding the exercise to the session.
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'Curl')
    await checkInfoButtonOpensGuide('Curl')
    expect(picker().getByRole('textbox', { name: 'Search exercises' })).toBeInTheDocument()
    await userEvent.click(picker().getByRole('button', { name: 'Close' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.getByText('Exercise 1 of 2')).toBeInTheDocument()
    expect(screen.queryByText('Curl')).toBeNull()
  })

  it('pickedLibraryExercise_getsItsRowWithItsFirstSet', async () => {
    // A library exercise that has never been used has no row.
    for (const row of await exerciseRows('Curl')) await db.exercises.delete(row.id!)
    const before = await db.exercises.toArray()
    await startSession(db, TODAY)
    show()

    await openPicker()
    await userEvent.click(await pickerRow('Curl'))
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Curl' })).toBeInTheDocument()
    // On the card, not in the data: taking it back leaves nothing behind.
    expect(await db.exercises.toArray()).toEqual(before)
    await tap('Remove')
    expect(await screen.findByText('Exercise 1 of 1')).toBeInTheDocument()
    expect(await db.exercises.toArray()).toEqual(before)

    await openPicker()
    await userEvent.click(await pickerRow('Curl'))
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    await typeWeight('20')
    await tap('Log set 1')
    await waitForSets(1)

    // One built-in row, saved with the set that needed it; the card stays open on it for set 2.
    const curls = await exerciseRows('Curl')
    expect(curls).toHaveLength(1)
    expect(curls[0]!.isBuiltIn).toBe(true)
    expect(await db.exercises.count()).toBe(before.length + 1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ exerciseId: curls[0]!.id, weightKg: 20, reps: 5, rpe: 8, failed: false, missPoint: null, isTopSet: false }])
    await findButton('Log set 2')
    expect(screen.getByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Curl' })).toBeInTheDocument()
    expect(weightField()).toHaveValue('20')

    await typeWeight('22.5')
    await tap('Log set 2')
    await waitForSets(2)
    expect((await setsOn(db, TODAY)).map((s) => s.exerciseId)).toEqual([curls[0]!.id, curls[0]!.id])
    expect(await exerciseRows('Curl')).toHaveLength(1)
    // The stand-in the card was shown under is never written, in the sets or beside them.
    expect((await db.sets.toArray()).every((s) => s.exerciseId > 0)).toBe(true)
    expect((await db.meta.toArray()).filter((row) => row.key.includes(':-'))).toEqual([])
  })

  it('pickedLibraryExercise_thatIsArchived_isRestoredWithItsFirstSet', async () => {
    const [curl] = await exerciseRows('Curl')
    await db.exercises.update(curl!.id!, { archived: true })
    await startSession(db, TODAY)
    show()

    await openPicker()
    await userEvent.click(await pickerRow('Curl'))
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    expect((await exerciseRows('Curl'))[0]!.archived).toBe(true)

    await typeWeight('20')
    await tap('Log set 1')
    await waitForSets(1)

    expect(await exerciseRows('Curl')).toEqual([{ ...curl, archived: false }])
    expect((await setsOn(db, TODAY))[0]!.exerciseId).toBe(curl!.id)
  })

  it('failedFirstSet_ofALibraryExercise_leavesNoRow', async () => {
    for (const row of await exerciseRows('Curl')) await db.exercises.delete(row.id!)
    const before = await db.exercises.toArray()
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.click(await pickerRow('Curl'))
    await screen.findByText('Exercise 2 of 2')
    await typeWeight('20')
    vi.spyOn(db.sets, 'add').mockRejectedValue(new Error('disk full'))
    await tap('Log set 1')

    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    expect(await db.exercises.toArray()).toEqual(before)
    // The card is still there to try again.
    expect(screen.getByRole('heading', { name: 'Curl' })).toBeInTheDocument()
  })

  it('createYourOwn_fromTodaysPicker_addsItToTheSession_withNoGuide', async () => {
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'Tyre flip')
    expect(picker().getByText('No exercises match. Try fewer filters, or create your own.')).toBeInTheDocument()

    await userEvent.click(picker().getByRole('button', { name: 'Create your own' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Add custom exercise' })).getByRole('button', { name: 'Create' }))
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Tyre flip' })).toBeInTheDocument()

    const sled = await exerciseRows('Tyre flip')
    expect(sled).toHaveLength(1)
    expect(sled[0]!.isBuiltIn).toBe(false)
    await checkInfoButtonOpensGuide('Tyre flip')
  })

  it('createYourOwn_whenTheSaveFails_formKeepsEverything_saysSoInIt_noCard_noTick', async () => {
    const vibrate = vi.fn(() => true)
    Object.defineProperty(navigator, 'vibrate', { configurable: true, writable: true, value: vibrate })
    setTapHaptics(true)
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    vi.spyOn(db.exercises, 'add').mockRejectedValueOnce(new Error('disk full'))
    await openCreateForm()
    await fillCustomExercise()
    await tapCreate()
    await expectSaveFailedInTheForm()
    expectCustomExerciseKept()
    expect(screen.queryByText('Exercise 2 of 2')).toBeNull()
    expect(await exerciseRows(OWN_NAME)).toEqual([])
    expect(vibrate).not.toHaveBeenCalled()

    // Once the disk has room, Create works again: stored as typed, added to the session, and the picker closes.
    await tapCreate()
    expect(await screen.findByText('Exercise 2 of 2')).toBeInTheDocument()
    expect(screen.queryByRole('dialog', { name: 'Add exercise' })).toBeNull()
    expect(await exerciseRows(OWN_NAME)).toEqual([expect.objectContaining({ category: 'VARIATION', parentLift: 'BENCH', muscles: ['Lats', 'Grip'] })])

    // A successful create carries nothing over to the next one.
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    await openCreateForm()
    expectCustomExerciseEmpty('zzz')
    setTapHaptics(false)
    Reflect.deleteProperty(navigator, 'vibrate')
  })

  it('createCustom_withCatalogueName_usesTheCatalogueEntry', async () => {
    for (const row of await exerciseRows('Curl')) await db.exercises.delete(row.id!)
    const before = await db.exercises.toArray()
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.click(await picker().findByRole('button', { name: 'Create your own' }))
    const custom = within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    await userEvent.type(custom.getByRole('textbox', { name: 'Name' }), 'biceps curl')
    await userEvent.click(custom.getByRole('button', { name: 'Create' }))

    // The catalogue's exercise, under its own name and with its guide; no row of the lifter's own.
    expect(await screen.findByRole('heading', { name: 'Curl' })).toBeInTheDocument()
    expect(await db.exercises.toArray()).toEqual(before)
    await checkInfoButtonOpensGuide('Curl')
    await typeWeight('20')
    await tap('Log set 1')
    await waitForSets(1)
    const curls = await exerciseRows('Curl')
    expect(curls).toHaveLength(1)
    expect(curls[0]!.isBuiltIn).toBe(true)
    expect(await exerciseRows('biceps curl')).toEqual([])
  })

  it('startCard_takesBodyweightAndReadiness', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    show()
    expect(await screen.findByText('Coach suggests 155 × 5 @ 8')).toBeInTheDocument()
    expect(screen.getByText('5 × 5 @ 8')).toBeInTheDocument()
    await tap('Add')
    await typeNumber('82.5')
    expect(button('82.5 kg')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('checkbox', { name: '4' }))
    await tap('Start session')
    await findButton('Log set 1')
    expect(await db.sessions.where('date').equals(TODAY).first()).toMatchObject({ bodyweightKg: 82.5, readiness: 4 })
  })

  it('nothingPlanned_saysSoAndStillStarts', async () => {
    await db.planItems.clear()
    show()
    expect(await screen.findByText('Nothing planned')).toBeInTheDocument()
    expect(screen.getByText('There is nothing in your plan for Wednesday. You can still start a session and add exercises as you go.')).toBeInTheDocument()
    await tap('Start session')
    expect(await screen.findByText('No exercises yet')).toBeInTheDocument()
    expect(screen.getByText('Add what you are training today and log it set by set.')).toBeInTheDocument()
  })

  // ---- Strict press is a main lift on Today (strict press spec §5.1) ----

  /** Strict press replaces the squat as today's plan: 3 × 5 @ 8, with no history, so no weight yet. */
  async function strictPressDay() {
    await db.planItems.clear()
    await plan(db, await resolveCatalogue(db, 'Strict press'), { sets: 3 })
  }

  it('today_strictPressMissLabel', async () => {
    await strictPressDay()
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')

    await typeWeight('60')
    await tap('Missed')
    await findButton('Log set 2')

    // A press stalls off the shoulders, not off the floor.
    const row = within(screen.getByTestId('miss-row-1'))
    expect(row.getAllByRole('radio').map((r) => r.parentElement?.textContent)).toEqual(['Off the shoulders', 'Mid', 'Lockout'])
    expect(row.queryByText('Floor')).toBeNull()

    // It is the same stored point as the other lifts' "Floor".
    await userEvent.click(row.getByRole('radio', { name: 'Off the shoulders' }))
    await waitFor(async () => expect((await setsOn(db, TODAY))[0]!.missPoint).toBe('FLOOR'))
    await waitFor(() => expect(within(screen.getByTestId('miss-row-1')).getByRole('radio', { name: 'Off the shoulders' })).toBeChecked())
  })

  it('today_strictPress_getsThePlateStripAndTheMainLiftRest', async () => {
    await strictPressDay()
    await startSession(db, TODAY)
    const rests: boolean[] = []
    show(db, { onSetLogged: (main) => rests.push(main) })
    await findButton('Log set 1')

    // The plate strip treats it as a barbell lift: only those are told a weight is under the bar.
    await typeWeight('15')
    expect(await screen.findByText('Lighter than the bar')).toBeInTheDocument()
    await typeWeight('62.5')
    expect(await screen.findByText('20 + 1.25')).toBeInTheDocument()

    await tap('Log set 1')
    await waitForSets(1)
    // A strict press set starts the main-lift rest.
    await waitFor(() => expect(rests).toEqual([true]))
  })
})

// ---- Stage 2: the buzz on the main actions (platform/haptics.ts), with a fake vibrator ----
describe('TodayScreen haptics', () => {
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

  it('logSet_ticksOnce', async () => {
    await startSession(db, TODAY)
    show()
    await tapWhenShown('Log set 1')
    await waitForSets(1)
    await findButton('Log set 2')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('missed_ticksOnce', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    await tap('Missed')
    await waitForSets(1)
    await findButton('Log set 2')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('stepperStep_ticksOncePerTap', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    await tap('Increase Weight')
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('typo_ticksOnlyWhenTheSetIsSaved', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    await typeWeight('250')
    await tap('Log set 1')
    await screen.findByRole('dialog', { name: 'Did you mean 250 kg × 5?' })
    expect(vibrate, 'the warning saves nothing, so it does not buzz').not.toHaveBeenCalled()
    await tap('Confirm')
    await waitForSets(1)
    // 250 × 5 beats last week's 150 × 5 @ 7: a new best, so the stronger buzz (round B).
    expect(vibrate).toHaveBeenCalledExactlyOnceWith([30, 40, 60])
  })

  it('pickerAdd_ticksOnce', async () => {
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'dead')
    await userEvent.click(await pickerRow('Deadlift'))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Add exercise' })).toBeNull())
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('coachSuggestionApply_ticksOnce', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    await startSession(db, TODAY)
    show()
    expect(await screen.findByText('155 × 5 @ 8')).toBeInTheDocument()
    await tap('Apply')
    await waitFor(() => expect(weightField()).toHaveValue('155'))
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('newBest_buzzesTheBestPattern_once_thenTheEqualSetAndAMissTick', async () => {
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    // Last week's best is 150 × 5 @ 7; 180 × 5 @ 8 beats it.
    await typeWeight('180')
    await tap('Log set 1')
    await waitForSets(1)
    await findButton('Log set 2')
    expect(vibrate.mock.calls).toEqual([[[30, 40, 60]]])
    await tap('Log set 2')
    await waitForSets(2)
    await findButton('Log set 3')
    await typeWeight('190')
    await tap('Missed')
    await waitForSets(3)
    expect(vibrate.mock.calls).toEqual([[[30, 40, 60]], [10], [10]])
  })

  it('coachSuggestionApply_doubleTap_appliesAndTicksOnce', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    await startSession(db, TODAY)
    show()
    expect(await screen.findByText('155 × 5 @ 8')).toBeInTheDocument()
    const apply = button('Apply')
    fireEvent.click(apply)
    fireEvent.click(apply)
    await waitFor(() => expect(weightField()).toHaveValue('155'))
    await waitFor(async () => expect((await db.advice.toArray()).filter((a) => a.status === 'APPLIED')).toHaveLength(1))
    expect(vibrate).toHaveBeenCalledExactlyOnceWith(10)
  })

  it('aFailingBestCheck_stillSavesTheSet_andTicks', async () => {
    const check = vi.spyOn(grit, 'isNewPr').mockImplementation(() => {
      throw new Error('history gone')
    })
    try {
      await startSession(db, TODAY)
      show()
      await findButton('Log set 1')
      await typeWeight('180') // would be a new best
      await tap('Log set 1')
      await waitForSets(1)
      await waitFor(() => expect(vibrate).toHaveBeenCalledExactlyOnceWith(10))
      expect(check).toHaveBeenCalledOnce()
    } finally {
      check.mockRestore()
    }
  })

  it('withNothingToBuzz_theBestCheckIsSkipped', async () => {
    setTapHaptics(false)
    const check = vi.spyOn(grit, 'isNewPr')
    try {
      await startSession(db, TODAY)
      show()
      await tapWhenShown('Log set 1')
      await waitForSets(1)
      await findButton('Log set 2')
      expect(check).not.toHaveBeenCalled()
    } finally {
      check.mockRestore()
    }
  })

  it('pickingAnExerciseAlreadyInTheSession_doesNotBuzz', async () => {
    await startSession(db, TODAY)
    show()
    await openPicker()
    await userEvent.type(picker().getByRole('textbox', { name: 'Search exercises' }), 'squat')
    await userEvent.click(await pickerRow('Squat'))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Add exercise' })).toBeNull())
    expect(vibrate).not.toHaveBeenCalled()
  })

  it('setting off_nothingBuzzes', async () => {
    setTapHaptics(false)
    await startSession(db, TODAY)
    show()
    await findButton('Log set 1')
    await tap('Increase Weight')
    await tap('Log set 1')
    await waitForSets(1)
    expect(vibrate).not.toHaveBeenCalled()
  })
})
