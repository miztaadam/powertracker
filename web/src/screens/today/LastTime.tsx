import { setUnit } from '../../db/schema'
import { grit } from '../../domain'
import type { LastTime as LastTimeSets, PastSet } from '../../state/useToday'
import { formatKg, formatLoad, formatRpe } from '../../ui/components/format'
import { formatSessionDate } from '../progress/dates'

/** "Wed 18 Nov"; "Wed 18 Nov 2025" when it is not the year of `today` (both `YYYY-MM-DD`). */
export function lastTimeDate(date: string, today: string): string {
  const full = formatSessionDate(date)
  return date.slice(0, 4) === today.slice(0, 4) ? full.slice(0, -5) : full
}

/** "Set 1: 25 kg × 10 at RPE 8"; "Set 2: plate 7 and a half × 9, missed". */
export function pastSetLabel(number: number, set: PastSet): string {
  let s = `Set ${number}: ${setUnit(set) === 'PLATES' ? grit.plateSpoken(set.weightKg) : `${formatKg(set.weightKg)} kg`} × ${set.reps}`
  if (set.rpe != null) s += ` at RPE ${formatRpe(set.rpe)}`
  return set.failed ? `${s}, missed` : s
}

/**
 * What the lifter did on this exercise last time (1.18), between today's sets and the weight entry on both sides:
 * every set of its last session on this side, in the order logged and each in the unit it was logged in, under the
 * session's date. Read-only and quieter than today's rows. Nothing at all, taking no room, for an exercise never
 * done on this side. The coach's "Last time: …" is the same session (the domain's `lastTime`).
 */
export function LastTime({ last, today }: { last: LastTimeSets | null; today: string }) {
  if (last == null || last.sets.length === 0) return null
  const date = lastTimeDate(last.date, today)
  return (
    <section className="today-last" aria-label={`Last time, ${date}`}>
      <p className="today-last-head eyebrow" aria-hidden="true">
        {`Last time · ${date}`}
      </p>
      <ol role="list" className="today-last-sets">
        {last.sets.map((set, i) => (
          // Each row read once, as one sentence; what shows is the number beside its figures.
          <li key={i} className="today-last-set">
            <span className="sr-only">{pastSetLabel(i + 1, set)}</span>
            <span className="today-last-index title-small" aria-hidden="true">
              {i + 1}
            </span>
            <span className="today-last-line title-medium" aria-hidden="true">
              <span className="today-last-load">
                {formatLoad(set.weightKg, setUnit(set))}
                <span className="today-last-soft"> × </span>
                {set.reps}
              </span>
              {set.rpe != null && (
                <span className="today-last-effort">
                  <span className="today-last-soft">@ </span>
                  {formatRpe(set.rpe)}
                </span>
              )}
              {set.failed && <span className="today-last-missed label-medium">Missed</span>}
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
}
