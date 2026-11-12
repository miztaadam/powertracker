import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from '../../App'
import { updateSettings, type GritDb } from '../../db/db'
import { grit, todayIso } from '../../domain'
import { pickerList, type StoredExercise } from '../../state/exercisePicker'
import { useToday, weekdayOf, type TodayActions, type TodayState } from '../../state/useToday'
import { TODAY, exerciseNamed, plan, setsOn, startSession, todayFixture } from '../../test/todayFixture'
import { ExercisesScreen } from './ExercisesScreen'
import { FormGuideSheet } from './FormGuideSheet'
import { AREAS } from './labels'

// Ported from Android's ExercisesScreenTest, under the same names. Sizes and the 375 px layout are checked in e2e/exercises.spec.ts.

const SAVE_FAILED = "Couldn't save — try again."
const NO_GUIDE = 'No form guide for your own exercises.'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  window.location.hash = ''
  fx = todayFixture()
  db = await fx.open()
})
// Unmount before the database closes.
afterEach(async () => {
  cleanup()
  await fx.dispose()
  vi.restoreAllMocks()
})

type Session = TodayState & TodayActions

/** What the test can reach of the screen under test: the Today session it shares, and where its snackbar actions led. */
type Shown = { session: () => Session; onOpenPlan: ReturnType<typeof vi.fn>; onOpenToday: ReturnType<typeof vi.fn> }

/**
 * The tab on its own, sharing a Today session as the app's shell shares it. `stale` freezes what the
 * tab is told about the session at its first loaded state, while its actions stay the live ones.
 */
async function showTab({ stale = false }: { stale?: boolean } = {}): Promise<Shown> {
  let latest: Session | undefined
  let frozen: Session | undefined
  const onOpenPlan = vi.fn()
  const onOpenToday = vi.fn()
  function Harness() {
    const session = useToday(db, () => TODAY)
    latest = session
    if (stale && !session.loading) frozen ??= session
    return <ExercisesScreen db={db} session={stale && frozen ? { ...session, loading: frozen.loading, sessionId: frozen.sessionId, summary: frozen.summary } : session} today={() => TODAY} onOpenPlan={onOpenPlan} onOpenToday={onOpenToday} />
  }
  render(<Harness />)
  await waitFor(() => expect(latest?.loading).toBe(false))
  await screen.findByRole('listitem', { name: 'Squat' })
  return { session: () => latest!, onOpenPlan, onOpenToday }
}

/** The whole app on the Exercises tab, on the device's own date. */
async function showApp(hash = '#/exercises') {
  window.location.hash = hash
  render(<App dbName={fx.name} />)
  await screen.findByRole('navigation', { name: 'Main' })
}

const tabLink = (name: string) => within(screen.getByRole('navigation', { name: 'Main' })).getByRole('link', { name })

/** The names of the rows on screen, top to bottom. */
const shownNames = () => screen.queryAllByRole('listitem').map((row) => row.getAttribute('aria-label'))
const headings = () => screen.queryAllByRole('heading', { level: 2 }).map((h) => h.textContent)
const searchBox = () => screen.getByRole('textbox', { name: 'Search exercises' })
const chip = (group: 'Body area' | 'Equipment', name: string) => within(screen.getByRole('group', { name: group })).getByRole('checkbox', { name })
const musclesButton = () => screen.getByRole('button', { name: /^Muscles/ })
const musclesSheet = () => within(screen.getByRole('dialog', { name: 'Muscles' }))
const chosen = () => screen.queryByRole('group', { name: 'Chosen muscles' })
const chosenLabels = () => (chosen() ? within(chosen()!).getAllByRole('button', { name: /^Remove / }).map((b) => b.getAttribute('aria-label')!.replace('Remove ', '')) : [])
const snackbar = () => screen.getByRole('status')
const guide = (name: string) => within(screen.getByRole('dialog', { name: `Form guide for ${name}` }))
const guideIsOpen = () => screen.queryAllByRole('dialog', { name: /^Form guide for / }).length > 0
const dayChooser = () => within(screen.getByRole('dialog', { name: 'Add to plan' }))
const before = (a: Element, b: Element) => Boolean(a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING)

const label = (group: string) => grit.muscleGroupLabel(group)
const libraryNames = (query = '', areas: string[] = [], equipment: string[] = []) => grit.searchLibrary(query, areas, equipment).map((e) => e.name)
const inGroups = (groups: string[], equipment: string[] = []) => grit.exercisesByMuscles(groups, equipment).map((e) => e.name)
/** A name as the domain sorts it. */
const key = (name: string) => name.toLowerCase().replace(/[-_]/g, ' ')
const sorted = (names: string[]) => [...names].sort((a, b) => (key(a) < key(b) ? -1 : key(a) > key(b) ? 1 : 0))

async function search(text: string) {
  await userEvent.clear(searchBox())
  if (text !== '') await userEvent.type(searchBox(), text)
}

/** Ticks (or unticks) muscle groups in the drop-down, then closes it. */
async function tick(...groups: string[]) {
  await userEvent.click(musclesButton())
  for (const group of groups) await userEvent.click(musclesSheet().getByRole('checkbox', { name: label(group) }))
  await userEvent.click(musclesSheet().getByRole('button', { name: 'Close' }))
}

const openRow = (name: string) => userEvent.click(screen.getByRole('button', { name: `Open ${name}` }))

/** Opens the exercise's sheet, taps "Add to plan" and picks the day. */
async function addToPlan(name: string, day: string) {
  await openRow(name)
  await userEvent.click(guide(name).getByRole('button', { name: 'Add to plan' }))
  await userEvent.click(dayChooser().getByRole('button', { name: day }))
}

async function addToToday(name: string) {
  await openRow(name)
  await userEvent.click(await guide(name).findByRole('button', { name: 'Add to today' }))
}

/** Lets whatever a tap set off run to its end, database writes included. */
const settled = () => act(() => new Promise<void>((done) => setTimeout(done, 100)))

const storedPlan = async () => (await db.planItems.toArray()).map(({ id: _id, ...rest }) => rest)
const rows = async (name: string) => (await db.exercises.toArray()).filter((e) => e.name.toLowerCase() === name.toLowerCase())
const own = (name: string, muscles: string[] = []) => db.exercises.add({ name, category: 'ACCESSORY', parentLift: null, muscles, isBuiltIn: false, archived: false }) as Promise<number>
const sessionNames = (shown: Shown) => shown.session().items.map((i) => i.exercise.name)

describe('Exercises tab', () => {
  it('exercisesTab_isTheFourthOfFiveTabs_andFits', async () => {
    await showApp('#/today')
    const links = within(screen.getByRole('navigation', { name: 'Main' })).getAllByRole('link')
    expect(links.map((a) => a.textContent)).toEqual(['Today', 'Coach', 'Progress', 'Exercises', 'Plan'])
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['#/today', '#/coach', '#/progress', '#/exercises', '#/plan'])
    // Five different icons: the Exercises tab is not a second dumbbell.
    expect(new Set(links.map((a) => a.querySelector('svg')!.innerHTML)).size).toBe(5)

    await userEvent.click(tabLink('Exercises'))
    expect(window.location.hash).toBe('#/exercises')
    expect(screen.getByRole('heading', { level: 1, name: 'Exercises' })).toBeInTheDocument()
    expect(tabLink('Exercises')).toHaveAttribute('aria-current', 'page')
    expect(tabLink('Today')).not.toHaveAttribute('aria-current')
  })

  it('exercisesTab_browseByMuscleAndSearchWork', async () => {
    await own('Tyre flip', ['Quads'])
    await showTab()

    // The catalogue and the lifter's own that it does not have.
    expect(grit.library()).toHaveLength(225)
    expect(screen.getByText('226 exercises')).toBeInTheDocument()
    // Nothing ticked: the lifter's own first, then everything under its body area, each exercise once.
    expect(headings()).toEqual(['Your exercises', ...AREAS.map((a) => a.label)])
    expect(sorted(shownNames() as string[])).toEqual(sorted([...libraryNames(), 'Tyre flip']))
    const row = (name: string) => screen.getByRole('listitem', { name })
    const heading = (name: string) => screen.getByRole('heading', { level: 2, name })
    expect(before(heading('Your exercises'), row('Tyre flip')) && before(row('Tyre flip'), heading('Main lifts'))).toBe(true)
    expect(before(heading('Main lifts'), row('Squat')) && before(row('Squat'), heading('Chest'))).toBe(true)
    expect(before(heading('Calves'), row('Standing calf raise')) && before(row('Standing calf raise'), heading('Core'))).toBe(true)
    // The tab has the drop-down, not the picker's tile grid.
    expect(screen.queryByRole('group', { name: 'Browse by muscle' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Create your own' })).toBeNull()

    // A search is ranked, not grouped.
    await search('calf')
    expect(shownNames()).toEqual(libraryNames('calf'))
    expect(headings()).toEqual([])
    await search('zzzz')
    expect(screen.getByRole('heading', { name: 'No exercises match.' })).toBeInTheDocument()
    expect(screen.getByText('Try fewer filters.')).toBeInTheDocument()
    await search('')

    // A body area keeps its heading; the lifter's own have no area.
    await userEvent.click(chip('Body area', 'Calves'))
    expect(shownNames()).toEqual(libraryNames('', ['CALVES']))
    expect(headings()).toEqual(['Calves'])
    await userEvent.click(chip('Body area', 'All'))

    // One muscle ticked: its exercises, the lifter's own among them, alphabetically.
    await tick('QUADS')
    expect(shownNames()).toEqual(sorted([...inGroups(['QUADS']), 'Tyre flip']))
    expect(headings()).toEqual([])
  })

  it('musclesDropdown_multiSelect_showsAnyOf', async () => {
    await showTab()
    expect(musclesButton()).toHaveTextContent(/^Muscles$/)

    await userEvent.click(musclesButton())
    // All 18 groups, in the domain's order.
    const options = musclesSheet().getAllByRole('checkbox')
    expect(options.map((o) => o.closest('label')!.textContent)).toEqual(grit.muscleGroups().map(label))
    expect(options).toHaveLength(18)
    expect(options.every((o) => !(o as HTMLInputElement).checked)).toBe(true)

    // Ticking does not close the sheet: any number can be chosen.
    await userEvent.click(musclesSheet().getByRole('checkbox', { name: 'Forearms' }))
    await userEvent.click(musclesSheet().getByRole('checkbox', { name: 'Calves' }))
    expect(musclesSheet().getByRole('checkbox', { name: 'Forearms' })).toBeChecked()
    expect(musclesSheet().getByRole('checkbox', { name: 'Calves' })).toBeChecked()
    await userEvent.click(musclesSheet().getByRole('button', { name: 'Close' }))
    expect(screen.queryByRole('dialog', { name: 'Muscles' })).toBeNull()

    // Every exercise that works either, each once, alphabetically.
    const either = inGroups(['CALVES', 'FOREARMS'])
    expect(shownNames()).toEqual(either)
    expect(either).toEqual(sorted([...new Set([...inGroups(['CALVES']), ...inGroups(['FOREARMS'])])]))
    expect(either).toContain('Seated calf raise')
    expect(either).toContain('Wrist curl')
    expect(musclesButton()).toHaveTextContent('Muscles · 2')
  })

  it('musclesDropdown_combinesWithEquipmentAndSearch', async () => {
    const mine = await own('Tyre flip', ['Calves'])
    const stored = (await db.exercises.toArray()) as StoredExercise[]
    const names = (list: ReturnType<typeof pickerList>) => list.all.map((e) => e.name)
    // The list itself: any ticked muscle, any ticked equipment, the search and the body area on top.
    expect(names(pickerList('', null, [], stored, [], null, ['CALVES', 'QUADS']))).toEqual(sorted([...inGroups(['CALVES', 'QUADS']), 'Tyre flip']))
    expect(pickerList('', null, [], stored, [mine], null, ['CALVES']).yours).toEqual([])
    expect(names(pickerList('', null, ['DUMBBELL', 'SMITH'], stored, [], null, ['CALVES', 'QUADS']))).toEqual(inGroups(['CALVES', 'QUADS'], ['DUMBBELL', 'SMITH']))
    expect(names(pickerList('', 'CALVES', [], stored, [], null, ['CALVES', 'QUADS']))).toEqual(inGroups(['CALVES', 'QUADS']).filter((n) => grit.findInLibrary(n)!.area === 'CALVES'))
    expect(names(pickerList('raise', null, ['DUMBBELL'], stored, [], null, ['CALVES']))).toEqual(libraryNames('raise', [], ['DUMBBELL']).filter((n) => inGroups(['CALVES']).includes(n)))
    expect(names(pickerList('tyre', null, [], stored, [], null, ['CALVES']))).toEqual(['Tyre flip'])
    expect(names(pickerList('tyre', null, [], stored, [], null, ['QUADS']))).toEqual([])

    await showTab()
    await tick('CALVES', 'QUADS')
    expect(shownNames()).toEqual(sorted([...inGroups(['CALVES', 'QUADS']), 'Tyre flip']))

    // Equipment narrows it, any of the chosen; the lifter's own have none.
    await userEvent.click(chip('Equipment', 'Dumbbell'))
    expect(shownNames()).toEqual(inGroups(['CALVES', 'QUADS'], ['DUMBBELL']))
    await userEvent.click(chip('Equipment', 'Smith machine'))
    const both = inGroups(['CALVES', 'QUADS'], ['DUMBBELL', 'SMITH'])
    expect(shownNames()).toEqual(both)
    expect(both).toContain('Smith calf raise')
    expect(both).toContain('Goblet squat')
    expect(both).not.toContain('Standing calf raise')

    // A search stays inside the ticked muscles and the equipment, ranked as a search is.
    await search('raise')
    const raises = libraryNames('raise', [], ['DUMBBELL', 'SMITH']).filter((n) => both.includes(n))
    expect(shownNames()).toEqual(raises)
    expect(raises).toContain('Dumbbell calf raise')
    expect(libraryNames('raise', [], ['DUMBBELL', 'SMITH']).length).toBeGreaterThan(raises.length)
  })

  it('musclesDropdown_chipsRemoveAndClear', async () => {
    await showTab()
    expect(chosen()).toBeNull()

    // Ticked in any order, shown in the domain's order.
    await tick('QUADS', 'BICEPS', 'CALVES')
    expect(chosenLabels()).toEqual(['Biceps', 'Quads', 'Calves'])
    expect(shownNames()).toEqual(inGroups(['QUADS', 'BICEPS', 'CALVES']))

    // A chip takes its own muscle away, in the list and in the drop-down.
    await userEvent.click(within(chosen()!).getByRole('button', { name: 'Remove Quads' }))
    expect(chosenLabels()).toEqual(['Biceps', 'Calves'])
    expect(shownNames()).toEqual(inGroups(['BICEPS', 'CALVES']))
    expect(musclesButton()).toHaveTextContent('Muscles · 2')
    await userEvent.click(musclesButton())
    expect(musclesSheet().getByRole('checkbox', { name: 'Quads' })).not.toBeChecked()
    expect(musclesSheet().getByRole('checkbox', { name: 'Biceps' })).toBeChecked()
    await userEvent.click(musclesSheet().getByRole('button', { name: 'Close' }))

    // "Clear" takes them all away: back to everything under its body area.
    await userEvent.click(within(chosen()!).getByRole('button', { name: 'Clear' }))
    expect(chosen()).toBeNull()
    expect(musclesButton()).toHaveTextContent(/^Muscles$/)
    expect(headings()).toEqual(AREAS.map((a) => a.label))
    expect(shownNames()).toHaveLength(225)
  })

  it('tapRow_opensGuideWithActions', async () => {
    await showTab()
    const entry = grit.findInLibrary('Hack squat')!
    const stored = await db.exercises.toArray()

    await openRow('Hack squat')
    expect(guide('Hack squat').getByRole('heading', { level: 2, name: 'Hack squat' })).toBeInTheDocument()
    expect(guide('Hack squat').getByText(entry.guide.purpose)).toBeInTheDocument()
    // The actions sit below the safety line. No session is open: only the plan is offered.
    const safety = guide('Hack squat').getByText(grit.safetyLine())
    expect(before(safety, guide('Hack squat').getByRole('button', { name: 'Add to plan' }))).toBe(true)
    expect(guide('Hack squat').queryByRole('button', { name: 'Add to today' })).toBeNull()
    await userEvent.click(guide('Hack squat').getByRole('button', { name: 'Close guide' }))
    expect(guideIsOpen()).toBe(false)

    // The info button opens the same sheet.
    await userEvent.click(screen.getByRole('button', { name: 'Form guide for Hack squat' }))
    expect(guide('Hack squat').getByText(entry.guide.steps[0]!)).toBeInTheDocument()
    expect(guide('Hack squat').getByRole('button', { name: 'Add to plan' })).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(guideIsOpen()).toBe(false)

    // Reading changes nothing.
    expect(await db.exercises.toArray()).toEqual(stored)
    expect(await db.planItems.count()).toBe(0)

    // Anywhere else the guide is only a guide.
    cleanup()
    render(<FormGuideSheet name="Hack squat" onClose={() => {}} />)
    expect(guide('Hack squat').getByText(entry.guide.purpose)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add to plan' })).toBeNull()
  })

  it('addToPlan_choosesDay_andUsesDefaults', async () => {
    await updateSettings(db, (s) => ({ ...s, trainingDays: [5, 1, 3], defaultRpe: 7.5 }))
    const shown = await showTab()
    const count = await db.exercises.count()

    // The lifter's training days, in weekday order.
    await openRow('Hack squat')
    await userEvent.click(guide('Hack squat').getByRole('button', { name: 'Add to plan' }))
    expect(dayChooser().getByText('Which day?')).toBeInTheDocument()
    expect(dayChooser().getByText('Hack squat')).toBeInTheDocument()
    const days = () => dayChooser().getAllByRole('button').map((b) => b.textContent).filter((text) => text !== '')
    expect(days()).toEqual(['Monday', 'Wednesday', 'Friday'])
    await userEvent.click(dayChooser().getByRole('button', { name: 'Wednesday' }))

    // A compound lift joins at 3 × 5 and the default RPE; its row is made with the plan item.
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to Wednesday'))
    expect(screen.queryByRole('dialog')).toBeNull()
    const hack = await rows('Hack squat')
    expect(hack).toMatchObject([{ name: 'Hack squat', isBuiltIn: true, archived: false }])
    expect(await db.exercises.count()).toBe(count + 1)
    expect(await storedPlan()).toEqual([{ weekday: 3, exerciseId: hack[0]!.id, sets: 3, reps: 5, targetRpe: 7.5, order: 0 }])
    await userEvent.click(within(snackbar()).getByRole('button', { name: 'Open Plan' }))
    expect(shown.onOpenPlan).toHaveBeenCalledOnce()

    // An accessory joins at 3 × 10, after what the day already has.
    await addToPlan('Face pull', 'Wednesday')
    await waitFor(() => expect(db.planItems.count()).resolves.toBe(2))
    const facePull = await rows('Face pull')
    expect((await storedPlan())[1]).toEqual({ weekday: 3, exerciseId: facePull[0]!.id, sets: 3, reps: 10, targetRpe: 7.5, order: 1 })

    // No training days set: the whole week is offered.
    await updateSettings(db, (s) => ({ ...s, trainingDays: [] }))
    await openRow('Leg press')
    await userEvent.click(guide('Leg press').getByRole('button', { name: 'Add to plan' }))
    await waitFor(() => expect(days()).toEqual(['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']))
    // Closing the chooser closes the sheet, with nothing added.
    await userEvent.click(dayChooser().getByRole('button', { name: 'Close' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await db.planItems.count()).toBe(2)
  })

  it('addToPlan_alreadyOnDay_saysSo', async () => {
    const squat = await exerciseNamed(db, 'Squat')
    await plan(db, squat.id, { weekday: 1 })
    const stored = await storedPlan()
    const shown = await showTab()

    await addToPlan('Squat', 'Monday')
    await waitFor(() => expect(snackbar()).toHaveTextContent('Already in your plan on Monday.'))
    // Nothing to open: nothing was added.
    expect(within(snackbar()).queryByRole('button')).toBeNull()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await storedPlan()).toEqual(stored)

    // Another day is another matter.
    await addToPlan('Squat', 'Wednesday')
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to Wednesday'))
    expect(await storedPlan()).toEqual([...stored, { weekday: 3, exerciseId: squat.id, sets: 3, reps: 5, targetRpe: 8, order: 0 }])
    expect(await rows('Squat')).toHaveLength(1)
    expect(shown.onOpenPlan).not.toHaveBeenCalled()
  })

  it('addToToday_onlyDuringOpenSession', async () => {
    const squat = await exerciseNamed(db, 'Squat')
    await plan(db, squat.id, { weekday: weekdayOf(TODAY) })
    const shown = await showTab()

    // No session yet: only the plan is offered.
    await openRow('Hack squat')
    expect(guide('Hack squat').getByRole('button', { name: 'Add to plan' })).toBeInTheDocument()
    expect(guide('Hack squat').queryByRole('button', { name: 'Add to today' })).toBeNull()

    // The session starts: the open sheet offers it too.
    await act(() => shown.session().startSession(null, null))
    await userEvent.click(await guide('Hack squat').findByRole('button', { name: 'Add to today' }))
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to today'))
    expect(guideIsOpen()).toBe(false)
    expect(sessionNames(shown)).toEqual(['Squat', 'Hack squat'])
    await userEvent.click(within(snackbar()).getByRole('button', { name: 'Open Today' }))
    expect(shown.onOpenToday).toHaveBeenCalledOnce()

    // Already there, added or planned: said, and left alone.
    await addToToday('Hack squat')
    await waitFor(() => expect(snackbar()).toHaveTextContent("Already in today's session."))
    expect(within(snackbar()).queryByRole('button')).toBeNull()
    await addToToday('Squat')
    await waitFor(() => expect(guideIsOpen()).toBe(false))
    expect(snackbar()).toHaveTextContent("Already in today's session.")
    expect(sessionNames(shown)).toEqual(['Squat', 'Hack squat'])

    // Finished: the button goes, also from a sheet that is open.
    await openRow('Leg press')
    expect(guide('Leg press').getByRole('button', { name: 'Add to today' })).toBeInTheDocument()
    await act(() => shown.session().finishSession())
    expect(shown.session().summary).not.toBeNull()
    expect(guide('Leg press').queryByRole('button', { name: 'Add to today' })).toBeNull()
    expect(guide('Leg press').getByRole('button', { name: 'Add to plan' })).toBeInTheDocument()
  })

  it('addToToday_createsNoRowUntilFirstSet', async () => {
    await startSession(db, todayIso())
    const stored = await db.exercises.toArray()
    await showApp()

    await openRow('Hack squat')
    await userEvent.click(await guide('Hack squat').findByRole('button', { name: 'Add to today' }))
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to today'))
    // On the session, not in the data.
    expect(await db.exercises.toArray()).toEqual(stored)

    // "Open Today" leads to its card on the Today tab: the two tabs share one session.
    await userEvent.click(within(snackbar()).getByRole('button', { name: 'Open Today' }))
    expect(window.location.hash).toBe('#/today')
    expect(await screen.findByRole('heading', { name: 'Hack squat' })).toBeInTheDocument()
    expect(await db.exercises.toArray()).toEqual(stored)

    // The first set brings the row with it.
    const weight = screen.getByRole('textbox', { name: 'Weight, kilograms' })
    await userEvent.clear(weight)
    await userEvent.type(weight, '80')
    await userEvent.keyboard('{Enter}')
    await userEvent.click(screen.getByRole('button', { name: 'Log set 1' }))
    await waitFor(async () => expect(await setsOn(db, todayIso())).toHaveLength(1))
    const hack = await rows('Hack squat')
    expect(hack).toMatchObject([{ name: 'Hack squat', isBuiltIn: true }])
    expect(await db.exercises.count()).toBe(stored.length + 1)
    expect(await setsOn(db, todayIso())).toMatchObject([{ exerciseId: hack[0]!.id, weightKg: 80 }])
  })

  it('addToToday_afterFinish_doesNotAdd', async () => {
    await startSession(db, TODAY)
    // The tab still shows what it drew while the session was open, as a tap already on its way would find it.
    const shown = await showTab({ stale: true })
    const stored = await db.exercises.toArray()

    await openRow('Hack squat')
    const staleButton = guide('Hack squat').getByRole('button', { name: 'Add to today' })
    await act(() => shown.session().finishSession())
    expect(shown.session().summary).not.toBeNull()

    // The tap lands after Finish: nothing is added, and the lifter is told.
    await userEvent.click(staleButton)
    await waitFor(() => expect(snackbar()).toHaveTextContent(SAVE_FAILED))
    expect(within(snackbar()).queryByRole('button')).toBeNull()
    expect(guideIsOpen()).toBe(false)
    expect(sessionNames(shown)).toEqual([])
    // Asked directly, the session refuses too.
    await act(async () => expect(await shown.session().addToOpenSession('Hack squat', null)).toBe('REFUSED'))

    // Nothing turns up once the lifter goes back to logging.
    act(() => shown.session().dismissSummary())
    await waitFor(() => expect(shown.session().summary).toBeNull())
    expect(sessionNames(shown)).toEqual([])
    expect(await db.exercises.toArray()).toEqual(stored)
    expect(await setsOn(db, TODAY)).toEqual([])
  })

  it('addToToday_withoutASession_refuses', async () => {
    const shown = await showTab()
    await act(async () => expect(await shown.session().addToOpenSession('Hack squat', null)).toBe('REFUSED'))
    // A name the catalogue does not know, and an id that is nobody's, are refused as well.
    await act(() => shown.session().startSession(null, null))
    await act(async () => expect(await shown.session().addToOpenSession('No such lift', null)).toBe('REFUSED'))
    await act(async () => expect(await shown.session().addToOpenSession(null, 98765)).toBe('REFUSED'))
    expect(sessionNames(shown)).toEqual([])
  })

  it('actions_doubleTap_addOnce', async () => {
    const shown = await showTab()

    // Two taps on the day before the first has answered.
    await openRow('Hack squat')
    await userEvent.click(guide('Hack squat').getByRole('button', { name: 'Add to plan' }))
    const monday = dayChooser().getByRole('button', { name: 'Monday' })
    fireEvent.click(monday)
    fireEvent.click(monday)
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to Monday'))
    // The second tap has had its time: it added nothing and said nothing ("Already in your plan").
    await settled()
    expect(snackbar()).toHaveTextContent('Added to Monday')
    expect(await storedPlan()).toMatchObject([{ weekday: 1, sets: 3, reps: 5, order: 0 }])
    expect(await rows('Hack squat')).toHaveLength(1)

    // And on "Add to today": added once, and not told it was already there.
    await act(() => shown.session().startSession(null, null))
    await openRow('Leg press')
    const today = await guide('Leg press').findByRole('button', { name: 'Add to today' })
    fireEvent.click(today)
    fireEvent.click(today)
    await waitFor(() => expect(guideIsOpen()).toBe(false))
    await settled()
    expect(snackbar()).toHaveTextContent('Added to today')
    expect(sessionNames(shown)).toEqual(['Leg press'])
  })

  it('failedWrite_showsMessage_andChangesNothing', async () => {
    const shown = await showTab()
    const stored = await db.exercises.toArray()

    // The plan item cannot be stored, after the catalogue row was made for it in the same transaction.
    const failing = vi.spyOn(db.planItems, 'add').mockImplementation(() => {
      throw new Error('disk full')
    })
    await addToPlan('Hack squat', 'Monday')
    await waitFor(() => expect(snackbar()).toHaveTextContent(SAVE_FAILED))
    expect(failing).toHaveBeenCalledOnce()
    expect(within(snackbar()).queryByRole('button')).toBeNull()
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(await db.planItems.count()).toBe(0)
    expect(await db.exercises.toArray()).toEqual(stored)

    // The next attempt is not swallowed by the guard.
    failing.mockRestore()
    await addToPlan('Hack squat', 'Monday')
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to Monday'))
    expect(await db.planItems.count()).toBe(1)

    // The same for today: the session cannot be read when the tap lands.
    await act(() => shown.session().startSession(null, null))
    vi.spyOn(db.sessions, 'get').mockRejectedValue(new Error('gone'))
    await addToToday('Leg press')
    await waitFor(() => expect(snackbar()).toHaveTextContent(SAVE_FAILED))
    expect(guideIsOpen()).toBe(false)
    expect(sessionNames(shown)).toEqual([])
  })

  it('customExercise_showsNoGuideText_withActions', async () => {
    const id = await own('Tyre flip', ['Quads'])
    await startSession(db, TODAY)
    const shown = await showTab()
    const count = await db.exercises.count()

    // Under "Your exercises", marked as the lifter's own.
    const row = screen.getByRole('listitem', { name: 'Tyre flip' })
    expect(before(screen.getByRole('heading', { name: 'Your exercises' }), row) && before(row, screen.getByRole('heading', { name: 'Main lifts' }))).toBe(true)
    expect(row).toHaveTextContent('Your own')

    await openRow('Tyre flip')
    expect(guide('Tyre flip').getByText(NO_GUIDE)).toBeInTheDocument()
    expect(guide('Tyre flip').getByText(grit.safetyLine())).toBeInTheDocument()
    expect(guide('Tyre flip').getByRole('button', { name: 'Add to today' })).toBeInTheDocument()
    await userEvent.click(guide('Tyre flip').getByRole('button', { name: 'Add to plan' }))
    await userEvent.click(dayChooser().getByRole('button', { name: 'Friday' }))
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to Friday'))
    // Its own row, with an accessory's defaults; no row is made for it.
    expect(await storedPlan()).toEqual([{ weekday: 5, exerciseId: id, sets: 3, reps: 10, targetRpe: 8, order: 0 }])
    expect(await db.exercises.count()).toBe(count)

    await addToToday('Tyre flip')
    await waitFor(() => expect(snackbar()).toHaveTextContent('Added to today'))
    expect(shown.session().items.map((i) => i.exercise.id)).toEqual([id])
    expect(await db.exercises.count()).toBe(count)
  })

  it('tabState_survivesSwitchingTabs', async () => {
    await showApp()
    await screen.findByRole('listitem', { name: 'Squat' })

    await tick('BICEPS', 'FOREARMS')
    await userEvent.click(chip('Body area', 'Arms'))
    await userEvent.click(chip('Equipment', 'Dumbbell'))
    await search('curl')
    const names = shownNames()
    expect(names).toEqual(libraryNames('curl', ['ARMS'], ['DUMBBELL']).filter((n) => inGroups(['BICEPS', 'FOREARMS']).includes(n)))
    expect(names.length).toBeGreaterThan(1)

    const kept = () => {
      expect(searchBox()).toHaveValue('curl')
      expect(chip('Body area', 'Arms')).toBeChecked()
      expect(chip('Equipment', 'Dumbbell')).toBeChecked()
      expect(chip('Equipment', 'Barbell')).not.toBeChecked()
      expect(chosenLabels()).toEqual(['Biceps', 'Forearms'])
      expect(musclesButton()).toHaveTextContent('Muscles · 2')
      expect(shownNames()).toEqual(names)
    }
    // To Plan and back, then to Today and back.
    for (const other of ['Plan', 'Today']) {
      await userEvent.click(tabLink(other))
      expect(screen.getByRole('heading', { level: 1, name: other })).toBeInTheDocument()
      await userEvent.click(tabLink('Exercises'))
      await screen.findByRole('listitem', { name: names[0]! })
      kept()
    }

    // It is kept in memory only: a reload starts the tab afresh.
    cleanup()
    await showApp()
    await screen.findByRole('listitem', { name: 'Squat' })
    expect(searchBox()).toHaveValue('')
    expect(chip('Body area', 'All')).toBeChecked()
    expect(chip('Equipment', 'Dumbbell')).not.toBeChecked()
    expect(chosen()).toBeNull()
    expect(shownNames()).toHaveLength(225)
  })
})
