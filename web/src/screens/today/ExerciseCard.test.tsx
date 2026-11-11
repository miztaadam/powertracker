import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../../db/db'
import { TODAY, addDays, baseline, exerciseNamed, logSet, plan, set, setsOn, startSession, targets, todayFixture } from '../../test/todayFixture'
import { TodayScreen } from './TodayScreen'

// Typing weight, reps and RPE straight into the exercise card.
let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squatId: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squatId = (await exerciseNamed(db, 'Squat')).id
  // Planned today: squat 5 × 5 @ 8. Last week's top set: 150 × 5 @ 7. The session is already open.
  await plan(db, squatId)
  await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150, { rpe: 7 }))
  await startSession(db, TODAY)
})
// Unmount before the database closes: Vitest runs this before the cleanup registered in the test setup.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = (database: GritDb = db) => render(<TodayScreen db={database} today={() => TODAY} onOpenCoach={() => {}} />)

const button = (name: string) => screen.getByRole('button', { name })
const field = (name: string) => screen.getByRole<HTMLInputElement>('textbox', { name })
const weight = () => field('Weight, kilograms')
const reps = () => field('Reps')
const rpe = () => field('RPE')
const ready = () => screen.findByRole('button', { name: 'Log set 1' })
const waitForSets = (count: number) => waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(count))
const savedEditor = async () => (await db.meta.get(`editor:${TODAY}:${squatId}`))?.value

/** Types over what the field holds and stays in it: nothing is committed until the lifter leaves or taps a button. */
async function type(input: HTMLInputElement, text: string) {
  await userEvent.clear(input)
  if (text !== '') await userEvent.type(input, text)
}

/** Long enough for a set to have been written, had one been logged. */
const settled = () => new Promise((done) => setTimeout(done, 50))

/** Leaves the field, as a tap elsewhere on the screen does. */
const leave = () => userEvent.click(screen.getByRole('heading', { level: 1, name: 'Today' }))

/** Squat is replaced by a bench day: no history, so there is no weight yet. */
async function benchWithNoWeight() {
  await db.planItems.clear()
  await plan(db, (await exerciseNamed(db, 'Bench')).id)
}

describe('typed numbers on the exercise card', () => {
  it('typeWeight_thenLog_logs140', async () => {
    await benchWithNoWeight()
    show()
    await ready()
    expect(weight()).toHaveValue('')
    expect(weight()).toHaveAttribute('placeholder', 'kg')
    expect(button('Log set 1')).toBeDisabled()
    expect(screen.queryByRole('dialog')).toBeNull()

    await type(weight(), '140')
    // The plates follow the typing.
    expect(await screen.findByText('25 + 25 + 10')).toBeInTheDocument()
    await userEvent.click(button('Log set 1'))

    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 140, reps: 5, rpe: 8, failed: false }])
    expect(weight()).toHaveValue('140')
  })

  it('tapPrefilled_selectsAndReplaces', async () => {
    show()
    await ready()
    expect(weight()).toHaveValue('150')

    // No dialog: the tap puts the cursor in the field with the value selected.
    await userEvent.click(weight())
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(weight()).toHaveFocus()
    expect([weight().selectionStart, weight().selectionEnd]).toEqual([0, 3])
    await userEvent.keyboard('160')
    expect(weight()).toHaveValue('160')
    await userEvent.keyboard('{Enter}')

    expect(weight()).not.toHaveFocus()
    expect(weight()).toHaveValue('160')
    expect(await screen.findByText('25 + 25 + 20')).toBeInTheDocument()
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 160, reps: 5, rpe: 8 }))
  })

  it('usesTheDecimalAndNumericKeypads_andKeepsIosFromZooming', async () => {
    show()
    await ready()
    expect(weight()).toHaveAttribute('inputmode', 'decimal')
    expect(rpe()).toHaveAttribute('inputmode', 'decimal')
    expect(reps()).toHaveAttribute('inputmode', 'numeric')
    for (const name of ['Decrease Weight', 'Increase Weight', 'Decrease Reps', 'Increase Reps', 'Decrease RPE', 'Increase RPE']) expect(button(name)).toBeInTheDocument()
  })

  it('invalid_showsMessage_andDoesNotLog', async () => {
    show()
    await ready()

    await type(weight(), 'abc')
    await leave()
    expect(weight()).toBeInvalid()
    expect(weight()).toHaveAccessibleDescription('Numbers only')
    expect(weight()).toHaveValue('abc')

    await type(weight(), '1200')
    // The message goes while the entry is being corrected, and comes back when it is still wrong.
    expect(weight()).toBeValid()
    await leave()
    expect(weight()).toHaveAccessibleDescription('0 to 1000 kg')

    await type(rpe(), '11')
    await leave()
    expect(rpe()).toBeInvalid()
    expect(rpe()).toHaveAccessibleDescription('6 to 10')

    await type(reps(), '0')
    await leave()
    expect(reps()).toHaveAccessibleDescription('Whole, 1 to 99')

    // Nothing was saved or logged with any of them: the editor keeps its last good numbers.
    expect(await savedEditor()).toBeUndefined()
    expect(await setsOn(db, TODAY)).toEqual([])
    expect(screen.getByText('25 + 25 + 15')).toBeInTheDocument() // still 150 kg

    // Log set does not fall back to those numbers while a refused entry is on the card: it points at the first one.
    await userEvent.click(button('Log set 1'))
    expect(weight()).toHaveFocus()
    await settled()
    expect(await setsOn(db, TODAY)).toEqual([])

    // Corrected, the set is logged.
    await userEvent.keyboard('152,5')
    await type(reps(), '4')
    await type(rpe(), '9')
    await userEvent.click(button('Log set 1'))
    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 152.5, reps: 4, rpe: 9 }])
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('logSet_withInvalidTypedValue_refusesAndFocusesField', async () => {
    show()
    await ready()
    await type(weight(), '1200')
    await userEvent.click(button('Log set 1'))

    expect(weight()).toHaveFocus()
    expect(weight()).toBeInvalid()
    expect(weight()).toHaveAccessibleDescription('0 to 1000 kg')
    expect(weight()).toHaveValue('1200')
    await settled()
    expect(await setsOn(db, TODAY)).toEqual([])
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await savedEditor()).toBeUndefined()

    // Straight from the keypad, with the cursor in another field: the good entry is kept, the bad one gets the cursor.
    reps().focus()
    fireEvent.change(reps(), { target: { value: '3' } })
    fireEvent.change(rpe(), { target: { value: '11' } })
    fireEvent.click(button('Log set 1'))
    expect(weight()).toHaveFocus()
    expect(rpe()).toHaveAccessibleDescription('6 to 10')
    expect(reps()).toHaveValue('3')
    await settled()
    expect(await setsOn(db, TODAY)).toEqual([])
  })

  it('missed_withInvalidTypedValue_refusesAndFocusesField', async () => {
    show()
    await ready()
    rpe().focus()
    fireEvent.change(rpe(), { target: { value: 'abc' } })
    fireEvent.click(button('Missed'))

    expect(rpe()).toHaveFocus()
    expect(rpe()).toBeInvalid()
    expect(rpe()).toHaveAccessibleDescription('Numbers only')
    await settled()
    expect(await setsOn(db, TODAY)).toEqual([])

    // The same after the field was left with the refused entry in it.
    await leave()
    await userEvent.click(button('Missed'))
    expect(rpe()).toHaveFocus()
    await settled()
    expect(await setsOn(db, TODAY)).toEqual([])
  })

  it('invalid_withNoWeightYet_keepsLogDisabled', async () => {
    await benchWithNoWeight()
    show()
    await ready()
    for (const text of ['abc', '1200']) {
      await type(weight(), text)
      await leave()
      expect(weight()).toBeInvalid()
      expect(button('Log set 1')).toBeDisabled()
      expect(button('Missed')).toBeDisabled()
    }
    expect(await setsOn(db, TODAY)).toEqual([])
  })

  it('commaDecimal_isSavedAsADecimalPoint', async () => {
    show()
    await ready()
    await type(weight(), '102,5')
    await userEvent.click(button('Log set 1'))
    await waitForSets(1)
    expect((await setsOn(db, TODAY))[0]!.weightKg).toBe(102.5)
    expect(weight()).toHaveValue('102.5')
  })

  it('typeThenPlus_stepsFromTyped', async () => {
    show()
    await ready()
    await type(weight(), '140')
    await userEvent.click(button('Increase Weight'))
    expect(weight()).toHaveValue('142.5')
    await type(reps(), '8')
    await userEvent.click(button('Decrease Reps'))
    expect(reps()).toHaveValue('7')
    await type(rpe(), '9')
    await userEvent.click(button('Increase RPE'))
    expect(rpe()).toHaveValue('9.5')
    await waitFor(async () => expect(await savedEditor()).toEqual({ weightKg: 142.5, reps: 7, rpe: 9.5 }))
  })

  it('typeThenPlus_withoutLeavingTheField_stepsFromTyped', async () => {
    show()
    await ready()
    // iOS keeps the field focused when a button is tapped: no blur comes before the tap.
    weight().focus()
    fireEvent.change(weight(), { target: { value: '140' } })
    fireEvent.click(button('Increase Weight'))
    expect(weight()).toHaveFocus()
    expect(weight()).toHaveValue('142.5')
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 142.5 }))
  })

  it('bigJump_stillAsksDidYouMean', async () => {
    show()
    await ready()
    await type(weight(), '250')
    await userEvent.click(button('Log set 1'))
    const dialog = await screen.findByRole('dialog', { name: 'Did you mean 250 kg × 5?' })
    expect(within(dialog).getByText('That is a big jump from 150 kg.')).toBeInTheDocument()
    expect(await setsOn(db, TODAY), 'nothing is saved before Confirm').toEqual([])

    // Typed reps are checked too.
    await userEvent.click(button('Edit'))
    await type(weight(), '150')
    await type(reps(), '13')
    await userEvent.click(button('Log set 1'))
    expect(await screen.findByRole('dialog', { name: 'Did you mean 150 kg × 13?' })).toBeInTheDocument()
  })

  it('reload_keepsTypedValue', async () => {
    const first = show()
    await ready()
    await type(weight(), '162,5')
    await type(reps(), '3')
    await type(rpe(), '9')
    await leave()
    await waitFor(async () => expect(await savedEditor()).toEqual({ weightKg: 162.5, reps: 3, rpe: 9 }))
    first.unmount()
    db.close()

    db = await fx.open()
    show()
    await ready()
    expect(weight()).toHaveValue('162.5')
    expect(reps()).toHaveValue('3')
    expect(rpe()).toHaveValue('9')
  })

  it('logSet_usesTheTypedValue_withoutBlur', async () => {
    show()
    await ready()
    weight().focus()
    fireEvent.change(weight(), { target: { value: '140' } })
    fireEvent.change(reps(), { target: { value: '3' } })
    fireEvent.change(rpe(), { target: { value: '8.7' } })
    // The tap lands while the weight field still has the cursor.
    fireEvent.click(button('Log set 1'))
    expect(weight()).toHaveFocus()

    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 140, reps: 3, rpe: 8.5, failed: false }])
    expect(await savedEditor()).toEqual({ weightKg: 140, reps: 3, rpe: 8.5 })
    expect(weight()).toHaveValue('140')
    expect(rpe()).toHaveValue('8.5')
  })

  it('missed_usesTheTypedValue_withoutBlur', async () => {
    show()
    await ready()
    weight().focus()
    fireEvent.change(weight(), { target: { value: '155' } })
    fireEvent.click(button('Missed'))
    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 155, failed: true }])
  })

  it('stepAfterInvalidText_stepsFromLastGoodValue', async () => {
    show()
    await ready()
    await type(weight(), 'abc')
    await leave()
    expect(weight()).toBeInvalid()

    await userEvent.click(button('Increase Weight'))
    expect(weight()).toHaveValue('152.5')
    expect(weight()).toBeValid()
    expect(screen.queryByText('Numbers only')).toBeNull()

    // The same when + is tapped straight from the field.
    weight().focus()
    fireEvent.change(weight(), { target: { value: 'abc' } })
    fireEvent.click(button('Increase Weight'))
    expect(weight()).toHaveValue('155')
    expect(weight()).toBeValid()
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 155 }))
  })

  it('applyWhileTyping_doesNotClobberKeystrokes', async () => {
    // A known best gives the coach enough history to suggest a target: 150 @ 7 -> 155 × 5 @ 8.
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    show()
    await ready()
    expect(await screen.findByText('155 × 5 @ 8')).toBeInTheDocument()

    // Half-way through typing "140" the lifter taps Apply; on iOS the field keeps the cursor.
    weight().focus()
    fireEvent.change(weight(), { target: { value: '14' } })
    fireEvent.click(button('Apply'))
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 155 }))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Apply' })).toBeNull())

    // Focus stays and the keystrokes are still there; the plates already show what was applied.
    expect(weight()).toHaveFocus()
    expect(weight()).toHaveValue('14')
    expect(screen.getByText('25 + 25 + 15 + 2.5')).toBeInTheDocument()

    // Leaving the field shows the applied value: the stale "14" is not saved over it.
    fireEvent.blur(weight())
    expect(weight()).toHaveValue('155')
    expect(weight()).toBeValid()
    expect(await savedEditor()).toMatchObject({ weightKg: 155 })
    expect((await targets(db)).map((a) => a.status)).toEqual(['APPLIED'])
  })

  it('applyAfterTyping_inAFieldWithoutTheCursor_showsTheAppliedValue', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    show()
    await ready()
    await screen.findByText('155 × 5 @ 8')

    // Text lands in the field without the cursor ever being in it, so nothing will leave it either.
    fireEvent.change(weight(), { target: { value: '14' } })
    expect(weight()).not.toHaveFocus()
    expect(weight()).toHaveValue('14')
    fireEvent.click(button('Apply'))
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 155 }))

    // The applied value shows at once: what was typed over the older value is stale.
    await waitFor(() => expect(weight()).toHaveValue('155'))
    expect(weight()).toBeValid()
    expect(screen.getByText('25 + 25 + 15 + 2.5')).toBeInTheDocument()

    // The field and the logged set agree.
    fireEvent.click(button('Log set 1'))
    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 155, reps: 5, rpe: 8 }])
    expect(weight()).toHaveValue('155')
  })

  it('applyWhileTyping_thenTypingOn_keepsWhatWasTyped', async () => {
    await baseline(db, squatId, 190, addDays(TODAY, -30))
    show()
    await ready()
    await screen.findByText('155 × 5 @ 8')

    weight().focus()
    fireEvent.change(weight(), { target: { value: '14' } })
    fireEvent.click(button('Apply'))
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 155 }))
    expect(weight()).toHaveValue('14')

    // The lifter finishes the number after Apply: that is the newer decision.
    fireEvent.change(weight(), { target: { value: '140' } })
    fireEvent.blur(weight())
    expect(weight()).toHaveValue('140')
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 140 }))
  })

  it('clearedField_keepsLastGoodValue', async () => {
    show()
    await ready()
    await type(weight(), '')
    expect(weight()).toHaveValue('')
    await leave()

    // Not a value and not an error: the last good number is back.
    expect(weight()).toHaveValue('150')
    expect(weight()).toBeValid()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(await savedEditor()).toBeUndefined()

    await type(reps(), '')
    await type(rpe(), '')
    fireEvent.click(button('Log set 1'))
    await waitForSets(1)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 150, reps: 5, rpe: 8 }])
  })

  it('clearedField_withNoWeightYet_keepsLogDisabled', async () => {
    await benchWithNoWeight()
    show()
    await ready()
    await type(weight(), '60')
    expect(button('Log set 1')).toBeEnabled()
    await type(weight(), '')
    await leave()
    expect(weight()).toHaveValue('')
    expect(weight()).toBeValid()
    expect(button('Log set 1')).toBeDisabled()
    expect(button('Missed')).toBeDisabled()
    expect(await setsOn(db, TODAY)).toEqual([])
  })
})
