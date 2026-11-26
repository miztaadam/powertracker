import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { updateSettings, type GritDb } from '../../db/db'
import type { TrainingMode } from '../../db/schema'
import { grit } from '../../domain'
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

// 1.18: what was done on the exercise last time, where the stretch and squeeze lines were, on both sides.
describe('last time on the exercise card', () => {
  const LAST_WEEK = addDays(TODAY, -7)
  const sessionOn = async (date: string) => (await db.sessions.where('date').equals(date).first())!.id!
  /** Today's plan is the exercise alone, 3 × 8–12 on `mode`, with today's session and the app on that side. */
  async function onlyToday(mode: TrainingMode, exerciseId: number) {
    await db.planItems.clear()
    await db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: 8, ...(mode === 'BODYBUILDING' ? { repsHigh: 12 } : {}), targetRpe: 8, order: 0, mode })
    await db.sessions.where('date').equals(TODAY).modify({ mode })
    await updateSettings(db, (s) => ({ ...s, trainingMode: mode }))
  }
  const block = () => document.querySelector('.today-last')
  /** What a screen reader reads for an element: its text, without what is hidden from it. */
  const spoken = (el: Element): string =>
    Array.from(el.childNodes)
      .map((n) => (n.nodeType === Node.TEXT_NODE ? (n.textContent ?? '') : n instanceof Element && n.getAttribute('aria-hidden') !== 'true' ? spoken(n) : ''))
      .join('')
  /** What shows in each row: its figures as drawn, without the sentence kept for a screen reader. */
  const drawn = () => Array.from(document.querySelectorAll('.today-last-line')).map((el) => el.textContent)
  const follows = (first: Element, second: Element) => Boolean(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING)

  it('powerliftingCard_showsEverySetOfLastTime_inOrder', async () => {
    // Last week: 150 × 5 @ 7 (the fixture's), then a set with no RPE, then a missed one.
    const lastWeek = await sessionOn(LAST_WEEK)
    await logSet(db, lastWeek, set(squatId, 152.5, { reps: 4, rpe: null }))
    await logSet(db, lastWeek, set(squatId, 155, { reps: 2, rpe: 9.5, failed: true }))
    show()
    await ready()

    const section = screen.getByRole('region', { name: 'Last time, Wed 4 Nov' })
    const head = within(section).getByText('Last time · Wed 4 Nov')
    expect(head).toHaveClass('eyebrow')
    const rows = within(within(section).getByRole('list')).getAllByRole('listitem')
    // Nothing is logged today, so last time's first set is the one to match (1.21).
    expect(rows.map(spoken)).toEqual(['Set 1: 150 kg × 5 at RPE 7, your next set', 'Set 2: 152.5 kg × 4', 'Set 3: 155 kg × 2 at RPE 9.5, missed'])
    // A set with no RPE has no "@" with nothing after it; only the missed set says so.
    expect(drawn()).toEqual(['150 kg× 5@ 7Next', '152.5 kg× 4', '155 kg× 2@ 9.5Missed'])
    expect(Array.from(document.querySelectorAll('.today-last-index')).map((el) => el.textContent)).toEqual(['1', '2', '3'])
    // No row can be tapped: the block's one button is "Use set N" (1.21).
    expect(within(section).getAllByRole('button').map((b) => b.textContent)).toEqual(['Use set 1: 150 kg × 5'])
    expect(within(within(section).getByRole('list')).queryByRole('button')).toBeNull()
    // Before the weight entry.
    expect(follows(section, weight())).toBe(true)
  })

  it('bodybuildingCard_showsItsOwnSidesLastTime_andNoStretchOrSqueeze', async () => {
    const squat = grit.muscleFocus('Squat')!
    // The squat was last done a week ago on the powerlifting side: not this side's last time.
    await onlyToday('BODYBUILDING', squatId)
    const view = show()
    await ready()
    expect(block()).toBeNull()
    expect(screen.queryByText(/Last time/)).toBeNull()
    // The lines moved to the guide: they are not on the card, whether or not there is a last time.
    expect(screen.queryByText('STRETCH')).toBeNull()
    expect(screen.queryByText(squat.squeeze!)).toBeNull()
    view.unmount()

    // Two weeks ago on this side, before the powerlifting session: that one is shown.
    const earlier = await startSession(db, addDays(TODAY, -14))
    await db.sessions.update(earlier, { mode: 'BODYBUILDING' })
    for (const reps of [12, 11, 10]) await logSet(db, earlier, set(squatId, 100, { reps }))
    show()
    const section = await screen.findByRole('region', { name: 'Last time, Wed 28 Oct' })
    expect(within(section).getAllByRole('listitem').map(spoken)).toEqual(['Set 1: 100 kg × 12 at RPE 8, your next set', 'Set 2: 100 kg × 11 at RPE 8', 'Set 3: 100 kg × 10 at RPE 8'])
    expect(screen.queryByText('STRETCH')).toBeNull()
  })

  it('card_neverDone_showsNothing_andTodaysOwnSetsAreNotLastTime', async () => {
    const bench = (await exerciseNamed(db, 'Bench')).id
    await db.planItems.clear()
    await plan(db, bench)
    await logSet(db, await sessionOn(TODAY), set(bench, 100))
    show()
    await screen.findByText('Set 2 of 5')
    expect(screen.getByRole('button', { name: /^Set 1: 100 kg/ })).toBeInTheDocument()
    expect(block()).toBeNull()
    expect(screen.queryByText(/Last time/)).toBeNull()
  })

  it('card_lastTimeSitsBetweenTodaysSetsAndTheEntry_thenAboveTheNextButton', async () => {
    const today = await sessionOn(TODAY)
    await logSet(db, today, set(squatId, 152.5))
    const view = show()
    await screen.findByText('Set 2 of 5')
    expect(follows(screen.getByRole('button', { name: /^Set 1:/ }), block()!)).toBe(true)
    expect(follows(block()!, weight())).toBe(true)
    view.unmount()

    // Every set done: the block stays, above the way on.
    for (let i = 0; i < 4; i++) await logSet(db, today, set(squatId, 152.5))
    show()
    const finish = await screen.findByRole('button', { name: 'Finish session' })
    expect(screen.queryByRole('textbox', { name: 'Weight, kilograms' })).toBeNull()
    expect(follows(screen.getByRole('button', { name: /^Set 5:/ }), block()!)).toBe(true)
    expect(follows(block()!, finish)).toBe(true)
  })

  it('lastTime_fromAnotherYear_carriesTheYear', async () => {
    await db.sessions.update(await sessionOn(LAST_WEEK), { date: '2025-12-31' })
    show()
    expect(await screen.findByRole('region', { name: 'Last time, Wed 31 Dec 2025' })).toBeInTheDocument()
    expect(screen.getByText('Last time · Wed 31 Dec 2025')).toBeInTheDocument()
  })

  it('lastTime_aPlateSetBesideAKgSet_eachInItsOwnUnit', async () => {
    await logSet(db, await sessionOn(LAST_WEEK), { ...set(squatId, 7.5, { reps: 12 }), unit: 'PLATES' })
    show()
    const section = await screen.findByRole('region', { name: /^Last time/ })
    expect(within(section).getAllByRole('listitem').map(spoken)).toEqual(['Set 1: 150 kg × 5 at RPE 7, your next set', `Set 2: ${grit.plateSpoken(7.5)} × 12 at RPE 8`])
    expect(drawn()).toEqual(['150 kg× 5@ 7Next', `${grit.plateText(7.5)}× 12@ 8`])
  })

  // 1.21: the row for the set about to be logged is marked, and one button puts its weight and reps in the fields.
  const nextRow = () => document.querySelector('.today-last-set-next')
  const useButton = () => screen.queryByRole('button', { name: /^Use set / })

  it('useSet_marksTheRowForTheNextSet_andFillsWeightAndReps_loggingNothing', async () => {
    // Last week worked up: 150 × 5 @ 7 (the fixture's), 152.5 × 4, 155 × 2 @ 9.5.
    const lastWeek = await sessionOn(LAST_WEEK)
    await logSet(db, lastWeek, set(squatId, 152.5, { reps: 4, rpe: null }))
    await logSet(db, lastWeek, set(squatId, 155, { reps: 2, rpe: 9.5 }))
    // One set done today, so the second of last time is the one to match.
    await logSet(db, await sessionOn(TODAY), set(squatId, 150, { rpe: 7.5 }))
    show()
    await screen.findByText('Set 2 of 5')
    expect(nextRow()).toHaveTextContent('Set 2: 152.5 kg × 4, your next set')
    expect(document.querySelectorAll('.today-last-set-next')).toHaveLength(1)
    expect(useButton()).toHaveTextContent('Use set 2: 152.5 kg × 4')
    // The field holds today's last set until the button is tapped.
    expect(weight()).toHaveValue('150')
    expect(reps()).toHaveValue('5')
    expect(rpe()).toHaveValue('7.5')

    await userEvent.click(useButton()!)
    await waitFor(() => expect(weight()).toHaveValue('152.5'))
    expect(reps()).toHaveValue('4')
    // The effort is left as it was, and nothing was logged.
    expect(rpe()).toHaveValue('7.5')
    await settled()
    expect(await setsOn(db, TODAY)).toHaveLength(1)
    // The plates follow, as after any change of weight.
    expect(await screen.findByText('25 + 25 + 15 + 1.25')).toBeInTheDocument()

    // Log it: the third row is next, and the button names it.
    await userEvent.click(button('Log set 2'))
    await waitForSets(2)
    expect(await setsOn(db, TODAY)).toMatchObject([{ weightKg: 150 }, { weightKg: 152.5, reps: 4, rpe: 7.5 }])
    await waitFor(() => expect(nextRow()).toHaveTextContent('Set 3: 155 kg × 2 at RPE 9.5, your next set'))
    expect(useButton()).toHaveTextContent('Use set 3: 155 kg × 2')
  })

  it('useSet_pastLastTimesSets_orWithTheDaysSetsDone_marksNothing_andHasNoButton', async () => {
    // Last time was one set; today's second set has none to match.
    const today = await sessionOn(TODAY)
    await logSet(db, today, set(squatId, 152.5))
    const view = show()
    await screen.findByText('Set 2 of 5')
    expect(block()).not.toBeNull()
    expect(nextRow()).toBeNull()
    expect(useButton()).toBeNull()
    expect(within(block() as HTMLElement).getAllByRole('listitem').map(spoken)).toEqual(['Set 1: 150 kg × 5 at RPE 7'])
    view.unmount()

    // Every set of the day done: the block stays, with nothing marked and no button, until another set is asked for.
    await db.sessions.update(await sessionOn(LAST_WEEK), {})
    for (let i = 0; i < 5; i++) await logSet(db, await sessionOn(LAST_WEEK), set(squatId, 140 + i))
    for (let i = 0; i < 4; i++) await logSet(db, today, set(squatId, 152.5))
    show()
    await screen.findByRole('button', { name: 'Finish session' })
    expect(block()).not.toBeNull()
    expect(nextRow()).toBeNull()
    expect(useButton()).toBeNull()
    await userEvent.click(button('Add another set'))
    // Six sets last time, five done today: the sixth is next.
    await waitFor(() => expect(nextRow()).toHaveTextContent('Set 6: 144 kg × 5 at RPE 8, your next set'))
    expect(useButton()).toHaveTextContent('Use set 6: 144 kg × 5')
  })

  it('useSet_onARamp_thenLog_isNotHeldAsATypo_butATypedJumpStillIs', async () => {
    // Last week ramped: 100, 140, 170. Today's first set is 100; the second, 140, is 40% up: a jump the typo check asks about.
    const lastWeek = await sessionOn(LAST_WEEK)
    await db.sets.where('sessionId').equals(lastWeek).modify({ weightKg: 100, rpe: 5 })
    await logSet(db, lastWeek, set(squatId, 140, { rpe: 6 }))
    await logSet(db, lastWeek, set(squatId, 170, { rpe: 7 }))
    await logSet(db, await sessionOn(TODAY), set(squatId, 100, { rpe: 5 }))
    show()
    await screen.findByText('Set 2 of 5')

    await userEvent.click(useButton()!)
    await waitFor(() => expect(weight()).toHaveValue('140'))
    await userEvent.click(button('Log set 2'))
    // Done last time as set 2: logged at once, no "is this right?".
    await waitForSets(2)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect((await setsOn(db, TODAY)).map((s) => s.weightKg)).toEqual([100, 140])

    // A weight not done last time as set 3, just as far up: still asked about.
    await waitFor(() => expect(useButton()).toHaveTextContent('Use set 3: 170 kg × 5'))
    await type(weight(), '190')
    await userEvent.click(button('Log set 3'))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    await settled()
    expect(await setsOn(db, TODAY)).toHaveLength(2)
  })

  it('useSet_withANumberTypedAndLeftInTheField_actsAsApplyDoes', async () => {
    const lastWeek = await sessionOn(LAST_WEEK)
    await logSet(db, lastWeek, set(squatId, 152.5, { reps: 4 }))
    await logSet(db, await sessionOn(TODAY), set(squatId, 150))
    show()
    await screen.findByText('Set 2 of 5')

    // Half-way through typing "140" the lifter taps the button; on iOS the field keeps the cursor.
    weight().focus()
    fireEvent.change(weight(), { target: { value: '14' } })
    fireEvent.click(useButton()!)
    await waitFor(async () => expect(await savedEditor()).toMatchObject({ weightKg: 152.5, reps: 4 }))
    // The keystrokes are still there while the cursor is; the reps field, untouched, shows the row's.
    expect(weight()).toHaveFocus()
    expect(weight()).toHaveValue('14')
    expect(reps()).toHaveValue('4')
    // Leaving the field shows the row's weight: the stale "14" is not saved over it.
    fireEvent.blur(weight())
    expect(weight()).toHaveValue('152.5')
    expect(await savedEditor()).toMatchObject({ weightKg: 152.5, reps: 4 })
  })

  it('useSet_aPlateNumberOffTheMachinesGrid_isMarked_butHasNoButton', async () => {
    // Lat pulldown counts whole plates now; last time's first set was plate 7½, logged when it counted half steps.
    const entry = grit.findInLibrary('Lat pulldown')!
    const pulldown = (await db.exercises.add({ name: entry.name, category: entry.category, parentLift: entry.parentLift ?? null, muscles: [...entry.muscles], isBuiltIn: true, archived: false, plateCount: 'WHOLE' })) as number
    await db.planItems.clear()
    await plan(db, pulldown, { sets: 3, reps: 10 })
    const lastWeek = await sessionOn(LAST_WEEK)
    await logSet(db, lastWeek, { ...set(pulldown, 7.5, { reps: 12 }), unit: 'PLATES' })
    await logSet(db, lastWeek, { ...set(pulldown, 8, { reps: 10 }), unit: 'PLATES' })
    const view = show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    await waitFor(() => expect(nextRow()).toHaveTextContent(`Set 1: ${grit.plateSpoken(7.5)} × 12 at RPE 8, your next set`))
    expect(useButton()).toBeNull()
    view.unmount()

    // With one set logged, the second row, a whole plate, can be used.
    await logSet(db, await sessionOn(TODAY), { ...set(pulldown, 7, { reps: 12 }), unit: 'PLATES' })
    show()
    await waitFor(() => expect(useButton()).toHaveTextContent(`Use set 2: ${grit.plateText(8)} × 10`))
  })

  it('useSet_aSetInAnotherUnitThanTheField_isMarked_butHasNoButton', async () => {
    // Last time's first set was a plate number; the squat counts in kg, so it cannot go in the field.
    await db.sets.where('sessionId').equals(await sessionOn(LAST_WEEK)).modify({ unit: 'PLATES', weightKg: 7.5 })
    show()
    await ready()
    expect(nextRow()).toHaveTextContent(`Set 1: ${grit.plateSpoken(7.5)} × 5 at RPE 7, your next set`)
    expect(useButton()).toBeNull()
  })

  it('lastTime_followsTheSideOfTodaysSession', async () => {
    // Today's session moves to the bodybuilding side, where the squat was never done: the same card, without the block; and back.
    await logSet(db, await sessionOn(TODAY), set(squatId, 100, { reps: 10 }))
    show()
    await screen.findByRole('region', { name: 'Last time, Wed 4 Nov' })
    const heading = screen.getByRole('heading', { name: 'Squat' })
    await db.sessions.where('date').equals(TODAY).modify({ mode: 'BODYBUILDING' })
    await waitFor(() => expect(block()).toBeNull())
    expect(screen.getByRole('heading', { name: 'Squat' })).toBe(heading)
    await db.sessions.where('date').equals(TODAY).modify({ mode: 'POWERLIFTING' })
    expect(await screen.findByRole('region', { name: 'Last time, Wed 4 Nov' })).toBeInTheDocument()
  })
})
