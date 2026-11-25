import type { CSSProperties } from 'react'
import { setUnit, type LoadUnit } from '../../db/schema'
import { grit } from '../../domain'
import { formatKg, formatRpe } from './format'
import './PastSets.css'

/** One set of a past session. `unit` is there only for plates. */
export type PastSet = { weightKg: number; reps: number; rpe: number | null; failed: boolean; unit?: LoadUnit }

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
 * The sets of one exercise in an earlier session, in the order given, numbered from 1: weight × reps, "@ RPE" when
 * there is one, "Missed" on a missed set, each in the unit it was logged in. Read-only. Each row is read once by a
 * screen reader, as one sentence.
 */
export function PastSets({ sets }: { sets: readonly PastSet[] }) {
  // The weights end on one line and the "@" signs start on one, whatever each row's digits: the weight's figure and
  // the reps each get the room of the longest one in the list (the digits are tabular, one `ch` each).
  const loads = sets.map((set) => loadParts(set))
  const columns = { '--last-load': `${Math.max(0, ...loads.map((l) => figureWidth(l.figure)))}ch`, '--last-reps': `${Math.max(0, ...sets.map((set) => String(set.reps).length))}ch` } as CSSProperties
  return (
    <ol role="list" className="today-last-sets" style={columns}>
      {sets.map((set, i) => (
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
  )
}
