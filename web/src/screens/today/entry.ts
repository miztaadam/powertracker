export const MAX_KG = 1000
export const MAX_REPS = 99
export const MIN_RPE = 6
export const MAX_RPE = 10

/** What a typed number is for: a weight in kilograms, the reps in a set, or its RPE. */
export type EntryKind = 'kg' | 'reps' | 'rpe'

export type EntryResult = { ok: true; value: number } | { ok: false; reason: 'empty' | 'nan' | 'range' }

/**
 * Reads what was typed into a number field. A comma counts as the decimal point. Weight is 0–1000 kg
 * rounded to two decimals, reps a whole number 1–99, RPE 6–10 snapped to the nearest half.
 */
export function parseEntry(text: string, kind: EntryKind): EntryResult {
  const typed = text.trim().replace(',', '.')
  if (typed === '') return { ok: false, reason: 'empty' }
  if (!/^-?(\d+\.?\d*|\.\d+)$/.test(typed)) return { ok: false, reason: 'nan' }
  const n = Number(typed)
  const range = { ok: false, reason: 'range' } as const
  switch (kind) {
    case 'kg':
      return n >= 0 && n <= MAX_KG ? { ok: true, value: Math.round(n * 100) / 100 } : range
    case 'reps':
      return Number.isInteger(n) && n >= 1 && n <= MAX_REPS ? { ok: true, value: n } : range
    case 'rpe':
      return n >= MIN_RPE && n <= MAX_RPE ? { ok: true, value: Math.round(n * 2) / 2 } : range
  }
}

const RANGE_MESSAGE: Record<EntryKind, string> = {
  kg: `0 to ${MAX_KG} kg`,
  reps: `Whole, 1 to ${MAX_REPS}`,
  rpe: `${MIN_RPE} to ${MAX_RPE}`,
}

/** The short message under a refused entry; an empty field is not an error. */
export function entryMessage(kind: EntryKind, reason: 'empty' | 'nan' | 'range'): string | undefined {
  return reason === 'empty' ? undefined : reason === 'nan' ? 'Numbers only' : RANGE_MESSAGE[kind]
}
