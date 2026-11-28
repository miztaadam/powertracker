import { useState } from 'react'
import type { FinishSummary, TodayItem } from '../../state/useToday'
import { PrimaryButton, QuietButton, SecondaryButton } from '../../ui/components/Button'
import { Card } from '../../ui/components/Card'
import { formatKg } from '../../ui/components/format'
import { BoltIcon, CheckIcon, PlayArrowIcon } from '../../ui/components/icons'
import { Eyebrow } from '../../ui/components/NumberDisplay'
import { ChoiceChip } from '../../ui/components/SegmentedChoice'
import { StatTile } from '../../ui/components/StatTile'
import { planText, targetHeadline } from './ExerciseCard'
import { NumberEntryDialog } from './TodayDialogs'
import { SkipMenu } from './SkipToday'
import './today.css'

const MAX_BODYWEIGHT_KG = 400
const READINESS = [1, 2, 3, 4, 5]

export type StartSessionCardProps = {
  items: TodayItem[]
  /** "Wednesday". */
  dayName: string
  onStart: (bodyweightKg: number | null, readiness: number | null) => void
  /** Opens the reason sheet for a planned row (1.13); left out, there are no row menus. */
  onSkipToday?: (item: TodayItem) => void
  /** How many of today's planned exercises are skipped (1.13). With none left on the list, the eyebrow counts them. */
  skipped?: number
  /** Opens the plate sheet for a row whose exercise is offered it (`plateOffered`, 1.17); left out, no row offers it. */
  onCountPlates?: (item: TodayItem) => void
}

/** Before the first set: what is planned today, two optional check-in numbers, and Start. */
export function StartSessionCard({ items, dayName, onStart, onSkipToday, skipped = 0, onCountPlates }: StartSessionCardProps) {
  // Everything planned today was skipped: it is not a day with nothing planned. Anything on the list, an exercise the
  // lifter added included, is left to do.
  const allSkipped = items.length === 0 && skipped > 0
  const [bodyweight, setBodyweight] = useState<number | null>(null)
  const [readiness, setReadiness] = useState<number | null>(null)
  const [typingBodyweight, setTypingBodyweight] = useState(false)

  return (
    <Card>
      <div>
        <Eyebrow
          text={
            allSkipped
              ? `${dayName} · ${skipped} skipped`
              : items.length === 0
                ? 'Nothing planned'
                : `${dayName} · ${items.length} ${items.length === 1 ? 'exercise' : 'exercises'}`
          }
        />
        <h2 className="headline-medium">Ready to train?</h2>
      </div>

      {/* With everything skipped there is no list and no "nothing in your plan": the skipped line follows the card. */}
      {items.length === 0 ? (
        !allSkipped && <p className="today-secondary body-medium">There is nothing in your plan for {dayName}. You can still start a session and add exercises as you go.</p>
      ) : (
        <ul className="today-plan">
          {items.map((item) => (
            <li key={item.exercise.id} className="today-plan-line">
              {/* The plan goes under the name when both do not fit beside the menu: the name is never cut short. */}
              <div className="today-grow today-pair">
                <div className="today-pair-first">
                  <p className="today-name title-medium">
                    {item.exercise.name}
                  </p>
                  {item.suggestion && (
                    <p className="today-plan-suggestion body-small">
                      <BoltIcon size={14} />
                      <span className="today-ellipsis">Coach suggests {targetHeadline(item.suggestion.target)}</span>
                    </p>
                  )}
                </div>
                {item.planned && <span className="today-pair-second today-nowrap headline-small">{planText(item.planned)}</span>}
              </div>
              <SkipMenu
                name={item.exercise.name}
                onSkipToday={onSkipToday && item.planned && item.sets.length === 0 ? () => onSkipToday(item) : undefined}
                onCountPlates={onCountPlates && item.plateOffered ? () => onCountPlates(item) : undefined}
              />
            </li>
          ))}
        </ul>
      )}

      <hr className="today-hairline" />

      <div className="today-checkin">
        <div className="today-grow">
          <p className="title-small">Bodyweight</p>
          <p className="today-muted body-small">Optional</p>
        </div>
        <SecondaryButton text={bodyweight != null ? `${formatKg(bodyweight)} kg` : 'Add'} onClick={() => setTypingBodyweight(true)} compact />
      </div>
      <div className="today-readiness">
        <div>
          <p className="title-small">Readiness</p>
          <p className="today-muted body-small">Optional · 1 is wrecked, 5 is great</p>
        </div>
        <div className="today-readiness-chips" role="group" aria-label="Readiness">
          {READINESS.map((level) => (
            <ChoiceChip key={level} className="today-grow" text={String(level)} selected={readiness === level} onClick={() => setReadiness(readiness === level ? null : level)} />
          ))}
        </div>
      </div>

      <PrimaryButton className="today-start" text="Start session" onClick={() => onStart(bodyweight, readiness)} icon={<PlayArrowIcon />} />

      {typingBodyweight && (
        <NumberEntryDialog
          title="Bodyweight"
          label="Kilograms"
          unit="kg"
          initial={bodyweight}
          min={20}
          max={MAX_BODYWEIGHT_KG}
          whole={false}
          onDismiss={() => setTypingBodyweight(false)}
          onSet={(kg) => {
            setBodyweight(kg)
            setTypingBodyweight(false)
          }}
        />
      )}
    </Card>
  )
}

export type SummaryCardProps = { summary: FinishSummary; onOpenCoach: () => void; onKeepLogging: () => void }

/** After Finish: what was logged and whether the coach has something new to say. */
export function SummaryCard({ summary, onOpenCoach, onKeepLogging }: SummaryCardProps) {
  const notes =
    (summary.newNotes === 0 ? 'No new coach notes' : summary.newNotes === 1 ? '1 new coach note' : `${summary.newNotes} new coach notes`) +
    (summary.skipped != null && summary.skipped >= 1 ? `, ${summary.skipped} skipped` : '')
  const formatVolume = (kg: number) => `${Math.round(kg).toLocaleString()} kg`
  return (
    <Card>
      <div className="today-summary-head">
        <span className="today-summary-tick">
          <CheckIcon />
        </span>
        <div>
          <h2 className="headline-medium">Session saved</h2>
          <p className="today-secondary body-medium">{notes}</p>
        </div>
      </div>
      <div className="today-summary-stats today-summary-grid">
        <StatTile className="today-grow" label="Sets" value={String(summary.setCount)} />
        <StatTile className="today-grow" label="Exercises" value={String(summary.exerciseCount)} />
        {summary.tonnageKg != null && summary.tonnageKg > 0 && (
          <StatTile className="today-grow" label="Volume" value={formatVolume(summary.tonnageKg)} />
        )}
        {summary.durationMin != null && summary.durationMin > 0 && (
          <StatTile className="today-grow" label="Duration" value={`${summary.durationMin} min`} />
        )}
        {summary.prCount != null && summary.prCount > 0 && (
          <StatTile className="today-grow" label={summary.prCount === 1 ? 'New PR' : 'New PRs'} value={String(summary.prCount)} note="All-time best" noteTone="success" />
        )}
      </div>
      {summary.newNotes > 0 ? (
        <PrimaryButton text="See coach notes" onClick={onOpenCoach} icon={<BoltIcon />} />
      ) : (
        <SecondaryButton text="Open Coach" onClick={onOpenCoach} icon={<BoltIcon />} />
      )}
      <div className="today-center">
        <QuietButton text="Keep logging" onClick={onKeepLogging} />
      </div>
    </Card>
  )
}
