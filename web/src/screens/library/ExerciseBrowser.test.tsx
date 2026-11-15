import { Profiler } from 'react'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { pickerList, useExerciseBrowser, type StoredExercise } from '../../state/exercisePicker'
import { ExerciseBrowser, FIRST_ROWS } from './ExerciseBrowser'
import { AREAS } from './labels'

// How the list is drawn: the first screenful at once, the rest straight after. What the rows say and where they lead
// is checked in ExercisePicker.test.tsx and ExercisesScreen.test.tsx.

const own = (id: number, name: string): StoredExercise => ({ id, name, category: 'ACCESSORY', muscles: ['Quads'], archived: false })

/** The rows on screen, top to bottom. */
const shownNames = () => screen.queryAllByRole('listitem').map((row) => row.getAttribute('aria-label'))

/** The whole list in the order it is shown: the lifter's own first, then the rest, under body areas where `byArea`. */
function expected(exercises: readonly StoredExercise[], query = '', byArea = false) {
  const list = pickerList(query, null, [], exercises, [])
  const all = byArea ? [...list.all.filter((e) => e.library == null), ...AREAS.flatMap((a) => list.all.filter((e) => e.library?.area === a.id))] : list.all
  return [...list.yours, ...all].map((e) => e.name)
}

/**
 * The browser as the picker (`grid`) or the Exercises tab (`dropdown`) has it. Each commit's row count is kept, read
 * from the page as React finishes drawing it.
 */
function show(muscleFilter: 'grid' | 'dropdown', exercises: readonly StoredExercise[] = [], create?: string) {
  const drawn: number[] = []
  function Harness() {
    const browser = useExerciseBrowser()
    return (
      <Profiler id="browser" onRender={() => drawn.push(document.querySelectorAll('.lib-row').length)}>
        <ExerciseBrowser state={browser} muscleFilter={muscleFilter} exercises={exercises} recentIds={[]} onRow={() => {}} create={create != null ? <button type="button">{create}</button> : undefined} />
      </Profiler>
    )
  }
  render(<Harness />)
  return drawn
}

const type = (text: string) => fireEvent.change(screen.getByRole('textbox', { name: 'Search exercises' }), { target: { value: text } })
/** Long enough for any draw still to come. */
const aWhile = () => new Promise<void>((done) => setTimeout(done, 100))

describe('ExerciseBrowser drawing', () => {
  it('firstRows_isAScreenful', () => {
    expect(FIRST_ROWS).toBe(24)
  })

  it('theFirstDraw_isTheFirst24RowsInOrder_yoursFirst_andTheRestFollow', async () => {
    const mine = [own(1, 'Tyre flip'), own(2, 'Keg carry'), own(3, 'Atlas stone')]
    const full = expected(mine)
    expect(full.length).toBeGreaterThan(FIRST_ROWS)
    const drawn = show('grid', mine)

    // The three of the lifter's own count towards the 24.
    expect(shownNames()).toEqual(full.slice(0, FIRST_ROWS))
    expect(new Set(shownNames().slice(0, 3))).toEqual(new Set(['Atlas stone', 'Keg carry', 'Tyre flip']))
    await waitFor(() => expect(shownNames()).toEqual(full))
    // Two draws: the first screenful, then the whole list.
    expect(drawn).toEqual([FIRST_ROWS, full.length])
  })

  it('onTheTab_theFirst24AreInTheOrderShown_acrossTheBodyAreas', async () => {
    const full = expected([], '', true)
    show('dropdown')

    expect(shownNames()).toEqual(full.slice(0, FIRST_ROWS))
    await waitFor(() => expect(shownNames()).toEqual(full))
    expect(screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)).toEqual(AREAS.map((a) => a.label))
  })

  it('aListOf24OrFewer_isDrawnOnce', async () => {
    const tyres = Array.from({ length: FIRST_ROWS }, (_, i) => own(i + 1, `Tyre ${String(i + 1).padStart(2, '0')}`))
    const drawn = show('grid', tyres)
    // Opening draws the 24 own and the catalogue's first screenful; the search leaves the 24 alone.
    const atSearch = drawn.length
    type('tyre')
    expect(expected(tyres, 'tyre')).toHaveLength(FIRST_ROWS)
    expect(shownNames()).toEqual(expected(tyres, 'tyre'))
    await aWhile()
    expect(drawn.slice(atSearch)).toEqual([FIRST_ROWS])
    expect(shownNames()).toEqual(expected(tyres, 'tyre'))
  })

  it('aListOf25_isDrawnTwice', async () => {
    const tyres = Array.from({ length: FIRST_ROWS + 1 }, (_, i) => own(i + 1, `Tyre ${String(i + 1).padStart(2, '0')}`))
    const drawn = show('grid', tyres)
    await waitFor(() => expect(shownNames()).toEqual(expected(tyres)))
    const atSearch = drawn.length
    type('tyre')
    expect(shownNames()).toEqual(expected(tyres, 'tyre').slice(0, FIRST_ROWS))
    await waitFor(() => expect(shownNames()).toEqual(expected(tyres, 'tyre')))
    expect(drawn.slice(atSearch)).toEqual([FIRST_ROWS, FIRST_ROWS + 1])
  })

  it('aNewSearch_restartsAt24_withoutDrawingTheWholeListFirst', async () => {
    const drawn = show('grid')
    await waitFor(() => expect(shownNames()).toEqual(expected([])))

    const atSearch = drawn.length
    type('a')
    const full = expected([], 'a')
    expect(full.length).toBeGreaterThan(FIRST_ROWS)
    expect(shownNames()).toEqual(full.slice(0, FIRST_ROWS))
    await waitFor(() => expect(shownNames()).toEqual(full))
    // The keystroke drew 24 rows, then the rest: never the whole list first.
    expect(drawn.slice(atSearch)).toEqual([FIRST_ROWS, full.length])
  })

  it('createYourOwn_showsBelowTheList_onceItIsAllDrawn', async () => {
    show('grid', [], 'Create your own')
    expect(shownNames()).toHaveLength(FIRST_ROWS)
    expect(screen.queryByRole('button', { name: 'Create your own' })).toBeNull()
    expect(await screen.findByRole('button', { name: 'Create your own' })).toBeInTheDocument()
    expect(shownNames()).toEqual(expected([]))
    // With nothing found it is in the empty state, at once.
    type('zercher carry')
    expect(screen.getByRole('button', { name: 'Create your own' })).toBeInTheDocument()
  })

  it('moreThan24OfYourOwn_theFirstDrawHasNoAllExercisesHeadingWithNothingUnderIt', async () => {
    const tyres = Array.from({ length: 30 }, (_, i) => own(i + 1, `Tyre ${String(i + 1).padStart(2, '0')}`))
    show('grid', tyres)
    const headings = () => screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)

    // The 24 rows are all the lifter's own: the catalogue's heading waits for the catalogue.
    expect(shownNames()).toEqual(expected(tyres).slice(0, FIRST_ROWS))
    expect(headings()).toEqual(['Browse by muscle', 'Your exercises'])
    await waitFor(() => expect(shownNames()).toEqual(expected(tyres)))
    expect(headings()).toEqual(['Browse by muscle', 'Your exercises', 'All exercises'])
  })

  it('aListThatGrowsPast24_isDrawnWhole', async () => {
    // The search is short at first; then the lifter's own exercises arrive (the tab reads them after it opens).
    const tyres = Array.from({ length: 30 }, (_, i) => own(i + 1, `Tyre ${String(i + 1).padStart(2, '0')}`))
    function Harness({ exercises }: { exercises: readonly StoredExercise[] }) {
      const browser = useExerciseBrowser()
      return <ExerciseBrowser state={browser} muscleFilter="grid" exercises={exercises} recentIds={[]} onRow={() => {}} />
    }
    const { rerender } = render(<Harness exercises={tyres.slice(0, 2)} />)
    type('tyre')
    expect(shownNames()).toEqual(['Tyre 01', 'Tyre 02'])
    rerender(<Harness exercises={tyres} />)
    await waitFor(() => expect(shownNames()).toEqual(expected(tyres, 'tyre')))
  })
})
