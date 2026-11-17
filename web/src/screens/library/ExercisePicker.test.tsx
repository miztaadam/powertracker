import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { updateSettings, type GritDb } from '../../db/db'
import type { ExerciseRow } from '../../db/schema'
import { grit } from '../../domain'
import { pickerList, type CustomExercise, type PickerEntry, type StoredExercise } from '../../state/exercisePicker'
import { checkInfoButtonOpensGuide } from '../../test/guideChecks'
import { wholeList } from '../../test/wholeList'
import { set, startSession, logSet, todayFixture, TODAY } from '../../test/todayFixture'
import { PlanScreen } from '../plan/PlanScreen'
import { ExercisePicker } from './ExercisePicker'
import { AREAS, EQUIPMENT } from './labels'

// Ported from Android's ExercisePickerTest. Sizes and the 375 px layout are checked in e2e/library.spec.ts.

const NO_MATCH = 'No exercises match. Try fewer filters, or create your own.'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const stored = async () => (await db.exercises.toArray()) as StoredExercise[]
const rows = async (name: string) => (await db.exercises.toArray()).filter((e) => e.name.toLowerCase() === name.toLowerCase())
const storedPlan = async () => (await db.planItems.toArray()).map(({ id: _id, ...rest }) => rest)
const names = (entries: PickerEntry[]) => entries.map((e) => e.name)
const libraryNames = (query = '', areas: string[] = [], equipment: string[] = []) => grit.searchLibrary(query, areas, equipment).map((e) => e.name)

const picker = () => within(screen.getByRole('dialog', { name: /^(Add exercise|Add to |Change exercise)/ }))
/** The names of the rows on screen, top to bottom. */
const shownNames = () => picker().queryAllByRole('listitem').map((row) => row.getAttribute('aria-label'))
const shows = (name: string) => shownNames().includes(name)
/** The part of a row that picks the exercise. */
const rowButton = (name: string) => within(picker().getByRole('listitem', { name })).getAllByRole('button')[0]!
const searchBox = () => picker().getByRole('textbox', { name: 'Search exercises' })
const chip = (group: 'Body area' | 'Equipment', name: string) => within(picker().getByRole('group', { name: group })).getByRole('checkbox', { name })

/** The muscle grid; null while it is not shown. */
const grid = () => picker().queryByRole('group', { name: 'Browse by muscle' })
const tile = (label: string) => within(grid()!).getByRole('button', { name: label })
const heading = (name: string) => picker().queryByRole('heading', { name })
const allMuscles = () => picker().queryByRole('button', { name: 'All muscles' })
/** A name as the domain sorts it. */
const key = (name: string) => name.toLowerCase().replace(/[-_]/g, ' ')
/** The names a muscle group lists: the catalogue's, and `mine` of the lifter's own, alphabetically. */
const inGroup = (group: string, equipment: string[] = [], mine: string[] = []) =>
  [...grit.exercisesByMuscle(group, equipment).map((e) => e.name), ...mine].sort((a, b) => (key(a) < key(b) ? -1 : key(a) > key(b) ? 1 : 0))

/** Searches, and waits for the whole list it finds. */
async function search(text: string) {
  await userEvent.clear(searchBox())
  if (text !== '') await userEvent.type(searchBox(), text)
  await wholeList()
}

/** Taps a chip, a muscle tile or a button that changes the list, and waits for the whole of the new list. */
async function tapAndWait(target: Element) {
  await userEvent.click(target)
  await wholeList()
}

const own = (id: number, name: string, muscles: string[] = []): StoredExercise => ({ id, name, category: 'ACCESSORY', muscles, archived: false })

/** The picker on its own, over the seeded exercises (and `extra`), reporting what it was asked to do. */
async function showPicker(extra: StoredExercise[] = [], recentIds: number[] = []) {
  const picked: PickerEntry[] = []
  const created: CustomExercise[] = []
  const exercises = [...(await stored()), ...extra]
  render(<ExercisePicker title="Add exercise" exercises={exercises} recentIds={recentIds} onPick={(e) => picked.push(e)} onCreateCustom={(c) => created.push(c)} onDismiss={() => {}} />)
  await wholeList()
  return { picked, created }
}

/** The real Plan screen with the picker open for Monday. */
async function showPlanPicker() {
  render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} />)
  await addToMonday()
}

/** Taps Add on the sets and reps form that a pick on Plan's Add opens. */
async function confirmAdd() {
  const form = (await screen.findByRole('group', { name: 'Sets' })).closest<HTMLElement>('[role="dialog"]')!
  await userEvent.click(within(form).getByRole('button', { name: 'Add' }))
}

async function addToMonday() {
  await userEvent.click(within(await screen.findByRole('region', { name: 'Monday' })).getByRole('button', { name: 'Add' }))
  expect(screen.getByRole('dialog', { name: 'Add to Monday' })).toBeInTheDocument()
  await wholeList()
}

describe('ExercisePicker', () => {
  it('search_filtersAsYouType', async () => {
    await showPicker()
    expect(shows('Squat') && shows('Pull-up')).toBe(true)
    // The search box is typed in on a phone: 16 px text, or iOS zooms the page.
    expect(searchBox()).toHaveClass('gr-field-input')

    await search('pu')
    expect(shows('Pull-up')).toBe(true)
    expect(shows('Squat')).toBe(false)

    // Typed the way people type: no hyphen, any case, an alias.
    await search('pull up')
    expect(shows('Pull-up')).toBe(true)
    await search('PULL-UP')
    expect(shows('Pull-up')).toBe(true)
    await search('romanian')
    expect(shows('RDL')).toBe(true)
    expect(shows('Pull-up')).toBe(false)

    // Every row on screen is one the shared search returns, in its order.
    await search('squat')
    expect(libraryNames('squat').length).toBeGreaterThan(1)
    expect(shownNames()).toEqual(libraryNames('squat'))

    await tapAndWait(picker().getByRole('button', { name: 'Clear search' }))
    expect(searchBox()).toHaveValue('')
    expect(shows('Bench')).toBe(true)
  })

  it('areaAndEquipmentChips_filter', async () => {
    await showPicker()

    await tapAndWait(chip('Body area', 'Back'))
    expect(chip('Body area', 'Back')).toBeChecked()
    expect(chip('Body area', 'All')).not.toBeChecked()
    expect(shownNames()).toEqual(libraryNames('', ['BACK']))
    expect(shows('Row') && shows('Pull-up')).toBe(true)

    // Equipment narrows it; a second equipment chip widens it again (any of the chosen).
    await tapAndWait(chip('Equipment', 'Bodyweight'))
    expect(shows('Pull-up')).toBe(true)
    expect(shows('Row')).toBe(false)
    await tapAndWait(chip('Equipment', 'Barbell'))
    expect(chip('Equipment', 'Bodyweight')).toBeChecked()
    expect(chip('Equipment', 'Barbell')).toBeChecked()
    expect(shows('Pull-up') && shows('Row')).toBe(true)

    // One body area at a time.
    await tapAndWait(chip('Body area', 'Arms'))
    expect(chip('Body area', 'Back')).not.toBeChecked()
    const arms = libraryNames('', ['ARMS'], ['BODYWEIGHT', 'BARBELL'])
    expect(arms.length).toBeGreaterThan(0)
    expect(shownNames()).toEqual(arms)

    // "All" with no equipment brings everything back.
    await tapAndWait(chip('Body area', 'All'))
    await tapAndWait(chip('Equipment', 'Bodyweight'))
    await tapAndWait(chip('Equipment', 'Barbell'))
    expect(chip('Body area', 'All')).toBeChecked()
    expect(shows('Squat') && shows('Pull-up') && shows('Curl')).toBe(true)
  })

  it('chestThenBarbell_listsBench_withItsVariations', async () => {
    await showPicker()
    await tapAndWait(chip('Body area', 'Chest'))
    await tapAndWait(chip('Equipment', 'Barbell'))
    expect(shows('Bench')).toBe(true)
    expect(shows('Close-grip bench') && shows('Incline bench press')).toBe(true)
    expect(shows('Squat')).toBe(false)
    // In the domain's order, not under area headings.
    expect(shownNames()).toEqual(libraryNames('', ['CHEST'], ['BARBELL']))
  })

  it('everyAreaAndEquipment_hasAChip', async () => {
    await showPicker()
    for (const e of grit.library()) {
      expect(AREAS.map((a) => a.id)).toContain(e.area)
      for (const q of e.equipment) expect(EQUIPMENT.map((x) => x.id)).toContain(q)
    }
    for (const a of AREAS) expect(chip('Body area', a.label)).toBeInTheDocument()
    for (const e of EQUIPMENT) expect(chip('Equipment', e.label)).toBeInTheDocument()
  })

  it('emptyResult_showsMessage', async () => {
    const { picked, created } = await showPicker()

    await search('zercher carry')

    expect(picker().getByText(NO_MATCH)).toBeInTheDocument()
    expect(shownNames()).toEqual([])
    // The custom-exercise action comes with it, and starts from what was typed.
    await userEvent.click(picker().getByRole('button', { name: 'Create your own' }))
    const custom = within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    expect(custom.getByRole('textbox', { name: 'Name' })).toHaveValue('zercher carry')
    await userEvent.click(custom.getByRole('button', { name: 'Create' }))
    expect(created.map((c) => c.name)).toEqual(['zercher carry'])
    expect(picked).toEqual([])

    // Filters with nothing in them say the same.
    cleanup()
    await showPicker()
    await tapAndWait(chip('Body area', 'Back'))
    await tapAndWait(chip('Equipment', 'Kettlebell'))
    expect(libraryNames('', ['BACK'], ['KETTLEBELL'])).toEqual([])
    expect(picker().getByText(NO_MATCH)).toBeInTheDocument()
  })

  it('createYourOwn_isOfferedBelowTheList', async () => {
    const { created } = await showPicker()

    await userEvent.click(picker().getByRole('button', { name: 'Create your own' }))
    const custom = within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    await userEvent.type(custom.getByRole('textbox', { name: 'Name' }), '  Tyre flip ')
    await userEvent.click(custom.getByRole('button', { name: 'Create' }))

    expect(created).toEqual([{ name: 'Tyre flip', category: 'ACCESSORY', parentLift: null, muscles: [] }])
  })

  it('pickerInfo_opensGuideWithoutPicking', async () => {
    // On its own: the info button reports no pick.
    const { picked } = await showPicker()
    await checkInfoButtonOpensGuide('Squat')
    expect(picked).toEqual([])
    await userEvent.click(rowButton('Squat'))
    expect(names(picked)).toEqual(['Squat'])
  })

  it('pickerInfo_opensGuideWithoutPicking_inThePlan', async () => {
    // A library exercise with no row yet: reading its guide must not store one either.
    for (const row of await rows('Curl')) await db.exercises.delete(row.id!)
    const before = await db.exercises.toArray()
    await showPlanPicker()

    await checkInfoButtonOpensGuide('Curl')

    // The picker stays open.
    expect(searchBox()).toBeInTheDocument()
    expect(await storedPlan()).toEqual([])
    expect(await db.exercises.toArray()).toEqual(before)

    // Tapping the row itself, then Add on its form, is what adds it.
    await userEvent.click(rowButton('Curl'))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect((await storedPlan())[0]!.exerciseId).toBe((await rows('Curl'))[0]!.id)
    expect(await rows('Curl')).toHaveLength(1)
  })

  it('restoredBuiltIn_appearsOnce_andIsReused', async () => {
    // A restore replaces the table: "Curl" comes back as an ordinary row under whatever id the backup had.
    for (const row of await rows('Curl')) await db.exercises.delete(row.id!)
    const restored: ExerciseRow = { id: 77, name: 'Curl', category: 'ACCESSORY', parentLift: null, muscles: ['Biceps'], isBuiltIn: true, archived: false }
    await db.exercises.add(restored)
    // Recently used, so it is also one of "Your exercises".
    await logSet(db, await startSession(db, '2026-11-09'), set(77, 30, { reps: 10 }))

    // Browsing and searching both list it once, tied to the restored row.
    for (const query of ['', 'curl']) {
      const list = pickerList(query, null, [], await stored(), [77])
      const curls = [...list.yours, ...list.all].filter((e) => e.name.toLowerCase() === 'curl')
      expect(curls.map((e) => e.exerciseId)).toEqual([77])
      expect(curls[0]!.library?.name).toBe('Curl')
    }

    await showPlanPicker()
    expect(shownNames().filter((n) => n === 'Curl')).toHaveLength(1)
    await userEvent.click(rowButton('Curl'))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))

    expect((await storedPlan())[0]!.exerciseId).toBe(77)
    expect(await rows('Curl')).toEqual([restored])
  })

  it('yourExercises_listedFirst_whenNoQuery', async () => {
    const all = await stored()
    const bench = all.find((e) => e.name === 'Bench')!
    const sled = own(900, 'Tyre flip', ['Quads'])
    const archived = { ...own(901, 'Old lift'), archived: true }

    // Recently used first, then the lifter's own; none of them again under "All exercises".
    const list = pickerList('', null, [], [...all, sled, archived], [bench.id])
    expect(names(list.yours)).toEqual(['Bench', 'Tyre flip'])
    expect(list.yours.map((e) => e.exerciseId)).toEqual([bench.id, 900])
    expect(names(list.all)).toEqual(libraryNames().filter((n) => n !== 'Bench'))
    // A search or a filter drops the section.
    expect(pickerList('s', null, [], [...all, sled], [bench.id]).yours).toEqual([])
    expect(pickerList('', 'BACK', [], [...all, sled], [bench.id]).yours).toEqual([])
    // The lifter's own exercise is searched by name and by muscle, and listed before the catalogue.
    expect(names(pickerList('tyre', null, [], [...all, sled], []).all)).toEqual(['Tyre flip'])
    expect(pickerList('quads', null, [], [...all, sled], []).all[0]!.name).toBe('Tyre flip')
    // It has no body area or equipment, so a filter hides it.
    expect(names(pickerList('tyre', 'BACK', [], [...all, sled], []).all)).toEqual([])

    await showPicker([sled, archived], [bench.id])
    const yours = picker().getByRole('heading', { name: 'Your exercises' })
    const everything = picker().getByRole('heading', { name: 'All exercises' })
    const row = (name: string) => picker().getByRole('listitem', { name })
    const before = (a: HTMLElement, b: HTMLElement) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    expect(before(yours, row('Bench')) && before(row('Bench'), row('Tyre flip')) && before(row('Tyre flip'), everything) && before(everything, row('Squat'))).toBe(true)
    expect(shownNames().filter((n) => n === 'Bench')).toHaveLength(1)
    expect(shows('Old lift')).toBe(false)
    // The lifter's own exercise says so where the catalogue's say what they need.
    expect(row('Tyre flip')).toHaveTextContent('Your own')
    expect(row('Squat')).toHaveTextContent('Barbell')
    expect(row('Squat')).toHaveTextContent('Quads, Glutes')

    await search('s')
    expect(picker().queryByRole('heading', { name: 'Your exercises' })).toBeNull()
    expect(picker().queryByRole('heading', { name: 'All exercises' })).toBeNull()
  })

  it('muscleGrid_showsWhenNoQueryOrArea', async () => {
    const bench = (await stored()).find((e) => e.name === 'Bench')!
    await showPicker([], [bench.id])

    // Every group with an exercise has a tile, in the domain's order, above the lifter's own exercises.
    expect(heading('Browse by muscle')).toBeInTheDocument()
    const groups = grit.muscleGroups()
    expect(groups).toHaveLength(18)
    expect(within(grid()!).getAllByRole('button').map((b) => b.textContent)).toEqual(groups.map((g) => grit.muscleGroupLabel(g)))
    expect(within(grid()!).getAllByRole('button').map((b) => b.textContent)).toEqual([
      ...['Chest', 'Lats', 'Upper back', 'Traps', 'Lower back', 'Front delts', 'Side delts', 'Rear delts', 'Biceps'],
      ...['Triceps', 'Forearms', 'Quads', 'Hamstrings', 'Glutes', 'Adductors', 'Calves', 'Abs', 'Obliques'],
    ])
    const before = (a: Element, b: Element) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)
    expect(before(picker().getByRole('group', { name: 'Equipment' }), grid()!)).toBe(true)
    expect(before(grid()!, heading('Your exercises')!)).toBe(true)
    // The tiles are not rows: the list is still the exercises.
    expect(shownNames()).toHaveLength(libraryNames().length)

    // A search hides it; clearing the search brings it back.
    await search('s')
    expect(grid()).toBeNull()
    expect(heading('Browse by muscle')).toBeNull()
    await search('')
    expect(grid()).toBeInTheDocument()

    // So does a body area; "All" brings it back. Equipment leaves it where it is.
    await tapAndWait(chip('Body area', 'Back'))
    expect(grid()).toBeNull()
    await tapAndWait(chip('Body area', 'All'))
    expect(grid()).toBeInTheDocument()
    await tapAndWait(chip('Equipment', 'Dumbbell'))
    expect(grid()).toBeInTheDocument()
    expect(shownNames()).toEqual(libraryNames('', [], ['DUMBBELL']))
  })

  it('tapMuscle_listsItsExercises', async () => {
    const all = await stored()
    const bench = all.find((e) => e.name === 'Bench')!
    const mine = own(900, 'Tyre flip', ['pecs', 'Whole body'])
    const unmapped = own(901, 'Tyre drag', ['Whole body'])
    const archived = { ...own(902, 'Old press', ['Chest']), archived: true }
    const exercises = [...all, mine, unmapped, archived]

    // The catalogue's exercises and the lifter's own whose muscles are in the group, as one alphabetical list.
    const chest = pickerList('', null, [], exercises, [bench.id], 'CHEST')
    expect(chest.yours).toEqual([])
    expect(names(chest.all)).toEqual(inGroup('CHEST', [], ['Tyre flip']))
    expect(chest.all.find((e) => e.name === 'Bench')!.exerciseId).toBe(bench.id)
    expect(chest.all.find((e) => e.name === 'Tyre flip')!.exerciseId).toBe(900)
    // A muscle string with no group puts an exercise in none of them.
    for (const g of grit.muscleGroups()) {
      expect(names(pickerList('', null, [], exercises, [], g).all).filter((n) => n === 'Tyre drag' || n === 'Old press'), g).toEqual([])
    }
    // It is still found by search and under "Your exercises".
    expect(names(pickerList('tyre drag', null, [], exercises, []).all)).toEqual(['Tyre drag'])
    expect(names(pickerList('', null, [], exercises, []).yours)).toContain('Tyre drag')

    const { picked } = await showPicker([mine, unmapped, archived], [bench.id])
    await tapAndWait(tile('Chest'))
    expect(heading('Chest exercises')).toBeInTheDocument()
    expect(heading('Your exercises')).toBeNull()
    expect(heading('All exercises')).toBeNull()
    expect(grid()).toBeNull()
    expect(shownNames()).toEqual(names(chest.all))
    expect(shows('Tyre flip') && shows('Bench') && shows('DB press')).toBe(true)
    expect(shows('Tyre drag') || shows('Old press') || shows('Squat')).toBe(false)
    // Rows are the picker's rows.
    expect(picker().getByRole('listitem', { name: 'Bench' })).toHaveTextContent('Pecs, Triceps, Front delts')
    expect(picker().getByRole('listitem', { name: 'Bench' })).toHaveTextContent('Barbell')
    expect(picker().getByRole('listitem', { name: 'Tyre flip' })).toHaveTextContent('Your own')

    // A row picks, as anywhere else in the picker.
    await userEvent.click(rowButton('Bench'))
    expect(picked.map((e) => [e.name, e.exerciseId])).toEqual([['Bench', bench.id]])
  })

  it('muscleAndEquipment_combine', async () => {
    const mine = own(900, 'Tyre flip', ['Chest'])
    const exercises = [...(await stored()), mine]
    // The lifter's own exercises have no equipment, so a chosen equipment leaves them out, as it does everywhere.
    const dumbbell = names(pickerList('', null, ['DUMBBELL'], exercises, [], 'CHEST').all)
    expect(dumbbell).toEqual(inGroup('CHEST', ['DUMBBELL']))
    expect(dumbbell.includes('DB press') && !dumbbell.includes('Bench')).toBe(true)

    await showPicker([mine])
    await tapAndWait(tile('Chest'))
    await tapAndWait(chip('Equipment', 'Dumbbell'))
    expect(chip('Equipment', 'Dumbbell')).toBeChecked()
    expect(heading('Chest exercises')).toBeInTheDocument()
    expect(shownNames()).toEqual(dumbbell)

    // A second equipment widens it (any of the chosen); none brings the whole group back.
    await tapAndWait(chip('Equipment', 'Barbell'))
    expect(shownNames()).toEqual(inGroup('CHEST', ['DUMBBELL', 'BARBELL']))
    expect(shows('DB press') && shows('Bench') && !shows('Squat')).toBe(true)
    await tapAndWait(chip('Equipment', 'Dumbbell'))
    await tapAndWait(chip('Equipment', 'Barbell'))
    expect(shownNames()).toEqual(inGroup('CHEST', [], ['Tyre flip']))

    // Nothing left says so, and the way back stays. An equipment chosen before the muscle counts too.
    await tapAndWait(chip('Equipment', 'Specialty bar'))
    expect(grit.exercisesByMuscle('CHEST', ['SPECIALTY_BAR'])).toHaveLength(0)
    expect(picker().getByText(NO_MATCH)).toBeInTheDocument()
    await tapAndWait(allMuscles()!)
    await tapAndWait(tile('Upper back'))
    expect(inGroup('UPPER_BACK', ['SPECIALTY_BAR']).length).toBeGreaterThan(0)
    expect(shownNames()).toEqual(inGroup('UPPER_BACK', ['SPECIALTY_BAR']))
  })

  it('typing_leavesMuscleMode', async () => {
    await showPicker()
    await tapAndWait(tile('Chest'))
    expect(shows('Squat')).toBe(false)

    // The search is the ordinary one, over everything.
    await search('squat')
    expect(heading('Chest exercises')).toBeNull()
    expect(allMuscles()).toBeNull()
    expect(shownNames()).toEqual(libraryNames('squat'))

    // Clearing it is back at the grid, not in the muscle.
    await search('')
    expect(grid()).toBeInTheDocument()
    expect(heading('Chest exercises')).toBeNull()
    expect(shownNames()).toEqual(libraryNames())

    // A body area leaves the muscle too.
    await tapAndWait(tile('Chest'))
    await tapAndWait(chip('Body area', 'Back'))
    expect(heading('Chest exercises')).toBeNull()
    expect(shownNames()).toEqual(libraryNames('', ['BACK']))
  })

  it('backFromMuscle_returnsToGrid', async () => {
    const bench = (await stored()).find((e) => e.name === 'Bench')!
    await showPicker([], [bench.id])
    await tapAndWait(tile('Calves'))
    expect(grid()).toBeNull()
    expect(shows('Bench')).toBe(false)

    await tapAndWait(allMuscles()!)
    expect(grid()).toBeInTheDocument()
    expect(heading('Calves exercises')).toBeNull()
    expect(allMuscles()).toBeNull()
    expect(heading('Your exercises')).toBeInTheDocument()
    expect(shownNames()).toHaveLength(libraryNames().length)

    // Another muscle can be opened straight away.
    await tapAndWait(tile('Quads'))
    expect(heading('Quads exercises')).toBeInTheDocument()
    expect(shows('Squat')).toBe(true)
  })

  it('escapeFromMuscle_returnsToGrid_thenCloses', async () => {
    // Like Android's back button: Escape steps out of a muscle first, and only then closes the picker.
    const onDismiss = vi.fn()
    render(<ExercisePicker title="Add exercise" exercises={await stored()} recentIds={[]} onPick={() => {}} onCreateCustom={() => {}} onDismiss={onDismiss} />)
    await tapAndWait(tile('Calves'))
    expect(heading('Calves exercises')).toBeInTheDocument()

    await userEvent.keyboard('{Escape}')
    expect(grid()).toBeInTheDocument()
    expect(heading('Calves exercises')).toBeNull()
    expect(onDismiss).not.toHaveBeenCalled()

    await userEvent.keyboard('{Escape}')
    expect(onDismiss).toHaveBeenCalledTimes(1)
  })

  it('muscleInfo_opensGuideWithoutPicking', async () => {
    const { picked } = await showPicker()
    await tapAndWait(tile('Quads'))
    await checkInfoButtonOpensGuide('Squat')
    expect(picked).toEqual([])
    // The picker is where it was.
    expect(heading('Quads exercises')).toBeInTheDocument()
    await userEvent.click(rowButton('Squat'))
    expect(names(picked)).toEqual(['Squat'])
  })

  it('compoundDefaults3x5_accessory3x10', async () => {
    expect(grit.findInLibrary('Squat')!.kind).toBe('COMPOUND')
    expect(grit.findInLibrary('Curl')!.kind).toBe('ACCESSORY')
    await updateSettings(db, (s) => ({ ...s, defaultRpe: 7.5 }))
    await showPlanPicker()

    await search('squat')
    await userEvent.click(rowButton('Squat'))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))
    await addToMonday()
    await search('curl')
    await userEvent.click(rowButton('Curl'))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(2))

    const squat = (await rows('Squat'))[0]!
    const curl = (await rows('Curl'))[0]!
    expect(await storedPlan()).toEqual([
      { weekday: 1, exerciseId: squat.id, sets: 3, reps: 5, targetRpe: 7.5, order: 0, mode: 'POWERLIFTING' },
      { weekday: 1, exerciseId: curl.id, sets: 3, reps: 10, targetRpe: 7.5, order: 1, mode: 'POWERLIFTING' },
    ])
    const monday = screen.getByRole('region', { name: 'Monday' })
    await waitFor(() => expect(within(monday).getAllByRole('listitem').map((r) => r.getAttribute('aria-label'))).toEqual(['Squat  3 × 5 @ 7.5', 'Curl  3 × 10 @ 7.5']))
  })

  it('createCustom_withCatalogueName_usesTheCatalogueEntry', async () => {
    const before = await db.exercises.toArray()
    await showPlanPicker()

    // "curl" is in the catalogue as "Curl": making it again by hand must not add a second row.
    await userEvent.click(picker().getByRole('button', { name: 'Create your own' }))
    const custom = within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    await userEvent.type(custom.getByRole('textbox', { name: 'Name' }), 'curl')
    await userEvent.click(custom.getByRole('button', { name: 'Create' }))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))

    expect(await db.exercises.toArray()).toEqual(before)
    const curl = (await rows('Curl'))[0]!
    // Added as the catalogue's accessory: 3 × 10.
    expect(await storedPlan()).toEqual([{ weekday: 1, exerciseId: curl.id, sets: 3, reps: 10, targetRpe: 8, order: 0, mode: 'POWERLIFTING' }])

    // An alias counts too, and an entry with no row yet gets the catalogue's built-in row, not a custom one.
    for (const row of await rows('RDL')) await db.exercises.delete(row.id!)
    await addToMonday()
    await userEvent.click(picker().getByRole('button', { name: 'Create your own' }))
    const again = within(screen.getByRole('dialog', { name: 'Add custom exercise' }))
    await userEvent.type(again.getByRole('textbox', { name: 'Name' }), 'Romanian deadlift')
    await userEvent.click(again.getByRole('button', { name: 'Create' }))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(2))

    expect(await rows('Romanian deadlift')).toEqual([])
    const rdl = await rows('RDL')
    expect(rdl).toHaveLength(1)
    expect(rdl[0]!.isBuiltIn).toBe(true)
    expect((await storedPlan())[1]!.exerciseId).toBe(rdl[0]!.id)
  })

  it('pickerRow_doubleTap_addsOnce', async () => {
    // On its own, where nothing closes the picker between the two taps.
    const { picked } = await showPicker()
    await userEvent.dblClick(rowButton('Squat'))
    await userEvent.click(rowButton('Bench'))
    expect(names(picked)).toEqual(['Squat'])
    cleanup()

    // And in the plan: one item, however fast the thumb.
    const add = vi.spyOn(db.planItems, 'add')
    await showPlanPicker()
    await userEvent.dblClick(rowButton('Squat'))
    await confirmAdd()
    await waitFor(async () => expect(await storedPlan()).toHaveLength(1))
    await new Promise((r) => setTimeout(r, 100))
    expect(await storedPlan()).toHaveLength(1)
    expect(add).toHaveBeenCalledOnce()
  })
})
