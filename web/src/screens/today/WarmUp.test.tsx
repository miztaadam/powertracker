import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../../db/db'
import { grit } from '../../domain'
import { warmUpKey, warmUpSets } from '../../state/warmUp'
import { TODAY, addDays, exerciseNamed, logSet, plan, set, setsOn, startSession, todayFixture } from '../../test/todayFixture'
import { TodayScreen } from './TodayScreen'
import { warmUpHint, warmUpLabel, warmUpText } from './WarmUp'

// The warm-up checklist on the exercise card (1.23).
let fx: ReturnType<typeof todayFixture>
let db: GritDb
let squatId: number
let benchId: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  squatId = (await exerciseNamed(db, 'Squat')).id
  benchId = (await exerciseNamed(db, 'Bench')).id
  // Planned today: squat 5 × 5 @ 8. Last week's top set: 150 × 5 @ 7. The session is already open.
  await plan(db, squatId)
  await logSet(db, await startSession(db, addDays(TODAY, -7)), set(squatId, 150, { rpe: 7 }))
  await startSession(db, TODAY)
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = () => render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} />)
const ready = () => screen.findByRole('button', { name: 'Log set 1' })
const block = () => screen.queryByRole('region', { name: 'Warm-up' })
const boxes = () => within(block()!).getAllByRole<HTMLInputElement>('checkbox')
const names = () => boxes().map((box) => box.getAttribute('aria-label'))
const weight = () => screen.getByRole<HTMLInputElement>('textbox', { name: 'Weight, kilograms' })
const follows = (first: Element, second: Element) => Boolean(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING)
const sessionToday = async () => (await db.sessions.where('date').equals(TODAY).first())!.id!

/** Today's plan is this exercise alone, with a top set last week so there is a weight in the field. */
async function only(exerciseId: number, lastKg: number, reps: number) {
  await db.planItems.clear()
  await plan(db, exerciseId, { sets: 3, reps })
  await logSet(db, (await db.sessions.where('date').equals(addDays(TODAY, -7)).first())!.id!, set(exerciseId, lastKg, { reps }))
}

describe('the warm-up checklist on the exercise card', () => {
  it('mainLift_twoSetsFromTheWeightInTheField_betweenLastTimeAndTheEntry', async () => {
    show()
    await ready()
    expect(names()).toEqual(['Warm-up set 1: 75 kg × 12, 25 + 2.5 per side', 'Warm-up set 2: 112.5 kg × 5, 25 + 20 + 1.25 per side'])
    expect(Array.from(block()!.querySelectorAll('.today-warm-line')).map((el) => el.textContent)).toEqual(['75 kg × 1225 + 2.5 per side', '112.5 kg × 525 + 20 + 1.25 per side'])
    // The heading says what a tick is not.
    expect(within(block()!).getByText('Warm-up · not logged')).toHaveClass('eyebrow')
    expect(follows(document.querySelector('.today-last')!, block()!)).toBe(true)
    expect(follows(block()!, weight())).toBe(true)
    expect(boxes().every((box) => !box.checked)).toBe(true)
  })

  it('followsTheTyping_beforeTheFieldIsLeft', async () => {
    show()
    await ready()
    await userEvent.clear(weight())
    await userEvent.type(weight(), '100')
    expect(names()).toEqual(['Warm-up set 1: 50 kg × 12, 15 per side', 'Warm-up set 2: 75 kg × 5, 25 + 2.5 per side'])
    // The bar itself has nothing lighter to load: no checklist.
    await userEvent.clear(weight())
    await userEvent.type(weight(), '20')
    expect(block()).toBeNull()
    // Just above the bar: the bar alone, once.
    await userEvent.type(weight(), '{Backspace}5')
    expect(names()).toEqual(['Warm-up set 1: 20 kg × 12, empty bar'])
  })

  it('aTick_isKeptWhileAnotherExerciseIsOpen_andStoresNothing', async () => {
    await plan(db, benchId, { order: 1 })
    show()
    await ready()
    const before = { sets: await db.sets.count(), meta: (await db.meta.toArray()).length }
    // The row's text ticks the box: the label is the target.
    await userEvent.click(within(block()!).getByText('25 + 2.5'))
    expect(boxes().map((box) => box.checked)).toEqual([true, false])
    expect(boxes()[0]!.closest('label')).toHaveClass('today-warm-done')

    // Bench has no weight yet, so no checklist; back on the squat the tick is still there.
    await userEvent.click(screen.getByRole('button', { name: /^Bench/ }))
    await screen.findByRole('heading', { name: 'Bench' })
    expect(block()).toBeNull()
    await userEvent.click(screen.getByRole('button', { name: /^Squat/ }))
    await screen.findByRole('heading', { name: 'Squat' })
    expect(boxes().map((box) => box.checked)).toEqual([true, false])

    // Unticked again with a second tap.
    await userEvent.click(boxes()[0]!)
    expect(boxes().map((box) => box.checked)).toEqual([false, false])
    await userEvent.click(boxes()[1]!)
    // No set was logged and nothing was written for the ticks.
    expect(await setsOn(db, TODAY)).toHaveLength(0)
    expect({ sets: await db.sets.count(), meta: (await db.meta.toArray()).length }).toEqual(before)
  })

  it('aTick_doesNotPassToASetWithOtherNumbers', async () => {
    show()
    await ready()
    await userEvent.click(boxes()[0]!)
    await userEvent.click(screen.getByRole('button', { name: 'Increase Weight' }))
    // 152.5: half is 76.25, the lighter of 75 and 77.5, so the first set is the same and keeps its tick; the second moved.
    await waitFor(() => expect(names()[1]).toBe('Warm-up set 2: 115 kg × 5, 25 + 20 + 2.5 per side'))
    expect(boxes().map((box) => box.checked)).toEqual([true, false])
    await userEvent.clear(weight())
    await userEvent.type(weight(), '100')
    expect(boxes().map((box) => box.checked)).toEqual([false, false])
  })

  it('goneOnceTheFirstSetIsLogged', async () => {
    show()
    await ready()
    expect(block()).not.toBeNull()
    await logSet(db, await sessionToday(), set(squatId, 150))
    await screen.findByText('Set 2 of 5')
    expect(block()).toBeNull()
  })

  it('doesNotComeBack_whenTheFirstSetIsDeleted', async () => {
    show()
    await ready()
    expect(block()).not.toBeNull()
    await userEvent.click(screen.getByRole('button', { name: 'Log set 1' }))
    await screen.findByText('Set 2 of 5')
    expect(block()).toBeNull()
    // Deleted again: the lifter has warmed up, and a block coming back would push the Log button down.
    await userEvent.click(screen.getByRole('button', { name: /^Set 1: 150 kg/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Delete set' }))
    await screen.findByRole('button', { name: 'Log set 1' })
    await waitFor(async () => expect(await setsOn(db, TODAY)).toHaveLength(0))
    expect(block()).toBeNull()
    // Typing another weight does not bring it back either.
    await userEvent.clear(weight())
    await userEvent.type(weight(), '100')
    expect(block()).toBeNull()
  })

  it('accessory_oneSet_withNoPlates_andNoneFromFifteenReps', async () => {
    const curl = (await exerciseNamed(db, 'Curl')).id
    await only(curl, 25, 10)
    const view = show()
    await ready()
    expect(names()).toEqual(['Warm-up set 1: 12.5 kg × 12'])
    expect(block()!.querySelector('.today-warm-hint')).toBeNull()
    view.unmount()

    // The plan asks for 15: a high-rep exercise needs none.
    await db.planItems.where('exerciseId').equals(curl).modify({ reps: 15 })
    show()
    await ready()
    expect(block()).toBeNull()
  })

  it('aBodybuildingRange_isJudgedByItsLowEnd', async () => {
    const curl = (await exerciseNamed(db, 'Curl')).id
    await only(curl, 25, 12)
    // 12 to 15 reps: the low end is under 15, so there is a warm-up.
    await db.planItems.where('exerciseId').equals(curl).modify({ repsHigh: 15 })
    show()
    await ready()
    expect(names()).toEqual(['Warm-up set 1: 12.5 kg × 12'])
  })

  it('aPlateCountedMachine_isInPlateNumbers', async () => {
    const entry = grit.findInLibrary('Lat pulldown')!
    const pulldown = (await db.exercises.add({ name: entry.name, category: entry.category, parentLift: entry.parentLift ?? null, muscles: [...entry.muscles], isBuiltIn: true, archived: false, plateCount: 'HALF' })) as number
    await db.planItems.clear()
    await plan(db, pulldown, { sets: 3, reps: 10 })
    await logSet(db, (await db.sessions.where('date').equals(addDays(TODAY, -7)).first())!.id!, { ...set(pulldown, 7, { reps: 10 }), unit: 'PLATES' })
    show()
    await ready()
    expect(names()).toEqual([`Warm-up set 1: ${grit.plateSpoken(3.5)} × 12`])
    expect(block()!.querySelector('.today-warm-line')).toHaveTextContent(`${grit.plateText(3.5)} × 12`)
  })
})

describe('warm-up wording', () => {
  const squat = { id: 1, name: 'Squat', category: 'SQUAT', parentLift: null, muscles: [], isBuiltIn: true, archived: false, plateCount: 'OFF' }

  it('theRowsTextHintAndSpokenName', () => {
    const [first, second] = warmUpSets(squat as never, 100, 5, false)
    expect([warmUpText(first!), warmUpHint(first!), warmUpLabel(1, first!)]).toEqual(['50 kg × 12', '15 per side', 'Warm-up set 1: 50 kg × 12, 15 per side'])
    expect([warmUpText(second!), warmUpHint(second!)]).toEqual(['75 kg × 5', '25 + 2.5 per side'])
    expect(warmUpHint({ weight: 20, reps: 12, unit: 'KG', perSide: [] })).toBe('empty bar')
    expect(warmUpHint({ weight: 12.5, reps: 12, unit: 'KG', perSide: null })).toBeNull()
    expect(warmUpText({ weight: 3.5, reps: 12, unit: 'PLATES', perSide: null })).toBe(`${grit.plateText(3.5)} × 12`)
  })

  it('noWeight_isNoSets_andAKeyIsTheSetsOwnNumbers', () => {
    expect(warmUpSets(squat as never, null, 5, false)).toEqual([])
    expect(warmUpSets(squat as never, 100, 15, false)).toEqual([])
    expect(warmUpKey({ weight: 50, reps: 12, unit: 'KG', perSide: [15] })).toBe('KG:50x12')
    expect(warmUpKey({ weight: 3.5, reps: 12, unit: 'PLATES', perSide: null })).toBe('PLATES:3.5x12')
  })
})
