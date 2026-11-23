import type { LoadUnit, PlateCount } from '../../db/schema'
import { grit } from '../../domain'
import { formatKg } from '../../ui/components/format'

export const MAX_KG = 1000
export const MAX_REPS = 99
export const MIN_RPE = 6
export const MAX_RPE = 10

/**
 * What a typed number is for: a weight in kilograms, the reps in a set, or its RPE; or, on a plate-counted machine
 * (1.17), a plate number in whole plates or in half steps. Both plate kinds read and write the editor's `weightKg`.
 */
export type EntryKind = 'kg' | 'reps' | 'rpe' | 'plate' | 'plateHalf'

/** `step` is a plate number off the setting's grid (a half with whole plates, or a quarter). */
export type EntryResult = { ok: true; value: number } | { ok: false; reason: 'empty' | 'nan' | 'range' | 'step' }

// Digits with at most one decimal mark, a point or a comma. No sign and no exponent.
const NUMBER = /^(\d+[.,]?\d*|[.,]\d+)$/

// "1,000": a comma with exactly three digits after it is a thousands separator, not a decimal mark.
const THOUSANDS = /^\d*,\d{3}$/

/** A plate number judged by the domain's plate stack: on its grid, and from plate 1 to the highest plate. */
function plate(n: number, count: PlateCount): EntryResult {
  switch (grit.plateCheck(n, count)) {
    case 'OK':
      return { ok: true, value: n }
    case 'STEP':
      return { ok: false, reason: 'step' }
    default:
      return { ok: false, reason: 'range' }
  }
}

/**
 * Reads what was typed into a number field. A comma counts as the decimal point. Weight is 0–1000 kg
 * rounded to two decimals, reps a whole number 1–99, RPE 6–10 snapped to the nearest half. A plate is on its
 * grid (whole, or whole and half) and from plate 1 to the highest plate.
 */
export function parseEntry(text: string, kind: EntryKind): EntryResult {
  const typed = text.trim()
  if (typed === '') return { ok: false, reason: 'empty' }
  if (!NUMBER.test(typed) || THOUSANDS.test(typed)) return { ok: false, reason: 'nan' }
  const n = Number(typed.replace(',', '.'))
  const range = { ok: false, reason: 'range' } as const
  switch (kind) {
    case 'kg':
      return n >= 0 && n <= MAX_KG ? { ok: true, value: Math.round(n * 100) / 100 } : range
    case 'reps':
      return Number.isInteger(n) && n >= 1 && n <= MAX_REPS ? { ok: true, value: n } : range
    case 'rpe':
      return n >= MIN_RPE && n <= MAX_RPE ? { ok: true, value: Math.round(n * 2) / 2 } : range
    case 'plate':
      return plate(n, 'WHOLE')
    case 'plateHalf':
      return plate(n, 'HALF')
  }
}

/** The field a weight is typed into: kg, or in plates the plate kind for the setting's grid. */
export function weightKind(unit: LoadUnit, count: PlateCount): EntryKind {
  if (unit !== 'PLATES') return 'kg'
  return count === 'HALF' ? 'plateHalf' : 'plate'
}

const PLATE_RANGE = `Plate 1 to ${formatKg(grit.maxPlate())}`

const RANGE_MESSAGE: Record<EntryKind, string> = {
  kg: `0 to ${MAX_KG} kg`,
  reps: `Whole, 1 to ${MAX_REPS}`,
  rpe: `${MIN_RPE} to ${MAX_RPE}`,
  plate: PLATE_RANGE,
  plateHalf: PLATE_RANGE,
}

/** The short message under a refused entry; an empty field is not an error. */
export function entryMessage(kind: EntryKind, reason: 'empty' | 'nan' | 'range' | 'step'): string | undefined {
  if (reason === 'empty') return undefined
  if (reason === 'nan') return 'Numbers only'
  // Only a plate is ever off its grid.
  if (reason === 'step') return kind === 'plateHalf' ? 'Whole or half plates' : 'Whole plates only'
  return RANGE_MESSAGE[kind]
}
