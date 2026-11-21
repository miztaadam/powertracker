import type { LoadUnit } from '../../db/schema'
import { grit } from '../../domain'

/** Formats 8 as "8" and 7.5 as "7.5". */
export function formatRpe(rpe: number): string {
  return String(rpe)
}

/** Formats 155 as "155" and 152.5 as "152.5": two decimals at most, so float noise (152.50000000000003) never shows. */
export function formatKg(kg: number): string {
  return String(Math.round(kg * 100) / 100)
}

/**
 * A weight with its unit (1.17): "155 kg", or a plate number on a plate-counted machine, "plate 7½". `formatKg`
 * stays kg-only; the plate wording is the domain's, the same on both apps and in the coach's reasons.
 */
export function formatLoad(value: number, unit: LoadUnit): string {
  return unit === 'PLATES' ? grit.plateText(value) : `${formatKg(value)} kg`
}

/** Chart figures to one decimal at most: 187.5, 92.4, 250. */
export function formatChartValue(value: number): string {
  return String(Math.round(value * 10) / 10)
}

/** Joins class names, skipping the empty ones. */
export function cx(...names: (string | false | null | undefined)[]): string {
  return names.filter(Boolean).join(' ')
}
