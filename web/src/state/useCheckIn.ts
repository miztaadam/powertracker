import { useCallback, useEffect, useRef, useState } from 'react'
import { planFor } from '../db/catalogue'
import { inGroupOrder, putCheckIn } from '../db/checkIns'
import { getSettings, loadHistory, type GritDb } from '../db/db'
import { adviceFromJs, adviceToJs, planItemToJs } from '../db/mapping'
import type { AdviceRow, TrainingMode } from '../db/schema'
import { grit, JsCheckIn } from '../domain'

/** The check-in's steps: the lagging body parts, the ones growing well, then what saving gave. */
export type CheckInStep = 'lagging' | 'growing' | 'saved'

/** Why the coach gave a lagging body part no more work (the domain's `NoteReason`). */
export type NoteReason = 'AT_CEILING' | 'NO_EXERCISE' | 'FULL' | 'LATER' | 'WOULD_PASS_CEILING'

/** What saving did for one lagging body part: more work suggested on Coach, or a note saying why not. */
export type SavedLine = { group: string } & ({ kind: 'advice' } | { kind: 'note'; reason: NoteReason; weeklySets: number })

export type CheckInState = {
  loading: boolean
  /** The side the app is on; the check-in belongs to the bodybuilding side only. */
  side: TrainingMode
  step: CheckInStep
  /** `MuscleGroup` names, in the domain's order. */
  lagging: string[]
  growingWell: string[]
  /** After saving: one line per lagging body part, in the domain's order. */
  saved: SavedLine[]
  /** After saving: whether any body part got more work, so Coach has a card to see. */
  hasAdvice: boolean
}

export type CheckInActions = {
  toggleLagging: (group: string) => void
  /** A body part ticked lagging cannot be growing well: it is left as it is. */
  toggleGrowing: (group: string) => void
  next: () => void
  back: () => void
  /** Stores the check-in and what it gives, in one transaction; rejects (changing nothing) if a write fails. */
  save: () => Promise<void>
}

const toggled = (groups: readonly string[], group: string) => inGroupOrder(groups.includes(group) ? groups.filter((g) => g !== group) : [...groups, group])

const withoutId = ({ id: _id, ...row }: AdviceRow & { id: number }): AdviceRow => row

/**
 * Saves a check-in dated `today` (the real clock) and what the coach makes of it, all at once: the check-in (replacing
 * one of the same date), every open MORE_WORK advice dismissed, and the new advice. The plan is not touched: a
 * suggestion changes it only when the lifter applies it on Coach.
 */
async function storeCheckIn(db: GritDb, today: string, lagging: string[], growingWell: string[]): Promise<SavedLine[]> {
  return db.transaction('rw', [db.checkIns, db.advice, db.sessions, db.sets, db.exercises, db.baselines, db.planItems], async () => {
    const [history, plan, advice] = await Promise.all([loadHistory(db), planFor(db, 'BODYBUILDING'), db.advice.toArray()])
    const prior = advice.map((a) => adviceToJs(a as AdviceRow & { id: number }))
    const outcome = grit.physiqueAfterCheckIn(new JsCheckIn(today, lagging, growingWell), history, plan.map(planItemToJs), today, prior)
    await putCheckIn(db, today, lagging, growingWell)
    for (const dismissed of outcome.dismissed) await db.advice.update(dismissed.id, { status: dismissed.status })
    if (outcome.advice.length > 0) await db.advice.bulkAdd(outcome.advice.map((a) => withoutId(adviceFromJs(a))))
    const advised = new Set(outcome.advice.map((a) => grit.advicePayload(a.payloadJson)?.group))
    const notes = new Map(outcome.notes.map((n) => [n.group, n]))
    return lagging.flatMap((group): SavedLine[] => {
      if (advised.has(group)) return [{ group, kind: 'advice' }]
      const note = notes.get(group)
      return note ? [{ group, kind: 'note', reason: note.reason as NoteReason, weeklySets: note.weeklySets }] : []
    })
  })
}

/**
 * The check-in screen's state (bodybuilding mode stage 3). Every body part starts on track; nothing is stored until
 * `save`, so leaving at any step changes nothing.
 */
export function useCheckIn(db: GritDb, today: () => string): CheckInState & CheckInActions {
  const clock = useRef(today)
  clock.current = today
  const [side, setSide] = useState<TrainingMode | null>(null)
  const [step, setStep] = useState<CheckInStep>('lagging')
  const [lagging, setLagging] = useState<string[]>([])
  const [growingWell, setGrowingWell] = useState<string[]>([])
  const [saved, setSaved] = useState<SavedLine[]>([])
  const saving = useRef(false)

  useEffect(() => {
    let live = true
    void getSettings(db).then((s) => live && setSide(s.trainingMode))
    return () => {
      live = false
    }
  }, [db])

  const toggleLagging = useCallback((group: string) => {
    setLagging((now) => toggled(now, group))
    // Lagging and growing well never overlap.
    setGrowingWell((now) => now.filter((g) => g !== group))
  }, [])
  const toggleGrowing = useCallback(
    (group: string) => {
      if (lagging.includes(group)) return
      setGrowingWell((now) => toggled(now, group))
    },
    [lagging],
  )
  const next = useCallback(() => setStep((s) => (s === 'lagging' ? 'growing' : s)), [])
  const back = useCallback(() => setStep((s) => (s === 'growing' ? 'lagging' : s)), [])
  const save = useCallback(async () => {
    if (saving.current) return
    saving.current = true
    try {
      const lines = await storeCheckIn(db, clock.current(), lagging, growingWell)
      setSaved(lines)
      setStep('saved')
    } finally {
      saving.current = false
    }
  }, [db, lagging, growingWell])

  return {
    loading: side == null,
    side: side ?? 'POWERLIFTING',
    step,
    lagging,
    growingWell,
    saved,
    hasAdvice: saved.some((l) => l.kind === 'advice'),
    toggleLagging,
    toggleGrowing,
    next,
    back,
    save,
  }
}
