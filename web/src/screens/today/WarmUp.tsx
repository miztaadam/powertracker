import { grit } from '../../domain'
import { warmUpKey, type WarmUpSet } from '../../state/warmUp'
import { formatKg, formatLoad } from '../../ui/components/format'
import { platesLabel } from '../../ui/components/PlateStrip'

/** How to load a barbell warm-up set: "15 per side", "25 + 2.5 per side", "empty bar". Null where there is no bar. */
export function warmUpHint(set: WarmUpSet): string | null {
  if (set.perSide == null) return null
  return set.perSide.length === 0 ? 'empty bar' : `${platesLabel(set.perSide)} per side`
}

/** "50 kg × 12"; "plate 3½ × 12". */
export function warmUpText(set: WarmUpSet): string {
  return `${formatLoad(set.weight, set.unit)} × ${set.reps}`
}

/** "Warm-up set 1: 50 kg × 12, 15 per side"; "Warm-up set 1: plate 3 and a half × 12". */
export function warmUpLabel(number: number, set: WarmUpSet): string {
  const load = set.unit === 'PLATES' ? grit.plateSpoken(set.weight) : `${formatKg(set.weight)} kg`
  const hint = warmUpHint(set)
  return `Warm-up set ${number}: ${load} × ${set.reps}${hint != null ? `, ${hint}` : ''}`
}

export type WarmUpProps = {
  sets: readonly WarmUpSet[]
  /** The `warmUpKey` of each set ticked. */
  done: readonly string[]
  onTick: (key: string, done: boolean) => void
}

/**
 * The warm-up checklist (1.23), above the weight entry until the exercise's first set is logged: one or two lighter
 * sets to do first, each with a box to tick. A tick is a note to the lifter and nothing more: no set is logged, so
 * nothing reaches History, the charts or the coach, and the ticks are forgotten when the app is closed. The sets follow
 * the weight in the field, as the plates do. Nothing at all, taking no room, when there is nothing to suggest.
 */
export function WarmUp({ sets, done, onTick }: WarmUpProps) {
  if (sets.length === 0) return null
  return (
    <section className="today-warm" aria-label="Warm-up">
      <p className="today-warm-head eyebrow" aria-hidden="true">
        Warm-up · not logged
      </p>
      <ul className="today-warm-sets">
        {sets.map((set, i) => {
          const key = warmUpKey(set)
          const ticked = done.includes(key)
          const hint = warmUpHint(set)
          return (
            <li key={key}>
              {/* The whole row is the box's target. */}
              <label className={ticked ? 'today-warm-set today-warm-done' : 'today-warm-set'}>
                <input type="checkbox" checked={ticked} aria-label={warmUpLabel(i + 1, set)} onChange={(event) => onTick(key, event.target.checked)} />
                <span className="today-warm-line body-large" aria-hidden="true">
                  <span className="today-warm-load">{warmUpText(set)}</span>
                  {hint != null && <span className="today-warm-hint body-medium">{hint}</span>}
                </span>
              </label>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
