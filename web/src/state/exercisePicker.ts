import { useMemo, useState } from 'react'
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

/**
 * Works the catalogue out once, in one task right after the app's first paint: the list and the full search the
 * picker and the Exercises tab open on. Cold, they cost about 30 ms (more on a phone), which the first open after a
 * launch would otherwise pay. The catalogue only, never figures: the tab's thumbnails are worked out when the app is
 * built, and a guide sheet works out its one figure when it opens. A failure is ignored: whoever needs the catalogue
 * works it out there and then, as without this. Not an idle callback: a fixed test clock fakes those and Safari has
 * none, and no tap lands within a few tens of ms of the first paint anyway.
 */
export function warmCatalogue(): void {
  requestAnimationFrame(() =>
    setTimeout(() => {
      try {
        library()
        grit.searchLibrary('', [], [])
      } catch {
        // Only a head start.
      }
    }, 0),
  )
}

let groups: readonly { id: string; label: string }[] | undefined

/** The muscle groups with at least one catalogue exercise, in the order the picker's grid shows them. */
export function muscleGroups(): readonly { id: string; label: string }[] {
  return (groups ??= grit.muscleGroups().map((id) => ({ id, label: grit.muscleGroupLabel(id) })))
}

/** How many exercises there are to browse: the catalogue, and the lifter's own that it does not have. */
export function exerciseCount(exercises: readonly StoredExercise[]): number {
  const names = new Set(library().map((e) => e.name.toLowerCase()))
  return names.size + exercises.filter((e) => !e.archived && !names.has(e.name.toLowerCase())).length
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
 * is searched and no body area is chosen. `muscles` are the groups ticked in the Muscles drop-down:
 * any of them, on top of the search and the other filters.
 */
export function pickerList(
  query: string,
  area: string | null,
  equipment: readonly string[],
  exercises: readonly StoredExercise[],
  recentIds: readonly number[],
  muscle: string | null = null,
  muscles: readonly string[] = [],
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
  // Searched like the catalogue: names starting with the query, then names containing it, then muscles.
  const searched = (own: readonly StoredExercise[]) =>
    own
      .flatMap((e) => {
        const name = normalise(e.name)
        const rank = name.startsWith(q) ? 0 : name.includes(q) ? 1 : e.muscles.some((m) => normalise(m).includes(q)) ? 2 : null
        return rank == null ? [] : [{ rank, e }]
      })
      .sort((a, b) => a.rank - b.rank || byLowerName(a.e, b.e))
      .map(({ e }) => ownEntry(e))
  const alphabetical = (entries: PickerEntry[]) => {
    const keyed = entries.map((entry) => ({ entry, key: normalise(entry.name) }))
    keyed.sort((a, b) => (a.key < b.key ? -1 : a.key > b.key ? 1 : a.entry.name < b.entry.name ? -1 : a.entry.name > b.entry.name ? 1 : 0))
    return keyed.map(({ entry }) => entry)
  }
  const works = (e: StoredExercise, group: string) => e.muscles.some((m) => grit.muscleGroupOf(m) === group)
  if (muscles.length > 0) {
    const inGroups = grit.exercisesByMuscles([...muscles], [...equipment]).filter((e) => area == null || e.area === area)
    // The lifter's own have no body area or equipment: they only show while neither is chosen.
    const theirs = equipment.length === 0 && area == null ? owned.filter((e) => muscles.some((group) => works(e, group))) : []
    if (q === '') return { yours: [], all: alphabetical([...inGroups.map(fromCatalogue), ...theirs.map(ownEntry)]) }
    // Ranked like any search, kept to the ticked muscles.
    const names = new Set(inGroups.map((e) => e.name))
    const found = grit.searchLibrary(query, area != null ? [area] : [], [...equipment]).filter((e) => names.has(e.name))
    return { yours: [], all: [...searched(theirs), ...found.map(fromCatalogue)] }
  }
  if (muscle != null && q === '' && area == null) {
    // One alphabetical list: the catalogue's exercises for the muscle, and the lifter's own that name it.
    const theirs = equipment.length === 0 ? owned.filter((e) => works(e, muscle)).map(ownEntry) : []
    return { yours: [], all: alphabetical([...grit.exercisesByMuscle(muscle, [...equipment]).map(fromCatalogue), ...theirs]) }
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
  return { yours: [], all: [...searched(owned), ...found] }
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

/** What an exercise browser is showing: the search, the filter chips, and the muscle or muscles chosen. */
export type ExerciseBrowserState = {
  query: string
  /** The one body area chosen, or null for all. */
  area: string | null
  /** Any of these; none means any equipment. */
  equipment: readonly string[]
  /** The muscle group opened from the picker's grid. */
  muscle: string | null
  /** The muscle groups ticked in the Exercises tab's drop-down. */
  muscles: readonly string[]
}

export type ExerciseBrowser = ExerciseBrowserState & {
  search: (text: string) => void
  /** Chooses the body area, or none for a second tap on the chosen one or for null. */
  chooseArea: (id: string | null) => void
  toggleEquipment: (id: string) => void
  browse: (muscle: string | null) => void
  toggleMuscle: (group: string) => void
  clearMuscles: () => void
}

const EMPTY_BROWSER: ExerciseBrowserState = { query: '', area: null, equipment: [], muscle: null, muscles: [] }

const toggled = (list: readonly string[], id: string) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id])

/**
 * The state of an exercise browser, kept by whoever must outlive it: the picker keeps its own for
 * one opening, the app's shell keeps the Exercises tab's while other tabs are on screen. It lives in
 * memory only.
 */
export function useExerciseBrowser(): ExerciseBrowser {
  const [state, setState] = useState(EMPTY_BROWSER)
  const actions = useMemo(
    () => ({
      // Searching is over everything, and a body area is its own way in: both leave the grid's muscle.
      search: (text: string) => setState((s) => ({ ...s, query: text, muscle: text !== '' ? null : s.muscle })),
      chooseArea: (id: string | null) => setState((s) => ({ ...s, area: id === s.area ? null : id, muscle: null })),
      toggleEquipment: (id: string) => setState((s) => ({ ...s, equipment: toggled(s.equipment, id) })),
      browse: (muscle: string | null) => setState((s) => ({ ...s, muscle })),
      toggleMuscle: (group: string) => setState((s) => ({ ...s, muscles: toggled(s.muscles, group) })),
      clearMuscles: () => setState((s) => ({ ...s, muscles: [] })),
    }),
    [],
  )
  return useMemo(() => ({ ...state, ...actions }), [state, actions])
}
