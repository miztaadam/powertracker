import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import Dexie from 'dexie'
import { grit, JsExercise, JsPlanChange, JsSet } from '../domain'
import { JsAddedPlanItem } from '../domain/generated/powertracker-domain.mjs'
import { TODAY, startSession, todayFixture } from '../test/todayFixture'
import { decodeBackup, encodeBackup, exportBackup, restoreBackup } from '../backup/backup'
import { addPlanItem, addSet, applyPlanChange, planFor, replacePlan, resolveCatalogue, setPlateCount, updatePlanItem } from './catalogue'
import { loadHistory, openGritDb, type GritDb } from './db'
import { exerciseFromJs, exerciseToJs, planItemToJs, setFromJs, setToJs } from './mapping'
import { plateCountOf, setUnit, type ExerciseRow, type PlanItemRow } from './schema'

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

    const itemId = await addPlanItem(db, planItem(), 'Hip thrust', 'POWERLIFTING')

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
    await expect(addPlanItem(db, planItem(), 'Curl', 'POWERLIFTING')).rejects.toThrow()
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
    await expect(addPlanItem(db, planItem(), 'Curl', 'POWERLIFTING')).rejects.toThrow()
    // The restore is rolled back with the plan item.
    expect(await rows('Curl')).toEqual([{ ...curl, archived: true }])
  })

  it('unknownName_isRefused_andNothingIsWritten', async () => {
    const before = await db.exercises.toArray()
    await expect(resolveCatalogue(db, 'Tyre flip')).rejects.toThrow('Tyre flip is not in the exercise catalogue')
    await expect(addPlanItem(db, planItem(), 'Tyre flip', 'POWERLIFTING')).rejects.toThrow()
    expect(await db.exercises.toArray()).toEqual(before)
    expect(await db.planItems.count()).toBe(0)
  })

  it('addPlanItem_goesAfterTheDaysLast_andASetKeepsItsExercise', async () => {
    const squat = (await rows('Squat'))[0]!.id!
    const first = await addPlanItem(db, planItem(squat), null, 'POWERLIFTING')
    await unseed('Curl')
    const second = await addPlanItem(db, planItem(), 'Curl', 'POWERLIFTING')
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
    expect(await db.planItems.get(first)).toEqual({ id: first, weekday: 1, exerciseId: row.id, sets: 4, reps: 8, targetRpe: 7, order: 0, mode: 'POWERLIFTING' })

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
    await addPlanItem(db, planItem(), name, 'POWERLIFTING')
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

describe('replacePlan', () => {
  const range = (weekday: number, exerciseId: number | null = null, sets = 3) => ({ weekday, exerciseId, sets, reps: 8, repsHigh: 12, targetRpe: 8 })
  const side = async (mode: 'POWERLIFTING' | 'BODYBUILDING') =>
    (await planFor(db, mode)).sort((a, b) => a.id - b.id)
  const names = async (mode: 'POWERLIFTING' | 'BODYBUILDING') => {
    const byId = new Map((await db.exercises.toArray()).map((e) => [e.id, e.name]))
    return (await planFor(db, mode)).sort((a, b) => a.id - b.id).map((p) => `${p.weekday} ${p.order} ${byId.get(p.exerciseId)} ${p.sets}`)
  }

  it('replacesOnlyThatSidesRows_inOrder_resolvingCatalogueNames', async () => {
    await unseed('Leg press')
    const curl = (await rows('Curl'))[0]!.id!
    const own = (await db.exercises.add({ name: 'My cable thing', category: 'ACCESSORY', parentLift: null, muscles: [], isBuiltIn: false, archived: false })) as number
    await addPlanItem(db, planItem(curl), null, 'POWERLIFTING')
    await addPlanItem(db, { ...range(3, curl) }, null, 'BODYBUILDING')
    await addPlanItem(db, { ...range(5, curl) }, null, 'BODYBUILDING')
    const powerlifting = await side('POWERLIFTING')

    await replacePlan(db, 'BODYBUILDING', [
      { item: range(1, null, 4), catalogueName: 'Leg press' },
      { item: range(1), catalogueName: 'Curl' },
      { item: range(4, own), catalogueName: null },
      { item: range(1), catalogueName: 'DB press' },
    ])

    expect(await names('BODYBUILDING')).toEqual(['1 0 Leg press 4', '1 1 Curl 3', '4 0 My cable thing 3', '1 2 DB press 3'])
    expect((await planFor(db, 'BODYBUILDING')).every((p) => p.mode === 'BODYBUILDING' && p.reps === 8 && p.repsHigh === 12 && p.targetRpe === 8)).toBe(true)
    // The catalogue exercise with no row yet got one, as any plan item would.
    expect(await rows('Leg press')).toEqual([expect.objectContaining({ isBuiltIn: true, archived: false })])
    expect(await side('POWERLIFTING')).toEqual(powerlifting)
  })

  it('aFailurePartWay_leavesTheOldPlan', async () => {
    const curl = (await rows('Curl'))[0]!.id!
    await addPlanItem(db, { ...range(3, curl) }, null, 'BODYBUILDING')
    await addPlanItem(db, planItem(curl), null, 'POWERLIFTING')
    const before = await db.planItems.toArray()
    await unseed('Leg press')
    const exercisesUnseeded = await db.exercises.count()

    // The second name is not in the catalogue: nothing of the new plan is kept, the old one is.
    await expect(replacePlan(db, 'BODYBUILDING', [{ item: range(1), catalogueName: 'Leg press' }, { item: range(1), catalogueName: 'Not an exercise at all' }])).rejects.toThrow()

    expect(await db.planItems.toArray()).toEqual(before)
    // Leg press got no row: the insert was undone with the rest.
    expect(await db.exercises.count()).toBe(exercisesUnseeded)
  })

  it('anEmptyList_clearsOnlyThatSide', async () => {
    const curl = (await rows('Curl'))[0]!.id!
    await addPlanItem(db, { ...range(3, curl) }, null, 'BODYBUILDING')
    await addPlanItem(db, planItem(curl), null, 'POWERLIFTING')
    await replacePlan(db, 'BODYBUILDING', [])
    expect(await planFor(db, 'BODYBUILDING')).toEqual([])
    expect(await planFor(db, 'POWERLIFTING')).toHaveLength(1)
  })
})

/** 1.17: an exercise's plate setting is stored on its row, and only when it is not off. */
describe('setPlateCount', () => {
  const advice = { type: 'TARGET', lift: null, createdAt: TODAY, reason: 'Beat it.', severity: 1, status: 'OPEN', mode: 'BODYBUILDING' as const }
  const everythingButExercises = async () => ({
    sessions: await db.sessions.toArray(),
    sets: await db.sets.toArray(),
    planItems: await db.planItems.toArray(),
    advice: await db.advice.toArray(),
    baselines: await db.baselines.toArray(),
  })

  it('setPlateCount on a stored row changes only that row', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    const before = await db.exercises.orderBy('id').toArray()

    expect(await setPlateCount(db, lat, null, 'WHOLE')).toBe(lat)

    const after = await db.exercises.orderBy('id').toArray()
    expect(after.find((e) => e.id === lat)).toStrictEqual({ ...before.find((e) => e.id === lat), plateCount: 'WHOLE' })
    expect(after.filter((e) => e.id !== lat)).toStrictEqual(before.filter((e) => e.id !== lat))
    await setPlateCount(db, lat, null, 'HALF')
    expect(plateCountOf((await db.exercises.get(lat))!)).toBe('HALF')
  })

  it('setPlateCount back to OFF leaves the row exactly as it was before plates', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    const before = (await db.exercises.get(lat))!
    expect('plateCount' in before).toBe(false)

    await setPlateCount(db, lat, null, 'HALF')
    await setPlateCount(db, lat, null, 'OFF')

    const after = (await db.exercises.get(lat))!
    expect(after).toStrictEqual(before)
    expect(Object.keys(after)).not.toContain('plateCount')
    expect(plateCountOf(after)).toBe('OFF')
    // OFF on a row that was never on writes nothing either.
    await setPlateCount(db, lat, null, 'OFF')
    expect(await db.exercises.get(lat)).toStrictEqual(before)
  })

  it('a kg set is stored with no unit key and a plate set with unit PLATES', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    const sessionId = await startSession(db, TODAY)
    const kgId = await addSet(db, setFromJs(new JsSet(lat, 60, 10, 8, false, null, true), sessionId), null)
    const plateId = await addSet(db, setFromJs(new JsSet(lat, 7.5, 10, 8, false, null, false, 'PLATES'), sessionId), null)

    const kg = (await db.sets.get(kgId))!
    const plate = (await db.sets.get(plateId))!
    expect('unit' in kg).toBe(false)
    expect(kg).toStrictEqual({ id: kgId, sessionId, exerciseId: lat, weightKg: 60, reps: 10, rpe: 8, failed: false, missPoint: null, isTopSet: true })
    expect(plate).toStrictEqual({ id: plateId, sessionId, exerciseId: lat, weightKg: 7.5, reps: 10, rpe: 8, failed: false, missPoint: null, isTopSet: false, unit: 'PLATES' })
    expect([setUnit(kg), setUnit(plate)]).toEqual(['KG', 'PLATES'])
    // And back to the domain each in its own unit.
    expect([setToJs(kg).unit, setToJs(plate).unit]).toEqual(['KG', 'PLATES'])
    const history = await loadHistory(db)
    expect(history.sessions.flatMap((s) => s.sets.map((x) => [x.weightKg, x.unit]))).toEqual([[60, 'KG'], [7.5, 'PLATES']])
  })

  it('an exercise row maps its setting both ways, and an off one carries no key', () => {
    const row = { id: 3, name: 'Lat pulldown', category: 'ACCESSORY', parentLift: null, muscles: ['Lats'], isBuiltIn: true, archived: false }
    expect(exerciseToJs(row).plateCount).toBe('OFF')
    expect(exerciseToJs({ ...row, plateCount: 'HALF' }).plateCount).toBe('HALF')
    expect(exerciseFromJs(exerciseToJs(row))).toStrictEqual(row)
    expect(exerciseFromJs(exerciseToJs({ ...row, plateCount: 'WHOLE' }))).toStrictEqual({ ...row, plateCount: 'WHOLE' })
    expect(() => exerciseFromJs(new JsExercise(3, 'Lat pulldown', 'ACCESSORY', null, [], true, false, 'LOTS'))).toThrow()
    expect(() => setFromJs(new JsSet(3, 7, 10, 8, false, null, false, 'STONES'), 1)).toThrow()
  })

  it('setPlateCount with a catalogue name and no row inserts the row once, with the setting', async () => {
    await unseed('Lat pulldown')
    const count = await db.exercises.count()

    const id = await setPlateCount(db, null, 'Lat pulldown', 'HALF')
    const again = await setPlateCount(db, null, 'Lat pulldown', 'HALF')

    expect(again).toBe(id)
    expect(await db.exercises.count()).toBe(count + 1)
    const entry = grit.findInLibrary('Lat pulldown')!
    expect(await rows('Lat pulldown')).toStrictEqual([
      { id, name: 'Lat pulldown', category: entry.category, parentLift: entry.parentLift ?? null, muscles: [...entry.muscles], isBuiltIn: true, archived: false, plateCount: 'HALF' },
    ])
  })

  it('setPlateCount with a catalogue name, when the write fails, leaves no row behind', async () => {
    await unseed('Lat pulldown')
    db.exercises.hook('updating', () => {
      throw new Error('forced failure')
    })
    await expect(setPlateCount(db, null, 'Lat pulldown', 'WHOLE')).rejects.toThrow()
    expect(await rows('Lat pulldown')).toEqual([])
  })

  it('no set and no advice row changes', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    const sessionId = await startSession(db, TODAY)
    await addSet(db, { ...set(lat), sessionId, weightKg: 60 }, null)
    await addSet(db, { ...set(lat), sessionId, weightKg: 7, unit: 'PLATES' }, null)
    await db.advice.add({ ...advice, payloadJson: `{"type":"com.powertracker.domain.model.AdvicePayload.Target","exerciseId":${lat},"weightKg":60.0,"reps":10,"rpe":8.0,"sets":3}` })
    await addPlanItem(db, planItem(lat), null, 'BODYBUILDING')
    const before = await everythingButExercises()

    await setPlateCount(db, lat, null, 'WHOLE')
    expect(await everythingButExercises()).toStrictEqual(before)
    await setPlateCount(db, lat, null, 'OFF')
    expect(await everythingButExercises()).toStrictEqual(before)
  })

  it('a restored archived row keeps its setting', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    await setPlateCount(db, lat, null, 'WHOLE')
    await db.exercises.update(lat, { archived: true })

    expect(await resolveCatalogue(db, 'Lat pulldown')).toBe(lat)
    expect(await db.exercises.get(lat)).toMatchObject({ archived: false, plateCount: 'WHOLE' })
    // Choosing a setting through the name restores it as well, and keeps the row.
    await db.exercises.update(lat, { archived: true })
    expect(await setPlateCount(db, null, 'Lat pulldown', 'HALF')).toBe(lat)
    expect(await db.exercises.get(lat)).toMatchObject({ archived: false, plateCount: 'HALF' })
  })

  it('an exercise swapped in by a plan change starts off', async () => {
    const lat = await resolveCatalogue(db, 'Lat pulldown')
    await setPlateCount(db, lat, null, 'WHOLE')
    await unseed('Seated cable row')
    const itemId = await addPlanItem(db, planItem(lat), null, 'BODYBUILDING')
    const item = planItemToJs((await db.planItems.get(itemId))! as PlanItemRow & { id: number })

    await applyPlanChange(db, new JsPlanChange([], [item], [new JsAddedPlanItem(item, 'Seated cable row')], 'Swapped', true))

    const [row] = await rows('Seated cable row')
    expect(row).toBeDefined()
    expect('plateCount' in row!).toBe(false)
    expect(plateCountOf(row!)).toBe('OFF')
    // The setting stays with the old exercise, ready if the lifter swaps back.
    expect(plateCountOf((await db.exercises.get(lat))!)).toBe('WHOLE')
  })
})
