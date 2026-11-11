import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { GritDb } from '../../db/db'
import { DELOAD_ADVICE, TODAY, addDays, baseline, exerciseNamed, logSet, plan, set, setsOn, startSession, targets, todayFixture } from '../../test/todayFixture'
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
    await tapWhenShown('Add exercise')
    const dialog = within(screen.getByRole('dialog', { name: 'Add exercise' }))
    // Squat is already in the session.
    expect(dialog.queryByRole('button', { name: 'Squat' })).toBeNull()
    await userEvent.type(dialog.getByRole('textbox', { name: 'Search exercises' }), 'zzz')
    expect(dialog.getByText('No exercise matches. Custom exercises are created on the Plan tab.')).toBeInTheDocument()
    await userEvent.clear(dialog.getByRole('textbox'))
    await userEvent.type(dialog.getByRole('textbox'), 'dead')
    await userEvent.click(dialog.getByRole('button', { name: 'Deadlift' }))

    // The added exercise is the one being logged, with no weight yet.
    expect(await screen.findByRole('heading', { name: 'Deadlift' })).toBeInTheDocument()
    expect(screen.getByText('No weight yet')).toBeInTheDocument()
    expect(screen.getByText('Type the weight to see the plates.')).toBeInTheDocument()
    expect(weightField()).toHaveValue('')
    expect(button('Log set 1')).toBeDisabled()
    expect(button('Missed')).toBeDisabled()
    await tap('Remove')
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Deadlift' })).toBeNull())
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
})
