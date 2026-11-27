import { cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { GritDb } from '../../db/db'
import { TODAY, exerciseNamed, todayFixture } from '../../test/todayFixture'
import { PlanScreen } from './PlanScreen'

// Supersets on the Plan tab (1.24): "Superset with the one above" in a row's menu, the pair's tags, and "Remove superset".
let fx: ReturnType<typeof todayFixture>
let db: GritDb
let ids: number[]

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
  // Monday: squat, bench, row, curl, in that order.
  ids = []
  for (const [order, name] of ['Squat', 'Bench', 'Row', 'Curl'].entries()) {
    ids.push((await db.planItems.add({ weekday: 1, exerciseId: (await exerciseNamed(db, name)).id, sets: 3, reps: 5, targetRpe: 8, order })) as number)
  }
})
afterEach(async () => {
  cleanup()
  await fx.dispose()
})

const show = () => render(<PlanScreen db={db} today={() => TODAY} onOpenSettings={() => {}} />)
const monday = () => screen.findByRole('region', { name: 'Monday' })
const rows = (section: HTMLElement) => within(section).getAllByRole('listitem')
const labels = (section: HTMLElement) => rows(section).map((row) => row.getAttribute('aria-label'))
const tags = (section: HTMLElement) => rows(section).map((row) => row.querySelector('.plan-row-superset')?.textContent ?? null)
const linked = async () => ((await db.planItems.toArray()) as { id: number; order: number; supersetWithAbove?: true }[]).sort((a, b) => a.order - b.order).filter((p) => p.supersetWithAbove === true).map((p) => p.id)

/** Opens the ⋮ menu of the row at `index` (from 0) and returns its items' names. */
async function menuOf(section: HTMLElement, index: number): Promise<string[]> {
  await userEvent.click(within(rows(section)[index]!).getByRole('button', { name: 'More' }))
  return screen.getAllByRole('menuitem').map((item) => item.textContent ?? '')
}
const pick = (name: string) => userEvent.click(screen.getByRole('menuitem', { name }))
const closeMenu = () => userEvent.keyboard('{Escape}')

describe('supersets on the Plan tab', () => {
  it('theMenuOffersTheLink_onlyWhereThereIsOneAbove', async () => {
    show()
    const day = await monday()
    await waitFor(() => expect(rows(day)).toHaveLength(4))
    // The first row of the day has none above it.
    expect(await menuOf(day, 0)).toEqual(['Edit', 'Delete'])
    await closeMenu()
    expect(await menuOf(day, 1)).toEqual(['Edit', 'Superset with the one above', 'Delete'])
  })

  it('linking_tagsBothRows_namesThePartner_andStoresTheLinkOnTheSecond', async () => {
    show()
    const day = await monday()
    await waitFor(() => expect(rows(day)).toHaveLength(4))
    await menuOf(day, 1)
    await pick('Superset with the one above')

    await waitFor(() => expect(tags(day)).toEqual(['Superset A', 'Superset B', null, null]))
    expect(labels(day)).toEqual(['Squat  3 × 5 @ 8, superset with Bench', 'Bench  3 × 5 @ 8, superset with Squat', 'Row  3 × 5 @ 8', 'Curl  3 × 5 @ 8'])
    expect(rows(day)[1]).toHaveClass('plan-row-second')
    expect(await linked()).toEqual([ids[1]])
    // Pairs only: the row under the pair cannot join it; the last one can still pair with the row above it.
    expect(await menuOf(day, 2)).toEqual(['Edit', 'Delete'])
    await closeMenu()
    expect(await menuOf(day, 3)).toEqual(['Edit', 'Superset with the one above', 'Delete'])
    await pick('Superset with the one above')
    await waitFor(() => expect(tags(day)).toEqual(['Superset A', 'Superset B', 'Superset A', 'Superset B']))
    expect(await linked()).toEqual([ids[1], ids[3]])
  })

  it('removeSuperset_isOnBothRowsOfThePair_andTakesTheLinkOff', async () => {
    await db.planItems.update(ids[1]!, { supersetWithAbove: true })
    show()
    const day = await monday()
    await waitFor(() => expect(tags(day)).toEqual(['Superset A', 'Superset B', null, null]))
    // From the second row.
    expect(await menuOf(day, 1)).toEqual(['Edit', 'Remove superset', 'Delete'])
    await pick('Remove superset')
    await waitFor(() => expect(tags(day)).toEqual([null, null, null, null]))
    expect(await linked()).toEqual([])
    expect('supersetWithAbove' in (await db.planItems.get(ids[1]!))!).toBe(false)

    // And from the first row: the link is on the row under it.
    await db.planItems.update(ids[1]!, { supersetWithAbove: true })
    await waitFor(() => expect(tags(day)).toEqual(['Superset A', 'Superset B', null, null]))
    expect(await menuOf(day, 0)).toEqual(['Edit', 'Remove superset', 'Delete'])
    await pick('Remove superset')
    await waitFor(() => expect(tags(day)).toEqual([null, null, null, null]))
    expect(await linked()).toEqual([])
  })

  it('movingOrDeletingOneOfThePair_unlinksIt', async () => {
    await db.planItems.update(ids[1]!, { supersetWithAbove: true })
    await db.planItems.update(ids[3]!, { supersetWithAbove: true })
    show()
    const day = await monday()
    await waitFor(() => expect(tags(day)).toEqual(['Superset A', 'Superset B', 'Superset A', 'Superset B']))
    // The bench moved up over the squat: that pair is gone, the other stands.
    await userEvent.click(within(rows(day)[1]!).getByRole('button', { name: 'Move up' }))
    await waitFor(() => expect(labels(day).slice(0, 2)).toEqual(['Bench  3 × 5 @ 8', 'Squat  3 × 5 @ 8']))
    expect(tags(day)).toEqual([null, null, 'Superset A', 'Superset B'])
    // The row deleted: the curl under it is alone again.
    await menuOf(day, 2)
    await pick('Delete')
    await waitFor(() => expect(rows(day)).toHaveLength(3))
    expect(tags(day)).toEqual([null, null, null])
    expect(await linked()).toEqual([])
  })
})
