import { setUnit } from '../../db/schema'
import type { LastTime as LastTimeSets } from '../../state/useToday'
import { SecondaryButton } from '../../ui/components/Button'
import { formatLoad } from '../../ui/components/format'
import { PastSets, type PastSet } from '../../ui/components/PastSets'
import { formatSessionDate } from '../progress/dates'

/** "Wed 18 Nov"; "Wed 18 Nov 2025" when it is not the year of `today` (both `YYYY-MM-DD`). */
export function lastTimeDate(date: string, today: string): string {
  const full = formatSessionDate(date)
  return date.slice(0, 4) === today.slice(0, 4) ? full.slice(0, -5) : full
}

export type LastTimeProps = {
  last: LastTimeSets | null
  /** The day shown, `YYYY-MM-DD`. */
  today: string
  /**
   * 1.21: the index, from 0, of the set about to be logged (how many are logged today), while the card takes sets. The
   * row with that index is marked as the one to match; past the last row, or once the day's sets are done, none is.
   */
  next?: number
  /**
   * 1.21: fills the weight and reps fields with the marked row's, logging nothing. Left out, there is no button: the
   * card leaves it out when that set was logged in another unit than the exercise counts in now.
   */
  onUse?: (set: PastSet) => void
}

/** "Use set 2: 25 kg × 9"; "Use set 1: plate 7½ × 12". */
export function useSetText(number: number, set: PastSet): string {
  return `Use set ${number}: ${formatLoad(set.weightKg, setUnit(set))} × ${set.reps}`
}

/**
 * What the lifter did on this exercise last time (1.18), between today's sets and the weight entry on both sides:
 * every set of its last session on this side, in the order logged and each in the unit it was logged in, under the
 * session's date. Read-only and quieter than today's rows. Nothing at all, taking no room, for an exercise never
 * done on this side. The coach's "Last time: …" names the same session (on the bodybuilding side through the same
 * domain rule, `lastTime`), with one exception: just after an exercise's plate setting changed the coach reads only the
 * sets in the new unit, so it can name an earlier day, while this shows what was really done last.
 *
 * 1.21: while the card takes sets, the row for the set about to be logged is marked, and "Use set N: …" under the
 * rows puts that row's weight and reps in the fields (the RPE is left as it is; nothing is logged).
 */
export function LastTime({ last, today, next, onUse }: LastTimeProps) {
  if (last == null || last.sets.length === 0) return null
  const date = lastTimeDate(last.date, today)
  const match = next != null ? last.sets[next] : undefined
  return (
    <section className="today-last" aria-label={`Last time, ${date}`}>
      <p className="today-last-head eyebrow" aria-hidden="true">
        {`Last time · ${date}`}
      </p>
      <PastSets sets={last.sets} next={match ? next : undefined} />
      {match && onUse && (
        <div className="today-last-use">
          <SecondaryButton text={useSetText(next! + 1, match)} onClick={() => onUse(match)} compact />
        </div>
      )}
    </section>
  )
}
