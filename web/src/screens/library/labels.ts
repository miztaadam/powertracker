import type { JsLibraryExercise } from '../../domain'

/** The catalogue's body areas, in the domain's order, as the picker's chips and the guide sheet name them. */
export const AREAS = [
  { id: 'MAIN_LIFTS', label: 'Main lifts' },
  { id: 'CHEST', label: 'Chest' },
  { id: 'BACK', label: 'Back' },
  { id: 'SHOULDERS', label: 'Shoulders' },
  { id: 'ARMS', label: 'Arms' },
  { id: 'QUADS', label: 'Quads' },
  { id: 'HAMSTRINGS_GLUTES', label: 'Hamstrings & glutes' },
  { id: 'CALVES', label: 'Calves' },
  { id: 'CORE', label: 'Core' },
] as const

/** The catalogue's equipment, in the domain's order. */
export const EQUIPMENT = [
  { id: 'BARBELL', label: 'Barbell' },
  { id: 'DUMBBELL', label: 'Dumbbell' },
  { id: 'KETTLEBELL', label: 'Kettlebell' },
  { id: 'CABLE', label: 'Cable' },
  { id: 'MACHINE', label: 'Machine' },
  { id: 'BODYWEIGHT', label: 'Bodyweight' },
  { id: 'BAND', label: 'Band' },
  { id: 'SPECIALTY_BAR', label: 'Specialty bar' },
  { id: 'SMITH', label: 'Smith machine' },
] as const

const labelOf = (list: readonly { id: string; label: string }[], id: string) => list.find((x) => x.id === id)?.label ?? id

export const areaLabel = (id: string) => labelOf(AREAS, id)
export const equipmentLabel = (id: string) => labelOf(EQUIPMENT, id)

/** "Main lifts · Barbell", "Quads · Dumbbell or barbell". */
export function whereAndWith(entry: JsLibraryExercise): string {
  const equipment = entry.equipment.map((e, i) => (i === 0 ? equipmentLabel(e) : equipmentLabel(e).toLowerCase())).join(' or ')
  return `${areaLabel(entry.area)} · ${equipment}`
}
