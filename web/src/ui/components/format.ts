/** Formats 8 as "8" and 7.5 as "7.5". */
export function formatRpe(rpe: number): string {
  return String(rpe)
}

/** Formats 155 as "155" and 152.5 as "152.5". */
export function formatKg(kg: number): string {
  return String(kg)
}

/** Chart figures to one decimal at most: 187.5, 92.4, 250. */
export function formatChartValue(value: number): string {
  return String(Math.round(value * 10) / 10)
}

/** Joins class names, skipping the empty ones. */
export function cx(...names: (string | false | null | undefined)[]): string {
  return names.filter(Boolean).join(' ')
}
