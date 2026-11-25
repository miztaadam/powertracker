import type { LastTime as LastTimeSets } from '../../state/useToday'
import { PastSets } from '../../ui/components/PastSets'
import { formatSessionDate } from '../progress/dates'

/** "Wed 18 Nov"; "Wed 18 Nov 2025" when it is not the year of `today` (both `YYYY-MM-DD`). */
export function lastTimeDate(date: string, today: string): string {
  const full = formatSessionDate(date)
  return date.slice(0, 4) === today.slice(0, 4) ? full.slice(0, -5) : full
}

/**
 * What the lifter did on this exercise last time (1.18), between today's sets and the weight entry on both sides:
 * every set of its last session on this side, in the order logged and each in the unit it was logged in, under the
 * session's date. Read-only and quieter than today's rows. Nothing at all, taking no room, for an exercise never
 * done on this side. The coach's "Last time: …" names the same session (on the bodybuilding side through the same
 * domain rule, `lastTime`), with one exception: just after an exercise's plate setting changed the coach reads only the
 * sets in the new unit, so it can name an earlier day, while this shows what was really done last.
 */
export function LastTime({ last, today }: { last: LastTimeSets | null; today: string }) {
  if (last == null || last.sets.length === 0) return null
  const date = lastTimeDate(last.date, today)
  return (
    <section className="today-last" aria-label={`Last time, ${date}`}>
      <p className="today-last-head eyebrow" aria-hidden="true">
        {`Last time · ${date}`}
      </p>
      <PastSets sets={last.sets} />
    </section>
  )
}
