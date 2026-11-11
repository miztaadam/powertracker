import type { GritDb } from '../db/db'
import { grit, type JsLibraryExercise } from '../domain'
import type { Lift } from '../router'

/** What the picker needs of a stored exercise: a table row or a domain exercise. */
export type StoredExercise = { id: number; name: string; category: string; muscles: readonly string[]; archived: boolean }

export type Kind = 'COMPOUND' | 'ACCESSORY'

/** One row of the picker: a catalogue exercise, or one of the lifter's own that the catalogue does not have. */
export type PickerEntry = {
  name: string
  muscles: readonly string[]
  /** Decides the sets × reps it joins a plan with: 3 × 5 for a compound lift, 3 × 10 for an accessory. */
  kind: Kind
  /** The catalogue entry, to be found, restored or inserted by its name on saving; null for the lifter's own exercise. */
  library: JsLibraryExercise | null
  /** The stored row to use: for a catalogue entry, the one with its name if there is one yet. */
  exerciseId: number | null
}

export const DEFAULT_SETS = 3
export const defaultReps = (entry: PickerEntry) => (entry.kind === 'COMPOUND' ? 5 : 10)

/** The picker's rows. `yours` is only filled while nothing is searched or filtered; no exercise is in both. */
export type PickerList = { yours: PickerEntry[]; all: PickerEntry[] }

/** What the lifter types to make an exercise of their own. */
export type CustomExercise = { name: string; category: 'VARIATION' | 'ACCESSORY'; parentLift: Lift | null; muscles: string[] }

/** How many recently used exercises lead "Your exercises". */
const RECENT_LIMIT = 6

let catalogue: readonly JsLibraryExercise[] | undefined

/** The whole catalogue, in the domain's order. It never changes while the app runs. */
export function library(): readonly JsLibraryExercise[] {
  return (catalogue ??= grit.library())
}

let groups: readonly { id: string; label: string }[] | undefined

/** The muscle groups with at least one catalogue exercise, in the order the picker's grid shows them. */
export function muscleGroups(): readonly { id: string; label: string }[] {
  return (groups ??= grit.muscleGroups().map((id) => ({ id, label: grit.muscleGroupLabel(id) })))
}

/** A name as the search compares it: lower case, no accents, `-` and `_` as spaces, single spaces. */
export function normalise(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[-_]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Exercise ids from the newest session back, each once. */
export function recentExerciseIds(sessions: readonly { id: number; date: string; sets: readonly { exerciseId: number }[] }[]): number[] {
  const newestFirst = [...sessions].sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
  return [...new Set(newestFirst.flatMap((s) => s.sets.map((set) => set.exerciseId)))]
}

/** The picker entry for a catalogue exercise, tied to the lifter's row `exerciseId` when there is one. */
export const catalogueEntry = (e: JsLibraryExercise, exerciseId: number | null): PickerEntry => ({ name: e.name, muscles: e.muscles, kind: e.kind as Kind, library: e, exerciseId })

/** The picker entry for an exercise of the lifter's own. */
export const ownEntry = (e: Pick<StoredExercise, 'id' | 'name' | 'category' | 'muscles'>): PickerEntry => ({
  name: e.name,
  muscles: e.muscles,
  kind: e.category === 'ACCESSORY' ? 'ACCESSORY' : 'COMPOUND',
  library: null,
  exerciseId: e.id,
})

function uniqueById(entries: PickerEntry[]): PickerEntry[] {
  const seen = new Set<number | null>()
  return entries.filter((e) => !seen.has(e.exerciseId) && seen.add(e.exerciseId))
}

/**
 * What the picker lists for a search and filters. The catalogue is browsed as it is; each entry is
 * tied to the lifter's row of the same name (ignoring case) when there is one, so an exercise that
 * was seeded, restored from a backup or made by hand under a catalogue name shows once. The lifter's
 * other exercises have no body area or equipment, so they only show while no filter chip is on.
 *
 * `muscle` is the muscle group being browsed (a `MuscleGroup` name). It only counts while nothing
 * is searched and no body area is chosen.
 */
export function pickerList(
  query: string,
  area: string | null,
  equipment: readonly string[],
  exercises: readonly StoredExercise[],
  recentIds: readonly number[],
  muscle: string | null = null,
): PickerList {
  const live = exercises.filter((e) => !e.archived).sort((a, b) => a.id - b.id)
  // Oldest first, like resolveCatalogue: with two rows of one name, the one shown is the one saved to.
  const rowByName = new Map<string, StoredExercise>()
  for (const e of live) if (!rowByName.has(e.name.toLowerCase())) rowByName.set(e.name.toLowerCase(), e)
  const byName = new Map(library().map((e) => [e.name.toLowerCase(), e]))

  const fromCatalogue = (e: JsLibraryExercise) => catalogueEntry(e, rowByName.get(e.name.toLowerCase())?.id ?? null)
  const owned = live.filter((e) => !byName.has(e.name.toLowerCase()))
  const byLowerName = (a: StoredExercise, b: StoredExercise) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())
  const q = normalise(query)
  if (muscle != null && q === '' && area == null) {
    // One alphabetical list: the catalogue's exercises for the muscle, and the lifter's own that name it.
    const theirs = equipment.length === 0 ? owned.filter((e) => e.muscles.some((m) => grit.muscleGroupOf(m) === muscle)).map(ownEntry) : []
    const all = [...grit.exercisesByMuscle(muscle, [...equipment]).map(fromCatalogue), ...theirs]
    const keyed = all.map((entry) => ({ entry, key: normalise(entry.name) }))
    keyed.sort((a, b) => (a.key < b.key ? -1 : a.key > b.key ? 1 : a.entry.name < b.entry.name ? -1 : a.entry.name > b.entry.name ? 1 : 0))
    return { yours: [], all: keyed.map(({ entry }) => entry) }
  }
  const found = grit.searchLibrary(query, area != null ? [area] : [], [...equipment]).map(fromCatalogue)
  if (q === '' && area == null && equipment.length === 0) {
    const byId = new Map(live.map((e) => [e.id, e]))
    const recent = recentIds.flatMap((id) => {
      const e = byId.get(id)
      if (!e) return []
      const entry = byName.get(e.name.toLowerCase())
      return [entry ? fromCatalogue(entry) : ownEntry(e)]
    })
    const yours = uniqueById([...uniqueById(recent).slice(0, RECENT_LIMIT), ...[...owned].sort(byLowerName).map(ownEntry)])
    const shown = new Set(yours.flatMap((e) => (e.library ? [e.library.name] : [])))
    return { yours, all: found.filter((e) => !shown.has(e.name)) }
  }
  if (area != null || equipment.length > 0) return { yours: [], all: found }
  // Searched like the catalogue: names starting with the query, then names containing it, then muscles.
  const mine = owned
    .flatMap((e) => {
      const name = normalise(e.name)
      const rank = name.startsWith(q) ? 0 : name.includes(q) ? 1 : e.muscles.some((m) => normalise(m).includes(q)) ? 2 : null
      return rank == null ? [] : [{ rank, e }]
    })
    .sort((a, b) => a.rank - b.rank || byLowerName(a.e, b.e))
    .map(({ e }) => ownEntry(e))
  return { yours: [], all: [...mine, ...found] }
}

/**
 * Makes an exercise of the lifter's own and returns it as a picker entry. A name the catalogue
 * knows (a name or an alias, ignoring case) is not made again: the catalogue's entry comes back
 * instead, tied to its row if it has one, and gets its row like any other catalogue exercise.
 */
export async function createCustomExercise(db: GritDb, { name, category, parentLift, muscles }: CustomExercise): Promise<PickerEntry> {
  const trimmed = name.trim()
  const known = grit.findInLibrary(trimmed)
  if (known) {
    const wanted = known.name.toLowerCase()
    const row = (await db.exercises.toArray()).find((e) => !e.archived && e.name.toLowerCase() === wanted)
    return catalogueEntry(known, row?.id ?? null)
  }
  const row = { name: trimmed, category, parentLift: category === 'VARIATION' ? parentLift : null, muscles, isBuiltIn: false, archived: false }
  const id = (await db.exercises.add(row)) as number
  return ownEntry({ ...row, id })
}
