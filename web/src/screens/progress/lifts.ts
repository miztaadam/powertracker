import type { Lift } from '../../router'

export const LIFT_LABEL: Record<Lift, string> = { SQUAT: 'Squat', BENCH: 'Bench', DEADLIFT: 'Deadlift', STRICT_PRESS: 'Strict press' }

/** A lift keeps its colour on every chart. */
export const LIFT_COLOR: Record<Lift, string> = { SQUAT: 'var(--chart-squat)', BENCH: 'var(--chart-bench)', DEADLIFT: 'var(--chart-deadlift)', STRICT_PRESS: 'var(--chart-press)' }
