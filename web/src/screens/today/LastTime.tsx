import type { CSSProperties } from 'react'
import { setUnit } from '../../db/schema'
import { grit } from '../../domain'
import type { LastTime as LastTimeSets, PastSet } from '../../state/useToday'
import { formatKg, formatRpe } from '../../ui/components/format'
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

/** A load as its figure and the words around it: "102.5" + " kg", or "plate " + "7½" (the domain's plate wording). */
function loadParts(set: PastSet): { before: string; figure: string; after: string } {
  if (setUnit(set) !== 'PLATES') return { before: '', figure: formatKg(set.weightKg), after: ' kg' }
  const text = grit.plateText(set.weightKg)
  const cut = text.lastIndexOf(' ') + 1
  return { before: text.slice(0, cut), figure: text.slice(cut), after: '' }
}

/** A figure's width in digits: a decimal point takes about half of one. */
const figureWidth = (figure: string) => [...figure].reduce((sum, c) => sum + (c === '.' ? 0.5 : 1), 0)

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
  // The weights end on one line and the "@" signs start on one, whatever each row's digits: the weight's figure and
  // the reps each get the room of the longest one in the list (the digits are tabular, one `ch` each).
  const loads = last.sets.map((set) => loadParts(set))
  const columns = { '--last-load': `${Math.max(...loads.map((l) => figureWidth(l.figure)))}ch`, '--last-reps': `${Math.max(...last.sets.map((set) => String(set.reps).length))}ch` } as CSSProperties
  return (
    <section className="today-last" aria-label={`Last time, ${date}`}>
      <p className="today-last-head eyebrow" aria-hidden="true">
        {`Last time · ${date}`}
      </p>
      <ol role="list" className="today-last-sets" style={columns}>
        {last.sets.map((set, i) => (
          // Each row read once, as one sentence; what shows is the number beside its figures.
          <li key={i} className="today-last-set">
            <span className="sr-only">{pastSetLabel(i + 1, set)}</span>
            <span className="today-last-index title-small" aria-hidden="true">
              {i + 1}
            </span>
            <span className="today-last-line title-medium" aria-hidden="true">
              {/* The weight, the reps and the effort each stay whole; on a very narrow screen one goes under the other. */}
              <span className="today-last-part">
                {loads[i]!.before}
                <span className="today-last-load">{loads[i]!.figure}</span>
                {loads[i]!.after}
              </span>
              <span className="today-last-part">
                <span className="today-last-soft">× </span>
                <span className="today-last-reps">{set.reps}</span>
              </span>
              {set.rpe != null && (
                <span className="today-last-part today-last-effort">
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
