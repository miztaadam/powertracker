import { act, cleanup, render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { resolveCatalogue } from '../../db/catalogue'
import type { GritDb } from '../../db/db'
import { grit } from '../../domain'
import { pastTheTapWindow } from '../../test/tapWindow'
import { TODAY, addDays, exerciseNamed, logSet, set, setsOn, startSession, todayFixture } from '../../test/todayFixture'
import { NO_TODAY_ACTIONS } from '../guide/guideSampleState'
import { TODAY_PICTURE } from '../guide/pictures/TodayPicture'
import { useToday, type TodayItem } from '../../state/useToday'
import { SetRow } from '../../ui/components/SetRow'
import { doseText, targetHeadline } from './ExerciseCard'
import { COUNT_PLATES, PlateCountSheet, plateNote, plateSheetLabel } from './PlateCount'
import { StartSessionCard } from './SessionCards'
import { TypoDialog } from './TodayDialogs'
import { TodayContent, TodayScreen } from './TodayScreen'

/** 1.17: counting plates on Today: the menu, the sheet, the PLATE stepper and plate sets (Android's `PlateCountScreenTest`). */

let fx: ReturnType<typeof todayFixture>
let db: GritDb
let pulldown: number

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  pulldown = await resolveCatalogue(db, 'Lat pulldown')
})
afterEach(async () => {
  vi.useRealTimers()
  cleanup()
  await fx.dispose()
})

const show = () => render(<TodayScreen db={db} today={() => TODAY} onOpenCoach={() => {}} />)
const menu = (name: string) => screen.queryByRole('button', { name: `Options for ${name}` })
const planToday = (exerciseId: number, order = 0) => db.planItems.add({ weekday: 3, exerciseId, sets: 3, reps: 8, targetRpe: 8, order, mode: 'POWERLIFTING' })
const plateField = () => screen.getByRole('textbox', { name: 'Weight, plates' }) as HTMLInputElement

/** The items of `name`'s row menu, by text. */
async function menuItems(name: string): Promise<string[]> {
  await userEvent.click(await screen.findByRole('button', { name: `Options for ${name}` }))
  const items = screen.getAllByRole('menuitem').map((i) => i.textContent ?? '')
  await userEvent.keyboard('{Escape}')
  return items
}

/** Opens `name`'s plate sheet from its row menu. */
async function openPlateSheet(name: string) {
  await userEvent.click(await screen.findByRole('button', { name: `Options for ${name}` }))
  await userEvent.click(screen.getByRole('menuitem', { name: COUNT_PLATES }))
  const sheet = within(await screen.findByRole('dialog', { name: plateSheetLabel(name) }))
  // Its choices take taps once it has been open a moment, as the skip sheet's answers do.
  pastTheTapWindow()
  return sheet
}

/** Pulldown counting `count`, in today's session. */
async function inPlates(count: 'WHOLE' | 'HALF') {
  await db.exercises.update(pulldown, { plateCount: count })
  await planToday(pulldown)
  return startSession(db, TODAY)
}

describe('the plate item and the sheet', () => {
  it('theStartSessionCard_offersCountPlates_beforeASession_andTheChoiceIsStored', async () => {
    await planToday(pulldown)
    show()
    expect(await screen.findByText('Ready to train?')).toBeInTheDocument()
    expect(await menuItems('Lat pulldown')).toEqual(['Skip today', COUNT_PLATES])
    const sheet = await openPlateSheet('Lat pulldown')
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    await waitFor(async () => expect((await db.exercises.get(pulldown))?.plateCount).toBe('WHOLE'))
    expect(await db.sessions.count()).toBe(0)
  })

  it('theStartSessionCard_withDefaultActions_hasNoPlateItem', () => {
    const item = { exercise: grit.seedExercises()[0]!, planned: null, suggestion: null, sets: [], editor: { weightKg: null, reps: 8, rpe: 8 }, targetSets: null, isComplete: false, perSide: null, lastWeightKg: null, lastTime: null, plateOffered: true } as TodayItem
    render(<StartSessionCard items={[item]} dayName="Wednesday" onStart={() => {}} />)
    expect(screen.queryByRole('button', { name: /^Options for/ })).toBeNull()
  })

  it('theMenuItem_showsOnAnOfferedExercise_alsoAfterItsFirstSet_andOnAnAddedOne', async () => {
    await planToday(pulldown)
    await logSet(db, await startSession(db, TODAY), set(pulldown, 40, { reps: 8 }))
    show()
    // After its first set there is no Skip today, but the plate item stays.
    expect(await menuItems('Lat pulldown')).toEqual([COUNT_PLATES])
    // An exercise added on Today (a catalogue machine with no row yet) has it too.
    await userEvent.click(screen.getByRole('button', { name: 'Add exercise' }))
    const picker = within(screen.getByRole('dialog', { name: 'Add exercise' }))
    await userEvent.type(picker.getByRole('textbox', { name: 'Search exercises' }), 'leg press')
    await userEvent.click(within(await picker.findByRole('listitem', { name: 'Leg press' })).getAllByRole('button')[0]!)
    expect(await screen.findByRole('button', { name: 'Remove' })).toBeInTheDocument()
    expect(await menuItems('Leg press')).toEqual([COUNT_PLATES])
  })

  it('noMenuItem_onBench_onACustomVariation_onAssistedDip', async () => {
    const bench = (await exerciseNamed(db, 'Bench')).id
    const own = (await db.exercises.add({ name: 'My bench', category: 'VARIATION', parentLift: 'BENCH', muscles: ['CHEST'], isBuiltIn: false, archived: false })) as number
    const dip = await resolveCatalogue(db, 'Assisted dip')
    for (const [i, id] of [bench, own, dip].entries()) await planToday(id, i)
    show()
    expect(await menuItems('Bench')).toEqual(['Skip today'])
    expect(await menuItems('My bench')).toEqual(['Skip today'])
    expect(await menuItems('Assisted dip')).toEqual(['Skip today'])
  })

  it('benchWithASet_hasNoMenuAtAll', async () => {
    const bench = (await exerciseNamed(db, 'Bench')).id
    await planToday(bench)
    await logSet(db, await startSession(db, TODAY), set(bench, 100))
    show()
    expect(await screen.findByRole('heading', { name: 'Bench' })).toBeInTheDocument()
    expect(menu('Bench')).toBeNull()
  })

  it('theGuidesToday_hasNoPlateItem', async () => {
    // The real screen's state, with an offered exercise and a set, drawn as the guide draws Today: with no actions.
    await planToday(pulldown)
    await logSet(db, await startSession(db, TODAY), set(pulldown, 40, { reps: 8 }))
    const vm = renderHook(() => useToday(db, () => TODAY)).result
    await waitFor(() => expect(vm.current.items[0]?.sets).toHaveLength(1))
    expect(vm.current.items[0]!.plateOffered).toBe(true)
    render(<TodayContent state={vm.current} actions={NO_TODAY_ACTIONS} onOpenCoach={() => {}} />)
    expect(screen.getByRole('heading', { name: 'Lat pulldown' })).toBeInTheDocument()
    expect(menu('Lat pulldown')).toBeNull()
  })

  it("the guide's Today picture has no plate item", () => {
    const steps = grit.guideSteps('TODAY', 'BODYBUILDING')
    const last = steps[steps.length - 1]!
    render(<TODAY_PICTURE.Draw mode="BODYBUILDING" step={last} stepIndex={steps.length - 1} progress={1} motionOn={false} />)
    expect(screen.queryByRole('button', { name: 'Options for Lat pulldown', hidden: true })).toBeNull()
    expect(screen.queryByText(COUNT_PLATES)).toBeNull()
  })

  it('theSheet_marksTheCurrentChoice_andAChoiceCallsOnCountPlates', async () => {
    const onChoose = vi.fn()
    const onDismiss = vi.fn()
    render(<PlateCountSheet name="Lat pulldown" current="WHOLE" note={null} onChoose={onChoose} onDismiss={onDismiss} />)
    const sheet = within(screen.getByRole('dialog', { name: 'Count plates on Lat pulldown' }))
    await act(async () => {}) // the sheet has finished opening
    pastTheTapWindow()
    expect(sheet.getByRole('heading', { name: 'Lat pulldown' })).toBeInTheDocument()
    const group = sheet.getByRole('radiogroup', { name: 'How does this machine show its weight?' })
    expect(within(group).getAllByRole('radio').map((r) => [r.getAttribute('aria-label') ?? r.closest('label')?.textContent, (r as HTMLInputElement).checked])).toHaveLength(3)
    expect(sheet.getByRole('radio', { name: 'In kg' })).not.toBeChecked()
    expect(sheet.getByRole('radio', { name: 'Whole plates' })).toBeChecked()
    expect(sheet.getByRole('radio', { name: 'Whole plates' })).toHaveAccessibleDescription('1, 2, 3 …')
    expect(sheet.getByRole('radio', { name: 'Half steps too' })).toHaveAccessibleDescription('1, 1½, 2 …')
    // The one already marked: nothing.
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(onChoose).not.toHaveBeenCalled()
    // A tap on the label (or its hint) chooses too.
    await userEvent.click(sheet.getByText('Half steps too'))
    expect(onChoose).toHaveBeenCalledWith('HALF')
    // Done only closes.
    await userEvent.click(sheet.getByRole('button', { name: 'Done' }))
    expect(onDismiss).toHaveBeenCalledTimes(1)
    expect(onChoose).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('plate-sheet')).toBeInTheDocument()
    expect(screen.getByTestId('plate-option-OFF')).toBeInTheDocument()
    expect(screen.getByTestId('plate-done')).toBeInTheDocument()
  })

  it('aDoubleTapOnTheMenuItem_doesNotPickAChoice', async () => {
    const onChoose = vi.fn()
    const onDismiss = vi.fn()
    render(<PlateCountSheet name="Lat pulldown" current="OFF" note={null} onChoose={onChoose} onDismiss={onDismiss} />)
    const sheet = within(screen.getByRole('dialog', { name: 'Count plates on Lat pulldown' }))
    // Where the finger that chose the menu item would tap again.
    await userEvent.click(sheet.getByText('Whole plates'))
    await userEvent.click(sheet.getByRole('radio', { name: 'Half steps too' }))
    expect(onChoose).not.toHaveBeenCalled()
    expect(sheet.getByRole('radio', { name: 'In kg' })).toBeChecked()
    await userEvent.click(sheet.getByRole('button', { name: 'Done' }))
    expect(onDismiss).toHaveBeenCalledTimes(1)
    pastTheTapWindow()
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(onChoose).toHaveBeenCalledWith('WHOLE')
  })

  it('theMarkedChoice_followsTheStoredSetting_onceASaveSettles', async () => {
    // Each save is held open until the test settles it.
    const saves: { value: string; done: () => void; fail: () => void }[] = []
    const onChoose = vi.fn(
      (value: string) =>
        new Promise<void>((done, reject) => {
          saves.push({ value, done, fail: () => reject(new Error('disk full')) })
        }),
    )
    const sheetWith = (current: 'OFF' | 'WHOLE' | 'HALF') => <PlateCountSheet name="Lat pulldown" current={current} note={null} onChoose={onChoose} onDismiss={() => {}} />
    const { rerender } = render(sheetWith('OFF'))
    const sheet = within(screen.getByRole('dialog', { name: 'Count plates on Lat pulldown' }))
    const checked = () => sheet.getAllByRole('radio').map((r) => (r as HTMLInputElement).checked)
    await act(async () => {})
    pastTheTapWindow()

    // Marked at once, while its save is in flight; a tap on the marked one again does nothing.
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(checked()).toEqual([false, true, false])
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(onChoose).toHaveBeenCalledTimes(1)
    // The store catches up, then the save settles: the stored setting shows.
    rerender(sheetWith('WHOLE'))
    await act(async () => saves[0]!.done())
    expect(checked()).toEqual([false, true, false])

    // A failed save: once it settles, the old setting shows again.
    await userEvent.click(sheet.getByRole('radio', { name: 'Half steps too' }))
    expect(checked()).toEqual([false, false, true])
    await act(async () => saves[1]!.fail())
    expect(checked()).toEqual([false, true, false])

    // The last tap wins: an earlier save counts as settled once a later tap is made.
    await userEvent.click(sheet.getByRole('radio', { name: 'Half steps too' }))
    await userEvent.click(sheet.getByRole('radio', { name: 'In kg' }))
    expect(onChoose.mock.calls.map(([v]) => v)).toEqual(['WHOLE', 'HALF', 'HALF', 'OFF'])
    expect(checked()).toEqual([true, false, false])
    await act(async () => saves[2]!.done())
    expect(checked()).toEqual([true, false, false])
    rerender(sheetWith('OFF'))
    await act(async () => saves[3]!.done())
    expect(checked()).toEqual([true, false, false])

    // Changed elsewhere (another tab): the mark follows the stored setting.
    rerender(sheetWith('HALF'))
    expect(checked()).toEqual([false, false, true])
  })

  it('theTappedChoice_staysMarked_afterAGoodSave_untilTheStoredSettingShowsIt', async () => {
    // The save (the write and the coach's pass) settles before the live query shows the new setting.
    const saves: { done: () => void; fail: () => void }[] = []
    const onChoose = vi.fn(() => new Promise<void>((done, reject) => saves.push({ done, fail: () => reject(new Error('disk full')) })))
    const sheetWith = (current: 'OFF' | 'WHOLE' | 'HALF') => <PlateCountSheet name="Lat pulldown" current={current} note={null} onChoose={onChoose} onDismiss={() => {}} />
    const { rerender } = render(sheetWith('OFF'))
    const sheet = within(screen.getByRole('dialog', { name: 'Count plates on Lat pulldown' }))
    const checked = () => sheet.getAllByRole('radio').map((r) => (r as HTMLInputElement).checked)
    await act(async () => {})
    pastTheTapWindow()

    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    await act(async () => saves[0]!.done())
    // Saved, but the stored setting on screen is still the old one: the mark does not flick back to In kg.
    expect(checked()).toEqual([false, true, false])
    rerender(sheetWith('OFF'))
    expect(checked()).toEqual([false, true, false])
    rerender(sheetWith('WHOLE'))
    expect(checked()).toEqual([false, true, false])
    // Once shown, the stored setting leads again: changed elsewhere, the mark follows it.
    rerender(sheetWith('HALF'))
    expect(checked()).toEqual([false, false, true])

    // A failed save goes back to what is stored at once.
    await userEvent.click(sheet.getByRole('radio', { name: 'In kg' }))
    expect(checked()).toEqual([true, false, false])
    await act(async () => saves[1]!.fail())
    expect(checked()).toEqual([false, false, true])
  })

  it('a failed save says so, and the sheet shows what is stored', async () => {
    await planToday(pulldown)
    show()
    const sheet = await openPlateSheet('Lat pulldown')
    vi.spyOn(db.exercises, 'where').mockImplementationOnce(() => {
      throw new Error('disk full')
    })
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(await screen.findByText("Couldn't save — try again.")).toBeInTheDocument()
    await waitFor(() => expect(sheet.getByRole('radio', { name: 'In kg' })).toBeChecked())
    expect((await db.exercises.get(pulldown))?.plateCount).toBeUndefined()
  })

  it('theNote_showsOnlyForANewUnitWithSetsInTheOldOne', async () => {
    expect(plateNote('KG', 'PLATES', true)).toBe('The coach starts this exercise fresh in plates. Your kg sets stay as they are.')
    expect(plateNote('PLATES', 'PLATES', true)).toBeNull()
    expect(plateNote('KG', 'PLATES', false)).toBeNull()
    expect(plateNote('PLATES', 'KG', true)).toBe('The coach goes back to kg. Your plate sets stay as they are.')
    expect(plateNote('KG', 'KG', true)).toBeNull()

    // On the screen: kg sets today, then Whole plates: the note, and still on Half steps too (the unit it opened in was kg).
    await planToday(pulldown)
    await logSet(db, await startSession(db, TODAY), set(pulldown, 40, { reps: 8 }))
    show()
    const sheet = await openPlateSheet('Lat pulldown')
    expect(sheet.queryByTestId('plate-note')).toBeNull()
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    expect(await sheet.findByText('The coach starts this exercise fresh in plates. Your kg sets stay as they are.')).toBeInTheDocument()
    await userEvent.click(sheet.getByRole('radio', { name: 'Half steps too' }))
    await waitFor(async () => expect((await db.exercises.get(pulldown))?.plateCount).toBe('HALF'))
    expect(sheet.getByTestId('plate-note')).toBeInTheDocument()
    await userEvent.click(sheet.getByRole('button', { name: 'Done' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: plateSheetLabel('Lat pulldown') })).toBeNull())
    // Opened again in plates with no plate set: whole ↔ half and back to kg say nothing... but the kg sets are not plate sets.
    const again = await openPlateSheet('Lat pulldown')
    await userEvent.click(again.getByRole('radio', { name: 'Whole plates' }))
    await waitFor(async () => expect((await db.exercises.get(pulldown))?.plateCount).toBe('WHOLE'))
    expect(again.queryByTestId('plate-note')).toBeNull()
    await userEvent.click(again.getByRole('radio', { name: 'In kg' }))
    await waitFor(async () => expect((await db.exercises.get(pulldown))?.plateCount).toBeUndefined())
    expect(again.queryByTestId('plate-note')).toBeNull()
  })

  it('the note going back to kg, with plate sets', async () => {
    await inPlates('WHOLE')
    await logSet(db, (await db.sessions.toArray())[0]!.id!, set(pulldown, 7, { reps: 8, unit: 'PLATES' }))
    show()
    const sheet = await openPlateSheet('Lat pulldown')
    await userEvent.click(sheet.getByRole('radio', { name: 'In kg' }))
    expect(await sheet.findByText('The coach goes back to kg. Your plate sets stay as they are.')).toBeInTheDocument()
  })
})

describe('the PLATE stepper', () => {
  it('theStepper_inWholePlates', async () => {
    await inPlates('WHOLE')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    expect(plateField().value).toBe('')
    expect(screen.getByRole('button', { name: 'Decrease Plate' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Increase Plate' }))
    await waitFor(() => expect(plateField().value).toBe('1'))
    expect(screen.getByRole('button', { name: 'Decrease Plate' })).toBeDisabled()
    await userEvent.clear(plateField())
    await userEvent.type(plateField(), '7{Enter}')
    await userEvent.click(screen.getByRole('button', { name: 'Increase Plate' }))
    await waitFor(() => expect(plateField().value).toBe('8'))
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Plate' }))
    await waitFor(() => expect(plateField().value).toBe('7'))
  })

  it('theStepper_inHalfSteps', async () => {
    await inPlates('HALF')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    await userEvent.type(plateField(), '7{Enter}')
    await userEvent.click(screen.getByRole('button', { name: 'Increase Plate' }))
    await waitFor(() => expect(plateField().value).toBe('7.5'))
    expect(plateField()).toHaveAttribute('inputmode', 'decimal')
  })

  it('afterHalfStepsAreTurnedOff_7AndAHalfGoesTo7And8', async () => {
    await inPlates('WHOLE')
    await logSet(db, (await db.sessions.toArray())[0]!.id!, set(pulldown, 7.5, { reps: 8, unit: 'PLATES' }))
    show()
    await waitFor(() => expect(plateField().value).toBe('7.5'))
    expect(plateField()).toHaveAttribute('inputmode', 'numeric')
    await userEvent.click(screen.getByRole('button', { name: 'Increase Plate' }))
    await waitFor(() => expect(plateField().value).toBe('8'))
    await userEvent.clear(plateField())
    await userEvent.type(plateField(), '7.5{Enter}')
    expect(await screen.findByText('Whole plates only')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Plate' }))
    await waitFor(() => expect(plateField().value).toBe('7'))
    // The logged 7½ stays 7½.
    expect(screen.getByText('Set 1: plate 7 and a half × 8 at RPE 8, done')).toBeInTheDocument()
  })

  it('noPlatePicture_forAPlateExercise', async () => {
    await inPlates('WHOLE')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    expect(screen.queryByText('No weight yet')).toBeNull()
    await userEvent.type(plateField(), '7{Enter}')
    await waitFor(() => expect(plateField().value).toBe('7'))
    expect(screen.queryByText('No weight yet')).toBeNull()
    expect(screen.queryByText(/Can't load/)).toBeNull()
    expect(document.querySelector('.gr-plates, .gr-plate-strip')).toBeNull()
  })

  it('theFieldsAccessibleName_isWeightPlates', async () => {
    await inPlates('WHOLE')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    expect(plateField()).toHaveAttribute('placeholder', 'plate')
    expect(screen.queryByRole('textbox', { name: 'Weight, kilograms' })).toBeNull()
    await userEvent.type(plateField(), '12{Enter}')
    await waitFor(() => expect(plateField().value).toBe('12'))
    // Nothing after the number.
    expect(plateField().closest('.gr-numfield')!.querySelector('.gr-numfield-unit')).toBeNull()
    expect(screen.getByText('Plate')).toBeInTheDocument()
    await userEvent.clear(plateField())
    await userEvent.type(plateField(), '51{Enter}')
    expect(await screen.findByText('Plate 1 to 50')).toBeInTheDocument()
  })

  it('a kg weight still being typed when plates are turned on never reaches the PLATE field', async () => {
    await planToday(pulldown)
    await startSession(db, TODAY)
    show()
    const kg = await screen.findByRole('textbox', { name: 'Weight, kilograms' })
    await userEvent.type(kg, '40')
    // The setting changes under the typing (another tab).
    await act(() => db.exercises.update(pulldown, { plateCount: 'WHOLE' }))
    await waitFor(() => expect(plateField()).toBeInTheDocument())
    expect(plateField().value).toBe('')
    await userEvent.click(plateField())
    await userEvent.tab()
    expect(plateField().value).toBe('')
    expect(screen.getByRole('button', { name: 'Log set 1' })).toBeDisabled()
    const kept = await db.meta.get(`editor:${TODAY}:${pulldown}`)
    expect((kept?.value as { weightKg?: number } | undefined)?.weightKg ?? null).not.toBe(40)
  })
})

describe('plate sets', () => {
  it('aPlateSetRow_readsPlate7AndAHalf_andSpeaksIt', () => {
    render(<SetRow index={1} weightKg={7.5} reps={12} rpe={8} state="done" unit="PLATES" />)
    expect(screen.getByText('Set 1: plate 7 and a half × 12 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('plate 7½')).toBeInTheDocument()
    cleanup()
    render(<SetRow index={1} weightKg={150} reps={5} rpe={8} state="done" />)
    expect(screen.getByText('Set 1: 150 kg × 5 at RPE 8, done')).toBeInTheDocument()
  })

  it('theChip_readsPlate7Times8To12', () => {
    expect(targetHeadline({ exerciseId: 1, weightKg: 7, reps: 8, repsHigh: 12, rpe: 8, sets: null, unit: 'PLATES' })).toBe('plate 7 × 8–12 @ 8')
    expect(targetHeadline({ exerciseId: 1, weightKg: 12.5, reps: 8, repsHigh: 12, rpe: 8, sets: null, unit: 'PLATES' })).toBe('plate 12½ × 8–12 @ 8')
    expect(targetHeadline({ exerciseId: 1, weightKg: 7, reps: 9, rpe: 8, sets: 2, unit: 'PLATES' })).toBe('plate 7 × 9 @ 8 · 2 sets')
    // Kg as it was.
    expect(targetHeadline({ exerciseId: 1, weightKg: 26, reps: 8, repsHigh: 12, rpe: 8, sets: null })).toBe('26 kg × 8–12 @ 8')
    expect(targetHeadline({ exerciseId: 1, weightKg: 155, reps: 5, rpe: 8, sets: null })).toBe('155 × 5 @ 8')
  })

  it('theTypoDialog_namesPlates', () => {
    render(<TypoDialog pending={{ exerciseId: 1, set: { weightKg: 12, reps: 10, rpe: 8, failed: false, unit: 'PLATES' }, warnings: ['BIG_JUMP'], lastWeightKg: 3 }} onConfirm={() => {}} onEdit={() => {}} />)
    expect(screen.getByRole('dialog', { name: 'Did you mean plate 12 × 10?' })).toBeInTheDocument()
    expect(screen.getByText('That is a big jump from plate 3.')).toBeInTheDocument()
    cleanup()
    render(<TypoDialog pending={{ exerciseId: 1, set: { weightKg: 120, reps: 10, rpe: 8, failed: false }, warnings: ['BIG_JUMP'], lastWeightKg: 60 }} onConfirm={() => {}} onEdit={() => {}} />)
    expect(screen.getByRole('dialog', { name: 'Did you mean 120 kg × 10?' })).toBeInTheDocument()
    expect(screen.getByText('That is a big jump from 60 kg.')).toBeInTheDocument()
  })

  it('topSet_isMarkedAmongThisUnitsSetsOnly', async () => {
    const session = await inPlates('WHOLE')
    await logSet(db, session, set(pulldown, 60, { reps: 8 }))
    await logSet(db, session, set(pulldown, 62.5, { reps: 8 }))
    await logSet(db, session, set(pulldown, 7, { reps: 8, unit: 'PLATES' }))
    await logSet(db, session, set(pulldown, 8, { reps: 8, unit: 'PLATES' }))
    show()
    expect(await screen.findByText('Set 4: plate 8 × 8 at RPE 8, top set, done')).toBeInTheDocument()
    expect(screen.getByText('Set 2: 62.5 kg × 8 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('Set 3: plate 7 × 8 at RPE 8, done')).toBeInTheDocument()
  })

  it('the selected set reads in its own unit', async () => {
    expect(doseText(60, 8, 8)).toBe('60 × 8 @ 8')
    expect(doseText(7.5, 8, 8, 'PLATES')).toBe('plate 7½ × 8 @ 8')
    const session = await inPlates('WHOLE')
    await logSet(db, session, set(pulldown, 60, { reps: 8, failed: true }))
    await logSet(db, session, set(pulldown, 7, { reps: 8, unit: 'PLATES' }))
    show()
    await userEvent.click(await screen.findByRole('button', { name: /^Set 1: 60 kg/ }))
    expect(within(screen.getByRole('dialog', { name: 'Set 1' })).getByText('60 × 8 @ 8 · failed')).toBeInTheDocument()
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Set 1' })).getByRole('button', { name: 'Keep' }))
    await userEvent.click(screen.getByRole('button', { name: /^Set 2: plate 7/ }))
    expect(within(screen.getByRole('dialog', { name: 'Set 2' })).getByText('plate 7 × 8 @ 8')).toBeInTheDocument()
  })

  it('a stand-in keeps its open sheet when choosing plates gives it its row', async () => {
    await startSession(db, TODAY)
    await db.exercises.delete(pulldown)
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Add exercise' }))
    const picker = within(screen.getByRole('dialog', { name: 'Add exercise' }))
    await userEvent.type(picker.getByRole('textbox', { name: 'Search exercises' }), 'lat pulldown')
    await userEvent.click(within(await picker.findByRole('listitem', { name: 'Lat pulldown' })).getAllByRole('button')[0]!)
    const sheet = await openPlateSheet('Lat pulldown')
    await userEvent.click(sheet.getByRole('radio', { name: 'Whole plates' }))
    await waitFor(async () => expect((await exerciseNamed(db, 'Lat pulldown')).plateCount).toBe('WHOLE'))
    await waitFor(() => expect(plateField()).toBeInTheDocument())
    expect(screen.getByRole('dialog', { name: plateSheetLabel('Lat pulldown') })).toBeInTheDocument()
    await userEvent.click(sheet.getByRole('radio', { name: 'Half steps too' }))
    await waitFor(async () => expect((await exerciseNamed(db, 'Lat pulldown')).plateCount).toBe('HALF'))
    expect(await db.exercises.where('name').equals('Lat pulldown').count()).toBe(1)
  })

  it('aPlateSetTypedAndLoggedOnTheScreen_isStoredInPlates', async () => {
    await inPlates('WHOLE')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    await userEvent.type(plateField(), '7{Enter}')
    await userEvent.click(screen.getByRole('button', { name: 'Log set 1' }))
    expect(await screen.findByText('Set 1: plate 7 × 8 at RPE 8, done')).toBeInTheDocument()
    expect(screen.getByText('plate 7')).toBeInTheDocument()
    expect((await setsOn(db, TODAY)).map((s) => [s.weightKg, s.unit])).toEqual([[7, 'PLATES']])
  })

  it('history before today in another unit never fills the field', async () => {
    await logSet(db, await startSession(db, addDays(TODAY, -7)), set(pulldown, 60, { reps: 8 }))
    await inPlates('WHOLE')
    show()
    await screen.findByRole('heading', { name: 'Lat pulldown' })
    expect(plateField().value).toBe('')
  })
})
