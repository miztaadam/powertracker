import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { grit } from '../domain'
import { TODAY, startSession, todayFixture } from '../test/todayFixture'
import { decodeBackup, encodeBackup, exportBackup, restoreBackup } from '../backup/backup'
import { addPlanItem, addSet, resolveCatalogue, updatePlanItem } from './catalogue'
import { openGritDb, type GritDb } from './db'
import type { ExerciseRow } from './schema'

let fx: ReturnType<typeof todayFixture>
let db: GritDb

beforeEach(async () => {
  fx = todayFixture()
  db = await fx.open()
})
afterEach(async () => {
  await fx.dispose()
})

const rows = async (name: string) => (await db.exercises.toArray()).filter((e) => e.name.toLowerCase() === name.toLowerCase())

/** Takes a seeded exercise out, so its catalogue entry has no row yet: the state of a library exercise never used. */
async function unseed(name: string) {
  for (const row of await rows(name)) await db.exercises.delete(row.id!)
  expect(await rows(name)).toEqual([])
}

/** Makes every insert into a table fail, the way a full phone or a broken row would. */
function failInsertsInto(table: 'planItems' | 'sets') {
  db[table].hook('creating', () => {
    throw new Error('forced failure')
  })
}

const planItem = (exerciseId: number | null = null) => ({ weekday: 1, exerciseId, sets: 3, reps: 10, targetRpe: 8 })
const set = (exerciseId: number | null = null) => ({ exerciseId, weightKg: 20, reps: 10, rpe: 8, failed: false, missPoint: null, isTopSet: false })

describe('resolveCatalogue', () => {
  it('firstAdd_insertsOneBuiltInRow', async () => {
    await unseed('Curl')
    const before = await db.exercises.count()

    const id = await resolveCatalogue(db, 'Curl')

    const entry = grit.findInLibrary('Curl')!
    expect(await db.exercises.count()).toBe(before + 1)
    expect(await rows('Curl')).toEqual([{ id, name: 'Curl', category: entry.category, parentLift: entry.parentLift ?? null, muscles: [...entry.muscles], isBuiltIn: true, archived: false }])
  })

  it('secondAdd_reusesIt', async () => {
    await unseed('Curl')

    const first = await resolveCatalogue(db, 'Curl')
    const second = await resolveCatalogue(db, 'Curl')

    expect(second).toBe(first)
    expect(await rows('Curl')).toHaveLength(1)
  })

  it('seededBuiltIn_isReused', async () => {
    const seeded = await db.exercises.toArray()
    const squat = seeded.find((e) => e.name === 'Squat')!

    expect(await resolveCatalogue(db, 'Squat')).toBe(squat.id)
    // Ignoring case, and by alias: the catalogue's own spelling is not needed to find the row.
    expect(await resolveCatalogue(db, 'squat')).toBe(squat.id)
    expect(await resolveCatalogue(db, 'Back squat')).toBe(squat.id)
    expect(await db.exercises.toArray()).toEqual(seeded)
  })

  it('archivedSameName_isRestored', async () => {
    const [curl] = await rows('Curl')
    await db.exercises.update(curl!.id!, { archived: true })
    expect((await rows('Curl'))[0]!.archived).toBe(true)

    expect(await resolveCatalogue(db, 'Curl')).toBe(curl!.id)

    expect(await rows('Curl')).toEqual([{ ...curl, archived: false }])
  })

  it('addCatalogue_reusesSameNamedCustomRow', async () => {
    // The lifter made their own "hip thrust" before the catalogue had one.
    await unseed('Hip thrust')
    const mine: ExerciseRow = { name: 'hip thrust', category: 'ACCESSORY', parentLift: null, muscles: ['Glutes'], isBuiltIn: false, archived: false }
    const own = (await db.exercises.add(mine)) as number

    const itemId = await addPlanItem(db, planItem(), 'Hip thrust')

    // No duplicate: the plan item points at the lifter's own row, which stays theirs.
    expect(await rows('Hip thrust')).toEqual([{ ...mine, id: own }])
    expect((await db.planItems.get(itemId))!.exerciseId).toBe(own)
    // The form guide follows the name, so the info button shows the catalogue guide for it.
    expect(grit.formGuide('hip thrust')).not.toBeNull()
    expect(grit.formGuide('hip thrust')).toEqual(grit.findInLibrary('Hip thrust')!.guide)
  })

  it('insertAndPlanItem_areOneTransaction', async () => {
    await unseed('Curl')
    failInsertsInto('planItems')
    await expect(addPlanItem(db, planItem(), 'Curl')).rejects.toThrow()
    // No exercise row is left behind.
    expect(await rows('Curl')).toEqual([])
    expect(await db.planItems.count()).toBe(0)

    // The same for a set logged on Today.
    const sessionId = await startSession(db, TODAY)
    failInsertsInto('sets')
    await expect(addSet(db, { ...set(), sessionId }, 'Curl')).rejects.toThrow()
    expect(await rows('Curl')).toEqual([])
    expect(await db.sets.count()).toBe(0)
  })

  it('archivedRowIsRestored_onlyWhenThePlanItemIsSaved', async () => {
    const [curl] = await rows('Curl')
    await db.exercises.update(curl!.id!, { archived: true })
    failInsertsInto('planItems')
    await expect(addPlanItem(db, planItem(), 'Curl')).rejects.toThrow()
    // The restore is rolled back with the plan item.
    expect(await rows('Curl')).toEqual([{ ...curl, archived: true }])
  })

  it('unknownName_isRefused_andNothingIsWritten', async () => {
    const before = await db.exercises.toArray()
    await expect(resolveCatalogue(db, 'Tyre flip')).rejects.toThrow('Tyre flip is not in the exercise catalogue')
    await expect(addPlanItem(db, planItem(), 'Tyre flip')).rejects.toThrow()
    expect(await db.exercises.toArray()).toEqual(before)
    expect(await db.planItems.count()).toBe(0)
  })

  it('addPlanItem_goesAfterTheDaysLast_andASetKeepsItsExercise', async () => {
    const squat = (await rows('Squat'))[0]!.id!
    const first = await addPlanItem(db, planItem(squat), null)
    await unseed('Curl')
    const second = await addPlanItem(db, planItem(), 'Curl')
    const curl = (await rows('Curl'))[0]!.id!
    expect((await db.planItems.bulkGet([first, second])).map((p) => [p!.exerciseId, p!.order])).toEqual([
      [squat, 0],
      [curl, 1],
    ])

    // Changing an item's exercise to a catalogue one resolves it the same way.
    await unseed('Row')
    await updatePlanItem(db, first, { exerciseId: null, sets: 4, reps: 8, targetRpe: 7 }, 'Row')
    const row = (await rows('Row'))[0]!
    expect(row.isBuiltIn).toBe(true)
    expect(await db.planItems.get(first)).toEqual({ id: first, weekday: 1, exerciseId: row.id, sets: 4, reps: 8, targetRpe: 7, order: 0 })

    const sessionId = await startSession(db, TODAY)
    const setId = await addSet(db, { ...set(squat), sessionId }, null)
    expect((await db.sets.get(setId))!.exerciseId).toBe(squat)
  })
})

describe('backup', () => {
  // Spec §5: a backup made after adding catalogue exercises restores on this version, and holds only ordinary rows.
  it('backupAfterCatalogueAdd_roundTrips', async () => {
    const name = 'Goblet squat'
    await unseed(name)
    await addPlanItem(db, planItem(), name)
    const sessionId = await startSession(db, TODAY)
    await addSet(db, { ...set(), sessionId }, name)

    const exported = await exportBackup(db, 1)
    const text = encodeBackup(exported)
    const row = exported.exercises.find((e) => e.name === name)!
    expect(row.isBuiltIn).toBe(true)
    expect(row.muscles).toEqual([...grit.findInLibrary(name)!.muscles])
    expect(exported.planItems.map((p) => p.exerciseId)).toEqual([row.id])
    expect(exported.sets.map((s) => s.exerciseId)).toEqual([row.id])
    // Every exercise is an ordinary row: the same fields as before the catalogue, and no catalogue data.
    const ordinary = ['archived', 'category', 'id', 'isBuiltIn', 'muscles', 'name', 'parentLift']
    for (const e of (JSON.parse(text) as { exercises: Record<string, unknown>[] }).exercises) expect(Object.keys(e).sort()).toEqual(ordinary)

    // Restored into an empty app on this version, it comes back the same.
    const decoded = decodeBackup(text)
    if (!decoded.ok) throw new Error(`decode failed: ${JSON.stringify(decoded)}`)
    const freshName = `${fx.name}-restored`
    const fresh = await openGritDb(freshName)
    try {
      await restoreBackup(fresh, decoded.file)
      expect(await exportBackup(fresh, 1)).toEqual(exported)
      const restored = (await fresh.exercises.toArray()).filter((e) => e.name === name)
      expect(restored).toEqual([expect.objectContaining({ id: row.id, isBuiltIn: true, archived: false })])
      expect((await fresh.planItems.toArray()).map((p) => p.exerciseId)).toEqual([row.id])
    } finally {
      fresh.close()
      await Dexie.delete(freshName)
    }
  })
})
